package com.novus.rugu

import com.jcraft.jsch._
import scala.util.control.Exception.allCatch

class Ssh(user: String, password: String, host: String, port: Int = 22, knownHosts: Option[String] = None) {
  
  val jsch = new JSch()
  knownHosts.foreach(jsch.setKnownHosts)
  // jsch.addIdentity("/path/to/private_key")
  
  def apply[I, O](c: Command[I, O]) =
    remote(c)
  
  def authorize(s: Session) = {
    s.setPassword(password)
    s
  }
  
  private def openExec(u: Unit) = {
    // Get an authorized session.
    val session = authorize(jsch.getSession(user, host, port))
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
    val procd = c(channel.getInputStream() /* boom */)
    close() // FIXME boom
    Right(procd)
  } 
}

object IO {
  type Resource = { def close(): Unit }
  def apply[A, R <: Resource](r: R)(op: R => A): Either[Throwable, A] =
    allCatch.andFinally(r.close()).either(op(r))
}
