#!/bin/bash
##
## Return status for networking interfaces, services, etc.
##

# get_interface_transfer
#
# Get interface transfer information and return in the format of:
# link/ether 00:15:5d:19:bf:60 660578145 667856 0 0 0 53470 28565686 381365 0 0 0 0
#
# @param $2 device name (e.g.,eth0)
get_interface_transfer()
{
    ip -s -d link show dev $2 | \
        sed -n -e '/link/{p}' -e '/RX/{n;p}' -e '/TX/{n;p}' | \
        sed -e 's/brd .*$//g' | \
        sed -e 's/promiscuity .*$/00:00:00:00:00:00/g' | \
        tr '\n' ' ' | \
        tr -s ' '
}

# get_interface_ip_addresses
#
# Get IP addresses for the interface and return in the format of:
# inet 192.168.25.58/29 brd 192.168.25.63 scope global eth0
#
# @param $2 device name (e.g.,eth0)
get_interface_ip_addresses()
{
    ip addr show dev $2 | \
        grep inet | \
        grep global | \
        tr '\n' ' ' | \
        tr -s ' '
}

# get_interface_arp_table
#
# Get ARP table addresses for the interface and return in the format of:
# 192.168.253.10 lladdr 00:15:5d:19:bf:2f REACHABLE
# 192.168.253.51 lladdr 00:15:5d:19:bf:36 STALE
# 192.168.253.25 lladdr 00:15:5d:19:bf:54 STALE
#
# @param $2 device name (e.g.,eth0)
get_interface_arp_table()
{
    ip neigh show dev $2 | \
        grep lladdr | \
        tr -s ' '
}

# get_dynamic_routing_table
#
# Get routes from dynamic routing in the format of:
# 192.168.25.64/29 via 192.168.25.58 dev eth0 metric 2
#
# @param None
get_dynamic_routing_table()
{
    ip route show proto zebra | \
        tr -s " " 
}

# get_dynamic_routing_bgp
#
# Get BGP status in the format of:
# 192.168.25.58 4 1234 0 2788 0 0 0 never Active
#
# @param None
get_dynamic_routing_bgp()
{
    vtysh -c 'show ip bgp summary' | \
        sed -e '/Neighbor/,$!d' | \
        sed -e '/Total/,$d' -e '1d' | \
        tr -s ' '
}

# get_dynamic_routing_ospf
#
# Get OSPF status in the format of:
# 172.217.4.110 1 Full/DR 37.840s 192.168.25.58 eth0:192.168.25.59 0 0 0
#
# @param None
get_dynamic_routing_ospf()
{
    vtysh -c 'show ip ospf neighbor' | \
        sed -e '/Neighbor/,$!d' | \
        sed -e '/Total/,$d' -e '1d' | \
        tr -s ' '
}

# get_routing_table
#
# Get full routing table.
#
# @param None
get_routing_table()
{
    /usr/share/untangle/bin/ut-routedump.sh
}

# get_routing_qos
#
# Get QOS status
#
# @param None
get_qos()
{
    /usr/share/untangle/bin/qos-status.py
}

# get_dhcp_leases
#
# Get DHCP leases
#
# @param None
get_dhcp_leases()
{
    cat /var/lib/misc/dnsmasq.leases
}

$1 "$@"
