package csw.params.events

import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class WFSDetectorEventTest extends AnyFunSpec with Matchers {
  describe("WFS Detector") {
    it("should create publish success observe event | CSW-118, CSW-119") {
      val sourcePrefix = "ESW.filter.wheel"
      val event        = WFSDetectorEvent.publishSuccess(sourcePrefix)

      event.eventName.name shouldBe "ObserveEvent.PublishSuccess"
      event.source shouldBe Prefix(sourcePrefix)
    }

    it("should create publish fail observe event | CSW-118, CSW-119") {
      val sourcePrefix = "ESW.filter.wheel"
      val event        = WFSDetectorEvent.publishFail(sourcePrefix)

      event.eventName.name shouldBe "ObserveEvent.PublishFail"
      event.source shouldBe Prefix(sourcePrefix)
    }

    it("should create exposure state observe event | CSW-118, CSW-119") {
      val sourcePrefix = "ESW.filter.wheel"
      val detector     = "my-detector"
      val event = WFSDetectorEvent.exposureState(
        sourcePrefix,
        detector,
        exposureInProgress = true,
        abortInProgress = true,
        isAborted = true,
        OperationalState.BUSY,
        ""
      )

      event.eventName.name shouldBe "ObserveEvent.WfsDetectorExposureState"
      event.source shouldBe Prefix(sourcePrefix)
      event.paramSet shouldBe Set(
        ObserveEventKeys.detector.set(detector),
        ObserveEventKeys.operationalState.set("BUSY"),
        ObserveEventKeys.errorMessage.set(""),
        ObserveEventKeys.exposureInProgress.set(true),
        ObserveEventKeys.abortInProgress.set(true),
        ObserveEventKeys.isAborted.set(true)
      )
    }
  }
}
