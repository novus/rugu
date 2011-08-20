package com.novus.rugu

import java.io.{
  BufferedReader,
  InputStream,
  InputStreamReader
}

import scala.io.Source

/** A type class for transforming the result stream of a command. */
trait StreamProcessor[O] extends (InputStream => Either[() => O, O]) {
  def apply(in: InputStream): Either[() => O, O]
}

object SP {
  def managed[I](p: InputStream => I) = new StreamProcessor[I] {
    def apply(in: InputStream) = Right(p(in))
  }
  
  def unmanaged[I](p: InputStream => I) = new StreamProcessor[I] {
    def apply(in: InputStream) = Left(() => p(in))
  }
  
  //implicit val AsBufferedReader = managed(in => new BufferedReader(new InputStreamReader(in)))
  
  /*implicit val AsSource         = managed(in => Source.fromInputStream(in))
  implicit val AsListString     = managed(in => AsSource(in).getLines.toList)
  implicit val AsUnit           = managed(_ => ())
  // TODO this is stupid.
  implicit val AsInt            = managed(in => AsListString(in).headOption.map(_ toInt))
  implicit val AsString         = managed(in => AsSource(in).mkString)*/
  
  implicit val AsListString     = managed(in => Source.fromInputStream(in).getLines.toList)
  implicit val AsInputStream    = unmanaged(in => () => in)
}
