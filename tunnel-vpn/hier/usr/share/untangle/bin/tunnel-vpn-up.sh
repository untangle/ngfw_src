#!/bin/sh

echo
echo "`date`"
echo "bytes_received: ${bytes_received}"
echo "bytes_sent: ${bytes_sent}"
echo "common_name: ${common_name}"
echo "config: ${config}"
echo "daemon: ${daemon}"
echo "daemon_log_redirect: ${daemon_log_redirect}"
echo "dev: ${dev}"
echo "dev_idx: ${dev_idx}"
echo "ifconfig_broadcast: ${ifconfig_broadcast}"
echo "ifconfig_ipv6_local: ${ifconfig_ipv6_local}"
echo "ifconfig_ipv6_netbits: ${ifconfig_ipv6_netbits}"
echo "ifconfig_ipv6_remote: ${ifconfig_ipv6_remote}"
echo "ifconfig_local: ${ifconfig_local}"
echo "ifconfig_remote: ${ifconfig_remote}"
echo "ifconfig_netmask: ${ifconfig_netmask}"
echo "ifconfig_pool_local_ip: ${ifconfig_pool_local_ip}"
echo "ifconfig_pool_netmask: ${ifconfig_pool_netmask}"
echo "ifconfig_pool_remote_ip: ${ifconfig_pool_remote_ip}"
echo "link_mtu: ${link_mtu}"
echo "local: ${local}"
echo "local_port: ${local_port}"
echo "password: ${password}"
echo "proto: ${proto}"
echo "route_net_gateway: ${route_net_gateway}"
echo "route_vpn_gateway: ${route_vpn_gateway}"
echo "peer_cert: ${peer_cert}"
echo "script_context: ${script_context}"
echo "script_type: ${script_type}"
echo "signal: ${signal}"
echo "time_ascii: ${time_ascii}"
echo "time_duration: ${time_duration}"
echo "time_unix: ${time_unix}"
echo "tun_mtu: ${tun_mtu}"
echo "trusted_port: ${trusted_port}"
echo "untrusted_port: ${untrusted_port}"
echo "username: ${username}"
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



