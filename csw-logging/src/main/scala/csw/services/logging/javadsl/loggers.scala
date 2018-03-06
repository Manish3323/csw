package csw.services.logging.javadsl

import acyclic.skipped
import akka.{actor, typed}
import akka.actor.{ActorPath, ActorRef}
import akka.serialization.Serialization
import akka.typed.javadsl.ActorContext
import akka.typed.scaladsl.adapter.TypedActorRefOps
import csw.services.logging.internal.JLogger
import csw.services.logging.scaladsl.{Logger, LoggerFactory, LoggerImpl}

private[logging] abstract class JBaseLoggerFactory(maybeComponentName: Option[String]) {
  def getLogger[T](ctx: ActorContext[T], klass: Class[_]): ILogger = new JLogger(logger(Some(actorPath(ctx.getSelf))), klass)
  def getLogger(ctx: actor.ActorContext, klass: Class[_]): ILogger = new JLogger(logger(Some(actorPath(ctx.self))), klass)
  def getLogger(klass: Class[_]): ILogger                          = new JLogger(logger(None), klass)

  private def logger(maybeActorRef: Option[String]): Logger  = new LoggerImpl(maybeComponentName, maybeActorRef)
  private def actorPath(actorRef: ActorRef): String          = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
  private def actorPath(actorRef: typed.ActorRef[_]): String = actorPath(actorRef.toUntyped)
}

/**
 * When using the `JLoggerFactory`, log statements will have `@componentName` tag with provided `componentName`
 */
class JLoggerFactory(componentName: String) extends JBaseLoggerFactory(Some(componentName)) {
  def asScala: LoggerFactory = new LoggerFactory(componentName)
}

/**
 * When using the `JGenericLoggerFactory`, log statements will not have `@componentName` tag
 */
object JGenericLoggerFactory extends JBaseLoggerFactory(None)
