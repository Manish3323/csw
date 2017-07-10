package csw.services.logging.scaladsl

import csw.services.logging.commons.Keys
import csw.services.logging.components.IrisSupervisorActor
import csw.services.logging.components.IrisSupervisorActor._
import csw.services.logging.internal.LoggingLevels
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.utils.LoggingTestSuite

class ActorLoggingTest extends LoggingTestSuite {
  private val irisActorRef =
    actorSystem.actorOf(IrisSupervisorActor.props(), name = "IRIS-Supervisor-Actor")

  def sendMessagesToActor() = {
    irisActorRef ! LogTrace
    irisActorRef ! LogDebug
    irisActorRef ! LogInfo
    irisActorRef ! LogWarn
    irisActorRef ! LogError
    irisActorRef ! LogFatal
    irisActorRef ! "Unknown"
    Thread.sleep(200)
  }

  // DEOPSCSW-116: Make log messages identifiable with components
  // DEOPSCSW-117: Provide unique name for each logging instance of components
  // DEOPSCSW-119: Associate source with each log message
  // DEOPSCSW-121: Define structured tags for log messages
  test("messages logged from actor should contain component name, file name, class name, line number and actor path") {

    sendMessagesToActor()

    // default log level for IrisSupervisorActor is ERROR in config
    var logMsgLineNumber = IrisSupervisorActor.ERROR_LINE_NO

    logBuffer.foreach { log ⇒
      log(Keys.COMPONENT_NAME) shouldBe IrisSupervisorActor.NAME
      log(Keys.ACTOR) shouldBe irisActorRef.path.toString
      log(Keys.FILE) shouldBe IrisSupervisorActor.FILE_NAME
      log(Keys.LINE) shouldBe logMsgLineNumber
      log(Keys.CLASS).toString shouldBe IrisSupervisorActor.CLASS_NAME
      logMsgLineNumber += 1
    }
  }

  // DEOPSCSW-115: Format and control logging content
  // DEOPSCSW-121: Define structured tags for log messages
  test("message logged with custom Map properties should get logged") {
    irisActorRef ! "Unknown"
    Thread.sleep(200)

    val errorLevelLogMessages =
      logBuffer.groupBy(json ⇒ json(Keys.SEVERITY))("ERROR")
    errorLevelLogMessages.size shouldEqual 1
    val expectedMessage =
      Map(Keys.MSG -> "Unknown message received", "reason" -> "Unknown", "actorRef" → irisActorRef.toString)
    errorLevelLogMessages.head(Keys.MESSAGE) shouldBe expectedMessage
  }

  // DEOPSCSW-126 : Configurability of logging characteristics for component / log instance
  test("should load default filter provided in configuration file and applied to actor messages") {

    sendMessagesToActor()
    //  IrisSupervisorActor is logging 7 messages
    //  As per the filter, hcd should log 3 message of level ERROR and FATAL
    val groupByComponentNamesLog =
      logBuffer.groupBy(json ⇒ json(Keys.COMPONENT_NAME).toString)
    val irisLogs = groupByComponentNamesLog(IrisSupervisorActor.NAME)

    irisLogs.size shouldBe 3

    // check that log level should be greater than or equal to debug and
    // assert on actual log message
    irisLogs.toList.foreach { log ⇒
      log.contains(Keys.ACTOR) shouldBe true
      val currentLogLevel = log(Keys.SEVERITY).toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.ERROR shouldBe true
    }

  }
}
