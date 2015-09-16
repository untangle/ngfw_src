#!/bin/dash

. @PREFIX@/usr/share/untangle/bin/wan-failover-test.sh $@

DNS_SERVER=$4
HOSTNAME="www.untangle.com"

if [ -z "$DNS_SERVER" ] || [ "${DNS_SERVER}x" = "0.0.0.0x" ] ; then
    # extract DNS server for this interface from the dnsmasq.conf file
    DNS_SERVER=`awk '/^.*server=.*uplink.'${WAN_FAILOVER_NETD_INTERFACE_ID}'/ { sub( /^.*server=/, "" ) ; print $1 ; exit }' /etc/dnsmasq.conf`
    if [ -z "${DNS_SERVER}" ]; then
        DNS_SERVER=4.2.2.1 # dirty.
    fi
fi

if [ -z "${HOSTNAME}" ]; then
    HOSTNAME="www.google.com"
fi

NUM_TRIES=3
TIMEOUT=$(( ${WAN_FAILOVER_TIMEOUT_MS} / ( ${NUM_TRIES} * 1000 )  ))
if [ "${TIMEOUT}" = "0" ]; then 
    TIMEOUT=1
fi

dig -b ${WAN_FAILOVER_PRIMARY_ADDRESS} +tries=${NUM_TRIES} +time=${TIMEOUT} @${DNS_SERVER} ${HOSTNAME} > /dev/null || {
    echo "Unable to resolve host from server ${DNS_SERVER}"
}

true

