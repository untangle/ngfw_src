#!/bin/dash

# This script just prints most of the relevent routing rules/tables
# to show the current routing state of the system

echo " = IPv4 Rules = "
ip -4 rule ls
echo

for i in main balance default local ; do
    echo " = IPv4 Table $i = "
    ip -4 route show table $i
    echo
done

echo " = IPv4 Dynamic Routing = "
ip -4 route show proto zebra
echo

awk '/uplink/ {print $2}' /etc/iproute2/rt_tables | while read table ; do
    echo " = IPv4 Table $table = "
    ip -4 route show table $table
    echo
done

echo " = IPv4 Route Rules = "
iptables -t mangle -nL wan-balancer-route-rules 2>/dev/null | grep -v -E '(Chain|target)'

echo
echo
echo


echo " = IPv6 Rules = "
ip -6 rule ls
echo

# no balance table for IPv6
for i in main default local ; do
    echo " = IPv6 Table $i = "
    ip -6 route show table $i
    echo
done

awk '/uplink/ {print $2}' /etc/iproute2/rt_tables | while read table ; do
    echo " = IPv6 Table $table = "
    ip -6 route show table $table
    echo
done

