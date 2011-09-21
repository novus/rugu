package com.novus.rugu

import net.schmizz.sshj.{Config, DefaultConfig, SSHClient}
import net.schmizz.sshj.connection.channel.direct.Session
import scala.util.control.Exception.allCatch
import java.io.{File, InputStream}
import net.schmizz.sshj.transport.verification.{PromiscuousVerifier, OpenSSHKnownHosts}

case class Host(name: String, port: Int = 22)

object Ssh {
  def apply(t: Template): SshSession =
    apply(t.host, t.auth, t.knownHostsFile, t.connectTimeout, t.config)
  
  def apply(host: Host, auth: Authentication, knownHostsFile: Option[String] = None, connectTimeout: Int = 0, config: Option[Config]): SshSession = {
    val executor = new Executor {
      /* Load host keys once. */
      val hostVerifier = knownHostsFile
        .map(OpenSSHKnownHosts(new File(_))
        .getOrElse(new PromiscuousVerifier)
      val cfg = config.getOrElse(new DefaultConfig())
      
      private def withClient[A](op: SSHClient => A): Either[Throwable, A] = {
        val ssh = new SSHClient(cfg)
        ssh.addHostKeyVerifier(hostVerifier)
        allCatch.andFinally({if(ssh.isConnected) ssh.disconnect()}).either {
          /* Connect and authenticate. */
          ssh.setConnectTimeout(connectTimeout)
          ssh.connect(host.name, host.port)
          auth match {
            case PublicKey(u, k, pass) =>
              val kp = pass.map(ssh.loadKeys(k, _)).getOrElse(ssh.loadKeys(k)) //FIXME don't load keys every time
              ssh.authPublickey(u, kp)
            case UsernameAndPassword(u, p) =>
              ssh.authPassword(u, p)
          }
          op(ssh)
        }
      }
      
      def upload(localFile: String, remotePath: String) =
        withClient(_.newSCPFileTransfer().upload(localFile, remotePath))
      
      def download(remotePath: String, localFile: String) =
        withClient(_.newSCPFileTransfer().download(remotePath, localFile))
      
      def apply[A](command: Command[_, A])(f: InputStream => A): Either[Throwable, (Option[Int], A, String)] = {
        withClient { ssh =>
          IO(ssh.startSession()) { s =>
            /* Exec command with input if any, collect and transform output,
             * error output, and exit status if given.
             */
            val c = s.exec(command.command)
            command.input.foreach { in =>
              IO(c.getOutputStream()) { _.write(in.getBytes()) }
            }
            val a = f(c.getInputStream())
            val err = scala.io.Source.fromInputStream(c.getErrorStream()).mkString
            c.join(5, java.util.concurrent.TimeUnit.SECONDS) //TODO
            (Option(c.getExitStatus).map(_.intValue), a, err)
          }
        }.fold(Left(_), identity)
      }
    }
    
    new SshSession(executor)
  }
}

class SshSession(executor: Executor) {
  /** Execute a remote command. Non-zero exit statuses result in Lefts of
   *  RuntimeException and include the exit status and error output.
   *  Blocks the calling thread.
   */
  def apply[I : StreamProcessor, O](c: Command[I, O]): Either[Throwable, O] =
    exec(c)(identity).fold(
      Left(_), {
        case (Some(0), o, os) => Right(o)
        case (i, o, os) => Left(new RuntimeException("%d: %s".format(i.getOrElse(-1), os)))
      })
      
  /** Execute a command. A handler for the result must decide how to proceed
   *  based on the exit status, transformed successful result, and error output.
   *  Blocks the calling thread.
   */
  def exec[I, O, OO](c: Command[I, O])(f: ((Option[Int], O, String)) => OO)(implicit sp: StreamProcessor[I]): Either[Throwable, OO] =
    executor(c)(sp andThen c).fold(Left(_), r => Right(f(r)))
  
  /** Upload a file via SCP. Blocks the calling thread. */
  def upload(localFile: String, remotePath: String) =
    executor.upload(localFile, remotePath)
  
  /** Download a file via SCP. Blocks the calling thread. */
  def download(remotePath: String, localFile: String) =
    executor.download(remotePath, localFile)
}
