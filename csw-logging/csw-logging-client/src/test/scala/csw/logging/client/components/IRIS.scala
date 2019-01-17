package csw.logging.client.components

import akka.actor.{Actor, Props}
import csw.logging.client.components.IRIS._
import csw.logging.api.scaladsl._
import csw.logging.client.scaladsl.{GenericLoggerFactory, LoggerFactory}

// DEOPSCSW-316: Improve Logger accessibility for component developers
object IRISLibraryLogger extends LoggerFactory(IRIS.COMPONENT_NAME)

class IRIS(logger: LoggerFactory) extends Actor {

  // DEOPSCSW-316: Improve Logger accessibility for component developers
  private val log: Logger = logger.getLogger(context)

  // Do not add any lines before this method
  // Tests are written to assert on this line numbers
  // In case any line needs to be added then update constants in companion object
  def receive: PartialFunction[Any, Unit] = {
    case LogTrace => log.trace(irisLogs("trace"))
    case LogDebug => log.debug(irisLogs("debug"))
    case LogInfo  => log.info(irisLogs("info"))
    case LogWarn  => log.warn(irisLogs("warn"))
    case LogError => log.error(irisLogs("error"))
    case LogFatal => log.fatal(irisLogs("fatal"))
    case x: Any   => log.error("Unknown message received", Map("reason" -> x, "actorRef" → self.toString))
  }
}

object IRIS {

  val TRACE_LINE_NO = 20
  val DEBUG_LINE_NO = TRACE_LINE_NO + 1
  val INFO_LINE_NO  = TRACE_LINE_NO + 2
  val WARN_LINE_NO  = TRACE_LINE_NO + 3
  val ERROR_LINE_NO = TRACE_LINE_NO + 4
  val FATAL_LINE_NO = TRACE_LINE_NO + 5
  val ANY_LINE_NO   = TRACE_LINE_NO + 6

  val COMPONENT_NAME = "IRIS"
  val CLASS_NAME     = "csw.logging.client.components.IRIS"
  val FILE_NAME      = "IRIS.scala"

  case object LogTrace
  case object LogDebug
  case object LogInfo
  case object LogWarn
  case object LogError
  case object LogFatal

  def props(componentName: String) = Props(new IRIS(new LoggerFactory(componentName)))

  val irisLogs = Map(
    "trace" → "iris: trace",
    "debug" → "iris: debug",
    "info"  → "iris: info",
    "warn"  → "iris: warn",
    "error" → "iris: error",
    "fatal" → "iris: fatal"
  )
}

class IrisTLA {
  import IRIS._

  // DEOPSCSW-316: Improve Logger accessibility for component developers
  val log: Logger = IRISLibraryLogger.getLogger

  def startLogging(): Unit = {
    log.trace(irisLogs("trace"))
    log.debug(irisLogs("debug"))
    log.info(irisLogs("info"))
    log.warn(irisLogs("warn"))
    log.error(irisLogs("error"))
    log.fatal(irisLogs("fatal"))
  }
}

class IrisUtil {

  // DEOPSCSW-316: Improve Logger accessibility for component developers
  val log: Logger = GenericLoggerFactory.getLogger

  def startLogging(logs: Map[String, String]): Unit = {
    log.trace(irisLogs("trace"))
    log.debug(irisLogs("debug"))
    log.info(irisLogs("info"))
    log.warn(irisLogs("warn"))
    log.error(irisLogs("error"))
    log.fatal(irisLogs("fatal"))
  }
}

class IrisActorUtil extends Actor {

  // DEOPSCSW-316: Improve Logger accessibility for component developers
  val log: Logger = GenericLoggerFactory.getLogger(context)

  def receive = {
    case LogTrace => log.trace(irisLogs("trace"))
    case LogDebug => log.debug(irisLogs("debug"))
    case LogInfo  => log.info(irisLogs("info"))
    case LogWarn  => log.warn(irisLogs("warn"))
    case LogError => log.error(irisLogs("error"))
    case LogFatal => log.fatal(irisLogs("fatal"))
    case x: Any   => log.error("Unknown message received", Map("reason" -> x, "actorRef" → self.toString))
  }
}

object IrisActorUtil {
  def props: Props = Props(new IrisActorUtil)
}
