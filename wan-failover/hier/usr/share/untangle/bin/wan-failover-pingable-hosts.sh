#!/bin/dash

. @PREFIX@/usr/share/untangle/bin/wan-failover-test.sh $@

traceroute -n -I -s ${WAN_FAILOVER_PRIMARY_ADDRESS} google.com 2>/dev/null | awk '/^ *[0-9]/ { if (( $1 > 2 ) && ( $1 < 9) && ( $2 != "*" ))  print $2 }'

true
