package com.novus.rugu.test

import com.novus.rugu._

object Hosts {
  val hostCfg = new java.io.File("tests.properties")
  
  lazy val all =
    if(! hostCfg.exists)
      Map.empty[String, Template]
    else {
      val props = new java.util.Properties()
      props.load(new java.io.FileInputStream("tests.properties"))
      templatesFromProperties(props)
    }
    
  lazy val single = all.get("single")
  
  def toScalaStringMap[JMAP <: java.util.Map[AnyRef, AnyRef]](jmap: JMAP) =
    scala.collection.JavaConversions.asScalaMap(jmap).map {
      case (k, v) => k.toString -> v.toString 
    }
    
  def templateFromMap(pfx: String)(m: scala.collection.Map[String, String]) = {
    def key(relative: String, dflt: => String) =
      m.get(pfx + "." + relative).getOrElse(dflt)
    Template(
      Host(key("host", "localhost"), key("port", "22").toInt),
      UsernameAndPassword(
        key("user", error("No username!")),
        key("password", error("No password!"))),
      Option(key("knownHosts", null))
    )
  }
  
  def templatesFromFlatMap(m: scala.collection.Map[String, String]) =
    for {
      (host, hostMap) <- m.groupBy(_._1.toString.split("\\.")(0))
    } yield host -> templateFromMap(host)(hostMap)
  
  val templatesFromProperties = toScalaStringMap _ andThen templatesFromFlatMap _
}
