package csw.services.config.server.commons

import csw.services.logging.scaladsl.LoggerFactory

private[config] object ConfigServerLogger extends LoggerFactory(ConfigServiceConnection.value.componentId.name)
