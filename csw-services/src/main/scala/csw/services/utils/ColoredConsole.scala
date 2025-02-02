/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services.utils

object ColoredConsole {

  private def colored(color: String, msg: Any): Unit = println(s"$color$msg${Console.RESET}")

  object GREEN {
    def println(msg: Any): Unit = colored(Console.GREEN, msg)
  }

  object RED {
    def println(msg: Any): Unit = colored(Console.RED, msg)
  }
}
