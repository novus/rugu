package com.novus.rugu

import org.specs2.mutable._
import java.io.ByteArrayInputStream

class RemoteCommandSpec extends Specification {
  args(skipAll = test.Hosts.single.isEmpty)
  val ssh = test.Hosts.single.map(Ssh(_)).getOrElse(error("Corrupt properties!"))
  
  "An SshSession" should {
    "yield and echo'd string" in {
      ssh("echo hello" :| identity) must_== Right("hello")
    }
    "list contents of /tmp" in {
      ssh("ls /tmp" ::| { _.isEmpty }) must_== Right(false)
    }
    "parse numeric output as an Int" in {
      ssh("echo 5" :#| { (_:Int) + 1 }) must_== Right(6)
    }
    "feed input to grep" in {
      ssh("the\nanswer\nis" |: "grep answer" :| identity) must_== Right("answer")
    }
  }
}
