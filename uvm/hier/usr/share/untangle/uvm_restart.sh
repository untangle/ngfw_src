#!/bin/sh

resetBunnicula() {
## Stop the UVM
    @PREFIX@/etc/init.d/uvm stop

    sleep 1

## Kill networking
    /etc/init.d/networking stop

    sleep 1
    
## Kill any leftover pump processes, they are restarted if necessary
    killall pump
    
    sleep 1
    
## Restart networking
    /etc/init.d/networking start

## Apt-get update
    apt-get update 
    
    sleep 1
    
## Restart the UVM
    @PREFIX@/etc/init.d/uvm start
}

## Execute these functions in a separate detached process, this way
## when bunnicula gets killed this process doesn't exit.
if [ $# -eq 0 ]; then
    sleep 1
    ## Just append any arguments, they don't matter
    nohup sh @PREFIX@/usr/share/metavize/uvm_restart.sh 1 2 > @PREFIX@/var/log/uvm/restart.log 2>&1 &
else
    resetBunnicula
fi

