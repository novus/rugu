package com.novus.rugu

import com.jcraft.jsch._
import scala.util.control.Exception.allCatch

case class Host(server: String, port: Int = 22)

object Ssh {
  
  def apply(host: Host, auth: Authentication, knownHostsFile: Option[String] = None) = {
    val jsch = new JSch()
    knownHostsFile.foreach(jsch.setKnownHosts)
    
    val factoryBase =
      (_:Unit) => jsch.getSession(auth.username, host.server, host.port)
    
    val factory = auth match {
      case UsernameAndPassword(u, p) =>
        factoryBase andThen (s => { s.setPassword(p); s })
      case PrivateKeyFile(u, keyFile, keyPass) =>
        keyPass match {
          case Some(p) => jsch.addIdentity(keyFile, p)
          case None => jsch.addIdentity(keyFile)
        }
        factoryBase
    }
    new SshSession(factory)
  }
}

class SshSession(factory: SessionFactory) {
  def apply[I, O](c: Command[I, O])(implicit sp: StreamProcessor[I]) =
    exec(c)(_._2).fold(Left(_), Right(_))
  
  def exec[I, O, OO](c: Command[I, O])(f: ((Int, O)) => OO)(implicit sp: StreamProcessor[I]) =
    remote(c, sp).fold(Left(_), r => Right(f(r)))
  
  /* Get a new, authorized session and prepare a channel for invoking
   * shell commands. Yield the active session, (executed) channel, and a
   * closure to clean them up.
   */
  def prepareExec(command: String) = {
    val s = factory(())
    s.connect() // FIXME boom
    val c = s.openChannel("exec").asInstanceOf[ChannelExec]
    c.setCommand(command)
    c.setInputStream(null)
    c.setErrStream(System.err)
    c.connect() // FIXME boom
    (s, c, () => { c.disconnect(); s.disconnect() })
  }
  
  def remote[I, O](c: Command[I, O], sp: StreamProcessor[I]): Either[Throwable, (Int, O)] = {
    val (session, channel, close) = prepareExec(c.command)
    allCatch.andFinally(close()).either {
      val processed = sp(channel.getInputStream())
      while(! channel.isClosed())
        Thread.`yield`()
      close()
      if(channel.getExitStatus == 0) channel.getExitStatus -> c(processed)
      else error("Non-zero exit status! " + channel.getExitStatus)
    }
  } 
}

object IO {
  type Resource = { def close(): Unit }
  def apply[A, R <: Resource](r: R)(op: R => A): Either[Throwable, A] =
    allCatch.andFinally(r.close()).either(op(r))
}
