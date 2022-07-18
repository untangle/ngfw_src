#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

testDiskFree()
{
    /usr/bin/df -k / | awk '//{printf("%d",$5)}'
}

testDiskError1()
{
    /usr/bin/tail -n 15000 /var/log/kern.log | grep -m1 -B3 'DRDY ERR'
}

testDiskError2()
{
    /usr/bin/tail -n 15000 /var/log/kern.log | grep -v 'dev fd0' | grep -m1 -B3 'I/O error'
}

testUpgradeErrors()
{
    /bin/egrep -q '^Status:.*(half-configured|triggers-pending)' /var/lib/dpkg/status
}

testBridgeBackwards1()
{
    /usr/sbin/brctl showstp " + bridgeName + " | grep '^eth.*' | sed -e 's/(//g' -e 's/)//g'
}

testBridgeBackwards2()
{
    /usr/sbin/brctl showmacs " + bridgeName + " | awk '/" + gatewayMac + "/ {print $1}'
}

testInterfaceErrors()
{
    ifconfig " + intf.getPhysicalDev() + " | awk '/errors/ {print $3}'
}

testQueueFullMessages()
{
    tail -n 20 /var/log/kern.log | grep -q 'nf_queue:.*dropping packets'
}

testRoutesToReachableAddresses1()
{
   arp -n $2 | grep -q ether 
}
testRoutesToReachableAddresses2()
{
    arp -n $2 | grep -q ether
}

$1 "$@"