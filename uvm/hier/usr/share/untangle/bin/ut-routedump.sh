#!/bin/dash

# This script just prints most of the relevent routing rules/tables
# to show the current routing state of the system

echo " = IPv4 Rules = "
ip -4 rule ls
echo

echo " = IPv4 Table main = "
ip -4 route show table main | grep -v '192.0.2.'
echo

echo " = IPv4 Table balance = "
ip -4 route show table balance
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

echo " = IPv6 Table main = "
ip -6 route show table main
echo

# Currently there is no balance table for IPv6
#echo " = IPv6 Table balance = "
#ip -6 route show table balance
#echo

awk '/uplink/ {print $2}' /etc/iproute2/rt_tables | while read table ; do
    echo " = IPv6 Table $table = "
    ip -6 route show table $table
    echo
done

