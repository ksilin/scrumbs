name := """scrumbs"""

version := "1.0"

scalaVersion := "2.12.1"

resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
                  Resolver.sonatypeRepo("snapshots"),
                  Resolver.bintrayRepo("projectseptemberinc", "maven"))

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

val scalazVersion    = "7.2.8"
val catsVersion      = "0.9.0"
val monixVersion     = "2.2.1"
val akkaVersion      = "2.4.17"
val shapelessVersion = "2.3.2"

libraryDependencies ++=
  Seq(
    "org.scalatest"              %% "scalatest"         % "3.0.1",
//    "org.scalacheck"             %% "scalacheck"        % "1.13.4",
    "com.jsuereth"               %% "scala-arm"         % "2.0",
    "com.chuusai"                %% "shapeless"         % shapelessVersion,
    "org.scalaz"                 %% "scalaz-core"       % scalazVersion,
    "org.scalaz"                 %% "scalaz-concurrent" % scalazVersion,
    "org.typelevel"              %% "cats"              % catsVersion,
    "org.typelevel"              %% "cats-free"         % catsVersion,
    "io.monix"                   %% "monix"             % monixVersion,
    "io.monix"                   %% "monix-cats"        % monixVersion,
//    "com.projectseptember"       %% "freek"             % "0.6.5",
    "com.fortysevendeg"          %% "fetch"             % "0.5.0", // TODO - try/test fetch
    "com.typesafe.akka"          %% "akka-actor"        % akkaVersion,
    "org.apache.commons"         % "commons-compress"   % "1.13",
    "com.github.pathikrit"       %% "better-files-akka" % "2.17.1",
    "com.typesafe.scala-logging" %% "scala-logging"     % "3.5.0",
    "ch.qos.logback"             % "logback-classic"    % "1.2.1"
  )

scalacOptions ++= Seq("-feature", "-language:higherKinds")

fork in run := true
