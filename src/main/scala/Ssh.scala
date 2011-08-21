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
  
  private def openExec(u: Unit) = {
    // Get an authorized session.
    val session = factory(())
    session.connect() // FIXME boom
    // Prepare a channel for invoking shell commands.
    val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
    (session, channel)
  }
  
  /* Yield the active session, (executed) channel, and a closure to clean them up. */
  def prepareExec(command: String)(sc: (Session, ChannelExec)) = {
    val (s, c) = sc
    c.setCommand(command)
    c.setInputStream(null)
    c.setErrStream(System.err)
    c.connect() // FIXME boom
    (s, c, () => { c.disconnect(); s.disconnect() })
  }
  
  def remote[I, O](c: Command[I, O]): Either[Throwable, O] = {
    val (session, channel, close) = (openExec _ andThen prepareExec(c.command)_ )(())
    // TODO channel.getExitStatus
    allCatch.andFinally(close()).either(c(channel.getInputStream()))
  } 
}

object IO {
  type Resource = { def close(): Unit }
  def apply[A, R <: Resource](r: R)(op: R => A): Either[Throwable, A] =
    allCatch.andFinally(r.close()).either(op(r))
}
