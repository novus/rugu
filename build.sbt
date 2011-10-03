organization := "com.novus"

name := "rugu"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.8.1"

crossScalaVersions ++= Seq("2.9.0-1", "2.9.1")

libraryDependencies ++= Seq(
  "net.schmizz" % "sshj" % "0.6.1",
  "org.bouncycastle" % "bcprov-jdk16" % "1.46",
  "org.specs2" %% "specs2" % "1.5" % "test",
  // with Scala 2.8.1
  "org.specs2" %% "specs2-scalaz-core" % "5.1-SNAPSHOT" % "test"
  // with Scala 2.9.0
  //"org.specs2" %% "specs2-scalaz-core" % "6.0.RC2" % "test"
)

resolvers ++= Seq(
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases" at "http://scala-tools.org/repo-releases"
)

publishTo <<= (version) { version: String =>
  val r = Resolver.sftp("repo.novus.com", "repo.novus.com", "/nv/repo/%s".format(
    if (version.trim().toString.endsWith("-SNAPSHOT")) "snapshots" else "releases"
    )) as (System.getProperty("user.name"))
  Some(r)
}

initialCommands := "import com.novus.rugu._"
