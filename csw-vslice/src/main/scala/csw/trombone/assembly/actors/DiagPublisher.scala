package csw.trombone.assembly.actors

import java.time.Instant
import java.util.Optional

import akka.actor.Cancellable
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.messages.CommandMessage.Oneway
import csw.messages.ccs.commands.{CommandName, CommandResponse, ComponentRef, Setup}
import csw.messages.models.PubSub
import csw.messages.params.models.Prefix
import csw.messages.params.states.CurrentState
import csw.trombone.assembly.DiagPublisherMessages._
import csw.trombone.assembly.TrombonePublisherMsg.{AxisStateUpdate, AxisStatsUpdate}
import csw.trombone.assembly.actors.DiagPublisher.Mode.{Diagnostic, Operations}
import csw.trombone.assembly.actors.DiagPublisher.{Mode, _}
import csw.trombone.assembly.{AssemblyContext, DiagPublisherMessages, TrombonePublisherMsg}
import csw.trombone.hcd.TromboneHcdState

import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.concurrent.duration.DurationDouble

object DiagPublisher {

  def make(
      assemblyContext: AssemblyContext,
      runningIn: Option[ComponentRef],
      eventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): Behavior[DiagPublisherMessages] =
    Actor.mutable(ctx ⇒ new DiagPublisher(ctx, assemblyContext, runningIn, eventPublisher))

  def jMake(
      assemblyContext: AssemblyContext,
      runningIn: Optional[ComponentRef],
      eventPublisher: Optional[ActorRef[TrombonePublisherMsg]]
  ): Behavior[DiagPublisherMessages] =
    Actor.mutable(ctx ⇒ new DiagPublisher(ctx, assemblyContext, runningIn.asScala, eventPublisher.asScala))

  sealed trait Mode
  object Mode {
    case object Operations extends Mode
    case object Diagnostic extends Mode
  }

  val diagnosticSkipCount       = 2
  val operationsSkipCount       = 5
  val diagnosticAxisStatsPeriod = 1
}

class DiagPublisher(
    ctx: ActorContext[DiagPublisherMessages],
    assemblyContext: AssemblyContext,
    runningIn: Option[ComponentRef],
    eventPublisher: Option[ActorRef[TrombonePublisherMsg]]
) extends MutableBehavior[DiagPublisherMessages] {

  val currentStateAdapter: ActorRef[CurrentState] = ctx.spawnAdapter(CurrentStateE)

  val pubSubRef: ActorRef[PubSub[CurrentState]] = ctx.system.deadLetters
  var stateMessageCounter: Int                  = 0
  var running: Option[ComponentRef]             = runningIn
  var context: Mode                             = _
  var cancelToken: Cancellable                  = _

  pubSubRef ! PubSub.Subscribe(currentStateAdapter)

  override def onMessage(msg: DiagPublisherMessages): Behavior[DiagPublisherMessages] = {
    context match {
      case Mode.Operations ⇒ operationsReceive(msg)
      case Mode.Diagnostic ⇒ diagnosticReceive(msg)
    }
    this
  }

  def operationsReceive(msg: DiagPublisherMessages): Unit = msg match {
    case CurrentStateE(cs) if cs.prefix == TromboneHcdState.axisStateCK =>
      if (stateMessageCounter % operationsSkipCount == 0) {
        publishStateUpdate(cs)
        stateMessageCounter = stateMessageCounter + 1
      }

    case CurrentStateE(cs) if cs.prefix == TromboneHcdState.axisStatsCK => // No nothing
    case TimeForAxisStats(_)                                            => // Do nothing, here so it doesn't paramFormat an error
    case OperationsState                                                => // Already in operations mode

    case DiagnosticState =>
      val cancelToken: Cancellable = ctx.schedule(
        Instant.now().plusSeconds(diagnosticAxisStatsPeriod).toEpochMilli.millis,
        ctx.self,
        TimeForAxisStats(diagnosticAxisStatsPeriod)
      )
      this.cancelToken = cancelToken
      context = Diagnostic

    case UpdateTromboneHcd(maybeRunning) =>
      this.running = maybeRunning
  }

  def diagnosticReceive(msg: DiagPublisherMessages): Unit = msg match {
    case CurrentStateE(cs) if cs.prefix == TromboneHcdState.axisStateCK =>
      if (stateMessageCounter % diagnosticSkipCount == 0) {
        publishStateUpdate(cs)
        stateMessageCounter = stateMessageCounter + 1
      }

    case CurrentStateE(cs) if cs.prefix == TromboneHcdState.axisStatsCK =>
      publishStatsUpdate(cs)

    case TimeForAxisStats(periodInSeconds) =>
      running.foreach(
        _.value ! Oneway(Setup(Prefix(""), CommandName("GetAxisStats"), None),
                         TestProbe[CommandResponse]()(ctx.system, TestKitSettings(ctx.system)).ref)
      )
      val canceltoken: Cancellable =
        ctx.schedule(Instant.now().plusSeconds(periodInSeconds).toEpochMilli.millis, ctx.self, TimeForAxisStats(periodInSeconds))
      this.cancelToken = canceltoken

    case DiagnosticState => // Do nothing, already in this mode

    case OperationsState =>
      cancelToken.cancel
      context = Operations

    case UpdateTromboneHcd(maybeRunning) =>
      running = maybeRunning
  }

  private def publishStateUpdate(cs: CurrentState): Unit = {
    eventPublisher.foreach(
      _ ! AxisStateUpdate(
        cs(TromboneHcdState.axisNameKey),
        cs(TromboneHcdState.positionKey),
        cs(TromboneHcdState.stateKey),
        cs(TromboneHcdState.inLowLimitKey),
        cs(TromboneHcdState.inHighLimitKey),
        cs(TromboneHcdState.inHomeKey)
      )
    )
  }

  private def publishStatsUpdate(cs: CurrentState): Unit = {
    eventPublisher.foreach(
      _ ! AxisStatsUpdate(
        cs(TromboneHcdState.axisNameKey),
        cs(TromboneHcdState.datumCountKey),
        cs(TromboneHcdState.moveCountKey),
        cs(TromboneHcdState.homeCountKey),
        cs(TromboneHcdState.limitCountKey),
        cs(TromboneHcdState.successCountKey),
        cs(TromboneHcdState.failureCountKey),
        cs(TromboneHcdState.cancelCountKey)
      )
    )
  }
}
