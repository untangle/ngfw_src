#! /bin/sh

RESTART_FILE=/etc/untangle/reports-to-restart
DAEMON=/usr/lib/python2.5/reports/server.py
LOGFILE=/var/log/uvm/reporter.log

pid=foo

while true ; do

  if ! ps -p $pid > /dev/null ; then
    $DAEMON >> $LOGFILE 2>&1 &
    pid=$!
  fi

  sleep 30
  if [ -f $RESTART_FILE ] ; then
    rm -f $RESTART_FILE
    pkill -f $DAEMON
    exit 0
  fi

done
