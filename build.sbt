name := """scrumbs"""

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val scalazVersion = "7.2.0"

libraryDependencies ++=
  Seq(
    "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test",
    "com.jsuereth" %% "scala-arm" % "1.4",

    "com.chuusai" %% "shapeless" % "2.2.5",

    "org.scalaz" %% "scalaz-core" % scalazVersion,
    "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
    "org.typelevel" %% "cats" % "0.5.0",
    "com.typesafe.akka" %% "akka-actor" % "2.4.1",

    "org.apache.commons" % "commons-compress" % "1.10",
    "com.github.pathikrit" %% "better-files-akka" % "2.14.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
    "ch.qos.logback" % "logback-classic" % "1.1.3"
  )



fork in run := true