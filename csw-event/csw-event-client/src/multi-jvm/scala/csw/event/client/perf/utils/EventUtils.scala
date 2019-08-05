package csw.event.client.perf.utils

import java.time.Instant

import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.params.core.generics.Key
import csw.params.core.generics.KeyType.{ByteKey, DoubleKey, LongKey, ShortKey}
import csw.params.core.models.{Id, Prefix}
import csw.time.core.models.UTCTime

object EventUtils {
  private val prefix = Prefix("csw.dummy")
  val testEventS     = "move"
  val endEventS      = "end"

  val payloadKey: Key[Byte]                  = ByteKey.make("payloadKey")
  val histogramKey: Key[Short]               = ShortKey.make("histogramKey")
  val initialLatencyHistogramKey: Key[Short] = ShortKey.make("initialLatencyHistogramKey")
  val throughputKey: Key[Double]             = DoubleKey.make("throughputKey")
  val totalDroppedKey: Key[Long]             = LongKey.make("totalDroppedKey")
  val totalOutOfOrderKey: Key[Long]          = LongKey.make("totalOutOfOrderKey")
  val avgLatencyKey: Key[Long]               = LongKey.make("avgLatencyKey")

  val baseTestEvent          = SystemEvent(prefix, EventName(testEventS))
  val basePerfEvent          = SystemEvent(prefix, EventName("perf"))
  val perfEventKey: EventKey = basePerfEvent.eventKey

  def perfResultEvent(
      payload: Array[Short],
      initialLatencyPayload: Array[Short],
      throughput: Double,
      totalDropped: Long,
      totalOutOfOrder: Long,
      avgLatency: Long
  ): SystemEvent =
    basePerfEvent.copy(
      paramSet = Set(
        histogramKey.set(payload),
        initialLatencyHistogramKey.set(initialLatencyPayload),
        throughputKey.set(throughput),
        totalDroppedKey.set(totalDropped),
        totalOutOfOrderKey.set(totalOutOfOrder),
        avgLatencyKey.set(avgLatency)
      )
    )

  def event(name: EventName, prefix: Prefix, id: Long = -1, payload: Array[Byte] = Array.emptyByteArray): SystemEvent =
    baseTestEvent.copy(
      eventId = Id(id.toString),
      source = prefix,
      eventName = name,
      paramSet = Set(payloadKey.set(payload)),
      eventTime = UTCTime.now()
    )

  def nanosToMicros(nanos: Double): Double  = nanos / Math.pow(10, 3)
  def nanosToMillis(nanos: Double): Double  = nanos / Math.pow(10, 6)
  def nanosToSeconds(nanos: Double): Double = nanos / Math.pow(10, 9)
  def getNanos(instant: Instant): Double    = instant.getEpochSecond * Math.pow(10, 9) + instant.getNano

}
