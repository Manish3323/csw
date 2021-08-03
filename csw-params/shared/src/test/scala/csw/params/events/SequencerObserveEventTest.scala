package csw.params.events

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.{ExposureId, ObsId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

class SequencerObserveEventTest extends AnyFunSpec with Matchers {
  describe("SequencerObserveEvents") {
    val obsId                         = "2020A-001-123"
    val exposureId                    = ExposureId("2020A-001-123-TCS-DET-SCI0-0001")
    val prefix                        = Prefix(ESW, "filter.wheel")
    val obsIdParam: Parameter[_]      = ObserveEventKeys.obsId.set(obsId)
    val exposureIdParam: Parameter[_] = ObserveEventKeys.exposureId.set(exposureId.toString)
    val sequencerObserveEvent         = SequencerObserveEvent(prefix)

    it("create Observe Event with obsId parameters | CSW-125") {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (sequencerObserveEvent.presetStart(ObsId(obsId)), "ObserveEvent.PresetStart", prefix),
        (sequencerObserveEvent.presetStart(ObsId(obsId)), "ObserveEvent.PresetStart", prefix),
        (sequencerObserveEvent.presetEnd(ObsId(obsId)), "ObserveEvent.PresetEnd", prefix),
        (sequencerObserveEvent.guidestarAcqStart(ObsId(obsId)), "ObserveEvent.GuidestarAcqStart", prefix),
        (sequencerObserveEvent.guidestarAcqEnd(ObsId(obsId)), "ObserveEvent.GuidestarAcqEnd", prefix),
        (sequencerObserveEvent.scitargetAcqStart(ObsId(obsId)), "ObserveEvent.ScitargetAcqStart", prefix),
        (sequencerObserveEvent.scitargetAcqEnd(ObsId(obsId)), "ObserveEvent.ScitargetAcqEnd", prefix),
        (sequencerObserveEvent.observationStart(ObsId(obsId)), "ObserveEvent.ObservationStart", prefix),
        (sequencerObserveEvent.observationEnd(ObsId(obsId)), "ObserveEvent.ObservationEnd", prefix),
        (sequencerObserveEvent.observeStart(ObsId(obsId)), "ObserveEvent.ObserveStart", prefix),
        (sequencerObserveEvent.observeEnd(ObsId(obsId)), "ObserveEvent.ObserveEnd", prefix)
      ).forEvery((observeEvent, expectedEventName, expectedPrefix) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(expectedPrefix)
        observeEvent.paramSet shouldBe Set(obsIdParam)
      })
    }

    it("create Observe Event with obsId and exposure Id parameters | CSW-125") {
      Table(
        ("Observe Event", "Event Name", "Prefix"),
        (sequencerObserveEvent.exposureStart(exposureId), "ObserveEvent.ExposureStart", prefix),
        (sequencerObserveEvent.exposureEnd(exposureId), "ObserveEvent.ExposureEnd", prefix),
        (sequencerObserveEvent.readoutEnd(exposureId), "ObserveEvent.ReadoutEnd", prefix),
        (sequencerObserveEvent.readoutFailed(exposureId), "ObserveEvent.ReadoutFailed", prefix),
        (sequencerObserveEvent.dataWriteStart(exposureId), "ObserveEvent.DataWriteStart", prefix),
        (sequencerObserveEvent.dataWriteEnd(exposureId), "ObserveEvent.DataWriteEnd", prefix),
        (sequencerObserveEvent.prepareStart(exposureId), "ObserveEvent.PrepareStart", prefix)
      ).forEvery((observeEvent, expectedEventName, expectedPrefixStr) => {
        observeEvent.eventName should ===(EventName(expectedEventName))
        observeEvent.source should ===(expectedPrefixStr)
        observeEvent.paramSet shouldBe Set(exposureIdParam)
      })
    }

    it("create downtimeStart Observe Event with fixed Parameter set | CSW-125") {
      val event = sequencerObserveEvent.downtimeStart(ObsId(obsId), "bad weather")
      event.eventName should ===(EventName("ObserveEvent.DowntimeStart"))
      event.source should ===(prefix)
      event.paramSet shouldBe Set(obsIdParam, StringKey.make("reason").set("bad weather"))
    }

    it("create observePaused event | CSW-125") {
      val event = sequencerObserveEvent.observePaused()
      event.eventName should ===(EventName("ObserveEvent.ObservePaused"))
      event.source should ===(prefix)
    }

    it("create observeResumed event | CSW-125") {
      val event = sequencerObserveEvent.observeResumed()
      event.eventName should ===(EventName("ObserveEvent.ObserveResumed"))
      event.source should ===(prefix)
    }
  }
}
