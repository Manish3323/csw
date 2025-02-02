/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.extensions

import java.net.URI

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.serialization.Serialization

object ActorExtension {
  implicit class RichActor(actorRef: ActorRef[_]) {
    def toURI: URI = new URI(Serialization.serializedActorPath(actorRef.toClassic))
  }
}
