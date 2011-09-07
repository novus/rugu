package com.novus.rugu.util

import com.novus.rugu._

/** A utility for loading Template definitions from java maps. */
object PropertyMapLoader {
  type JMAP = java.util.Map[AnyRef, AnyRef]
  
  def toScalaStringMap(jmap: JMAP) =
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
  
  def apply(jm: JMAP) = templatesFromFlatMap(toScalaStringMap(jm))
}
