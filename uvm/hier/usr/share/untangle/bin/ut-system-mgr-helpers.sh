#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

downloadUpgrades()
{
    apt-get dist-upgrade --yes --print-uris | awk '/^.http/ {print $1}'
}

upgradesAvailable()
{
    apt-get -s dist-upgrade | grep -q '^Inst'
}

diskHealthCheck()
{
    python3 /usr/lib/python3/dist-packages/uvm/disk_health.py
}

$1 "$@"