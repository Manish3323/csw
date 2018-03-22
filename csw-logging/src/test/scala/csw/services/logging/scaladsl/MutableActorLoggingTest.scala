package csw.services.logging.scaladsl

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.services.logging.LogCommand
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.LogCommand._
import csw.services.logging.utils.LoggingTestSuite

object TromboneMutableActor {
  def beh(componentName: String): Behavior[LogCommand] =
    Behaviors.mutable[LogCommand](ctx ⇒ new TromboneMutableActor(ctx, new LoggerFactory(componentName)))
}

class TromboneMutableActor(
    ctx: ActorContext[LogCommand],
    loggerFactory: LoggerFactory
) extends Behaviors.MutableBehavior[LogCommand] {
  override def onMessage(msg: LogCommand): Behavior[LogCommand] = {

    val log: Logger = loggerFactory.getLogger(ctx)

    msg match {
      case LogTrace => log.trace("Level is trace")
      case LogDebug => log.debug("Level is debug")
      case LogInfo  => log.info("Level is info")
      case LogWarn  => log.warn("Level is warn")
      case LogError => log.error("Level is error")
      case LogFatal => log.fatal("Level is fatal")
      case Unknown  => log.error("Unexpected actor message", Map("message" -> Unknown))
    }
    this
  }
}

// DEOPSCSW-280 SPIKE: Introduce Akkatyped in logging
class MutableActorLoggingTest extends LoggingTestSuite {
  import akka.actor.typed.scaladsl.adapter._

  private val tromboneActorRef =
    actorSystem.spawn(TromboneMutableActor.beh("tromboneMutableHcdActor"), "TromboneMutableActor")

  def sendMessagesToActor(): Unit = {
    tromboneActorRef ! LogTrace
    tromboneActorRef ! LogDebug
    tromboneActorRef ! LogInfo
    tromboneActorRef ! LogWarn
    tromboneActorRef ! LogError
    tromboneActorRef ! LogFatal
    tromboneActorRef ! Unknown
    Thread.sleep(200)
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-117: Provide unique name for each logging instance of components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("messages logged from actor should contain component name, file name, class name, line number and actor path") {

    sendMessagesToActor()

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe true
      log.contains("actor") shouldBe true
      log("@componentName") shouldBe "tromboneMutableHcdActor"
      log("actor") shouldBe tromboneActorRef.path.toString
      log("file") shouldBe "MutableActorLoggingTest.scala"
      log.contains("line") shouldBe true
      log("class") shouldBe "csw.services.logging.scaladsl.TromboneMutableActor"
    }
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to actor messages") {

    sendMessagesToActor()
    //  TromboneHcd component is logging 7 messages
    //  As per the filter, hcd should log 3 message of level ERROR and FATAL
    val groupByComponentNamesLog =
      logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneHcdLogs = groupByComponentNamesLog("tromboneMutableHcdActor")

    tromboneHcdLogs.size shouldBe 3

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    tromboneHcdLogs.toList.foreach { log ⇒
      log.contains("actor") shouldBe true
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.ERROR shouldBe true
    }

  }

}
