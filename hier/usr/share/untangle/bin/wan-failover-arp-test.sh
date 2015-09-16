#!/bin/dash

. @PREFIX@/usr/share/untangle/bin/wan-failover-test.sh $@

if [ "${WAN_FAILOVER_OS_NAME#ppp}" != "${WAN_FAILOVER_OS_NAME}" ]; then
    ## If this is a PPP interface, it just needs to be online (you can't ARP a PPPoE interface.)
    ifconfig ${WAN_FAILOVER_OS_NAME} > /dev/null || echo "PPPoE ${WAN_FAILOVER_OS_NAME} Interface is down"
    exit 0
fi

arping -s ${WAN_FAILOVER_PRIMARY_ADDRESS} -I ${WAN_FAILOVER_OS_NAME}  -c 1 ${WAN_FAILOVER_GATEWAY} > /dev/null || {
    echo "Unable to arp '${WAN_FAILOVER_GATEWAY}'"
}

true
