/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.core.models.utils

import java.time.Instant

object TestUtil {

  def formatWithPrecision(tmtTime: Instant, precision: Int): String = {
    val nanos                    = tmtTime.getNano
    val digitsExceedingPrecision = Math.pow(10, 9 - precision).toInt
    (nanos / digitsExceedingPrecision).toString
  }
}
