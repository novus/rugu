package com.novus.rugu

import java.io.{
  BufferedReader,
  InputStream
}

object `package` extends LowPriorityProcessors {
  /** A type class for transforming the result stream of a command.
   *  The intent for processors it to massage shell command output in some
   *  way before passing it on.
   */
  type StreamProcessor[O] = InputStream => O
  
  /* Pimps for String => Command. */
  implicit def string2Piped(s: String) = new CommandString(s)
  implicit def string2Discarded(s: String)(implicit ev: StreamProcessor[Unit]) = Piped(None, s, (_:Unit) => ())
}

private[rugu] class CommandString(command: String) {
  def :#|[I : StreamProcessor, O](f: I => O) = Piped(None, command, f)
  def :|[O](f: String => O)(implicit ev: StreamProcessor[String]) = Piped(None, command, f)
  def ::|[O](f: List[String] => O)(implicit ev: StreamProcessor[List[String]]) = Piped(None, command, f)
  def :>(file: String)(implicit ev: StreamProcessor[BufferedReader]) = FileRedirect(None, command, file, false)
  def :>>(file: String)(implicit ev: StreamProcessor[BufferedReader]) = FileRedirect(None, command, file, true)
}

/** A command to be executed in the remote shell. A Command[I, O] embodies
 *  both the command to be executed and the means by which the resulting
 *  output will be transformed.
 */
sealed trait Command[I, O] extends (I => O) {
  type C >: Command[I, O]
  val command: String
  val input: Option[String]
  def |:(in: String): C
}

/** Pipe a command's output to a function. */
case class Piped[I, O](input: Option[String], command: String, f: I => O) extends Command[I, O] {
  def apply(in: I) = f(in)
  def |:(_in: String) = copy(input = Some(_in))
}

/** Redirect output (as a character stream) to a file. */
case class FileRedirect(input: Option[String], command: String, name: String, append: Boolean) extends Command[BufferedReader, Unit] {
  def apply(in: BufferedReader) =
    IO(in) { r =>
      IO(new java.io.BufferedWriter(new java.io.FileWriter(name, append))) { w =>
        var line: String = null
        while({ line = r.readLine(); line != null})
          w.write(line)
      }
    }
  def |:(_in: String) = copy(input = Some(_in))
}
