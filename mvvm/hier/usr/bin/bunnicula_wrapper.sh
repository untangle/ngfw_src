#! /bin/sh
# $Id: bunnicula_wrapper.sh,v 1.8 2005/03/22 18:46:43 cng Exp $

BUNNICULA_CONSOLE_LOG=${BUNNICULA_CONSOLE_LOG:-"@PREFIX@/var/log/mvvm/console.log"}
BUNNICULA_WRAPPER_LOG=${BUNNICULA_WRAPPER_LOG:-"@PREFIX@/var/log/mvvm/wrapper.log"}
BUNNICULA_LAUNCH=${BUNNICULA_LAUNCH:-"@PREFIX@/usr/bin/bunnicula"}

reapChildHardest() {
    echo "bunnicula_wrapper.sh: Killing -9 all bunnicula \"$pid\" (`date`)" >> $BUNNICULA_WRAPPER_LOG
    kill -9 $pid
    kill -9 `ps awwx | grep java | grep bunnicula | awk '{print $1}'` 2>/dev/null
    flushIptables ; exit
}

reapChildHarder() {
    echo "bunnicula_wrapper.sh: Killing -15 all bunnicula \"$pid\" (`date`)" >> $BUNNICULA_WRAPPER_LOG
    kill $pid
    sleep 1
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "bunnicula_wrapper.sh: Killing -15 all bunnicula \"$pid\" (`date`)" >> $BUNNICULA_WRAPPER_LOG
        for i in `seq 1 5` ; do
            if [ -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
                flushIptables ; exit
            fi
            sleep 1
        done
        if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
            reapChildHardest
        fi
    fi
    flushIptables ; exit
}

reapChild() {
    echo "bunnicula_wrapper.sh: shutting down bunnicula " >> $BUNNICULA_WRAPPER_LOG
    @PREFIX@/usr/bin/mcli -t 20000 shutdown &> /dev/null
    sleep 1
    kill -INT $pid
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "bunnicula_wrapper.sh: Killing -INT all bunnicula \"$pid\" (`date`)" >> $BUNNICULA_WRAPPER_LOG
        for i in `seq 1 5` ; do
            if [ -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
                flushIptables ; exit
            fi
            sleep 1
        done
        if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
            reapChildHarder
        fi
    fi
    flushIptables ; exit
}


flushIptables() {
    iptables -t nat -F
    iptables -t mangle -F
    iptables -t filter -F
    iptables -t raw -F
}

raiseFdLimit() {
    ulimit -n 2000
    ulimit -n 4000
    ulimit -n 8000
    ulimit -n 16000
    ulimit -n 32000
    ulimit -n 64000
    ulimit -n 128000
    ulimit -n 256000
    ulimit -n 512000
    ulimit -n 1024000
}

trap reapChildHardest 6
trap reapChildHarder 15
trap reapChild 2

while true; do
    echo > $BUNNICULA_CONSOLE_LOG
    echo "============================" >> $BUNNICULA_CONSOLE_LOG
    echo $BUNNICULA_LAUNCH >> $BUNNICULA_CONSOLE_LOG
    echo "============================" >> $BUNNICULA_CONSOLE_LOG

    echo >> $BUNNICULA_WRAPPER_LOG
    echo "============================" >> $BUNNICULA_WRAPPER_LOG
    echo $BUNNICULA_LAUNCH >> $BUNNICULA_WRAPPER_LOG
    echo "============================" >> $BUNNICULA_WRAPPER_LOG

    raiseFdLimit
    flushIptables

    $BUNNICULA_LAUNCH $* >>$BUNNICULA_CONSOLE_LOG 2>&1 &

    pid=$!
    echo "Bunnicula launched. (pid:$pid) (`date`)" >> $BUNNICULA_WRAPPER_LOG
    wait $pid

    flushIptables
    echo "*** bunnicula returned $? on `date` in `pwd` ***" >> $BUNNICULA_WRAPPER_LOG
    echo "*** copied $BUNNICULA_CONSOLE_LOG to $BUNNICULA_CONSOLE_LOG.crash ***" >> $BUNNICULA_WRAPPER_LOG

    cp -f $BUNNICULA_CONSOLE_LOG $BUNNICULA_CONSOLE_LOG.crash
    sleep 2
done
