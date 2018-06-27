package csw.services.event.internal.redis

import akka.stream.Materializer
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.{ExecutionContext, Future}

class RedisEventService(eventServiceResolver: EventServiceResolver, masterId: String, redisClient: RedisClient)(
    implicit val executionContext: ExecutionContext,
    mat: Materializer
) extends EventService {

  override def makeNewPublisher(): Future[EventPublisher] = redisURI().map(new RedisPublisher(_, redisClient))

  override def makeNewSubscriber(): Future[EventSubscriber] = redisURI().map(new RedisSubscriber(_, redisClient))

  // resolve event service every time before creating a new publisher or subscriber
  private def redisURI(): Future[RedisURI] =
    eventServiceResolver.uri().map(uri ⇒ RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build())

}
