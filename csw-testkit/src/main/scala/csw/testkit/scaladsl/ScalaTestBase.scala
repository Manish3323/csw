/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.testkit.scaladsl
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, OptionValues, TestSuite}
import org.scalatest.matchers.should.Matchers

trait ScalaTestBase extends TestSuite with Matchers with BeforeAndAfterAll with ScalaFutures with Eventually with OptionValues
