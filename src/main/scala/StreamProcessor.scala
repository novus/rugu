package com.novus.rugu

import java.io.{
  BufferedReader,
  InputStream,
  InputStreamReader
}
import scala.io.Source

/** A type class for transforming the result stream of a command.
 *  The intent for processors it to massage shell command output in some
 *  way before passing it on.
 */
trait StreamProcessor[O] extends (InputStream => O) {
  def apply(in: InputStream): O
}

object StreamProcessor {
  def managed[I](p: InputStream => I) = new StreamProcessor[I] {
    def apply(in: InputStream) = p(in)
  }
}

trait LowPriorityProcessors {
  import StreamProcessor._
  
  implicit val AsListString     = managed(in => Source.fromInputStream(in).getLines.toList)
  implicit val AsUnit           = managed(_ => ())
  implicit val AsString         = managed(in => Source.fromInputStream(in).mkString.trim)
  implicit val Default          = AsListString.asInstanceOf[StreamProcessor[Any]]
  implicit val AsInt            = managed(AsString(_).toInt)
  implicit val AsInputStream    = managed(identity)
  implicit val AsReader         = managed(in => new BufferedReader(new InputStreamReader(in)))
}
