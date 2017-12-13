package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.ccs.CommandIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.CommandResponse.{Completed, NoLongerValid}
import csw.messages.ccs.commands.{CommandResponse, Setup, WrappedComponent}
import csw.messages.models.PubSub
import csw.trombone.assembly.actors.TromboneState.{TromboneState, _}
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext}

import scala.concurrent.Future

class FollowCommand(ctx: ActorContext[AssemblyCommandHandlerMsgs],
                    ac: AssemblyContext,
                    s: Setup,
                    tromboneHCD: Option[WrappedComponent],
                    startState: TromboneState,
                    stateActor: ActorRef[PubSub[AssemblyState]])
    extends AssemblyCommand(ctx, startState, stateActor) {

  import ctx.executionContext

  override def startCommand(): Future[CommandResponse] = {
    if (startState.cmdChoice == cmdUninitialized
        || startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving
        || !startState.sodiumLayerValue) {
      Future(
        NoLongerValid(
          s.runId,
          WrongInternalStateIssue(
            s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice}/${startState.sodiumLayerValue} does not allow follow"
          )
        )
      )
    } else {
      publishState(
        TromboneState(cmdItem(cmdContinuous),
                      moveItem(moveMoving),
                      sodiumItem(startState.sodiumLayerValue),
                      nssItem(s(ac.nssInUseKey).head))
      )
      Future(Completed(s.runId))
    }
  }

  override def stopCommand(): Unit = {}

}
