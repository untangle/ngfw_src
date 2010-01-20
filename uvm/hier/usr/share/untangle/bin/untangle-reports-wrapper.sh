#! /bin/sh

RESTART_FILE=@PREFIX@/etc/untangle/reports-to-restart
DAEMON=@PREFIX@/usr/lib/python2.5/reports/server.py
LOGFILE=@PREFIX@/var/log/uvm/reporter.log

pid=foo

while true ; do

  if ! ps -p $pid > /dev/null ; then
    $DAEMON 2>> $LOGFILE &
    pid=$!
  fi

  sleep 30
  if [ -f $RESTART_FILE ] ; then
    rm -f $RESTART_FILE
    pkill -f $DAEMON
    exit 0
  fi

done
