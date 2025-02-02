/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.auth

import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.aas.http.Roles
import csw.params.commands.ControlCommand
import csw.prefix.models.Prefix

object CommandPolicy {
  def apply(roles: CommandRoles, command: ControlCommand, destinationPrefix: Prefix): CustomPolicy =
    CustomPolicy(token =>
      roles.hasAccess(CommandKey(destinationPrefix, command.commandName), destinationPrefix.subsystem, Roles(token))
    )
}
