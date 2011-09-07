package com.novus.rugu.test

import com.novus.rugu._
import com.novus.rugu.util.PropertyMapLoader

object Hosts {
  val hostCfg = new java.io.File("tests.properties")
  
  lazy val all =
    if(! hostCfg.exists)
      Map.empty[String, Template]
    else {
      val props = new java.util.Properties()
      IO(new java.io.FileInputStream("tests.properties"))(props.load(_))
      PropertyMapLoader(props)
    }
  
  lazy val single = all.get("single")
}
