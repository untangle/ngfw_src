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

ip route show table ${table_name} >/dev/null 2>&1
if [ $? -ne 0 ] ; then
    # Expected tunnel does not exist
    echo "`date`: ${script_name}: unable to find table ${table name}"
fi

index=1
while true; do
    eval "route_network=\${route_network_$index}"
    eval "route_netmask=\${route_netmask_$index}"
    eval "route_gateway=\${route_gateway_$index}"
    if [ "$route_network" = "" ] ; then
        break
    fi
    index=$((index+1))

    if [ "$route_network" = "0.0.0.0" ] || [ "$route_netmask" = "0.0.0.0" ] ; then
        echo "ignoring default as a passed route: $route_network/$route_netmask"
        continue
    fi

    command="ip route add table $table_name $route_network/$route_netmask via $route_gateway dev ${dev}"
    echo "`date`: ${script_name}: $command"
    eval $command
done

