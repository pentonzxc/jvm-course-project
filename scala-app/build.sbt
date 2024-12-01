val prometheusVersion = "1.3.3"
val doobieVersion = "1.0.0-RC4"
val circeVersion = "0.14.1"


lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-course-project",
    version := "0.1.0-SNAPSHOT",
    mainClass := Some("Main"),
    fork := true,
    scalaVersion := "2.13.14",
    libraryDependencies ++=
        zioHttp ++
        prometheus ++
        netty ++
        micrometer ++
        circe ++
        munit,
    assembly / mainClass := Some("Main"),
    assembly / assemblyOutputPath := file("./scala-app.jar"),
    ThisBuild / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    }
  )

val netty = List(
  "io.netty" % "netty-resolver-dns-native-macos" % "4.1.100.Final" % "runtime"
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


val zioHttp = List(
  "dev.zio" %% "zio-http" % "3.0.1"
)
