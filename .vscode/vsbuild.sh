#!/bin/bash
##
## Compile ngfw and restart uvm if neccessary.
##
RAKEOUTPUT=/tmp/rakeoutput.txt
rm -f $RAKEOUTPUT
rake |& tee $RAKEOUTPUT

if [ $(grep -c "missing documentation" $RAKEOUTPUT) -gt 0 ] ; then 
    exit
fi

if [ $(grep -c error $RAKEOUTPUT) -gt 0 ] ; then 
    exit
fi

if [ $(grep -c "jslint failed" $RAKEOUTPUT) -gt 0 ] ; then 
    exit
fi

RESTART=0
if [ $(grep -c javac $RAKEOUTPUT) -gt 0 ] ; then 
    RESTART=1
    ./dist/etc/init.d/untangle-vm restart
fi

if [ -f .vscode/remote-hosts ]; then
    while read address; do
        [[ $address = \#* ]] && continue
        echo "Synchronzing begin with $address"
        rsync -a dist/usr/share/untangle/lib root@$address:/usr/share/untangle
        rsync -a dist/usr/share/untangle/bin root@$address:/usr/share/untangle
        rsync -a dist/usr/share/untangle/web root@$address:/usr/share/untangle

        if [ $RESTART -eq 1 ] ; then
            ssh root@$address "/etc/init.d/untangle-vm restart"
        fi
        echo "Synchronzing end with $address"
    done < .vscode/remote-hosts
fi
