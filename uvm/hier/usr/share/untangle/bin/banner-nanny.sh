#! /bin/bash

############
# constants
BANNER_MSG="PING SPAMC/1.2"
LOGFILE="/var/log/uvm/wrapper.log"
MAX_TRIES=3

############
# functions
gotJob() {
  if ps -p $1 > /dev/null ; then # job still running
    return 0 
  else # not running anymore
    wait $1 # wait on it first anyway
    return 1
  fi
}

testBanner() { 
  echo $BANNER_MSG | nc localhost $1
}

log() {
  echo "*** `date -Iseconds` $1" >> $LOGFILE
}

##########
# main
PORT=$1
TIMEOUT=$2

let failures=0
while [ $failures -lt $MAX_TRIES ] ; do
  testBanner $PORT > /dev/null 2>&1 & # background the actual test
  pid=$! # store its PID

  # wait for $TIMEOUT seconds or the backgrounded test to return,
  # whichever comes first.
  let seconds=0
  while [ $seconds -lt $TIMEOUT ] ; do
    sleep 1
    let seconds=seconds+1
    # if the test is not running anymore, it could mean 1 of 2 things:
    #   1. connection to the given port was refused -> 1st gen nannies
    #      will take care of this, as it most likely the process is not
    #      running
    #  or
    #   2. connection succeeded, and we got a banner back -> service is
    #      working fine.
    # So in both cases we want to exit with RC=0.
    gotJob $pid || exit 0
  done

  # we made it here, so the test timed out; let's kill the test if
  # need be (it could have completed in the meantime), log the
  # failure, and try again.
  gotJob $pid && kill $pid
  let failures=failures+1
  log "failure #$failures on port $PORT"
done

exit 1 # we failed $MAX_TRIES in a row, exit with RC=1
