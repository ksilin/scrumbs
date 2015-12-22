name := """scrumbs"""

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++=
  Seq("org.scalatest" %% "scalatest" % "3.0.0-M14" % "test",
    "com.jsuereth" %% "scala-arm" % "1.4",
    "com.chuusai" %% "shapeless" % "2.2.5",
    "org.scalaz" %% "scalaz-core" % "7.2.0",
    "com.typesafe.akka" %% "akka-actor" % "2.4.1"
  )



fork in run := false