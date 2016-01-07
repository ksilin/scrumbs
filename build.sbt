name := """scrumbs"""

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++=
  Seq("org.scalatest" %% "scalatest" % "3.0.0-M15" % "test",
    "com.jsuereth" %% "scala-arm" % "1.4",
    "com.chuusai" %% "shapeless" % "2.2.5",
    "org.scalaz" %% "scalaz-core" % "7.2.0",
    "com.typesafe.akka" %% "akka-actor" % "2.4.1",
    "com.scalawilliam" %% "xs4s" % "0.2-SNAPSHOT",
    "org.apache.commons" % "commons-compress" % "1.10",
    "com.github.pathikrit"  %% "better-files-akka"  % "2.14.0",
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "org.spire-math" %% "cats" % "0.3.0"
  )



fork in run := true