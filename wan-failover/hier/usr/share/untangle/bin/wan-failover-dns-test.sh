#!/bin/dash

. @PREFIX@/usr/share/untangle/bin/wan-failover-test.sh $@

DNS_SERVER=$4
HOSTNAME="www.untangle.com"

if [ -z "$DNS_SERVER" ] || [ "${DNS_SERVER}x" = "0.0.0.0x" ] ; then
    # extract DNS server for this interface from the dnsmasq.conf file
    DNS_SERVER=`awk '/^.*server=.*uplink.'${WAN_FAILOVER_NETD_INTERFACE_ID}'/ { sub( /^.*server=/, "" ) ; print $1 ; next ; exit }' /etc/dnsmasq.conf | head -n 1`
    # extract DNS server for this dhcp dnsmasq file if it wasnt in dnsmasq.conf 
    if [ -z "${DNS_SERVER}" ] && [ -f /etc/dnsmasq.d/dhcp-upstream-dns-servers ]; then
        DNS_SERVER=`awk '/^.*server=.*uplink.'${WAN_FAILOVER_NETD_INTERFACE_ID}'/ { sub( /^.*server=/, "" ) ; print $1 ; next ; exit }' /etc/dnsmasq.d/dhcp-upstream-dns-servers | head -n 1`
    fi
    if [ -z "${DNS_SERVER}" ]; then
        echo "Unable to determine current DNS server for interface ${WAN_FAILOVER_NETD_INTERFACE_ID}."
        exit 1
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
