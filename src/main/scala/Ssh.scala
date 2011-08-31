package com.novus.rugu

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import scala.util.control.Exception.allCatch

case class Host(name: String, port: Int = 22)

object Ssh {
  
  trait Executor {
    def apply[A](command: Command[_, A])(f: java.io.InputStream => A): Either[Throwable, (Option[Int], A, String)]
  }
  
  def apply(host: Host, auth: Authentication, knownHostsFile: Option[String] = None) = {
    val hostVerifier = knownHostsFile.map(f => new OpenSSHKnownHosts(new java.io.File(f)))
    
    val executor = auth match {
      case UsernameAndPassword(u, p) =>
        new Executor {
          def apply[A](command: Command[_, A])(f: java.io.InputStream => A): Either[Throwable, (Option[Int], A, String)] = {
            val ssh = new SSHClient()
            hostVerifier.foreach(ssh.addHostKeyVerifier(_))
            
            allCatch.either {
              ssh.connect(host.name, host.port) //BOOM
              ssh.authPassword(u, p) //BOOM
              val s = ssh.startSession() // BOOM
              val c = s.exec(command.command) //BOOM
              command.input.foreach { in =>
                IO(c.getOutputStream()) { _.write(in.getBytes())} //BOOM
              }
              val a = f(c.getInputStream()) //BOOM
              val err = scala.io.Source.fromInputStream(c.getErrorStream()).mkString
              c.join(5, java.util.concurrent.TimeUnit.SECONDS) //TODO
              s.close() //BOOM
              ssh.disconnect() //BOOM
              (Option(c.getExitStatus).map(_.intValue), a, err)
            }
          }
        }
    }
    new SshSession(executor)
  }
}

class SshSession(executor: Ssh.Executor) {
  def apply[I, O](c: Command[I, O])(implicit sp: StreamProcessor[I]) =
    exec(c)(identity).fold(
      Left(_),
      { case (i, o, os) => Either.cond(i.getOrElse(-1) == 0, o, i -> os.toString) })
  
  def exec[I, O, OO](c: Command[I, O])(f: ((Option[Int], O, String)) => OO)(implicit sp: StreamProcessor[I]) =
    remote(c, sp).fold(Left(_), r => Right(f(r)))
  
  /* Left[Throwable] on network error. */
  def remote[I, O](c: Command[I, O], sp: StreamProcessor[I]): Either[Throwable, (Option[Int], O, String)] =
    executor(c)(sp andThen c)
}

object IO {
  type Resource = { def close(): Unit }
  def apply[A, R <: Resource](r: R)(op: R => A): Either[Throwable, A] =
    allCatch.andFinally(r.close()).either(op(r))
}
