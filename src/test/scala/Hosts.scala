package com.novus.rugu.test

import com.novus.rugu._
import com.novus.rugu.util.PropertyMapLoader

object Hosts {
  val hostCfg = new java.io.File("tests.properties")
  
  lazy val config = new net.schmizz.sshj.DefaultConfig()
  
  lazy val all =
    if(! hostCfg.exists)
      Map.empty[String, Template]
    else {
      val props = new java.util.Properties()
      IO(new java.io.FileInputStream("tests.properties"))(props.load(_))
      /* Install a shared config to prevent resource contention (https://github.com/novus/rugu/issues/12). */
      PropertyMapLoader(props).map { case (k, t) =>
        k -> t.copy(config = Some(config))
      }
    }
  
  lazy val single = all.get("single")
}
