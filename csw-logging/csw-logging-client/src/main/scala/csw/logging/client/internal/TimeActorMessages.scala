/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.internal

import csw.logging.models.RequestId

private[logging] object TimeActorMessages {

  sealed trait TimeActorMessage

  case class TimeStart(id: RequestId, name: String, uid: String, time: Long) extends TimeActorMessage

  case class TimeEnd(id: RequestId, name: String, uid: String, time: Long) extends TimeActorMessage

  case object TimeDone extends TimeActorMessage

}
