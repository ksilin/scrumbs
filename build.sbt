name := """scrumbs"""

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
                  Resolver.sonatypeRepo("snapshots"),
                  Resolver.bintrayRepo("projectseptemberinc", "maven"))

addCompilerPlugin("com.milessabin"  % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full)
addCompilerPlugin("org.spire-math"  %% "kind-projector"  % "0.9.0")
addCompilerPlugin("org.scalamacros" % "paradise"         % "2.1.0" cross CrossVersion.full)

val scalazVersion    = "7.2.6"
val catsVersion      = "0.7.2"
val monixVersion     = "2.0.1"
val akkaVersion      = "2.4.11"
val shapelessVersion = "2.3.2"

libraryDependencies ++=
  Seq("org.scalatest"  %% "scalatest"         % "3.0.0",
      "org.scalacheck" %% "scalacheck"        % "1.13.2",
      "com.jsuereth"   %% "scala-arm"         % "1.4",
      "com.chuusai"    %% "shapeless"         % shapelessVersion,
      "org.scalaz"     %% "scalaz-core"       % scalazVersion,
      "org.scalaz"     %% "scalaz-concurrent" % scalazVersion,
      "org.typelevel"  %% "cats"              % catsVersion,
      "org.typelevel"  %% "cats-free"         % catsVersion,
      "io.monix"       %% "monix"             % monixVersion,
      "io.monix"       %% "monix-cats"        % monixVersion,
      // TODO - bump & retest
      "com.projectseptember"       %% "freek"             % "0.3.0",
      "com.fortysevendeg"          %% "fetch"             % "0.3.0-SNAPSHOT", // TODO - try/test fetch
      "com.typesafe.akka"          %% "akka-actor"        % akkaVersion,
      "org.apache.commons"         % "commons-compress"   % "1.10",
      "com.github.pathikrit"       %% "better-files-akka" % "2.14.0",
      "com.typesafe.scala-logging" %% "scala-logging"     % "3.4.0",
      "ch.qos.logback"             % "logback-classic"    % "1.1.3")

scalacOptions ++= Seq("-feature", "-language:higherKinds")

fork in run := true
