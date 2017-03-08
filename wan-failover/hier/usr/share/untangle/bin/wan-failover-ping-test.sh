#!/bin/dash

. @PREFIX@/usr/share/untangle/bin/wan-failover-test.sh $@

PING_HOST=$4

if [ "${WAN_FAILOVER_PRIMARY_ADDRESS}" = "0.0.0.0" ]; then
    ping -I ${WAN_FAILOVER_OS_NAME} -w ${WAN_FAILOVER_TIMEOUT_SEC} -c 1 ${PING_HOST} > /dev/null || {
         echo "Unable to ping '${PING_HOST}'"
    }
else
    ping -I ${WAN_FAILOVER_PRIMARY_ADDRESS} -w ${WAN_FAILOVER_TIMEOUT_SEC} -c 1 ${PING_HOST} > /dev/null || {
        echo "Unable to ping '${PING_HOST}'"
    }
fi

true