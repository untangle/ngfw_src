#!/bin/bash

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

copyAuthorizedKeys()
{
    if [ ! -d /root/.ssh ] ; then
        mkdir /root/.ssh
        chmod 700 /root/.ssh
    fi
    
    if [ ! -f /root/.ssh/authorized_keys2 ] ; then
        cp -f /usr/share/untangle-support-keyring/authorized_keys2 /root/.ssh/
        chmod 700 /root/.ssh/authorized_keys2
    fi
}

$1 "$@"