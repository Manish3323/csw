/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.handlers

import csw.command.api.messages.CommandServiceRequest

object TestHelper {
  implicit class Narrower(x: CommandServiceRequest) {
    def narrow: CommandServiceRequest = x
  }

}
