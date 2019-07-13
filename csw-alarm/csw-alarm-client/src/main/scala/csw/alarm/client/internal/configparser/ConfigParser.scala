package csw.alarm.client.internal.configparser

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.alarm.api.exceptions.ConfigParseException
import csw.alarm.api.internal.ValidationResult.{Failure, Success}
import csw.alarm.api.internal.{AlarmJsonSupport, AlarmMetadataSet}
import play.api.libs.json.{Format, JsValue, Json}

/**
 * Parses the information represented in configuration files into respective models
 */
private[client] object ConfigParser extends AlarmJsonSupport {
  val ALARMS_SCHEMA: Config = ConfigFactory.parseResources("alarms-schema.conf")

  def parseAlarmMetadataSet(config: Config): AlarmMetadataSet = parse[AlarmMetadataSet](config)

  private def parse[T: Format](config: Config): T =
    ConfigValidator.validate(config, ALARMS_SCHEMA) match {
      case Success          => configToJsValue(config).as[T]
      case Failure(reasons) => throw ConfigParseException(reasons)
    }

  private def configToJsValue(config: Config): JsValue = Json.parse(config.root().render(ConfigRenderOptions.concise()))
}
