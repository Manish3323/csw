package csw.time.client
import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

import csw.time.api.models.TMTTime

/**
 * This API allows users to get a representation of [[csw.time.api.models.TMTTime TMTTime]] in a specific Time Zone,
 * returned as a [[java.time.ZonedDateTime]].
 */
object TMTTimeHelper {

  /**
   * Combines the [[csw.time.api.models.TMTTime TMTTime]] with the given timezone to get a [[java.time.ZonedDateTime]]
   *
   * @param zoneId id of the required zone
   * @return time atZone the given zone
   */
  def atZone(tmtTime: TMTTime, zoneId: ZoneId): ZonedDateTime = tmtTime.value.atZone(zoneId)

  /**
   * Combines the [[csw.time.api.models.TMTTime TMTTime]] with the Local timezone to get a [[java.time.ZonedDateTime]]. Local timezone is the system's default timezone.
   *
   * @return time atZone the Local zone
   */
  def atLocal(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneId.systemDefault())

  /**
   * Combines the [[csw.time.api.models.TMTTime TMTTime]] with the Hawaii timezone to get a [[java.time.ZonedDateTime]].
   *
   * @return time atZone the Hawaii-Aleutian Standard Time (HST) zone
   */
  def atHawaii(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneId.of("US/Hawaii"))

  /**
   * Converts the [[csw.time.api.models.TMTTime TMTTime]] instance to [[java.time.ZonedDateTime]] by adding 0 offset of UTC timezone.
   *
   * @return zoned representation of the TMTTime
   */
  def toZonedDateTime(tmtTime: TMTTime): ZonedDateTime = atZone(tmtTime, ZoneOffset.UTC)
}
