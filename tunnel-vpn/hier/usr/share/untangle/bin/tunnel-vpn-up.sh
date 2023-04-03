#!/bin/sh
script_name=$0
exec >> /var/log/uvm/tunnel.log 2>&1

# It would be nice to match priorities to ids, but some apps like
# strongswan force priorities that would conflict.
priority_floor=1000

addRoute()
{
    /usr/share/untangle-sync-settings/bin/add-uplink.sh $1 $2 uplink.$3 -4
}

echo "`date`: ${script_name}: dev:${dev} local:${ifconfig_local} remote:${ifconfig_remote} gateway:${route_vpn_gateway}"

if [ -z "${dev}" ] ; then
    echo "Missing dev!"
    exit 1
fi
if [ -z "${ifconfig_remote}" ] && [ -z "${route_vpn_gateway}" ]; then
    echo "`date`: ${script_name}: missing remote ip address!"
    exit 1
fi

interface_id="`echo ${dev} | sed -e 's/[a-zA-Z]//g'`"
if [ -z "${interface_id}" ] ; then
    echo "`date`: ${script_name}: missing id!"
    exit 1
fi

if [ ! -z "${ifconfig_remote}" ] ; then
    addRoute ${dev} ${ifconfig_remote} ${interface_id}
else
    addRoute ${dev} ${route_vpn_gateway} ${interface_id}
fi

priority=$(($interface_id + $priority_floor))
table_name=tunnel.$interface_id
ip rule add priority $priority lookup $table_name

/sbin/iptables -t mangle -I tunnel-vpn-${interface_id} -j ACCEPT -m comment --comment "stop processing all other rules"
/sbin/iptables -t mangle -I tunnel-vpn-${interface_id} -j MARK --set-mark $((${interface_id}<<8))/0xff00 -m comment --comment "Set destination interface to use tunnel ${interface_id}"

/sbin/iptables -t mangle -I tunnel-vpn-any -j ACCEPT -m comment --comment "Set destination interface to use tunnel ${interface_id}"
/sbin/iptables -t mangle -I tunnel-vpn-any -j MARK --set-mark $((${interface_id}<<8))/0xff00 -m comment --comment "Set destination interface to use tunnel ${interface_id}"

exit 0
