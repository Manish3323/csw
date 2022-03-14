/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.internal

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.location.server.BuildInfo
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

private[location] class ActorRuntime(_typedSystem: ActorSystem[SpawnProtocol.Command]) {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = _typedSystem
  implicit val ec: ExecutionContextExecutor                    = actorSystem.executionContext
  implicit val scheduler: Scheduler                            = actorSystem.scheduler

  private[location] val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(name: String, hostname: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, hostname, actorSystem)

  def shutdown(): Future[Done] = {
    actorSystem.terminate()
    actorSystem.whenTerminated
  }
}
