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
    assembly / mainClass := Some("Main"),
    assembly / assemblyOutputPath := file("./scala-app.jar"),
    ThisBuild / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case x                        => MergeStrategy.first
    }
  )
  .settings(
    graalVMNativeImageOptions ++= List(
      "--allow-incomplete-classpath",
      "--initialize-at-run-time=io.netty.channel.DefaultFileRegion",
      "--initialize-at-run-time=io.netty.channel.epoll.Native",
      "--initialize-at-run-time=io.netty.channel.epoll.Epoll",
      "--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop",
      "--initialize-at-run-time=io.netty.channel.epoll.EpollEventArray",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueue",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop",
      "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray",
      "--initialize-at-run-time=io.netty.channel.kqueue.Native",
      "--initialize-at-run-time=io.netty.channel.unix.Limits",
      "--initialize-at-run-time=io.netty.channel.unix.Errors",
      "--initialize-at-run-time=io.netty.channel.unix.IovArray",
      "--initialize-at-run-time=io.netty.incubator.channel.uring",
      "--initialize-at-run-time=io.netty.handler.ssl.BouncyCastleAlpnSslUtils"
      // s"-H:ReflectionConfigurationFiles=${baseDirectory.value}/src/main/resources/META-INF/native-image/io.netty/netty-handler/native-image.properties"
    )
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
