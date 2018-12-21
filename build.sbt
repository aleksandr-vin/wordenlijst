val Http4sVersion = "0.18.21"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val CirceVersion = "0.9.3"

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
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.47deg" %% "github4s" % "0.19.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion
    ),

    resolvers += Resolver.bintrayRepo("aleksandrvin", "maven"),
    libraryDependencies ++= Seq(
      "org.picoworks" %% "pico-hashids"  % "4.4.145-5e94364"
    ),

    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),
    scalacOptions ++= Seq("-Ypartial-unification")
  )
  .enablePlugins(GitVersioning)
  .enablePlugins(JavaAppPackaging)
