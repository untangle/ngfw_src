#!/bin/dash

. @PREFIX@/usr/share/untangle/bin/wan-failover-test.sh $@

URL=$4

NUM_TRIES=3
TIMEOUT=$(( ${WAN_FAILOVER_TIMEOUT_MS} / ( ${NUM_TRIES} * 1000 )  ))
if [ "${TIMEOUT}" = "0" ]; then 
    TIMEOUT=1
fi

wget --no-check-certificate --bind-address=${WAN_FAILOVER_PRIMARY_ADDRESS} --header="Wan-Failover-Flag: true" -T ${TIMEOUT} --tries=${NUM_TRIES} -O /dev/null ${URL} 2>/dev/null || {
    echo "Unable to complete a web request to '${URL}'"
}

