package csw.common.framework.javadsl.assembly

import java.util.Optional
import java.util.concurrent.CompletableFuture

import akka.typed.ActorRef
import akka.typed.javadsl.ActorContext
import csw.common.ccs.{CommandStatus, Validation}
import csw.common.framework.models.Component.AssemblyInfo
import csw.common.framework.models._
import csw.common.framework.scaladsl.assembly.AssemblyActor
import csw.param.Parameters

import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class JAssemblyActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[AssemblyMsg],
                                                          assemblyInfo: AssemblyInfo,
                                                          supervisor: ActorRef[AssemblyComponentLifecycleMessage])
    extends AssemblyActor[Msg](ctx.asScala, assemblyInfo, supervisor) {

  def jInitialize(): CompletableFuture[Unit]
  def jOnRun(): Unit
  def jSetup(s: Parameters.Setup,
             commandOriginator: Optional[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation
  def jObserve(o: Parameters.Observe, replyTo: Optional[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation
  def jOnDomainMsg(msg: Msg): Unit
  def jOnLifecycle(message: ToComponentLifecycleMessage): Unit

  implicit val ec                         = ctx.getExecutionContext
  override def initialize(): Future[Unit] = jInitialize().toScala.map(_ ⇒ ())

  override def onRun(): Unit = jOnRun()

  override def setup(s: Parameters.Setup,
                     commandOriginator: Option[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation =
    jSetup(s, commandOriginator.asJava)

  override def observe(o: Parameters.Observe,
                       replyTo: Option[ActorRef[CommandStatus.CommandResponse]]): Validation.Validation =
    jObserve(o, replyTo.asJava)

  override def onDomainMsg(msg: Msg): Unit = jOnDomainMsg(msg)

  override def onLifecycle(message: ToComponentLifecycleMessage): Unit = jOnLifecycle(message)

}
