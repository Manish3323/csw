# Copyright (c) [year] Thirty Meter Telescope International Observatory
# SPDX-License-Identifier: Apache-2.0

filebeat setup --pipelines --template -E output.logstash.enabled=false -E 'output.elasticsearch.hosts=["elasticsearch:9200"]' -E setup.kibana.host=kibana:5601
