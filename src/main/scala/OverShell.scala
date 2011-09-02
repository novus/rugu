package com.novus.rugu

import java.util.concurrent._

case class Template(host: Host, auth: Authentication, knownHostsFile: Option[String] = None) {
  def newSession = Ssh(host, auth, knownHostsFile)
}

class OverShell(sessions: Seq[SshSession]) {
  val cpus = Runtime.getRuntime.availableProcessors
  val execSvc = Executors.newFixedThreadPool(cpus * 4)
  
  import scala.collection.JavaConversions._ //FIXME saner conversions
  def apply[I, O](c: Command[I, O])(implicit sp: StreamProcessor[I]): Seq[Future[Either[Throwable, O]]] =
    execSvc.invokeAll {
      sessions.map { ssh =>
        new Callable[Either[Throwable, O]] { def call = ssh(c) }
      }
    }
}

object OverShell {
  def stage[A](stg: Seq[A])(ssh: Template)(f: (Template, A) => SshSession) =
    new OverShell((stg :\ List.empty[SshSession]) { f(ssh, _) :: _ })
}
