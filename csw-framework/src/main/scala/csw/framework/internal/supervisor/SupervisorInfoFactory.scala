package csw.framework.internal.supervisor

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import csw.framework.internal.wiring.CswFrameworkSystem
import csw.framework.models.CswContext
import csw.messages.ContainerIdleMessage
import csw.messages.framework.{Component, ComponentInfo, SupervisorInfo}
import csw.services.location.api.scaladsl.LocationService
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.event.EventServiceFactory
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.scaladsl.RegistrationFactory
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.async.Async._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

/**
 * The factory for creating supervisor actors of a component specified by [[csw.messages.framework.ComponentInfo]]
 */
private[framework] class SupervisorInfoFactory(containerName: String) {
  private val log: Logger = new LoggerFactory(containerName).getLogger

  def make(
      containerRef: ActorRef[ContainerIdleMessage],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      eventServiceFactory: EventServiceFactory,
      alarmServiceFactory: AlarmServiceFactory,
      registrationFactory: RegistrationFactory
  ): Future[Option[SupervisorInfo]] = {
    implicit val system: ActorSystem          = ActorSystemFactory.remote(s"${componentInfo.name}-system")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val richSystem                            = new CswFrameworkSystem(system)

    async {
      val cswCtxF = CswContext.make(locationService, eventServiceFactory, alarmServiceFactory, componentInfo)(richSystem)
      val supervisorBehavior =
        SupervisorBehaviorFactory.make(Some(containerRef), registrationFactory, await(cswCtxF))
      val actorRefF = richSystem.spawnTyped(supervisorBehavior, componentInfo.name)
      Some(SupervisorInfo(system, Component(await(actorRefF), componentInfo)))
    } recoverWith {
      case NonFatal(exception) ⇒
        async {
          log.error(s"Exception :[${exception.getMessage}] occurred while spawning supervisor: [${componentInfo.name}]",
                    ex = exception)
          await(system.terminate())
          None
        }
    }
  }
}
