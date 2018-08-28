package csw.services.alarm.client.internal.auto_refresh

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps

import scala.concurrent.duration.FiniteDuration

class AutoRefreshSeverityActorFactory {
  def make(alarm: Refreshable,
           refreshInterval: FiniteDuration)(implicit actorSystem: ActorSystem): ActorRef[AutoRefreshSeverityMessage] =
    actorSystem.spawnAnonymous(
      Behaviors.withTimers[AutoRefreshSeverityMessage](AutoRefreshSeverityActor.behavior(_, alarm, refreshInterval))
    )
}
