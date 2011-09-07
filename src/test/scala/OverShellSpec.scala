package com.novus.rugu

import org.specs2.mutable._
import java.io.ByteArrayInputStream

class OverShellSpec extends Specification {
  args(skipAll = test.Hosts.all.isEmpty)
  val templates = test.Hosts.all.values
  val overshell = new OverShell(templates.map(Ssh(_)).toList)
  
  "An OverShell" should {
    "compute 1 + 1 from echoed strings across all servers" in {
      val futures = overshell("echo 1" :#| { (_:Int) + 1 })
      val ints = futures.map(_.get.fold(_ => 0, identity))
      ints.reduceLeft(_ + _) == templates.size * 2
    }
    "upload and then remove a file" in {
      val remoteFile = "rugu-upload-%d-%d".format(
          scala.util.Random.nextInt(1000000), System.currentTimeMillis) 
      val uploadResults = overshell.upload("LICENSE.md", remoteFile)
      val rmResults = overshell("rm " + remoteFile)
      (rmResults ++ uploadResults).map(_.get).forall(_.isRight)
    }
  }
}
