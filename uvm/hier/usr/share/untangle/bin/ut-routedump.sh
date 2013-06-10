#!/bin/dash

# This script just prints most of the relevent routing rules/tables
# to show the current routing state of the system

echo " = Rules = "
ip rule ls
echo 

echo " = Table main = "
ip route show table main | grep -v '192.0.2.42'
echo 

echo " = Table balance = "
ip route show table balance
echo 

cat /etc/iproute2/rt_tables | grep uplink | awk '{print $2}' | while read table ; do
    echo " = Table $table = "
    ip route show table $table
    echo
done

echo " = Route rules = "
iptables -t mangle -nL splitd-route-rules 2>/dev/null | grep -v Chain 
echo