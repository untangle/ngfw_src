#!/bin/dash

# 
# This script is sourced by the other testing scripts
# It parses the arguments and sets the various necessary variables in the env
# 
# WAN_FAILOVER_PRIMARY_ADDRESS : the address to use when running the test - this assures it uses the correct WAN to run the test
# WAN_FAILOVER_MAC_ADDRESS     : the MAC address to use when running the test - this assures it uses the correct WAN to run the test
#

WAN_FAILOVER_NETD_INTERFACE_ID=$1
WAN_FAILOVER_OS_NAME=$2
if [ -n "$3" ] ; then
    WAN_FAILOVER_TIMEOUT_MS=$3
    WAN_FAILOVER_TIMEOUT_SEC=$(($3/1000))
fi

if [ -d "/sys/class/net/${WAN_FAILOVER_OS_NAME}/brport" ]; then
    WAN_FAILOVER_OS_NAME=`readlink /sys/class/net/${WAN_FAILOVER_OS_NAME}/brport/bridge`
    WAN_FAILOVER_OS_NAME=`basename ${WAN_FAILOVER_OS_NAME}`
fi

UPLINK_TABLE=`printf "uplink.%d" ${WAN_FAILOVER_NETD_INTERFACE_ID}`
WAN_FAILOVER_GATEWAY=`ip route show table ${UPLINK_TABLE} | awk '/scope link/ { next } ; { print $3 ; exit }'`
if [ -n "${WAN_FAILOVER_GATEWAY}" ]; then
    WAN_FAILOVER_PRIMARY_ADDRESS=`ip route get ${WAN_FAILOVER_GATEWAY} oif ${WAN_FAILOVER_OS_NAME} | awk '/src/ { print $5 ; exit }'`
else
    WAN_FAILOVER_PRIMARY_ADDRESS=`ip addr show ${WAN_FAILOVER_OS_NAME} | awk '/inet/ { print $2; exit }'`
fi

if [ "${WAN_FAILOVER_OS_NAME#ppp}" = "${WAN_FAILOVER_OS_NAME}" ]; then
    WAN_FAILOVER_MAC_ADDRESS=`ip link show ${WAN_FAILOVER_OS_NAME} | awk '/link/ { print $2 }'`
else
    WAN_FAILOVER_MAC_ADDRESS="00:00:00:00:00:00"
fi

