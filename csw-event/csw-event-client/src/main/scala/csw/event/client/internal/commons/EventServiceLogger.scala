/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

/**
 * All the logs generated from location service will have a fixed prefix, which is the value of "event-service-lib".
 * The prefix helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from location service.
 */
private[event] object EventServiceLogger extends LoggerFactory(Prefix(CSW, "event_service_lib"))
