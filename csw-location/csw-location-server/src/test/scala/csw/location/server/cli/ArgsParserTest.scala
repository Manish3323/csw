/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.cli

import java.io.ByteArrayOutputStream

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ArgsParserTest extends AnyFunSuite with Matchers {

  // Capture output/error generated by the parser, for cleaner test output. If interested, errCapture.toString will return capture errors.
  val outCapture = new ByteArrayOutputStream
  val errCapture = new ByteArrayOutputStream
  val parser     = new ArgsParser("csw-location-server")

  def silentParse(args: Array[String]): Option[Options] =
    Console.withOut(outCapture) {
      Console.withErr(errCapture) {
        parser.parse(args.toList)
      }
    }

  test("parse without arguments") {
    val args = Array("")
    silentParse(args) shouldBe None
  }

  test("parse with clusterPort argument and without outsideNetwork argument | CSW-89") {
    val args    = Array("--clusterPort", "1234")
    val port    = 1234
    val `false` = false
    silentParse(args) shouldBe Some(Options(Some(port), outsideNetwork = `false`))
  }

  test("parse with outsideNetwork argument | CSW-89") {
    val args = Array("--outsideNetwork")
    silentParse(args) shouldBe Some(Options(outsideNetwork = true))
  }
}
