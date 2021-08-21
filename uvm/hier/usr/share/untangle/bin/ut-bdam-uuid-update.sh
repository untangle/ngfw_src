#!/bin/bash

CONFFILE=/etc/bdamserver/bdamserver.conf
MATCH='++'

if [ -f $CONFFILE ]; then
    url=`egrep "^UpdateURLAntivirus" /etc/bdamserver/bdamserver.conf | awk -F \= '{print $2}'`
    if [[ -z $url ]]; then
        exit 0
    fi
    if [[ "$url" == *"$MATCH"* ]]; then
        exit 0
    fi

    uid=`cat /usr/share/untangle/conf/uid`

    newurl="UpdateURLAntivirus=$url++$uid"
    sed -i "/^UpdateURLAntivirus/c $newurl" $CONFFILE
fi