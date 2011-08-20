organization := "com.novus"

name := "scalash"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.0-1"

libraryDependencies ++= Seq(
  "com.jcraft" % "jsch" % "0.1.44",
  "org.specs2" %% "specs2" % "1.5",
  // with Scala 2.8.1
  "org.specs2" %% "specs2-scalaz-core" % "5.1-SNAPSHOT" % "test"
  // with Scala 2.9.0
  //"org.specs2" %% "specs2-scalaz-core" % "6.0.RC2" % "test"
)

resolvers ++= Seq(
  "jsch" at " http://jsch.sf.net/maven2/"
)
