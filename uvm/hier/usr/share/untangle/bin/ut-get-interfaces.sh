#!/bin/bash

# This script contains a helper functions which were moved out
# of UVM to accomplish additional security hardning.

/usr/bin/find /sys/class/net -type l -name $1* | sed -e 's|/sys/class/net/||' | sort -n -k 1.4