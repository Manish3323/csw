/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.deploy.hostconfig.cli

import java.nio.file.Paths

import csw.framework.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
private[hostconfig] class ArgsParser(name: String) {

  private[hostconfig] val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    opt[Unit]("local") action { (_, c) =>
      c.copy(local = true)
    } text "Optional: if provided, get the host configuration file from local machine located at hostConfigPath, else fetch it from config service"

    arg[String]("<file>").required() action { (x, c) =>
      c.copy(hostConfigPath = Some(Paths.get(x)))
    } text "Required: host configuration file path"

    opt[String]('s', "container-script").required() action { (x, c) =>
      c.copy(containerCmdAppScript = Some(x))
    } text "Specifies the generated shell script path of container command app from task `universal:publish` (sbt-native-packager task)"

    help("help")
    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
