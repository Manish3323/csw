/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.framework.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks

import scala.concurrent.{ExecutionContextExecutor, Future}

// $COVERAGE-OFF$
/**
 * A convenient class wrapping actor system and providing handles for execution context and clean up of actor system
 */
class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = actorSystem.executionContext
  lazy val coordinatedShutdown: CoordinatedShutdown            = CoordinatedShutdown(actorSystem)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, actorSystem)

  def shutdown(): Future[Done] = {
    actorSystem.terminate()
    actorSystem.whenTerminated
  }
}
// $COVERAGE-ON$
