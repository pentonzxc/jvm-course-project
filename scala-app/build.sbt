val scala3Version = "2.13.14"

val VersionVertx = "4.4.0"
val VersionPrometheus = "1.3.3"
val doobieVersion = "1.0.0-RC4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-course-project",
    version := "0.1.0-SNAPSHOT",
    mainClass := Some("Main"),
    fork := true,
    scalaVersion := scala3Version,
    libraryDependencies ++=
      vertx ++
        prometheus ++
        netty ++
        micrometer ++
        circe ++
        munit,
    assembly / mainClass := Some("Main"),
    assembly / assemblyOutputPath := file("./scala-app.jar"),
    ThisBuild / assemblyMergeStrategy := {
      case PathList(
            "META-INF",
            "services",
            "io.vertx.core.spi.VertxServiceProvider"
          ) =>
        MergeStrategy.first
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    }
  )

val vertx = List(
  "io.vertx" % "vertx-core" % VersionVertx,
  "io.vertx" % "vertx-web" % VersionVertx,
  "io.vertx" % "vertx-micrometer-metrics" % VersionVertx
)

val netty = List(
  "io.netty" % "netty-resolver-dns-native-macos" % "4.1.100.Final" % "runtime"
)

val micrometer = List(
  "io.micrometer" % "micrometer-registry-prometheus" % "1.11.10"
)

val prometheus = List(
  "io.prometheus" % "prometheus-metrics-core" % VersionPrometheus,
  "io.prometheus" % "prometheus-metrics-instrumentation-jvm" % VersionPrometheus,
  "io.prometheus" % "prometheus-metrics-exporter-httpserver" % VersionPrometheus
)

val munit = List(
  "org.scalameta" %% "munit" % "1.0.0" % Test
)

val circe = List(
  "io.circe" %% "circe-core" % "0.14.10",
  "io.circe" %% "circe-parser" % "0.14.10",
  "io.circe" %% "circe-generic-extras" % "0.14.4"
)
