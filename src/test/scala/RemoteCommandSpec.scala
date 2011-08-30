package com.novus.rugu

import org.specs2.mutable._
import java.io.ByteArrayInputStream

class RemoteCommandSpec extends Specification {
  val props = new java.util.Properties()
  props.load(new java.io.FileInputStream("tests.properties"))
  val host = props.getProperty("localhost.host", "localhost")
  val port = props.getProperty("localhost.port", "22").toInt
  val user = props.getProperty("localhost.user", System.getProperty("user.name"))
  val password = props.getProperty("localhost.password")
  val knownHosts = Option(props.getProperty("localhost.knownHosts", null))
  
  val ssh = Ssh(Host(host, port), UsernameAndPassword(user, password), knownHosts)
  
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
