/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models

import csw.alarm.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}

// DEOPSCSW-441: Model to represent Alarm Acknowledgement status
class AcknowledgementStatusTest extends EnumTest(AcknowledgementStatus, "| DEOPSCSW-441") {
  override val expectedValues = Set(Acknowledged, Unacknowledged)
}
