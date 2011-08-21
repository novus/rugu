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
  
  def apply[I, O](c: Command[I, O]) =
    remote(c)
  
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
  
  def remote[I, O](c: Command[I, O]): Either[Throwable, O] = {
    val (session, channel, close) = prepareExec(c.command)
    // Do we care about channel.getExitStatus?
    allCatch.andFinally(close()).either(c(channel.getInputStream()))
  } 
}

object IO {
  type Resource = { def close(): Unit }
  def apply[A, R <: Resource](r: R)(op: R => A): Either[Throwable, A] =
    allCatch.andFinally(r.close()).either(op(r))
}
