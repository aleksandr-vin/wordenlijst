import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoOptions

val Http4sVersion = "0.18.21"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val CirceVersion = "0.9.3"
val Github4sVersion = "0.20.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.xvygyjau",
    name := "wordenlijst",
    scalaVersion := "2.12.7",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.scalamock" %% "scalamock" % "4.1.0" % Test,
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.47deg" %% "github4s" % Github4sVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion
    ),
    resolvers += Resolver.bintrayRepo("aleksandrvin", "maven"),
    libraryDependencies ++= Seq(
      "org.picoworks" %% "pico-hashids" % "4.4.145-5e94364"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),
    scalacOptions ++= Seq("-Ypartial-unification"),
    buildInfoPackage := "com.xvygyjau.wordenlijst",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      resolvers,
      libraryDependencies
    ),
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson
  )
  .enablePlugins(GitVersioning)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(BuildInfoPlugin)
