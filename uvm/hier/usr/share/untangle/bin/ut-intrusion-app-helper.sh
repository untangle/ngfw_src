#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

getSuricataErrors() {
    /bin/journalctl -u suricata --no-pager -S \"$(/bin/systemctl show suricata | grep StateChangeTimestamp= | cut -d= -f2)\" | grep '<Error' | grep -v 'libnet_write_raw_ipv6 failed'
}

getStatusCommand() {
    /usr/bin/tail -20 /var/log/suricata/suricata.log | /usr/bin/tac
}
$1 "$@"