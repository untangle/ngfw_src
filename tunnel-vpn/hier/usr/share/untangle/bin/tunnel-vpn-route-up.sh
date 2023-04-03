#!/bin/sh
script_name=$0

exec >> /var/log/uvm/tunnel.log 2>&1

echo "`date`: ${script_name}: dev:${dev} local:${ifconfig_local} remote:${ifconfig_remote} gateway:${route_vpn_gateway}"

interface_id="`echo ${dev} | sed -e 's/[a-zA-Z]//g'`"
if [ -z "${interface_id}" ] ; then
    echo "`date`: ${script_name}: missing id!"
    exit 1
fi

table_name=tunnel.$interface_id

index=1
while true; do
    eval "route_network=\${route_network_$index}"
    eval "route_netmask=\${route_netmask_$index}"
    eval "route_gateway=\${route_gateway_$index}"
    if [ "$route_network" = "" ] ; then
        break
    fi
    index=$((index+1))

    command="ip route add table $table_name $route_network/$route_netmask via $route_gateway dev ${dev}"
    echo "`date`: ${script_name}: $command"
    eval $command
done

