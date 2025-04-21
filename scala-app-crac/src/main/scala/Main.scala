import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.micrometer.core.instrument.{Tag, Timer}
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import zio.http.{handler, _}
import zio.{Scope, ZIO, ZIOAppDefault}

import java.time.Duration
import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters.IterableHasAsJava

object Main extends ZIOAppDefault {

  val orderMap: TrieMap[UUID, Model.Order] = TrieMap.empty
  val prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  override def run: ZIO[Scope, Throwable, Unit] = {
    val port = sys.env.get("APP_PORT").flatMap(_.toIntOption).getOrElse(8080)
    val label = sys.env
      .get("APP_LABEL")
      .getOrElse(throw new RuntimeException("Specify APP_LABEL in env"))

    ZIO.succeed(println(s"Starting server on port $port")) *> {
      Server
        .serve(
          appRouter(label).handleErrorCauseZIO(err =>
            zio.Console.printLine(err.squash).orDie *> ZIO.succeed(
              Response.internalServerError
            )
          )
        )
        .provide(Server.defaultWithPort(port))
    }
  }

  def appRouter(label: String): Routes[Any, Throwable] =
    Routes(
      Method.GET / "metrics" -> handler(
        Response.text(prometheusRegistry.scrape())
      ),
      Method.POST / "order" / "create" -> handler { req: Request =>
        withMetrics(handleCreateOrder(req), "/order/create", label)
          .tapErrorCause(err =>
            zio.Console.printLine(s"Error on /order/create: ${err.squash}")
          )
      },
      Method.GET / "order" -> handler { req: Request =>
        withMetrics(handleGetOrder(req), "/order", label)
          .tapError(err => zio.Console.printLine(s"Error on /order: $err"))
      }
    )

  def handleCreateOrder(req: Request): ZIO[Any, Throwable, Response] = {
    for {
      contentType <- ZIO.succeed(req.header(Header.ContentType))
      response <- contentType match {
        case Some(ct) if ct.mediaType.matches(MediaType.application.json) =>
          for {
            body <- req.body.asString
            decoded <- ZIO
              .fromEither(decode[Model.Order](body))
              .mapError(err => new IllegalArgumentException(err))
            result <- ZIO.succeed {
              if (orderMap.contains(decoded.id)) {
                Response
                  .text(s"Order for ${decoded.id} already exists")
                  .status(Status.Conflict)
              } else {
                orderMap.put(decoded.id, decoded)
                Response
                  .text(s"Order created with ID: ${decoded.id}")
                  .status(Status.Created)
              }
            }
          } yield result

        case _ =>
          ZIO.succeed(
            Response
              .text("Expected 'Content-Type: application/json' header")
              .status(Status.UnsupportedMediaType)
          )
      }
    } yield response
  }

  def handleGetOrder(req: Request): ZIO[Any, Throwable, Response] = {
    req.url.queryParams.queryParam("id") match {
      case Some(idStr) =>
        ZIO
          .attempt(UUID.fromString(idStr))
          .map { id =>
            orderMap.get(id) match {
              case Some(order) =>
                Response.json(order.asJson.noSpaces).status(Status.Ok)
              case None =>
                Response
                  .text(s"Order for $id does not exist")
                  .status(Status.NotFound)
            }
          }
          .catchAll(_ =>
            ZIO.succeed(
              Response.text("Invalid ID format").status(Status.BadRequest)
            )
          )

      case None =>
        ZIO.succeed(
          Response
            .text("Missing 'id' query parameter")
            .status(Status.BadRequest)
        )
    }
  }

  def withMetrics[R, E](
      effect: ZIO[R, E, Response],
      endpoint: String,
      label: String
  ): ZIO[R, E, Response] = {
    val startTime = System.nanoTime()
    for {
      response <- effect
      endTime = System.nanoTime()
      _ = Timer
        .builder("http.request.duration.seconds")
        .tags(
          List(
            Tag.of("app", s"scala-$label"),
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
    } yield response
  }
}

object Model {
  import io.circe.generic.semiauto._

  final case class Order(
      id: UUID,
      items: List[ItemInfo],
      customer: CustomerInfo
  )
  final case class CustomerInfo(id: UUID, name: String, city: String)
  final case class ItemInfo(id: UUID, item: Item, cost: Int, amount: Int)
  final case class Item(name: String)

  implicit val itemCodec: Codec[Item] = deriveCodec[Item]
  implicit val itemInfoCodec: Codec[ItemInfo] = deriveCodec[ItemInfo]
  implicit val customerInfoCodec: Codec[CustomerInfo] =
    deriveCodec[CustomerInfo]
  implicit val orderCodec: Codec[Order] = deriveCodec[Order]
}
