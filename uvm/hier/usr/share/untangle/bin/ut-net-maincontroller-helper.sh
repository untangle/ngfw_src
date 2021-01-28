#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

getInterfaceStatus1()
{
    ip -s -d link show dev ${2}| sed -n -e "/link/{p}" -e "/RX/{n;p}" -e "/TX/{n;p}" | sed -e "s/brd .*$//g" | sed -e "s/promiscuity .*$/00:00:00:00:00:00/g" | tr "\\n" " " | tr -s " "
}

getInterfaceStatus2()
{
    ip addr show dev ${2} | grep inet | grep global | tr "\\n" " " | tr -s " "
}

getInterfaceArp1()
{
    ip neigh show dev ${2} | grep lladdr | tr -s " "
}

getDynamicRoutingStatus1()
{
    ip route show proto zebra | tr -s " " 
}
getDynamicRoutingStatus2()
{
    vtysh -c "show ip bgp summary" | sed -e "/Neighbor/,\\$!d" | sed -e "/Total/,\\$d" -e "1d" | tr -s " "
}
getDynamicRoutingStatus3()
{
    vtysh -c "show ip ospf neighbor" | sed -e "/Neighbor/,\\$!d" | sed -e "1d" | tr -s " "
}
$1 "$@"
