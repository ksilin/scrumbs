name := """hello-scala"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++=
  Seq("org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "com.jsuereth" %% "scala-arm" % "1.4",
    "com.chuusai" %% "shapeless" % "2.2.5",
    "org.scalaz" %% "scalaz-core" % "7.1.3"
  )



fork in run := true