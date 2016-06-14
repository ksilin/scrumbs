name := """scrumbs"""

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(Resolver.sonatypeRepo("releases")
, Resolver.sonatypeRepo("snapshots"),
  Resolver.bintrayRepo("projectseptemberinc", "maven"))

addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full)

val scalazVersion = "7.2.0"
val catsVersion = "0.6.0"
val monixVersion = "2.0-RC5"
val akkaVersion = "2.4.6"

libraryDependencies ++=
  Seq(
    "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test",
    "com.jsuereth" %% "scala-arm" % "1.4",

    "com.chuusai" %% "shapeless" % "2.2.5",

    "org.scalaz" %% "scalaz-core" % scalazVersion,
    "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
    "org.typelevel" %% "cats" % catsVersion,
    "org.typelevel" %% "cats-free" % catsVersion,
    "io.monix" %% "monix" % monixVersion,
    "io.monix" %% "monix-cats" % monixVersion,

    "com.projectseptember" %% "freek" % "0.3.0"
    , "org.spire-math" %% "kind-projector" % "0.7.1"
    , "com.milessabin" %% "si2712fix-library" % "1.2.0" cross CrossVersion.full,

    "com.fortysevendeg" %% "fetch" % "0.3.0-SNAPSHOT",

"com.typesafe.akka" %% "akka-actor" % akkaVersion,

    "org.apache.commons" % "commons-compress" % "1.10",
    "com.github.pathikrit" %% "better-files-akka" % "2.14.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
    "ch.qos.logback" % "logback-classic" % "1.1.3"
  )

scalacOptions ++= Seq(
  "-feature",
  "-language:higherKinds"
)

fork in run := true