#!/bin/sh

exec >> /var/log/uvm/tunnel.log 2>&1

addRoute()
{
    /usr/share/untangle-netd/bin/add-uplink.sh $1 $2 uplink.$3 -4
}

echo
echo "`date`"
echo "dev: ${dev}"
echo "ifconfig_local: ${ifconfig_local}"
echo "ifconfig_remote: ${ifconfig_remote}"
echo "route_vpn_gateway: ${route_vpn_gateway}"
echo

if [ -z "${dev}" ] ; then
    echo "Missing dev!"
    exit 1
fi
if [ -z "${ifconfig_remote}" ] && [ -z "${route_vpn_gateway}" ]; then
    echo "Missing remote IP!"
    exit 1
fi

interface_id="`echo ${dev} | sed -e 's/[a-zA-Z]//g'`"
if [ -z "${interface_id}" ] ; then
    echo "Missing ID!"
    exit 1
fi

if [ ! -z "${ifconfig_remote}" ] ; then
    addRoute ${dev} ${ifconfig_remote} ${interface_id}
else
    addRoute ${dev} ${route_vpn_gateway} ${interface_id}
fi

/sbin/iptables -t mangle -I tunnel-vpn-${interface_id} -j ACCEPT -m comment --comment "stop processing all other rules"
/sbin/iptables -t mangle -I tunnel-vpn-${interface_id} -j MARK --set-mark $((${interface_id}<<8))/0xff00 -m comment --comment "Set destination interface to use tunnel ${interface_id}"

/sbin/iptables -t mangle -I tunnel-vpn-any -j ACCEPT -m comment --comment "Set destination interface to use tunnel ${interface_id}"
/sbin/iptables -t mangle -I tunnel-vpn-any -j MARK --set-mark $((${interface_id}<<8))/0xff00 -m comment --comment "Set destination interface to use tunnel ${interface_id}"

