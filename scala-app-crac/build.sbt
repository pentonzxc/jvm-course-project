import org.apache.tools.ant.util.MergingMapper
import org.apache.logging.log4j.core.config.composite
val prometheusVersion = "1.3.3"
val doobieVersion = "1.0.0-RC4"
val circeVersion = "0.14.1"

val os: String = System.getProperty("os.name").toLowerCase
def isMacos: Boolean = os.contains("macos")

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
    assembly / mainClass := Some("Main"),
    assembly / assemblyOutputPath := file("./target/scala-app.jar"),
    // for testing
    // GraalVMNativeImage / graalVMNativeImageOptions ++= List("--verbose"),

    ThisBuild / assemblyMergeStrategy := {
      case x @ PathList("META-INF", "native-image", xs @ _*) =>
        // idk, just os.contains("linux") doesn't work properly on alpine
        if (isMacos && xs.contains("netty-handler-default"))
          MergeStrategy.first
        else if (!isMacos && xs.contains("netty-handler-default"))
          MergeStrategy.discard
        else MergeStrategy.first

      case x @ PathList("META-INF", _*) => MergeStrategy.discard
      case x                            => MergeStrategy.first
    }
  )
  .settings(
    libraryDependencies ++=
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
// run / javaOptions += s"-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image",
