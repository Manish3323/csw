/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.deploy.hostconfig.cli

import java.nio.file.Path

/**
 * Command line options
 *
 * @param local               if true, get the host configuration file from local machine located at provided hostConfigPath
 *                            else, fetch the host configuration file from configuration service
 * @param hostConfigPath      host configuration file path
 */
private[hostconfig] case class Options(
    local: Boolean = false,
    hostConfigPath: Option[Path] = None,
    containerCmdAppScript: Option[String] = None
)
