/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.commons

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

object TestFutureExtension {

  implicit class RichFuture[T](val f: Future[T]) extends AnyVal {
    def await: T        = Await.result(f, 20.seconds)
    def done: Future[T] = Await.ready(f, 20.seconds)
  }

}
