val prometheusVersion = "1.3.3"
val doobieVersion = "1.0.0-RC4"
val circeVersion = "0.14.1"

enablePlugins(GraalVMNativeImagePlugin)

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-course-project",
    version := "0.1.0-SNAPSHOT",
    mainClass := Some("Main"),
    fork := true,
    scalaVersion := "2.13.15",
    libraryDependencies ++=
      zioHttp ++
        prometheus ++
        micrometer ++
        circe ++
        munit,
    assembly / mainClass := Some("Main"),
    assembly / assemblyOutputPath := file("./scala-app.jar"),
    ThisBuild / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case x                        => MergeStrategy.first
    }
  )

val micrometer = List(
  "io.micrometer" % "micrometer-registry-prometheus" % "1.11.10"
)

val prometheus = List(
  "io.prometheus" % "prometheus-metrics-core" % prometheusVersion,
  "io.prometheus" % "prometheus-metrics-instrumentation-jvm" % prometheusVersion,
  "io.prometheus" % "prometheus-metrics-exporter-httpserver" % prometheusVersion
)

val munit = List(
  "org.scalameta" %% "munit" % "1.0.0" % Test
)

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-generic-extras",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

// dependencyOverrides ++= List(
//   "io.netty" % "netty-codec" % "4.1.80.Final",
//   "io.netty" % "netty-codec-http" % "4.1.80.Final",
//   "io.netty" % "netty-common" % "4.1.80.Final",
//   "io.netty" % "netty-handler" % "4.1.80.Final",
//   "io.netty" % "netty-buffer" % "4.1.80.Final",
//   "io.netty" % "netty-resolver" % "4.1.80.Final",
//   "io.netty" % "netty-transport" % "4.1.80.Final"
// )

val zioHttp = List(
  "dev.zio" %% "zio-http" % "3.0.1"
)
