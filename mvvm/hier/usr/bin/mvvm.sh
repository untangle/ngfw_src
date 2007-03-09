#! /bin/sh
# $Id$

# get a bunch of default values
source @PREFIX@/etc/default/mvvm

MVVM_CONSOLE_LOG=${MVVM_CONSOLE_LOG:-"@PREFIX@/var/log/mvvm/console.log"}
MVVM_MVVM_LOG=${MVVM_MVVM_LOG:-"@PREFIX@/var/log/mvvm/mvvm.log"}
MVVM_GC_LOG=${MVVM_GC_LOG:-"@PREFIX@/var/log/mvvm/gc.log"}
MVVM_WRAPPER_LOG=${MVVM_WRAPPER_LOG:-"@PREFIX@/var/log/mvvm/wrapper.log"}
MVVM_LAUNCH=${MVVM_LAUNCH:-"@PREFIX@/usr/bin/bunnicula"}

# fucking hideous.  XXX
PGMV82="`dpkg --get-selections postgresql-mv-8.2 | egrep 'install$' | awk '{print $1}'`"
PGMV81="`dpkg --get-selections postgresql-mv-8.1 | egrep 'install$' | awk '{print $1}'`"
if [ ! -z "$PGMV82" ] ; then
    PGDATA=${POSTGRES_DATA:-/var/lib/postgresql/8.2/main}
    PGSERVICE="postgresql-8.2"
elif [ ! -z "$PGMV81" ] ; then
    PGDATA=${POSTGRES_DATA:-/var/lib/postgresql/8.1/main}
    PGSERVICE="postgresql-8.1"
else
    PGDATA=${POSTGRES_DATA:-/var/lib/postgres/data}
    PGSERVICE="postgresql"
fi

# Short enough to restart mvvm promptly
SLEEP_TIME=5

# Used to kill a child with extreme prejudice
nukeIt() {
    echo "mvvm.sh: Killing -9 all bunnicula \"$pid\" (`date`)" >> $MVVM_WRAPPER_LOG
    kill -3 $pid
    kill -9 $pid
    kill -9 `ps awwx | grep java | grep bunnicula | awk '{print $1}'` 2>/dev/null
}

reapChildHardest() {
    nukeIt
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
    kill -3 $pid
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
    /sbin/iptables -t nat -F
    /sbin/iptables -t mangle -F
    /sbin/iptables -t filter -F
    /sbin/iptables -t raw -F
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

getLicenseKey() {
  # if the temp file isn't there, it means we already have a valid key
  # Copyright TeamJanky 2007: the double test is to avoid a possible
  # race condition where we pass 1), but the other backgrounded curl
  # completes before 2), and then when we reach 3) our temp file has
  # already been deleted and we're left in the cold...
  [[ -f $ACTIVATION_KEY_FILE_TMP ]] || return
  killall curl
  [[ -f $ACTIVATION_KEY_FILE_TMP ]] || return

  KEY=`cat $ACTIVATION_KEY_FILE_TMP`

  # for CD downloads, the temp key is only 0s, so we need to ask the 
  # server for a brand new one; that's done by not supplying any value
  # to the CGI variable
  [[ $KEY = $FAKE_KEY ]] && KEY="" 

  if curl --insecure --fail -o $TMP_ARCHIVE ${ACTIVATION_URL}$KEY ; then
    tar -C / -xf $TMP_ARCHIVE
    rm -f $ACTIVATION_KEY_FILE_TMP
    /usr/bin/mvactivate
    /usr/bin/mvregister # trigger root passwd generation
  fi
}

isServiceRunning() {
  ps aux -w -w | grep -v "grep -q $1" | grep -q "$1"
  return $?
}

restartServiceIfNeeded() {
  serviceName=$1

  needToRun=no

  case $serviceName in
    postgresql)
      pidFile=$PGDATA/postmaster.pid
      isServiceRunning postmaster && return
      serviceName=$PGSERVICE
      needToRun=yes # always has to run
      ;;
    slapd)
      pidFile=/var/run/slapd/slapd.pid
      isServiceRunning slapd && return
      needToRun=yes # always has to run
      ;;
    spamassassin)
      pidFile=/var/run/spamd.pid
      isServiceRunning spamd && return
      confFile=/etc/default/spamassassin
      [ -f $confFile ] && grep -q ENABLED=1 $confFile && needToRun=yes
      ;;
    clamav-daemon)
      pidFile="/var/run/clamav/clamd.pid"
      isServiceRunning clamd && return
      dpkg -l clamav-daemon | grep -q -E '^ii' && needToRun=yes
      ;;
    clamav-freshclam)
      pidFile="/var/run/clamav/freshclam.pid"
      isServiceRunning freshclam && return
      dpkg -l clamav-freshclam | grep -q -E '^ii' && needToRun=yes
      ;;
  esac

  if [ $needToRun == "yes" ] ; then
    echo "*** restarting missing $serviceName on `date` ***" >> $MVVM_WRAPPER_LOG
    rm -f $pidFile
#    /etc/init.d/$serviceName stop
    /etc/init.d/$serviceName start
  fi
}

# Return true (0) when we need to reap and restart the mvvm.
needToRestart() {
    cheaphigh=`head -3 /proc/$pid/maps | tail -1 | awk '{ high=split($1, arr, "-"); print arr[2]; }'`
    if [ -z $cheaphigh ]; then
        # not fatal, process has probably just died, which we'll catch soon.
        echo "*** no heap size ($cheaphigh) on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
    else
        bignibble=${cheaphigh:0:1}
        case $bignibble in
            0 | 1)
                # less than 384Meg native heap
                ;;
            2)
                # 384Meg < native heap < 640Meg
                if [ $MEM -lt 1000000 ] || [ `date +%H` -eq 1 ] ; then
                    echo "*** bunnicula heap soft limit on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
                    return 0;
                fi
                ;;
            3 | 4 | 5 | 6 | 7 | 8 | 9)
                # native heap > 640Meg
                echo "*** bunnicula heap hard limit ($bignibble) on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
                return 0;
                ;;
            *)
                echo "*** unexpected heap size ($bignibble) on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
                ;;
        esac
    fi

    # gc failure (persistent heap full)
    cmfcount=`tail -50 $MVVM_GC_LOG | grep -ci "concurrent mode failure"`
    if [ $cmfcount -gt 2 ]; then
        echo "*** java heap cmf on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
        return 0;
    fi

    # extra nightime checks
    if [ `date +%H` -eq 1 ]; then
        # VSZ greater than 1.1 gigs reboot
        VIRT="`cat /proc/$pid/status | grep VmSize | awk '{print $2}'`"
        if [ $VIRT -gt $MAX_VIRTUAL_SIZE ] ; then
            echo "*** Virt Size too high ($VIRT) on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
            return 0;
        fi
    fi

    return 1;
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

# Instead of waiting, we now monitor.
    while true; do

        # try to fetch a key right away; bg'ed so as to not block the
        # rest of the mvvm.sh tasks. We ensure only one key-fetching function runs at all times

        getLicenseKey &

        sleep $SLEEP_TIME
        if [ ! -d /proc/$pid ] ; then
            echo "*** restarting missing bunnicula $? on `date` ***" >> $MVVM_WRAPPER_LOG
            break
        fi
        if needToRestart; then
            echo "*** need to restart bunnicula $? on `date` ***" >> $MVVM_WRAPPER_LOG
            nukeIt
            break
        fi
        restartServiceIfNeeded postgresql
	restartServiceIfNeeded clamav-freshclam
	restartServiceIfNeeded clamav-daemon
	restartServiceIfNeeded spamassassin
	restartServiceIfNeeded slapd
    done

# Clean up the zombie.  Risky? XXX
#    wait $pid

# Crash/Kill
    flushIptables
    echo "*** bunnicula exited on `date` in `pwd` ***" >> $MVVM_WRAPPER_LOG
    echo "*** copied $MVVM_CONSOLE_LOG to $MVVM_CONSOLE_LOG.crash ***" >> $MVVM_WRAPPER_LOG
    echo "*** copied $MVVM_MVVM_LOG to $MVVM_MVVM_LOG.crash ***" >> $MVVM_WRAPPER_LOG
    echo "*** copied $MVVM_GC_LOG to $MVVM_GC_LOG.crash ***" >> $MVVM_WRAPPER_LOG
    cp -fa $MVVM_CONSOLE_LOG.crash.1 $MVVM_CONSOLE_LOG.crash.2
    cp -fa $MVVM_CONSOLE_LOG.crash $MVVM_CONSOLE_LOG.crash.1
    cp -fa $MVVM_CONSOLE_LOG $MVVM_CONSOLE_LOG.crash
    cp -fa $MVVM_MVVM_LOG.crash.1 $MVVM_MVVM_LOG.crash.2
    cp -fa $MVVM_MVVM_LOG.crash $MVVM_MVVM_LOG.crash.1
    cp -fa $MVVM_MVVM_LOG $MVVM_MVVM_LOG.crash
    cp -fa $MVVM_GC_LOG.crash.1 $MVVM_GC_LOG.crash.2
    cp -fa $MVVM_GC_LOG.crash $MVVM_GC_LOG.crash.1
    cp -fa $MVVM_GC_LOG $MVVM_GC_LOG.crash

    sleep 2
    echo "*** restarting on `date` ***" >> $MVVM_WRAPPER_LOG
done
