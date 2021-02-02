#!/bin/bash

if [[ -x "/usr/bin/radwho" ]]
then
    CMDFILE="/usr/bin/radwho"
fi

if [[ -x "/bin/radwho" ]]
then
    CMDFILE="/bin/radwho"
fi

if [[ "$CMDFILE" == "" ]]
then
    exit 1
fi

$CMDFILE -r | awk -F',' '{ print $1, $7 }' 2> /dev/null
