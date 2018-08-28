package csw.services.alarm.client.internal.extensions

import java.time.format.DateTimeFormatter
import java.time._

object TimeExtensions {

  private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:m a")

  implicit class RichClock(val clock: Clock) extends AnyVal {
    def untilNext(hour: Int): Duration    = untilNext(LocalTime.of(hour, 0))
    def untilNext(text: String): Duration = untilNext(LocalTime.parse(text, TimeFormatter))

    def untilNext(localTime: LocalTime): Duration = {
      val currentTime = LocalTime.now(clock)
      val targetTime  = localTime.adjustInto(currentTime) // Adjust localTime to the timezone of clock. In our case UTC timezone.
      val duration    = Duration.between(currentTime, targetTime)
      if (duration.isNegative) duration.plusDays(1) else duration
    }
  }
}
