#! /bin/bash
# $Id$

NAME=$0

# get a bunch of default values
source @PREFIX@/etc/default/untangle-vm

UVM_CONSOLE_LOG=${UVM_CONSOLE_LOG:-"@PREFIX@/var/log/uvm/console.log"}
UVM_UVM_LOG=${UVM_UVM_LOG:-"@PREFIX@/var/log/uvm/uvm.log"}
UVM_GC_LOG=${UVM_GC_LOG:-"@PREFIX@/var/log/uvm/gc.log"}
UVM_WRAPPER_LOG=${UVM_WRAPPER_LOG:-"@PREFIX@/var/log/uvm/wrapper.log"}
UVM_LAUNCH=${UVM_LAUNCH:-"@PREFIX@/usr/share/untangle/bin/bunnicula"}

# Short enough to restart services and uvm promptly
SLEEP_TIME=15

# Used to kill a child with extreme prejudice
nukeIt() {
    echo "$NAME: Killing -9 all bunnicula \"$pid\" (`date`)" >> $UVM_WRAPPER_LOG
    kill -3 $pid
    kill -9 $pid
    kill -9 `ps awwx | grep java | grep bunnicula | awk '{print $1}'` 2>/dev/null
}

reapChildHardest() {
    nukeIt
    flushIptables ; exit
}

reapChildHarder() {
    echo "$NAME: Killing -15 all bunnicula \"$pid\" (`date`)" >> $UVM_WRAPPER_LOG
    kill $pid
    sleep 1
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "$NAME: Killing -15 all bunnicula \"$pid\" (`date`)" >> $UVM_WRAPPER_LOG
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
    echo "$NAME: shutting down bunnicula " >> $UVM_WRAPPER_LOG
    kill -3 $pid
    @PREFIX@/usr/bin/mcli -t 20000 shutdown &> /dev/null
    sleep 1
    kill -INT $pid
    if [ ! -z "`ps awwx | grep java | grep bunnicula | awk '{print $1}'`" ] ; then
        echo "$NAME: Killing -INT all bunnicula \"$pid\" (`date`)" >> $UVM_WRAPPER_LOG
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
    if [ ! -f /etc/untangle-net-alpaca/nonce ] ; then
        echo "the nonce doesn't exist, unable to regenerate rules."
        return
    fi

    local t_nonce=`head -n 1 /etc/untangle-net-alpaca/nonce`

    ## Tell the alpaca to reload the rules
    ruby <<EOF
require "xmlrpc/client"
 
client = XMLRPC::Client.new( "localhost", "/alpaca/uvm/api?argyle=${t_nonce}", 3000 )
ok, status = client.call( "generate_rules" )
EOF
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
  # the wizard has not run yet, exit right away
  [[ ! -f $ACTIVATION_KEY_FILE ]] && return

  # if the activation temp file isn't there, but the activation one
  # is, it means we already have a valid key, so we don't want to
  # proceed any further.
  #
  # Copyright TeamJanky 2007: the double test is to avoid a possible
  # race condition where we pass 1), but the other backgrounded curl
  # completes before 2), and then when we reach 3) our temp file has
  # already been deleted and we're left in the cold...
  [[ -f $ACTIVATION_KEY_FILE_TMP ]] || return
  ## Let the other curl take precedence
  ps aux | grep -q '[c]url' && return
  [[ -f $ACTIVATION_KEY_FILE_TMP ]] || return

  KEY=`cat $ACTIVATION_KEY_FILE_TMP`

  # for CD downloads, the temp key is only 0s, so we need to ask the
  # server for a brand new one; that's done by not supplying any value
  # to the CGI variable
  [[ $KEY = $FAKE_KEY ]] && KEY=""

  if curl --insecure --fail -o $TMP_ARCHIVE `printf ${ACTIVATION_URL_TEMPLATE} "$KEY" $(/usr/share/untangle/bin/utip)`; then
    echo "$NAME curl call for license key succeeded" >> $UVM_WRAPPER_LOG
    echo "$NAME $TMP_ARCHIVE is a `file $TMP_ARCHIVE`" >> $UVM_WRAPPER_LOG
    rm -f $ACTIVATION_KEY_FILE_TMP
    tar -C / -xf $TMP_ARCHIVE
    @UVM_HOME@/bin/utactivate
    @UVM_HOME@/bin/utregister # register under new key
  else
    echo "$NAME curl call for license key succeeded (RC=$?)" >> $UVM_WRAPPER_LOG    
  fi
}

isServiceRunning() {
  extraArgs=""
  if [[ -n $2 ]] ; then
    extraArgs="-x"
    shift
  fi
  # If you can believe it, pidof sometimes doesn't work!
  # Calling it three times seems to be enough to work around the pidof
  # bug and "make sure" (#2534)
  let i=0
  while [[ $i -lt 3 ]] ; do
    pidof $extraArgs "$1" && return 0
    let i=$i+1
    sleep .5
  done
  return 1
}

restartServiceIfNeeded() {
  serviceName=$1

  case $serviceName in
    postgresql)
# Removing the postgres pid file just makes restarting harder.  The init.d script deals ok as is.
      pidFile=/tmp/foo-doesnt-exist
      isServiceRunning $PG_DAEMON_NAME && return
      serviceName=$PGSERVICE
      ;;
    slapd)
      pidFile=/var/run/slapd/slapd.pid
      dpkg -l untangle-ldap-server | grep -q -E '^ii' || return
      isServiceRunning slapd && return
      ;;
    spamassassin)
      pidFile=$SPAMASSASSIN_PID_FILE
      dpkg -l untangle-spamassassin-update | grep -q -E '^ii' || return
      isServiceRunning --find-shell spamd && return
      ;;
    clamav-daemon)
      pidFile=$CLAMD_PID_FILE
      dpkg -l untangle-clamav-config | grep -q -E '^ii' || return
      isServiceRunning clamd && return
      ;;
    clamav-freshclam)
      pidFile="/var/run/clamav/freshclam.pid"
      dpkg -l untangle-clamav-config | grep -q -E '^ii' || return
      isServiceRunning freshclam && return
      ;;
    untangle-support-agent)
      pidFile="/var/run/rbot.pid"
      dpkg -l untangle-support-agent | grep -q -E '^ii' || return
      # this is a bit janky, need something better...
      isServiceRunning ruby && return
      ;;
  esac

  restartService $serviceName $pidFile "missing"
}

restartService() {
  serviceName=$1
  pidFile=$2
  reason=$3
  stopFirst=$4
  echo "*** restarting $reason $serviceName on `date` ***" >> $UVM_WRAPPER_LOG
  if [ -n "$stopFirst" ] ; then
    # netstat -plunt >> $UVM_WRAPPER_LOG
    [ -n "$pidFile" ] && pid=`cat $pidFile`
    /etc/init.d/$serviceName stop
    # just to be sure
    [ -n "$pid" ] && kill -9 $pid
  else # remove the pidfile
    [ -n "$pidFile" ] && rm -f $pidFile
  fi
  /etc/init.d/$serviceName start
}

# Return true (0) when we need to reap and restart the uvm.
needToRestart() {
    cheaphigh=`head -3 /proc/$pid/maps | tail -1 | awk '{ high=split($1, arr, "-"); print arr[2]; }'`
    if [ -z $cheaphigh ]; then
        # not fatal, process has probably just died, which we'll catch soon.
        echo "*** no heap size ($cheaphigh) on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
    else
        bignibble=${cheaphigh:0:1}
        case $bignibble in
            0 | 1)
                # less than 384Meg native heap
                ;;
            2)
                # 384Meg < native heap < 640Meg
                if [ $MEM -lt 1000000 ] || [ `date +%H` -eq 1 ] ; then
                    echo "*** bunnicula heap soft limit on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
                    return 0;
                fi
                ;;
            3 | 4 | 5 | 6 | 7 | 8 | 9)
                # native heap > 640Meg
                echo "*** bunnicula heap hard limit ($bignibble) on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
                return 0;
                ;;
            *)
                echo "*** unexpected heap size ($bignibble) on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
                ;;
        esac
    fi

    #  garbage collection failure (usually happens when persistent heap full)
    cmfcount=`tail -50 $UVM_GC_LOG | grep -ci "concurrent mode failure"`
    if [ $cmfcount -gt 2 ]; then
        echo "*** java heap cmf on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
        return 0;
    fi

    # extra nightime checks
    if [ `date +%H` -eq 1 ]; then
        # VSZ greater than 1.1 gigs reboot
        VIRT="`cat /proc/$pid/status | grep VmSize | awk '{print $2}'`"
        if [ $VIRT -gt $MAX_VIRTUAL_SIZE ] ; then
            echo "*** Virt Size too high ($VIRT) on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
            return 0;
        fi
    fi

    return 1;
}


trap reapChildHardest 6
trap reapChildHarder 15
trap reapChild 2

while true; do
    echo > $UVM_CONSOLE_LOG
    echo "============================" >> $UVM_CONSOLE_LOG
    echo $UVM_LAUNCH >> $UVM_CONSOLE_LOG
    echo "============================" >> $UVM_CONSOLE_LOG

    echo >> $UVM_WRAPPER_LOG
    echo "============================" >> $UVM_WRAPPER_LOG
    echo $UVM_LAUNCH >> $UVM_WRAPPER_LOG
    echo "============================" >> $UVM_WRAPPER_LOG

    raiseFdLimit
    flushIptables

    $UVM_LAUNCH $* >>$UVM_CONSOLE_LOG 2>&1 &

    pid=$!
    echo "Bunnicula launched. (pid:$pid) (`date`)" >> $UVM_WRAPPER_LOG

# Instead of waiting, we now monitor.

    counter=0

    while true; do

        # try to fetch a key right away; bg'ed so as to not block the
        # rest of the uvm.sh tasks. We ensure only one key-fetching
        # function runs at all times
        getLicenseKey &

        sleep $SLEEP_TIME
	let counter=${counter}+$SLEEP_TIME

        if [ "x" = "x@PREFIX@" ] ; then
            if [ ! -d /proc/$pid ] ; then
                echo "*** restarting missing bunnicula $? on `date` ***" >> $UVM_WRAPPER_LOG
                break
            fi
            if needToRestart; then
                echo "*** need to restart bunnicula $? on `date` ***" >> $UVM_WRAPPER_LOG
                nukeIt
                break
            fi
            restartServiceIfNeeded postgresql
            restartServiceIfNeeded clamav-freshclam
            restartServiceIfNeeded clamav-daemon
            restartServiceIfNeeded spamassassin
            restartServiceIfNeeded slapd
            restartServiceIfNeeded untangle-support-agent
	    
        fi

	if [ $counter -gt 60 ] ; then # fire up the other nannies
	  if dpkg -l untangle-spamassassin-update | grep -q ii ; then # we're managing spamassassin
	      [ `tail -n 50 /var/log/mail.info | grep -c "$SPAMASSASSIN_LOG_ERROR"` -gt 2 ] && restartService spamassassin $SPAMASSASSIN_PID_FILE "non-functional" stopFirst
	      $BANNER_NANNY $SPAMASSASSIN_PORT $TIMEOUT || restartService spamassassin $SPAMASSASSIN_PID_FILE "hung" stopFirst
	  fi
	  if dpkg -l untangle-clamav-config | grep -q -E '^ii' ; then # we're managing clamav
              $BANNER_NANNY $CLAMD_PORT $TIMEOUT || restartService clamav-daemon $CLAMD_PID_FILE "hung" stopFirst
	  fi
	  if dpkg -l untangle-support-agent | grep -q ii ; then # support-agent is supposed to run
	      if [ -f "$SUPPORT_AGENT_PID_FILE" ] && ps `cat $SUPPORT_AGENT_PID_FILE` > /dev/null ; then # it runs
	          if [ $(ps -o %cpu= `cat $SUPPORT_AGENT_PID_FILE` | perl -pe 's/\..*//') -gt $SUPPORT_AGENT_MAX_ALLOWED_CPU ] ; then
		      restartService untangle-support-agent $SUPPORT_AGENT_PID_FILE "spinning" stopFirst
	          fi
	      fi
	  fi
	  counter=0
	fi
    done

# Clean up the zombie.  Risky? XXX
#    wait $pid

# Crash/Kill
    flushIptables
    echo "*** bunnicula exited on `date` in `pwd` ***" >> $UVM_WRAPPER_LOG
    echo "*** copied $UVM_CONSOLE_LOG to $UVM_CONSOLE_LOG.crash ***" >> $UVM_WRAPPER_LOG
    echo "*** copied $UVM_UVM_LOG to $UVM_UVM_LOG.crash ***" >> $UVM_WRAPPER_LOG
    echo "*** copied $UVM_GC_LOG to $UVM_GC_LOG.crash ***" >> $UVM_WRAPPER_LOG
    cp -fa $UVM_CONSOLE_LOG.crash.1 $UVM_CONSOLE_LOG.crash.2
    cp -fa $UVM_CONSOLE_LOG.crash $UVM_CONSOLE_LOG.crash.1
    cp -fa $UVM_CONSOLE_LOG $UVM_CONSOLE_LOG.crash
    cp -fa $UVM_UVM_LOG.crash.1 $UVM_UVM_LOG.crash.2
    cp -fa $UVM_UVM_LOG.crash $UVM_UVM_LOG.crash.1
    cp -fa $UVM_UVM_LOG $UVM_UVM_LOG.crash
    cp -fa $UVM_GC_LOG.crash.1 $UVM_GC_LOG.crash.2
    cp -fa $UVM_GC_LOG.crash $UVM_GC_LOG.crash.1
    cp -fa $UVM_GC_LOG $UVM_GC_LOG.crash

    sleep 2
    echo "*** restarting on `date` ***" >> $UVM_WRAPPER_LOG
done
