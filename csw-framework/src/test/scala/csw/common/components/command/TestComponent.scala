package csw.common.components.command

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{Behavior, PostStop}
import csw.framework.models.CswContext
import csw.framework.scaladsl.TopLevelComponent._
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid, Started}
import csw.common.components.command.CommandComponentState._
import csw.framework.exceptions.FailureRestart
import csw.params.commands.CommandIssue
import csw.time.core.models.UTCTime

object TestComponent {

  case class MyFailure(msg: String) extends FailureRestart(s"Damn another Failure: + $msg")

  private case class MyInitState(val1: String, val2: String)

  def apply(cswCtx: CswContext): Behavior[InitializeMessage] = {
    import InitializeMessage._

    val log = cswCtx.loggerFactory.getLogger

    Behaviors
      .receiveMessage[InitializeMessage] {
        case Initialize(ref) =>
          log.info(s"Initializing TestComponent for: $ref")

          val runBehavior = running(cswCtx, MyInitState("kim", "gillies"))
          ref ! InitializeSuccess(runBehavior)
          log.info(s"TestComponent TLA sent InitializeSuccess to: $ref")
          Behaviors.same
      }
      .receiveSignal {
        case (_, signal) =>
          log.debug(s"TestComponent TLA Initialize actor got signal: $signal")
          Behaviors.same
      }
  }

  private def running(cswCtx: CswContext, myState: MyInitState): Behavior[RunningMessage] = {
    import RunningMessage._

    val log = cswCtx.loggerFactory.getLogger
    log.debug(s"MyState: $myState")

    // Context useful for creating workers, etc.
    Behaviors.setup { ctx =>
      log.debug(s"Running ctx: ${ctx.system.name}")
      Behaviors.receiveMessage[RunningMessage] {
        case Validate(runId, cmd, svr) =>
          log.info(s"TestComponent TLA received Validate with runId:name: $runId:$cmd")
          cmd.commandName match {
            case `invalidCmd` =>
              svr ! Invalid(runId, CommandIssue.OtherIssue("Invalid"))
            case `immediateCmd` =>
              svr ! Accepted(runId)
            case `longRunningCmd` =>
              svr ! Accepted(runId)
            case cmd =>
              svr ! Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"Nope: $cmd"))
          }
          Behaviors.same
        case Submit(runId, cmd, svr) =>
          log.info(s"TestComponent received Submit: id:name: $runId:$cmd")
          cmd.commandName match {
            case `immediateCmd` =>
              svr ! Completed(runId)
            case `longRunningCmd` =>
              cswCtx.timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(3))) {
                log.debug(s"TestComponent TLA sending Completed for longRunningCmd: $runId")
                svr ! Completed(runId)
              }
              log.debug(s"TestComponent TLA returning Started for longRunningCmd: $runId")
              svr ! Started(runId)
            case other =>
              log.error(s"TestComponent TLA received some other Submit: $other")
              svr ! Completed(runId)
          }
          Behaviors.same
        case Oneway(runId, cmd) =>
          log.info(s"TestComponent TLA received Oneway: id:name: $runId:$cmd")
          // No response needed
          Behaviors.same
        case Shutdown(svr) =>
          log.info("TestComponent TLA got Shutdown--responding success")
          svr ! ShutdownSuccessful
          Behaviors.same
        case GoOnline(svr) =>
          log.info("TLA got GoOnline")
          svr ! OnlineSuccess
          Behaviors.same
        case GoOffline(svr) =>
          log.info("TLA got GoOffline")
          svr ! OfflineSuccess
          Behaviors.same
        case DiagnosticMode(startTime, hint, svr) =>
          log.info(s"TLA got Diagnostic Mode: $startTime and $hint")
          svr ! DiagnosticModeSuccess
          Behaviors.same
        case TrackingEventReceived(trackingEvent) =>
          log.info(s"Tracking event received: $trackingEvent")
          Behaviors.same
      }.receiveSignal {
        case (_: ActorContext[RunningMessage], PostStop) =>
          log.debug(s"TestComponent TLA Running PostStop signal received")
          Behaviors.same
      }
    }
  }

}
