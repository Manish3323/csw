/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.scaladsl

import akka.actor.typed.ActorRef
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaRegistration, Metadata}

/**
 * `RegistrationFactory` helps creating an AkkaRegistration. It is currently used by `csw-framework` to register different components on jvm boot-up.
 */
class RegistrationFactory {

  /**
   * Creates an AkkaRegistration from provided parameters. Currently, it is used to register components except Container.
   * A [[csw.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed]] can be thrown if the actorRef provided
   * is not a remote actorRef.
   *
   * @param akkaConnection the AkkaConnection representing the component
   * @param actorRef the supervisor actorRef of the component
   * @param metadata represents additional information associated with registration. If not provided, defaulted to empty value
   * @return a handle to the AkkaRegistration that is used to register in location service
   */
  def akkaTyped(akkaConnection: AkkaConnection, actorRef: ActorRef[_], metadata: Metadata = Metadata.empty): AkkaRegistration =
    AkkaRegistrationFactory.make(akkaConnection, actorRef, metadata)
}
