/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.generator

import io.bullet.borer.{Encoder, Json}
import play.api.libs.json

object JsonHelper extends ContractCodecs {
  def toJson[T: Encoder](data: T): String = {
    val jsonString = Json.encode(data).toUtf8String
    json.Json.prettyPrint(json.Json.parse(jsonString))
  }
}
