package com.novus.rugu

import java.util.concurrent._

case class Template(host: Host, auth: Authentication, knownHostsFile: Option[String] = None, connectTimeout: Int = 0)

class OverShell(sessions: Seq[SshSession]) {
  private val execSvc =
    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors * 4)

  def apply[I : StreamProcessor, O](c: Command[I, O]): Seq[Future[Either[Throwable, O]]] = 
    invokeAll(_(c))
  
  def upload(localFile: String, remotePath: String) =
    invokeAll(_.upload(localFile, remotePath))
    
  def download(remotePath: String, localFile: String) =
    invokeAll(_.download(remotePath, localFile))
  
  import scala.collection.JavaConversions._ //FIXME saner conversions
  private def invokeAll[O](f: SshSession => Either[Throwable, O]): Seq[Future[Either[Throwable, O]]] =
    execSvc.invokeAll(callables(f))
  
  private def callables[O](f: SshSession => Either[Throwable, O]) =
    sessions.map { ssh =>
      new Callable[Either[Throwable, O]] {
        def call = f(ssh)
      }
    }
}

object OverShell {
  def stage[A](stg: Seq[A])(ssh: Template)(f: (Template, A) => Template) =
    new OverShell((stg :\ List.empty[SshSession]) {
      (t, l) => Ssh(f(ssh, t)) :: l
    })
  
  def stageHosts(hosts: Seq[String])(ssh: Template) =
    stage(hosts)(ssh) { (t, h) => t.copy(host = Host(h)) }
}
