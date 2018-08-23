package csw.services.event.internal.commons.javawrappers

import csw.services.event.api.javadsl.IEventService
import csw.services.event.api.scaladsl.EventService

import scala.concurrent.ExecutionContext

/**
 * Java API for [[csw.services.event.api.scaladsl.EventService]]
 */
class JEventService(eventService: EventService) extends IEventService {

  implicit val executionContext: ExecutionContext = eventService.executionContext

  override def makeNewPublisher(): JEventPublisher = new JEventPublisher(eventService.makeNewPublisher())

  override def makeNewSubscriber(): JEventSubscriber = new JEventSubscriber(eventService.defaultSubscriber)

  override def asScala: EventService = eventService
}
