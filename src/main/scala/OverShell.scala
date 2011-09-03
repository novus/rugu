package com.novus.rugu

import java.util.concurrent._

case class Template(host: Host, auth: Authentication, knownHostsFile: Option[String] = None)

class OverShell(sessions: Seq[SshSession]) {
  val cpus = Runtime.getRuntime.availableProcessors
  val execSvc = Executors.newFixedThreadPool(cpus * 4)
  
  import scala.collection.JavaConversions._ //FIXME saner conversions
  def apply[I : StreamProcessor, O](c: Command[I, O]): Seq[Future[Either[Throwable, O]]] = 
    execSvc.invokeAll(callables(c))
    
  def callables[I : StreamProcessor, O](c: Command[I, O]) =
    sessions.map { ssh =>
      new Callable[Either[Throwable, O]] { def call = ssh(c) }
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
