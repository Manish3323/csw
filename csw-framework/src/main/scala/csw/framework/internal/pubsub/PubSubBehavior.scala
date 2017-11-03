package csw.framework.internal.pubsub

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior, Signal, Terminated}
import csw.messages.PubSub
import csw.messages.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.services.logging.scaladsl.ComponentLogger

/**
 * The actor which can be used by a component to publish its data of a given type, to all the components who subscribe
 * @param ctx             The Actor Context under which the actor instance of this behavior is created
 * @param componentName   The name of the component using this actor for publishing its data
 * @tparam T              The type of the data which will be published or subscribed to using this actor
 */
class PubSubBehavior[T](ctx: ActorContext[PubSub[T]], componentName: String)
    extends ComponentLogger.MutableActor[PubSub[T]](ctx, componentName) {

  // list of subscribers who subscribe to the component using this pub-sub actor for the data of type [[T]]
  var subscribers: Set[ActorRef[T]] = Set.empty

  override def onMessage(msg: PubSub[T]): Behavior[PubSub[T]] = {
    msg match {
      case Subscribe(ref)   => subscribe(ref)
      case Unsubscribe(ref) => unsubscribe(ref)
      case Publish(data)    => notifySubscribers(data)
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[PubSub[T]]] = {
    case Terminated(ref) ⇒ unsubscribe(ref.upcast); this
  }

  private def subscribe(actorRef: ActorRef[T]): Unit = {
    if (!subscribers.contains(actorRef)) {
      subscribers += actorRef
      ctx.watch(actorRef)
    }
  }

  private def unsubscribe(actorRef: ActorRef[T]): Unit = {
    subscribers -= actorRef
  }

  protected def notifySubscribers(data: T): Unit = {
    log.debug(s"Notifying subscribers :[${subscribers.mkString(",")}] with data :[$data]")
    subscribers.foreach(_ ! data)
  }
}
