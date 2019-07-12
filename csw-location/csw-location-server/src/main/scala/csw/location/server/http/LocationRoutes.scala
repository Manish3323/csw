package csw.location.server.http

import akka.NotUsed
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.location.api.codec.DoneCodec
import csw.location.api.scaladsl.LocationService
import csw.location.model.codecs.LocationCodecs
import csw.location.model.scaladsl._
import csw.location.server.internal.ActorRuntime
import io.bullet.borer.Json

import scala.concurrent.duration.{Duration, DurationLong, FiniteDuration}

private[csw] class LocationRoutes(
    locationService: LocationService,
    locationExceptionHandler: LocationExceptionHandler,
    actorRuntime: ActorRuntime
) extends HttpCodecs
    with LocationCodecs
    with DoneCodec {

  import actorRuntime._

  val routes: Route = cors() {
    locationExceptionHandler.route {
      pathPrefix("location") {
        get {
          path("list") {
            parameters(("componentType".?, "connectionType".?, "hostname".?, "prefix".?)) {
              case (None, None, None, None) =>
                complete(locationService.list)
              case (Some(componentName), None, None, None) =>
                complete(locationService.list(ComponentType.withNameInsensitive(componentName)))
              case (None, Some(connectionType), None, None) =>
                complete(locationService.list(ConnectionType.withNameInsensitive(connectionType)))
              case (None, None, Some(hostname), None) =>
                complete(locationService.list(hostname))
              case (None, None, None, Some(prefix)) =>
                complete(locationService.listByPrefix(prefix))
              case _ =>
                throw new QueryFilterException(
                  "please provides exactly zero or one of the following filters: componentType, connectionType, hostname, prefix"
                )
            }
          } ~
          path("find" / Segment) { connectionName =>
            complete(
              locationService.find(Connection.from(connectionName).asInstanceOf[TypedConnection[Location]])
            )
          } ~
          path("resolve" / Segment) { connectionName =>
            parameter("within") { within =>
              val duration = Duration(within).asInstanceOf[FiniteDuration]
              complete {
                locationService.resolve(Connection.from(connectionName).asInstanceOf[TypedConnection[Location]], duration)
              }
            }
          } ~
          path("track" / Segment) { connectionName =>
            val connection = Connection.from(connectionName)
            val stream: Source[ServerSentEvent, NotUsed] = locationService
              .track(connection)
              .mapMaterializedValue(_ => NotUsed)
              .map(trackingEvent => ServerSentEvent(new String(Json.encode(trackingEvent).toByteArray), "uft8"))
              .keepAlive(2.second, () => ServerSentEvent.heartbeat)
            complete(stream)
          }
        } ~
        post {
          path("register") {
            entity(as[Registration]) { registration =>
              complete(locationService.register(registration).map(_.location))
            }
          } ~
          path("unregister") {
            entity(as[Connection]) { connection =>
              complete(locationService.unregister(connection))
            }
          } ~
          path("unregisterAll") {
            complete(locationService.unregisterAll())
          }
        }
      }
    }
  }
}
