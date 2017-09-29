#!/bin/sh

exec >> /var/log/uvm/tunnel.log 2>&1

echo "`date`: dev:${dev} local:${ifconfig_local} remote:${ifconfig_remote} gateway:${route_vpn_gateway}"

if [ -z "${dev}" ] ; then
    echo "Missing dev!"
    exit 1
fi

interface_id="`echo ${dev} | sed -e 's/[a-zA-Z]//g'`"
if [ -z "${interface_id}" ] ; then
    echo "Missing ID!"
    exit 1
fi

# table can not exist at this point - hide errors
ip -4 route flush table "uplink.${interface_id}" >/dev/null 2>&1
ip -4 route delete table "uplink.${interface_id}" >/dev/null 2>&1

/sbin/iptables -t mangle -D tunnel-vpn-${interface_id} -j MARK --set-mark $((${interface_id}<<8))/0xff00 -m comment --comment "Set destination interface to use tunnel ${interface_id}"  >/dev/null 2>&1
/sbin/iptables -t mangle -D tunnel-vpn-${interface_id} -j ACCEPT -m comment --comment "stop processing all other rules" >/dev/null 2>&1

/sbin/iptables -t mangle -D tunnel-vpn-any -j MARK --set-mark $((${interface_id}<<8))/0xff00 -m comment --comment "Set destination interface to use tunnel ${interface_id}" >/dev/null 2>&1
/sbin/iptables -t mangle -D tunnel-vpn-any -j ACCEPT -m comment --comment "Set destination interface to use tunnel ${interface_id}" >/dev/null 2>&1

exit 0
