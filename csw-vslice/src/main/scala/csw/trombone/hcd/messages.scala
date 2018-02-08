package csw.trombone.hcd

import akka.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.RunningMessage.DomainMessage
import csw.trombone.hcd.AxisResponse.{AxisStatistics, AxisUpdate}

sealed trait SimulatorCommand extends TMTSerializable

sealed trait AxisRequest extends SimulatorCommand
object AxisRequests {
  case object Home                                            extends AxisRequest
  case object Datum                                           extends AxisRequest
  case class Move(position: Int, diagFlag: Boolean = false)   extends AxisRequest
  case object CancelMove                                      extends AxisRequest
  case class GetStatistics(replyTo: ActorRef[AxisStatistics]) extends AxisRequest
  case object PublishAxisUpdate                               extends AxisRequest
  case class InitialState(replyTo: ActorRef[AxisUpdate])      extends AxisRequest

  def jHome(): AxisRequest       = Home
  def jDatum(): AxisRequest      = Datum
  def jCancelMove(): AxisRequest = CancelMove
}

// Internal
sealed trait InternalMessages extends SimulatorCommand
object InternalMessages {
  case object DatumComplete              extends InternalMessages
  case class HomeComplete(position: Int) extends InternalMessages
  case class MoveComplete(position: Int) extends InternalMessages
  case object InitialStatistics          extends InternalMessages
}

sealed trait MotionWorkerMsgs extends SimulatorCommand

object MotionWorkerMsgs {
  case class Start(replyTo: ActorRef[MotionWorkerMsgs]) extends MotionWorkerMsgs
  case class End(finalpos: Int)                         extends MotionWorkerMsgs
  case class Tick(current: Int)                         extends MotionWorkerMsgs
  case class MoveUpdate(destination: Int)               extends MotionWorkerMsgs
  case object Cancel                                    extends MotionWorkerMsgs
}

////////////////

sealed trait TromboneMessage

sealed trait TromboneEngineering extends TromboneMessage
object TromboneEngineering {
  case object GetAxisStats                                     extends TromboneEngineering
  case object GetAxisUpdate                                    extends TromboneEngineering
  case class GetAxisUpdateNow(replyTo: ActorRef[AxisResponse]) extends TromboneEngineering
  case object GetAxisConfig                                    extends TromboneEngineering
}

sealed trait AxisResponse extends TromboneMessage
object AxisResponse {
  case object AxisStarted                                extends AxisResponse
  case class AxisFinished(newRef: ActorRef[AxisRequest]) extends AxisResponse
  case class AxisUpdate(
      axisName: String,
      state: AxisState,
      current: Int,
      inLowLimit: Boolean,
      inHighLimit: Boolean,
      inHomed: Boolean
  ) extends AxisResponse
  case class AxisFailure(reason: String) extends AxisResponse
  case class AxisStatistics(
      axisName: String,
      initCount: Int,
      moveCount: Int,
      homeCount: Int,
      limitCount: Int,
      successCount: Int,
      failureCount: Int,
      cancelCount: Int
  ) extends AxisResponse {
    override def toString =
      s"name: $axisName, inits: $initCount, moves: $moveCount, homes: $homeCount, limits: $limitCount, success: $successCount, fails: $failureCount, cancels: $cancelCount"
  }
}
