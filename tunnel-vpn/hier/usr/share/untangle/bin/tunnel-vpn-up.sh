#!/bin/sh

exec >> /var/log/uvm/tunnel.log 2>&1

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

/usr/share/untangle-netd/bin/add-uplink.sh ${dev} ${ifconfig_remote} uplink.${interface_id} -4

/sbin/iptables -t mangle -A tunnel-vpn-${interface_id} -j MARK --set-mark $((${interface_id}<<8))/0xff00 -m comment --comment "Set destination interface to use tunnel ${interface_id}"
/sbin/iptables -t mangle -A tunnel-vpn-${interface_id} -j ACCEPT -m comment --comment "stop processing all other rules"

/sbin/iptables -t mangle -A tunnel-vpn-any -j MARK --set-mark $((${interface_id}<<8))/0xff00 -m comment --comment "Set destination interface to use tunnel ${interface_id}"
/sbin/iptables -t mangle -A tunnel-vpn-any -j ACCEPT -m comment --comment "Set destination interface to use tunnel ${interface_id}"

