#! /bin/sh
# $Id: mvvm.sh,v 1.8 2005/03/22 18:46:43 cng Exp $

MVVM_CONSOLE_LOG=${MVVM_CONSOLE_LOG:-"@PREFIX@/var/log/mvvm/console.log"}
MVVM_WRAPPER_LOG=${MVVM_WRAPPER_LOG:-"@PREFIX@/var/log/mvvm/wrapper.log"}
MVVM_LAUNCH=${MVVM_LAUNCH:-"@PREFIX@/usr/bin/bunnicula"}

reapChildHardest() {
    echo "mvvm.sh: Killing -9 all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
    kill -9 $pid
    kill -9 `ps awwx | grep java | grep bunnicula | awk '{print $1}'` 2>/dev/null
    flushIptables ; exit
}

reapChildHarder() {
    echo "mvvm.sh: Killing -15 all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
    kill $pid
    sleep 1
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "mvvm.sh: Killing -15 all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
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
    echo "mvvm.sh: shutting down bunnicula " >> $MVVM_WRAPPER_LOG
    @PREFIX@/usr/bin/mcli -t 20000 shutdown &> /dev/null
    sleep 1
    kill -INT $pid
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "mvvm.sh: Killing -INT all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
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
    echo > $MVVM_CONSOLE_LOG
    echo "============================" >> $MVVM_CONSOLE_LOG
    echo $MVVM_LAUNCH >> $MVVM_CONSOLE_LOG
    echo "============================" >> $MVVM_CONSOLE_LOG

    echo >> $MVVM_WRAPPER_LOG
    echo "============================" >> $MVVM_WRAPPER_LOG
    echo $MVVM_LAUNCH >> $MVVM_WRAPPER_LOG
    echo "============================" >> $MVVM_WRAPPER_LOG

    raiseFdLimit
    flushIptables

    $MVVM_LAUNCH $* >>$MVVM_CONSOLE_LOG 2>&1 &

    pid=$!
    echo "Bunnicula launched. (pid:$pid) (`date`)" >> $MVVM_WRAPPER_LOG
    wait $pid

    flushIptables
    echo "*** bunnicula returned $? on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
    echo "*** copied $MVVM_CONSOLE_LOG to $MVVM_CONSOLE_LOG.crash ***" >> $MVVM_WRAPPER_LOG

    cp -f $MVVM_CONSOLE_LOG $MVVM_CONSOLE_LOG.crash
    sleep 2
done
