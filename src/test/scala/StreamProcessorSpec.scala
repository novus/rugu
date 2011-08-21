package com.novus.rugu

import org.specs2.mutable._
import java.io.ByteArrayInputStream

class StreamProcessorSpec extends Specification {
  def in = new ByteArrayInputStream(List("one", "two", "three").mkString("\n").getBytes("UTF-8"))
  def in(any: Any) = new ByteArrayInputStream(any.toString.getBytes("UTF-8"))
  
  "StreamProcessor instances" should {
    "properly transform a stream into a list" in { AsListString(in) == List("one", "two", "three") }
    "yield the unit value" in { AsUnit(in) == () }
    "properly transform a stream into a string" in { AsString(in) == "one\ntwo\nthree" }
    "properly transform a stream into an int" in { AsInt(in(1)) == 1 }
    "wrap the stream in a buffered reader" in { AsReader(in).isInstanceOf[java.io.BufferedReader] }
    "return the exact stream" in {
      val anIn = in
      AsInputStream(anIn) == anIn
    }
  }
}
