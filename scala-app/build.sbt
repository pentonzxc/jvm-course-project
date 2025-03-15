val prometheusVersion = "1.3.3"
val doobieVersion = "1.0.0-RC4"
val circeVersion = "0.14.1"

lazy val root = project
  .in(file("."))
  .enablePlugins(GraalVMNativeImagePlugin)
  .settings(
    name := "scala-course-project",
    version := "1.0.0",
    scalaVersion := "2.13.15"
  )
  .settings(
    Compile / mainClass := Some("Main"),
    assembly / mainClass := Some("Main")
  )
  .settings(
    libraryDependencies ++=
      zioHttp ++
        prometheus ++
        micrometer ++
        circe ++
        munit
  )
  .settings(
    fork := true
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

// run / javaOptions += s"-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image",
