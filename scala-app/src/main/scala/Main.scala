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

object Main:
  val orderMap: TrieMap[UUID, Order] = TrieMap.empty

  def main(args: Array[String]): Unit =
    // metrics
    val prometheusRegistry = new PrometheusMeterRegistry(
      PrometheusConfig.DEFAULT
    )

    val port =
      sys.env.get("APP_PORT").map(p => Try(p.toInt)) match
        case None => 
          println("port isn't specified")
          8080
        case Some(Failure(ex)) => throw ex
        case Some(Success(p))  => p

    JvmMemoryMetrics().bindTo(prometheusRegistry)
    JvmGcMetrics().bindTo(prometheusRegistry)
    JvmThreadMetrics().bindTo(prometheusRegistry)
    ProcessorMetrics().bindTo(prometheusRegistry)
    JvmCompilationMetrics().bindTo(prometheusRegistry)

    val counter = Counter
      .builder("test.counter")
      .tags("test_tag", "test")
      .register(prometheusRegistry)

    // server
    val vertx = Vertx.vertx(
      new VertxOptions().setMetricsOptions(
        new MicrometerMetricsOptions()
          .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
          .setMicrometerRegistry(prometheusRegistry)
          .setEnabled(true)
      )
    );

    val registry =
      BackendRegistries.getDefaultNow().asInstanceOf[PrometheusMeterRegistry];

    registry
      .config()
      .meterFilter(new MeterFilter() {
        override def configure(
            id: Meter.Id,
            config: DistributionStatisticConfig
        ): DistributionStatisticConfig = {
          DistributionStatisticConfig
            .builder()
            .percentiles(0.95, 0.99)
            .build()
            .merge(config);
        }
      });

    // Set up a router to handle requests
    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())

    router
      .get("/")
      .handler(ctx => {
        counter.increment()
        handleRoot(ctx)
      })

    router
      .post("/order/create")
      .handler(handleCreateOrder)

    router
      .get("/order")
      .handler(handleGetOrder)

    router.get("/metrics").handler(PrometheusScrapingHandler.create());

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

def handleCreateOrder(ctx: RoutingContext): Unit =
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

def handleGetOrder(ctx: RoutingContext): Unit =
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
