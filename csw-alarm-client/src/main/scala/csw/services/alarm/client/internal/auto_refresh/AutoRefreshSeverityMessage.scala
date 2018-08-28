package csw.services.alarm.client.internal.auto_refresh

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey

sealed trait AutoRefreshSeverityMessage {
  def key: AlarmKey
}

object AutoRefreshSeverityMessage {
  case class AutoRefreshSeverity(key: AlarmKey, severity: AlarmSeverity) extends AutoRefreshSeverityMessage
  case class SetSeverity(key: AlarmKey, severity: AlarmSeverity)         extends AutoRefreshSeverityMessage
  case class CancelAutoRefresh(key: AlarmKey)                            extends AutoRefreshSeverityMessage
}
