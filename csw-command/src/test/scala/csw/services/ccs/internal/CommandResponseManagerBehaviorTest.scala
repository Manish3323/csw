package csw.services.ccs.internal

import akka.actor.ActorSystem
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.testkit.{StubbedActorContext, TestKitSettings}
import akka.{actor, typed}
import csw.messages.CommandResponseManagerMessage
import csw.messages.CommandResponseManagerMessage._
import csw.messages.ccs.commands.CommandResponse
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed, Error}
import csw.messages.params.models.Id
import csw.services.logging.scaladsl.{Logger, LoggerFactory}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-207: Report on Configuration Command Completion
// DEOPSCSW-208: Report failure on Configuration Completion command
class CommandResponseManagerBehaviorTest extends FunSuite with Matchers with MockitoSugar {

  private val actorSystem                        = ActorSystem("test-command-status-service-system")
  implicit val typedSystem: typed.ActorSystem[_] = actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private val ctx: StubbedActorContext[CommandResponseManagerMessage] =
    new StubbedActorContext[CommandResponseManagerMessage]("ctx-command-status-service", 100, typedSystem)

  def createCommandStatusService(): CommandResponseManagerBehavior =
    new CommandResponseManagerBehavior(ctx, getMockedLogger)

  test("should be able to add command entry in Command Response Manager") {
    val commandStatusService = createCommandStatusService()

    val runId = Id()
    commandStatusService.onMessage(AddOrUpdateCommand(runId, Accepted(runId)))

    commandStatusService.commandStatus.get(runId) shouldBe Accepted(runId)
    commandStatusService.commandCoRelation.parentToChildren shouldBe empty
    commandStatusService.commandCoRelation.childToParent.isEmpty shouldBe true
  }

  test("should be able to add correlation between parent and child via AddSubCommand") {
    val commandStatusService = createCommandStatusService()
    val parentId             = Id()
    val childId              = Id()

    commandStatusService.onMessage(AddOrUpdateCommand(parentId, Accepted(parentId)))
    commandStatusService.onMessage(AddSubCommand(parentId, childId))

    commandStatusService.commandCoRelation.childToParent(childId) shouldBe parentId
    commandStatusService.commandCoRelation.parentToChildren(parentId) shouldBe Set(childId)
  }

  test("should be able to add subscriber and publish current state to newly added subscriber") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val runId                = Id()

    commandStatusService.onMessage(AddOrUpdateCommand(runId, Completed(runId)))
    commandStatusService.commandStatus.cmdToCmdStatus(runId).subscribers shouldBe empty

    commandStatusService.onMessage(Subscribe(runId, commandResponseProbe.ref))
    commandStatusService.commandStatus.cmdToCmdStatus(runId).subscribers should contain(commandResponseProbe.ref)
    commandResponseProbe.expectMsg(Completed(runId))
  }

  test("should be able to remove subscriber") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val runId                = Id()

    commandStatusService.onMessage(AddOrUpdateCommand(runId, Accepted(runId)))

    commandStatusService.onMessage(Subscribe(runId, commandResponseProbe.ref))
    commandStatusService.commandStatus.cmdToCmdStatus(runId).subscribers should contain(commandResponseProbe.ref)

    commandStatusService.onMessage(Unsubscribe(runId, commandResponseProbe.ref))
    commandStatusService.commandStatus.cmdToCmdStatus(runId).subscribers should not contain commandResponseProbe.ref
  }

  test("should be able to get current status of command on Query message") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val runId                = Id()

    commandStatusService.onMessage(AddOrUpdateCommand(runId, Accepted(runId)))

    commandStatusService.onMessage(Query(runId, commandResponseProbe.ref))
    commandResponseProbe.expectMsg(Accepted(runId))
  }

  test("should be able to update and publish command status to all subscribers") {
    val commandStatusService  = createCommandStatusService()
    val commandResponseProbe1 = TestProbe[CommandResponse]
    val commandResponseProbe2 = TestProbe[CommandResponse]
    val runId                 = Id()

    commandStatusService.onMessage(AddOrUpdateCommand(runId, Accepted(runId)))
    commandStatusService.onMessage(Subscribe(runId, commandResponseProbe1.ref))
    commandStatusService.onMessage(Subscribe(runId, commandResponseProbe2.ref))

    commandStatusService.onMessage(AddOrUpdateCommand(runId, Completed(runId)))

    commandStatusService.commandStatus
      .cmdToCmdStatus(runId)
      .commandStatus
      .currentCmdStatus shouldBe Completed(runId)

    commandResponseProbe1.expectMsg(Completed(runId))
    commandResponseProbe2.expectMsg(Completed(runId))
  }

  test("should be able to infer command status when status of sub command is updated") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val commandId            = Id()
    val subCommandId         = Id()

    commandStatusService.onMessage(AddOrUpdateCommand(commandId, Accepted(commandId)))
    commandStatusService.onMessage(Subscribe(commandId, commandResponseProbe.ref))

    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId))

    commandStatusService.onMessage(UpdateSubCommand(subCommandId, Completed(subCommandId)))

    // Update of a sub command status(above) should update the status of parent command
    commandResponseProbe.expectMsg(Completed(commandId))
  }

  // DEOPSCSW-208: Report failure on Configuration Completion command
  test("should be able to update command status with the status of subcommand if one of the subcommand fails") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val commandId            = Id()
    val subCommandId1        = Id()
    val subCommandId2        = Id()

    commandStatusService.onMessage(AddOrUpdateCommand(commandId, Accepted(commandId)))
    commandStatusService.onMessage(Subscribe(commandId, commandResponseProbe.ref))

    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId1))
    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId2))

    commandStatusService.onMessage(UpdateSubCommand(subCommandId1, Error(subCommandId1, "Sub command 1 failed")))
    commandStatusService.onMessage(UpdateSubCommand(subCommandId2, Completed(subCommandId2)))

    // Update of a failed sub command status(above) should update the status of parent command as failed irrespective
    // of the result of other sub command
    commandResponseProbe.expectMsg(Error(commandId, "Sub command 1 failed"))
  }

  // DEOPSCSW-207: Report on Configuration Command Completion
  test("should be able to update successful command status when all the subcommand completes with success") {
    val commandStatusService = createCommandStatusService()
    val commandResponseProbe = TestProbe[CommandResponse]
    val commandId            = Id()
    val subCommandId1        = Id()
    val subCommandId2        = Id()

    commandStatusService.onMessage(AddOrUpdateCommand(commandId, Accepted(commandId)))
    commandStatusService.onMessage(Subscribe(commandId, commandResponseProbe.ref))

    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId1))
    commandStatusService.onMessage(AddSubCommand(commandId, subCommandId2))

    // Update status of sub command 1 as completed
    commandStatusService.onMessage(UpdateSubCommand(subCommandId1, Completed(subCommandId1)))

    // Status update of sub command 1 does not make the parent command complete
    commandStatusService.onMessage(Query(commandId, commandResponseProbe.ref))
    commandResponseProbe.expectMsg(Accepted(commandId))

    // Update status of sub command 2 with some intermediate status
    commandStatusService.onMessage(UpdateSubCommand(subCommandId2, Accepted(subCommandId2)))

    // Status update of sub command 2  with intermediate does not make the parent command complete
    commandStatusService.onMessage(Query(commandId, commandResponseProbe.ref))
    commandResponseProbe.expectMsg(Accepted(commandId))

    // Update status of sub command 2 as completed
    commandStatusService.onMessage(UpdateSubCommand(subCommandId2, Completed(subCommandId2)))

    // Update of final sub command as Completed where other sub commands have completed earlier
    // should update the status of parent command as Completed
    commandResponseProbe.expectMsg(Completed(commandId))
  }

  private def getMockedLogger: LoggerFactory = {
    val loggerFactory: LoggerFactory = mock[LoggerFactory]
    val logger: Logger               = mock[Logger]

    when(loggerFactory.getLogger).thenReturn(logger)
    when(loggerFactory.getLogger(any[actor.ActorContext])).thenReturn(logger)
    when(loggerFactory.getLogger(any[ActorContext[_]])).thenReturn(logger)

    loggerFactory
  }
}
