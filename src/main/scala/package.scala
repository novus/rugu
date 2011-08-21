package com.novus

package object rugu extends LowPriorityProcessors {
  import java.io.{BufferedReader, InputStream}
  /* Pimps for String => Command. */
  implicit def string2Piped(s: String) = new CommandString(s)
  implicit def string2Discarded(s: String) = new Discarded(s, implicitly[StreamProcessor[Unit]])
  
  class CommandString(command: String) {
    def :|[I, O](f: I => O)(implicit ev: StreamProcessor[I]) = Piped(command, ev, f)
    def :>(file: String)(implicit ev: StreamProcessor[BufferedReader]) = FileRedirect(command, file, ev, false)
    def :>>(file: String)(implicit ev: StreamProcessor[BufferedReader]) = FileRedirect(command, file, ev, true)
  }
}
