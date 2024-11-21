import Model.Order
import io.circe.generic.semiauto._
import io.circe.{Codec, Encoder, jawn}
import io.micrometer.core.instrument.{Tag, Timer}
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import io.vertx.core.http.{HttpServer, HttpServerOptions}
import io.vertx.core.{AsyncResult, Handler, Vertx, Future => VertxFuture}
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.{Router, RoutingContext}
import io.vertx.micrometer.PrometheusScrapingHandler

import java.time.{Duration, Instant}
import java.util.UUID
import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import scala.concurrent.Promise
import scala.util.Try

object Main {

  val orderMap: TrieMap[UUID, Order] = TrieMap.empty
  val prometheusRegistry = new PrometheusMeterRegistry(
    PrometheusConfig.DEFAULT
  )

  def main(args: Array[String]): Unit = {
    val port =
      sys.env.get("APP_PORT").flatMap(p => Try(p.toInt).toOption).getOrElse {
        println("port isn't specified")
        8080
      }

    val vertx = Vertx.vertx()

    // Set up a router to handle requests
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router
      .get("/")
      .handler(new Handler[RoutingContext] {
        override def handle(ctx: RoutingContext): Unit = handleRoot(ctx)
      })
    router
      .post("/order/create")
      .handler(observeMetrics(handleCreateOrder, "/order/create"))
    router
      .get("/order")
      .handler(observeMetrics(handleGetOrder, "/order"))
    router
      .get("/metrics")
      .handler((ctx: RoutingContext) => {
        println(s"scrape scala metrics at ${Instant.now()}")
        PrometheusScrapingHandler.create(prometheusRegistry).handle(ctx)
      })

    val serverOptions = new HttpServerOptions().setPort(port)
    val server = vertx
      .createHttpServer(serverOptions)
      .requestHandler(router)
      .listen((event: AsyncResult[HttpServer]) => {
        if (event.succeeded()) {
          println(
            s"Server is running on ${serverOptions.getHost}:${serverOptions.getPort}"
          )
        } else {
          println(s"Failed to start server: ${event.cause().getMessage}")
        }
      })

    val promise = Promise[Unit]()
    sys.addShutdownHook {
      server.close().onComplete(_ => promise.success(()))
      promise.future
    }
  }

  def handleRoot(ctx: RoutingContext): Unit = {
    ctx
      .response()
      .putHeader("content-type", "text/plain")
      .end("Hello, World!")
  }

  def handleCreateOrder(ctx: RoutingContext): VertxFuture[_] = {
    val contentType = ctx.request().getHeader("Content-Type")

    if ("application/json".equalsIgnoreCase(contentType)) {
      val body = ctx.getBodyAsString()
      jawn.decode[Order](body) match {
        case Left(err) =>
          err.printStackTrace()
          ctx.response().setStatusCode(400).end("Can't parse json")
        case Right(order) =>
          if (orderMap.contains(order.id)) {
            ctx
              .response()
              .setStatusCode(409)
              .end(s"Order for ${order.id} already exists")
          } else {
            orderMap.put(order.id, order)
            ctx
              .response()
              .setStatusCode(201)
              .end(s"Order created with ID: ${order.id}")
          }
      }
    } else {
      ctx
        .response()
        .setStatusCode(415)
        .end("Expected 'Content-Type: application/json' header")
    }
  }

  def handleGetOrder(ctx: RoutingContext): VertxFuture[_] = {
    val id = ctx.queryParam("id").get(0)
    val orderOpt = orderMap.get(UUID.fromString(id))
    orderOpt match {
      case Some(order) =>
        ctx
          .response()
          .putHeader("Content-Type", "application/json")
          .setStatusCode(200)
          .end(Encoder[Order].apply(order).toString)
      case None =>
        ctx.response().setStatusCode(404).end(s"Order for $id does not exist")
    }
  }

  def observeMetrics(
      observe: RoutingContext => VertxFuture[_],
      endpoint: String
  ): Handler[RoutingContext] = (arg: RoutingContext) => {
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
    }
  }
}

object Model {

  case class Order(id: UUID, items: List[ItemInfo], customer: CustomerInfo)
  case class CustomerInfo(id: UUID, name: String, city: String)
  case class ItemInfo(id: UUID, item: Item, cost: Int, amount: Int)
  case class Item(name: String)

  implicit val orderJsonCodec: Codec[Order] = deriveCodec[Order]
  implicit val customerInfoJsonCodec: Codec[CustomerInfo] =
    deriveCodec[CustomerInfo]
  implicit val itemInfoJsonCodec: Codec[ItemInfo] = deriveCodec[ItemInfo]
  implicit val itemJsonCodec: Codec[Item] = deriveCodec[Item]
}
