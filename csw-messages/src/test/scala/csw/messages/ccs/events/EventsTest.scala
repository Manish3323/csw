package csw.messages.ccs.events

import csw.messages.params.generics.KeyType
import csw.messages.params.models.Prefix
import org.scalatest.{FunSpec, Matchers}

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
class EventsTest extends FunSpec with Matchers {
  private val s1: String = "encoder"

  private val s1Key = KeyType.IntKey.make(s1)

  private val ck        = Prefix("wfos.blue.filter")
  private val eventName = EventName("filter wheel")

  // DEOPSCSW-327: Define Event Data Structure
  // DEOPSCSW-329: Providing Mandatory information during Event Creation
  describe("SystemEvent Test") {
    val k1     = KeyType.IntKey.make("encoder")
    val k2     = KeyType.IntKey.make("windspeed")
    val k3     = KeyType.IntKey.make("notUsed")
    val prefix = Prefix("wfos.prog.cloudcover")

    it("should create with prefix and eventName") {
      val i1 = k1.set(22)
      val i2 = k2.set(44)

      val sc1 = SystemEvent(prefix, eventName).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("should create with prefix, eventName, paramSet") {
      val i1 = k1.set(22)
      val i2 = k2.set(44)

      val sc1 = SystemEvent(prefix, eventName, Set(i1, i2)).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }
    // DEOPSCSW-328: Basic information of Event needed for routing and Diagnostic use
    it("Should allow removing") {
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

    it("Should allow adding") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val sc1 = SystemEvent(prefix, eventName).madd(i1)

      assert(sc1.size == 1)
      assert(sc1.exists(k1))

      val mutatedSc1 = sc1.add(i2)

      assert(mutatedSc1.size == 2)
      assert(mutatedSc1.eventId != sc1.eventId)
    }

    // DEOPSCSW-328: Basic information of Event needed for routing and Diagnostic use
    it("Should access metadata fields") {
      val i1  = k1.set(22)
      val sc1 = SystemEvent(prefix, eventName).madd(i1)

      assert(sc1.size == 1)
      assert(sc1.exists(k1))
      sc1.eventId should not equal null
      sc1.eventTime should not equal null
      sc1.eventName shouldEqual eventName
      sc1.source shouldEqual prefix

    }
  }

  // DEOPSCSW-327: Define Event Data Structure
  // DEOPSCSW-329: Providing Mandatory information during Event Creation
  describe("ObserveEvent Test") {
    val k1     = KeyType.IntKey.make("encoder")
    val k2     = KeyType.IntKey.make("windspeed")
    val k3     = KeyType.IntKey.make("notUsed")
    val prefix = Prefix("wfos.prog.cloudcover")

    it("should create with prefix and eventName") {
      val i1 = k1.set(22)
      val i2 = k2.set(44)

      val sc1 = ObserveEvent(prefix, eventName).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("should create with prefix, eventName and paramSet") {
      val i1 = k1.set(22)
      val i2 = k2.set(44)

      val sc1 = ObserveEvent(prefix, eventName, Set(i1, i2))
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("Should allow removing") {
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

    it("Should allow adding") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val oc1 = ObserveEvent(prefix, eventName).madd(i1)

      assert(oc1.size == 1)
      assert(oc1.exists(k1))

      val mutatedOc1 = oc1.add(i2)

      assert(mutatedOc1.size == 2)
      assert(mutatedOc1.eventId != oc1.eventId)
    }

    // DEOPSCSW-328: Basic information of Event needed for routing and Diagnostic use
    it("Should access metadata fields") {
      val i1  = k1.set(22)
      val oc1 = ObserveEvent(prefix, eventName).madd(i1)

      assert(oc1.size == 1)
      assert(oc1.exists(k1))
      oc1.eventId should not equal null
      oc1.eventTime should not equal null
      oc1.eventName shouldEqual eventName
      oc1.source shouldEqual prefix

    }
  }

  describe("Check equal for events") {
    it("each event created should be unique") {

      //Every event should be unique because every event creation will create new eventId
      val ev1 = SystemEvent(ck, eventName).add(s1Key -> 2)

      val ev2 = SystemEvent(ck, eventName).add(s1Key -> 2)

      val ev3 = SystemEvent(ck, eventName).add(s1Key -> 22)

      ev1 should not equal ev2
      ev1 should not equal ev3
    }
  }

}
