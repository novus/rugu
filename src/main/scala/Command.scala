package com.novus.rugu

import java.io.InputStream

object `package` {
  /* Pimps for String => Command. */
  implicit def string2Piped(s: String) = new StringCommand(s)
  implicit def string2Discarded(s: String) = new Discarded(s)
  
  class StringCommand(command: String) {
    def :|[I : StreamProcessor, O](f: I => O) = Piped(command, f)
    //def :>(file: String) = FileRedirect(command, file, false)
    //def :>>(file: String) = Redirected(command, file, true)
  }
}

/** A command to be executed in the remote shell. A Command[I, O] embodies
 *  both the command to be executed and the means by which the resulting
 *  output will be transformed.
 */
sealed trait Command[I, O] extends (I => O) {
  val command: String
  def apply(in: I): O
}
case class Discarded(command: String) extends Command[Unit, Unit] {
  def apply(in: Unit) = ()
}
case class Piped[I, O](command: String, f: I => O) extends Command[I, O] {
  def apply(in: I) = f(in)
}


sealed trait RedirectedIO { self: Command[InputStream, Unit] => // TODO
  def io[R <: IO.Resource](r: R)(op: R => Unit) = IO(r)(op)
}
case class FileRedirect(command: String, file: String, append: Boolean) extends Command[InputStream, Unit] with RedirectedIO { //TODO
  def apply(in: InputStream) = {
    io(in) { r =>
      io(new java.io.BufferedWriter(new java.io.FileWriter(file, append))) { w =>
        var line = r.read()
        println("read: %d; %s".format(line, line.toString))
        while(line != -1) {
          println("read: %d; %s".format(line, line.toString))
          w.write(line.toString)
          line = r.read()
        }
      }
    }
  }
}
