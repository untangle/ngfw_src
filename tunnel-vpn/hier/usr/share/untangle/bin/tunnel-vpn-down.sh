#!/bin/sh

echo
echo "`date`"
echo "dev: ${dev}"
echo "ifconfig_local: ${ifconfig_local}"
echo "ifconfig_remote: ${ifconfig_remote}"
echo

if [ -z "${dev}" ] ; then
    echo "Missing dev!"
    exit 1
fi
if [ -z "${ifconfig_remote}" ] ; then
    echo "Missing remote IP!"
    exit 1
fi

interface_id="`echo ${dev} | sed -e 's/[a-zA-Z]//g'`"
if [ -z "${interface_id}" ] ; then
    echo "Missing ID!"
    exit 1
fi

ip -4 route flush table "uplink.${interface_id}"
ip -4 route delete table "uplink.${interface_id}"
ip -6 route flush table "uplink.${interface_id}"
ip -6 route delete table "uplink.${interface_id}"
