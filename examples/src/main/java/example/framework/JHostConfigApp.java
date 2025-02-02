/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework;

import csw.framework.javadsl.JHostConfig;
import csw.prefix.javadsl.JSubsystem;

//#jhost-config-app
public class JHostConfigApp {

    public static void main(String[] args) {
        JHostConfig.start("JHost-Config-App", JSubsystem.CSW, args);
    }

}
//#jhost-config-app