/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.helpers
import scala.concurrent.duration.{Duration, DurationDouble}
import scala.concurrent.{Await, Future}

object TestFutureExt {
  implicit class RichFuture[T](f: Future[T]) {
    def await(duration: Duration): T = Await.result(f, duration)
    def await: T                     = await(10.seconds)
  }
}
