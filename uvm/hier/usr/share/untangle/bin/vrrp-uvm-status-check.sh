#!/bin/sh

# Keepalived health check for the local UVM HTTP endpoint.
# Exit 0 only when UVM returns HTTP 200. Keepalived supplies the
# consecutive-failure/recovery thresholds; this script performs one bounded
# request and does not add its own retry delay.

HTTP_CODE=$(/usr/bin/curl --silent --show-error \
    --connect-timeout 2 \
    --max-time 2 \
    --output /dev/null \
    --write-out '%{http_code}' \
    http://127.0.0.1/uvm/status 2>/dev/null)

if [ "${HTTP_CODE}" = "200" ]; then
    exit 0
fi

exit 1
