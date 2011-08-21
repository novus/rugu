package com.novus.rugu

import java.io.{BufferedReader, InputStream}

object `package` extends LowPriorityProcessors {
  /* Pimps for String => Command. */
  implicit def string2Piped(s: String) = new CommandString(s)
  implicit def string2Discarded(s: String) = new Discarded(s, implicitly[StreamProcessor[Unit]])
  
  class CommandString(command: String) {
    def :|[I, O](f: I => O)(implicit ev: StreamProcessor[I]) = Piped(command, ev, f)
    def :>(file: String)(implicit ev: StreamProcessor[BufferedReader]) = FileRedirect(command, file, ev, false)
    def :>>(file: String)(implicit ev: StreamProcessor[BufferedReader]) = FileRedirect(command, file, ev, true)
  }
}

/** A command to be executed in the remote shell. A Command[I, O] embodies
 *  both the command to be executed and the means by which the resulting
 *  output will be transformed.
 */
trait Command[I, O] extends (InputStream => O) {
  val command: String
  val sp: StreamProcessor[I]
  def apply(in: InputStream): O
}

/** A command whose output will be discarded. */
case class Discarded(command: String, sp: StreamProcessor[Unit]) extends Command[Unit, Unit] {
  def apply(in: InputStream) = ()
}

/** Pipe a command's output to a function. */
case class Piped[I, O](command: String, sp: StreamProcessor[I], f: I => O) extends Command[I, O] {
  def apply(in: InputStream) = (sp andThen f)(in)
}

/** Redirect output (as a character stream) to a file. */
case class FileRedirect(command: String, name: String, sp: StreamProcessor[BufferedReader], append: Boolean) extends Command[BufferedReader, Unit] {
  def apply(in: InputStream) =
    IO(sp(in)) { r =>
      IO(new java.io.BufferedWriter(new java.io.FileWriter(name, append))) { w =>
        var line: String = null
        while({ line = r.readLine(); line != null})
          w.write(line)
      }
    }
}
