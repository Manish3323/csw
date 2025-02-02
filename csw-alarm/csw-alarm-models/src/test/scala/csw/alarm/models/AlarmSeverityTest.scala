/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.models

import csw.alarm.models.AlarmSeverity._

class AlarmSeverityTest extends EnumTest(AlarmSeverity, "") {
  override val expectedValues = Set(Indeterminate, Okay, Warning, Major, Critical)
}
