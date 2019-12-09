package csw.admin.server.log.http

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Directive1, Directives, MalformedQueryParamRejection}
import csw.admin.server.commons.AdminLogger
import csw.logging.api.scaladsl.Logger
import csw.logging.models.Level

trait HttpParameter extends Directives {

  val log: Logger = AdminLogger.getLogger

  val logLevelParam: Directive1[Level] = parameter("value").flatMap {
    case value if Level.hasLevel(value) => provide(Level(value))
    case _ =>
      reject(MalformedQueryParamRejection("value", s"Supported logging levels are [${Level.stringify()}]"))
  }

  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  private def logRequest(req: HttpRequest): Unit =
    log.info(
      "HTTP Request received",
      Map("url" -> req.uri.toString(), "method" -> req.method.value, "headers" -> req.headers.mkString(","))
    )
}
