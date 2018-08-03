name := """scrumbs"""

version := "1.0"

scalaVersion := "2.12.6"

resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
                  Resolver.sonatypeRepo("snapshots"),
                  Resolver.bintrayRepo("projectseptemberinc", "maven"))

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

lazy val lib = new {
  object Version {
    val scalaz    = "7.2.13"
    val cats      = "0.9.0"
    val monix     = "2.3.0"
    val akka      = "2.5.2"
    val shapeless = "2.3.3"
  }

  val scalatest        = "org.scalatest"              %% "scalatest"         % "3.0.3"
  val scalaArm         = "com.jsuereth"               %% "scala-arm"         % "2.0"
  val shapeless        = "com.chuusai"                %% "shapeless"         % Version.shapeless
  val scalaz           = "org.scalaz"                 %% "scalaz-core"       % Version.scalaz
  val scalazConcurrent = "org.scalaz"                 %% "scalaz-concurrent" % Version.scalaz
  val cats             = "org.typelevel"              %% "cats"              % Version.cats
  val catsFree         = "org.typelevel"              %% "cats-free"         % Version.cats
  val monix            = "io.monix"                   %% "monix"             % Version.monix
  val monixCats        = "io.monix"                   %% "monix-cats"        % Version.monix
  val fetch            = "com.fortysevendeg"          %% "fetch"             % "0.5.0" // TODO - try/test fetch
  val akkaActor        = "com.typesafe.akka"          %% "akka-actor"        % Version.akka
  val commonsCompress  = "org.apache.commons"         % "commons-compress"   % "1.14"
  val betterFiles      = "com.github.pathikrit"       %% "better-files-akka" % "3.6.0"
  val scalaLogging     = "com.typesafe.scala-logging" %% "scala-logging"     % "3.9.0"
  val logback          = "ch.qos.logback"             % "logback-classic"    % "1.2.3"
}

libraryDependencies ++=
  Seq(
    lib.scalatest,
    lib.scalaArm,
    lib.shapeless,
    lib.scalaz,
    lib.scalazConcurrent,
    lib.cats,
    lib.catsFree,
    lib.monix,
    lib.monixCats,
    lib.akkaActor,
    lib.commonsCompress,
    lib.betterFiles,
    lib.scalaLogging,
    lib.logback
  )

scalacOptions ++= Seq("-feature", "-language:higherKinds")

fork in run := true
