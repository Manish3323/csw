/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.events

import csw.params.core.generics.KeyType
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
// DEOPSCSW-327: Define Event Data Structure
// DEOPSCSW-328: Basic information of Event needed for routing and Diagnostic use
// DEOPSCSW-329: Providing Mandatory information during Event Creation
// DEOPSCSW-330: Include complex payloads - paramset in Event and ObserveEvent
// DEOPSCSW-331: Complex payload - Include byte in paramset for Event and ObserveEvent
// | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331
class EventsTest extends AnyFunSpec with Matchers {
  private val s1: String = "encoder"

  private val s1Key = KeyType.IntKey.make(s1)

  private val ck        = Prefix("wfos.blue.filter")
  private val eventName = EventName("filter wheel")

  describe("SystemEvent Test") {
    val k1     = KeyType.IntKey.make("encoder")
    val k2     = KeyType.IntKey.make("windspeed")
    val k3     = KeyType.IntKey.make("notUsed")
    val k4     = KeyType.ByteKey.make("image")
    val prefix = Prefix("wfos.prog.cloudcover")

    it(
      "should create with prefix and eventName | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val i1 = k1.set(22)
      val i2 = k2.set(44)

      val sc1 = SystemEvent(prefix, eventName).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.parameter(k1).head == 22)
      assert(sc1.parameter(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
      val ex = intercept[NoSuchElementException](sc1(k4))
      ex.getMessage shouldBe s"Parameter set does not contain key: ${k4.keyName}"
      val ex2 = intercept[NoSuchElementException](sc1.parameter(k3))
      ex2.getMessage shouldBe s"Parameter set does not contain key: ${k3.keyName}"
    }

    it(
      "should create with prefix, eventName, paramSet | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val i1 = k1.set(22)
      val i2 = k2.set(44)
      val i4 = k4.setAll("sensor image".getBytes)

      val sc1 = SystemEvent(prefix, eventName, Set(i1, i2, i4))
      assert(sc1.size == 3)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1.exists(k4))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1(k4).values === "sensor image".getBytes)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it(
      "Should allow removing | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val sc1 = SystemEvent(prefix, eventName).madd(i1, i2)

      assert(sc1.size == 2)
      assert(sc1.exists(k1))

      val mutatedSc1 = sc1.remove(k1)

      assert(!mutatedSc1.exists(k1))
      assert(mutatedSc1.size == 1)
      assert(mutatedSc1.eventId != sc1.eventId)
    }

    it("Should allow adding | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val sc1 = SystemEvent(prefix, eventName).madd(i1)

      assert(sc1.size == 1)
      assert(sc1.exists(k1))

      val mutatedSc1 = sc1.add(i2)

      assert(mutatedSc1.size == 2)
      assert(mutatedSc1.eventId != sc1.eventId)
    }

    it(
      "Should access metadata fields | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val i1  = k1.set(22)
      val sc1 = SystemEvent(prefix, eventName).madd(i1)

      assert(sc1.size == 1)
      assert(sc1.exists(k1))
      sc1.eventId should not equal null
      sc1.eventTime should not equal null
      sc1.eventName shouldEqual eventName
      sc1.source shouldEqual prefix
      sc1.eventKey.toString shouldEqual s"$prefix.$eventName"

    }

    it(
      "each event created should be unique | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val ev1 = SystemEvent(ck, eventName)
      val ev2 = ev1.add(s1Key -> 2)
      val ev3 = ev2.remove(s1Key)

      ev1.eventId should not equal ev2.eventId
      ev1.eventName shouldEqual ev2.eventName
      ev1.source shouldEqual ev2.source

      ev3.eventId should not equal ev2.eventId
      ev3.eventName shouldEqual ev2.eventName
      ev3.source shouldEqual ev2.source
    }
  }

  describe("ObserveEvent Test") {
    val k1     = KeyType.IntKey.make("encoder")
    val k2     = KeyType.IntKey.make("windspeed")
    val k3     = KeyType.IntKey.make("notUsed")
    val k4     = KeyType.ByteKey.make("image")
    val prefix = Prefix("wfos.prog.cloudcover")

    it(
      "should create with prefix and eventName | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val i1 = k1.set(22)
      val i2 = k2.set(44)

      val sc1 = ObserveEvent(prefix, eventName).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.parameter(k1).head == 22)
      assert(sc1.parameter(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
      val ex = intercept[NoSuchElementException](sc1(k4))
      ex.getMessage shouldBe s"Parameter set does not contain key: ${k4.keyName}"
      val ex2 = intercept[NoSuchElementException](sc1.parameter(k3))
      ex2.getMessage shouldBe s"Parameter set does not contain key: ${k3.keyName}"
    }

    it(
      "should create with prefix, eventName and paramSet | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val i1 = k1.set(22)
      val i2 = k2.set(44)
      val i4 = k4.setAll("sensor image".getBytes)

      val sc1 = ObserveEvent(prefix, eventName, Set(i1, i2, i4))
      assert(sc1.size == 3)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1.exists(k4))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1(k4).values === "sensor image".getBytes)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it(
      "Should allow removing | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val oc1 = ObserveEvent(prefix, eventName).madd(i1, i2)

      assert(oc1.size == 2)
      assert(oc1.exists(k1))

      val mutatedOc1 = oc1.remove(k1)

      assert(!mutatedOc1.exists(k1))
      assert(mutatedOc1.size == 1)
      assert(mutatedOc1.eventId != oc1.eventId)
    }

    it("Should allow adding | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val oc1 = ObserveEvent(prefix, eventName).madd(i1)

      assert(oc1.size == 1)
      assert(oc1.exists(k1))

      val mutatedOc1 = oc1.add(i2)

      assert(mutatedOc1.size == 2)
      assert(mutatedOc1.eventId != oc1.eventId)
    }

    it(
      "Should access metadata fields | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val i1  = k1.set(22)
      val oc1 = ObserveEvent(prefix, eventName).madd(i1)

      assert(oc1.size == 1)
      assert(oc1.exists(k1))
      oc1.eventId should not equal null
      oc1.eventTime should not equal null
      oc1.eventName shouldEqual eventName
      oc1.source shouldEqual prefix
      oc1.eventKey.toString shouldEqual s"$prefix.$eventName"

    }

    it(
      "each event created should be unique | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-327, DEOPSCSW-328, DEOPSCSW-329, DEOPSCSW-330, DEOPSCSW-331"
    ) {
      val ev1 = ObserveEvent(ck, eventName)
      val ev2 = ev1.add(s1Key -> 2)
      val ev3 = ev2.remove(s1Key)

      ev1.eventId should not equal ev2.eventId
      ev1.eventName shouldEqual ev2.eventName
      ev1.source shouldEqual ev2.source

      ev3.eventId should not equal ev2.eventId
      ev3.eventName shouldEqual ev2.eventName
      ev3.source shouldEqual ev2.source
    }
  }
}
