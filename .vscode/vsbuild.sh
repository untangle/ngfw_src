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

if [ $(grep -c javac $RAKEOUTPUT) -gt 0 ] ; then 
    ./dist/etc/init.d/untangle-vm restart
fi
