package com.novus.rugu

import java.io.{
  BufferedReader,
  BufferedInputStream,
  InputStream,
  InputStreamReader
}
import scala.io.Source

object StreamProcessor {
  def managed[I](p: InputStream => I) = new StreamProcessor[I] {
    def apply(in: InputStream) = p(in)
  }
}

trait LowPriorityProcessors {
  import StreamProcessor.managed
  
  implicit val AsListString           = managed(in => Source.fromInputStream(in).getLines.toList)
  implicit val AsUnit                 = managed(_ => ())
  implicit val AsString               = managed(in => Source.fromInputStream(in).mkString.trim)
  implicit val AsInt                  = managed(AsString(_).toInt)
  implicit val AsInputStream          = managed(i => i)
  implicit val AsBufferedInputStream  = managed(new BufferedInputStream(_))
  implicit val AsReader               = managed(in => new BufferedReader(new InputStreamReader(in)))
}
