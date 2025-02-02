/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server

import java.util.concurrent.CompletableFuture

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.dispatch.MessageDispatcher
import akka.{Done, actor}
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context and clean up of actor system
 */
private[config] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command], val settings: Settings) {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = typedSystem.executionContext

  val classicSystem: actor.ActorSystem         = typedSystem.toClassic
  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(classicSystem)

  val blockingIoDispatcher: MessageDispatcher = classicSystem.dispatchers.lookup(settings.`blocking-io-dispatcher`)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedSystem)

  def shutdown(): Future[Done] = {
    typedSystem.terminate()
    typedSystem.whenTerminated
  }

  def jShutdown(): CompletableFuture[Done] = shutdown().toJava.toCompletableFuture
}
