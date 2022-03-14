# Copyright (c) [year] Thirty Meter Telescope International Observatory
# SPDX-License-Identifier: Apache-2.0

import logging
import time

class UTCFormatter(logging.Formatter):
    converter = time.gmtime