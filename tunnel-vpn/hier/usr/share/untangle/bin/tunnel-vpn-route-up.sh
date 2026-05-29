#!/bin/bash
script_name=$0

exec >> /var/log/uvm/tunnel.log 2>&1

echo "`date`: ${script_name}: dev:${dev} local:${ifconfig_local} remote:${ifconfig_remote} gateway:${route_vpn_gateway}"

interface_id="`echo ${dev} | sed -e 's/[a-zA-Z]//g'`"
if [ -z "${interface_id}" ] ; then
    echo "`date`: ${script_name}: missing id!"
    exit 1
fi

table_name=tunnel.$interface_id

echo "table_name=$table_name"

ip rule | grep ${table_name} >/dev/null 2>&1
if [ $? -ne 0 ] ; then
    # Expected tunnel does not exist
    echo "`date`: ${script_name}: unable to find table ${table_name}"
fi

_valid_ipv4() { [[ $1 =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; }
_valid_dev()  { [[ $1 =~ ^[a-zA-Z0-9_.-]{1,15}$ ]]; }

index=1
while true; do
    nv="route_network_$index"; route_network="${!nv}"
    nm="route_netmask_$index"; route_netmask="${!nm}"
    gw="route_gateway_$index"; route_gateway="${!gw}"
    if [ -z "$route_network" ] ; then
        break
    fi
    index=$((index+1))

    if [ "$route_network" = "0.0.0.0" ] || [ "$route_netmask" = "0.0.0.0" ] ; then
        echo "ignoring default as a passed route: $route_network/$route_netmask"
        continue
    fi

    if ! _valid_ipv4 "$route_network" || ! _valid_ipv4 "$route_netmask" \
       || ! _valid_ipv4 "$route_gateway" || ! _valid_dev "$dev" ; then
        logger -t tunnel-vpn "Rejected unsafe route push: net=$route_network mask=$route_netmask gw=$route_gateway dev=$dev"
        echo "`date`: ${script_name}: rejected unsafe route push: net=$route_network mask=$route_netmask gw=$route_gateway dev=$dev"
        continue
    fi

    echo "`date`: ${script_name}: ip route add table $table_name $route_network/$route_netmask via $route_gateway dev $dev"
    ip route add table "$table_name" "$route_network/$route_netmask" via "$route_gateway" dev "$dev"
done
