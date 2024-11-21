import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import scala.jdk.FutureConverters._
import scala.concurrent.Promise
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import io.prometheus.metrics.core.metrics.Histogram
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.vertx.core.VertxOptions
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions
import io.vertx.micrometer.PrometheusScrapingHandler
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.Counter
import java.util.UUID
import scala.collection.JavaConverters._
import io.vertx.core.Future as VertxFuture

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.collection.concurrent.TrieMap
import io.circe.Decoder
import io.circe.derivation.Configuration
import io.circe.Encoder
import Model.*
import io.circe.JsonObject
import io.circe.derivation.ConfiguredDecoder
import io.circe.derivation.ConfiguredEncoder
import io.circe.parser.*
import Main.orderMap
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.micrometer.backends.BackendRegistries
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import scala.util.{Try, Failure, Success}
import io.micrometer.core.instrument.Timer
import java.time.Duration
import Main.prometheusRegistry
import io.vertx.core.Handler
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Meter.Id
import io.vertx.micrometer.backends.BackendRegistry
import java.time.Instant

object Main:

  val orderMap: TrieMap[UUID, Order] = TrieMap.empty
  val prometheusRegistry = new PrometheusMeterRegistry(
    PrometheusConfig.DEFAULT
  )

  def main(args: Array[String]): Unit =
    val port =
      sys.env.get("APP_PORT").map(p => Try(p.toInt)) match
        case None =>
          println("port isn't specified")
          8080
        case Some(Failure(ex)) => throw ex
        case Some(Success(p))  => p

    val vertx = Vertx.vertx();

    // Set up a router to handle requests
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router
      .get("/")
      .handler(ctx => {
        handleRoot(ctx)
      })
    router
      .post("/order/create")
      .blockingHandler(observeMetrics(handleCreateOrder, "/order/create"))
    router
      .get("/order")
      .handler(observeMetrics(handleGetOrder, "/order"))
    router
      .get("/metrics")
      .handler(ctx => {
        println(s"scrape scala metrics at ${Instant.now()}")
        PrometheusScrapingHandler.create(prometheusRegistry).handle(ctx)
      })

    val serverOptions = new HttpServerOptions()
    serverOptions.setPort(port)
    val server = vertx
      .createHttpServer(serverOptions)
      .requestHandler(router)
      .listen { result =>
        if result.succeeded() then
          println(
            s"Server is running on ${serverOptions.getHost()}:${serverOptions.getPort()}"
          )
        else println(s"Failed to start server: ${result.cause().getMessage}")
      }

    val promise = Promise[Unit]
    sys.addShutdownHook {
      server.close().andThen(_ => promise.success(()))
      promise.future
    }

  end main

def handleRoot(ctx: RoutingContext): Unit =
  ctx
    .response()
    .putHeader("content-type", "text/plain")
    .end("Hello, World!")

import io.vertx.core.json.JsonObject

def handleCreateOrder(ctx: RoutingContext): VertxFuture[?] =

  val contentType = ctx.request().getHeader("Content-Type")

  if "application/json".equalsIgnoreCase(contentType) then
    val body = ctx.getBodyAsString()
    decode[Order](body) match
      case Left(err) =>
        err.printStackTrace()
        ctx
          .response()
          .setStatusCode(400)
          .end("Can't parse json")
      case Right(order) =>
        if orderMap.contains(order.id) then
          ctx
            .response()
            .setStatusCode(409)
            .end(s"Order for ${order.id} already exists")
        else
          orderMap.put(order.id, order)
          ctx
            .response()
            .setStatusCode(201)
            .end(s"Order created with ID: ${order.id}")
  else
    ctx
      .response()
      .setStatusCode(415)
      .end("Expected 'Content-Type: application/json' header")

def handleGetOrder(ctx: RoutingContext): VertxFuture[?] =
  val id = ctx.queryParam("id").get(0)
  val orderOpt = orderMap.get(UUID.fromString(id))
  orderOpt match
    case Some(order) =>
      ctx
        .response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
        .end(Encoder[Order].apply(order).toString)
    case None =>
      ctx
        .response()
        .setStatusCode(404)
        .end(s"Order for $id does not exist")

object Model:

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  case class Order(id: UUID, items: List[ItemInfo], customer: CustomerInfo)
      derives ConfiguredDecoder,
        ConfiguredEncoder

  case class CustomerInfo(id: UUID, name: String, city: String)
      derives ConfiguredDecoder,
        ConfiguredEncoder

  case class ItemInfo(id: UUID, item: Item, cost: Int, amount: Int)
      derives ConfiguredDecoder,
        ConfiguredEncoder
  case class Item(name: String) derives ConfiguredDecoder, ConfiguredEncoder

def observeMetrics(
    observe: RoutingContext => VertxFuture[?],
    endpoint: String
): Handler[RoutingContext] =
  arg => {
    val startTime = System.nanoTime()
    val future = observe(arg)
    future.onComplete { _ => 
        val endTime = System.nanoTime()
        Timer
          .builder("http.request.duration.seconds")
          .tags(
            List(
              Tag.of("app", "scala"),
              Tag.of("endpoint", endpoint)
            ).asJava
          )
          .serviceLevelObjectives(
            Duration.ofMillis(5),
            Duration.ofMillis(10),
            Duration.ofMillis(25),
            Duration.ofMillis(50),
            Duration.ofMillis(100),
            Duration.ofMillis(250),
            Duration.ofMillis(500),
            Duration.ofMillis(1000),
            Duration.ofMillis(2500),
            Duration.ofMillis(5000),
            Duration.ofMillis(10000)
          )
          .register(prometheusRegistry)
          .record(Duration.ofNanos(endTime - startTime))

          // println(s"Request latency - ${startTime - endTime} (nanos)")
    }
  }
