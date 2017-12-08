package csw.common.components.command

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.common.components.command.ComponentStateForCommand._
import csw.common.components.command.TopLevelActorDomainMessage.CommandCompleted
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed}
import csw.messages.ccs.commands.{CommandResponse, ControlCommand, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{AkkaLocation, TrackingEvent}
import csw.messages.models.PubSub
import csw.messages.models.PubSub.Publish
import csw.messages.params.models.RunId
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage, TopLevelActorMessage}
import csw.services.ccs.common.ActorRefExts.RichComponentActor
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class McsAssemblyComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PubSub.PublisherMessage[CurrentState]],
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers[TopLevelActorDomainMessage](
      ctx,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService,
      loggerFactory
    ) {

  implicit val timeout: Timeout          = 10.seconds
  implicit val scheduler: Scheduler      = ctx.system.scheduler
  implicit val ec: ExecutionContext      = ctx.executionContext
  var completedCommands: Int             = 0
  var hcdRef: ActorRef[ComponentMessage] = _
  var commandId: RunId                   = _
  var shortSetup: Setup                  = _
  var mediumSetup: Setup                 = _
  var longSetup: Setup                   = _

  override def initialize(): Future[Unit] =
    componentInfo.connections.headOption match {
      case Some(hcd) ⇒
        locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
          case Some(akkaLocation) ⇒ hcdRef = akkaLocation.componentRef()
          case None               ⇒ throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
        }
      case None ⇒ Future.successful(Unit)
    }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = Unit

  override def onDomainMsg(msg: TopLevelActorDomainMessage): Unit = msg match {
    case CommandCompleted(response) ⇒
      response.runId match {
        case id if id == shortSetup.runId ⇒
          pubSubRef ! Publish(CurrentState(shortSetup.prefix, Set(choiceKey.set(shortCmdCompleted))))
        case id if id == mediumSetup.runId ⇒
          pubSubRef ! Publish(CurrentState(mediumSetup.prefix, Set(choiceKey.set(mediumCmdCompleted))))
        case id if id == longSetup.runId ⇒
          pubSubRef ! Publish(CurrentState(longSetup.prefix, Set(choiceKey.set(longCmdCompleted))))
      }

      completedCommands += 1
      if (completedCommands == 3) commandResponseManager ! AddOrUpdateCommand(commandId, Completed(commandId))
    case _ ⇒
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand.prefix match {
      case `longRunningCmdPrefix` ⇒ Accepted(controlCommand.runId)
      case _                      ⇒ CommandResponse.Error(controlCommand.runId, "")
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    commandId = controlCommand.runId
    shortSetup = Setup(`prefix`, `shortRunningCmdPrefix`, controlCommand.maybeObsId)
    mediumSetup = Setup(`prefix`, `mediumRunningCmdPrefix`, controlCommand.maybeObsId)
    longSetup = Setup(`prefix`, `longRunningCmdPrefix`, controlCommand.maybeObsId)

    // this is to simulate that assembly is splitting command into three sub commands and forwarding same to hcd
    // longSetup takes 5 seconds to finish
    // shortSetup takes 1 second to finish
    // mediumSetup takes 3 seconds to finish
    processCommand(longSetup)
    processCommand(shortSetup)
    processCommand(mediumSetup)
  }

  private def processCommand(controlCommand: ControlCommand) = {
    hcdRef
      .submit(controlCommand)
      .map {
        case _: Accepted ⇒
          hcdRef.getCommandResponse(controlCommand.runId).map {
            case _: Completed ⇒ ctx.self ! CommandCompleted(Completed(controlCommand.runId))
            case _            ⇒ // Do nothing
          }
        case _ ⇒ // Do nothing
      }
  }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = Future.successful(Unit)

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
