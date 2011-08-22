package com.novus.rugu

import java.io.{BufferedReader, InputStream}

object `package` extends LowPriorityProcessors {
  /** A type class for transforming the result stream of a command.
   *  The intent for processors it to massage shell command output in some
   *  way before passing it on.
   */
  type StreamProcessor[O] = InputStream => O
  type SessionFactory = Unit => com.jcraft.jsch.Session
  
  /* Pimps for String => Command. */
  implicit def string2Piped(s: String) = new CommandString(s)
  implicit def string2Discarded[Unit : StreamProcessor](s: String) = new Discarded(s) //StreamProcessor[Unit]
  
  class CommandString(command: String) {
    def :|[O](f: String => O)(implicit ev: StreamProcessor[String]) = Piped(command, f)
    def ::|[O](f: List[String] => O)(implicit ev: StreamProcessor[List[String]]) = Piped(command, f)
    def :#|[I : StreamProcessor, O](f: I => O) = Piped(command, f)
    def :>(file: String)(implicit ev: StreamProcessor[BufferedReader]) = FileRedirect(command, file, false)
    def :>>(file: String)(implicit ev: StreamProcessor[BufferedReader]) = FileRedirect(command, file, true)
  }
}

/** A command to be executed in the remote shell. A Command[I, O] embodies
 *  both the command to be executed and the means by which the resulting
 *  output will be transformed.
 */
sealed trait Command[I, O] extends (I => O) {
  val command: String
}

/** A command whose output will be discarded. */
case class Discarded(command: String) extends Command[Unit, Unit] {
  def apply(in: Unit) = ()
}

/** Pipe a command's output to a function. */
case class Piped[I, O](command: String, f: I => O) extends Command[I, O] {
  def apply(in: I) = f(in)
}

/** Redirect output (as a character stream) to a file. */
case class FileRedirect(command: String, name: String, append: Boolean) extends Command[InputStream, Unit] {
  def apply(in: InputStream) =
    IO(in) { r =>
      IO(new java.io.BufferedWriter(new java.io.FileWriter(name, append))) { w =>
        var line: String = null
        while({ line = r.readLine(); line != null})
          w.write(line)
      }
    }
}
