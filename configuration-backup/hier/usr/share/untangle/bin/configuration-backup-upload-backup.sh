#! /bin/bash

#=============================================================
# This application creates a backup of the system, then
# sends it to the given URL.  The intention is to post to
# Untangle for the 24-hour backup service
#
#
# Exit codes:
#   1 - invalid arguments (i.e. not enough stuff provided).
#   2 - an error was returned from the remote web server.  This
#       means we found the server, but it was angry in some way.
#   3 - This is a subset of (2), meaning the remote server
#       returned either a 401 or 403 (bad-password type stuff).
#   4 - Unable to contact the specified server
#   5 - Timed out
#
#==============================================================

#================================================================
#
# **************************************************
# ******************** WARNING  ********************
# **************************************************
#
# As this file is maintained, note that its behavior is bound the
# configuration backup app.  Any changes
# this this script should be reflected in that Java code
#================================================================

# Example:
#./ut-remotebackup.sh -u http://localhost/boxtrack/backup/backup.php -k xyz678 -f /tmp/foo.backup


# Constants
TIMEOUT=3
URL=INVALID
SERVER_UID=INVALID
BACKUP_FILE=INVALID
VERBOSE=false


function debug() {
  if [ "true" == $VERBOSE ]; then
    echo $*
  fi
}

function err() {
  echo $* >> /dev/stderr
}

function doHelp() {
    echo "$0 -t <timeout> -u <URL> -k <uid> -f <file>"
    echo "Options:"
    echo "    -h       help"
    echo "    -v       verbose"
    echo 
}

# Gets the HTTP status code from the output of CURL.  Note
# that for odd reasons (not sure why) there is often a "100"
# (is that "continue?) then a "404", so we choose the *last*
# response code as the status.
function getHTTPStatus() {
  cat ${1} | sed -n "/HTTP\/1.1/ p" | awk '{ print $2; }' | tail -1
}


# 1 = name of backup file
# 2 = name of file to write response headers
#
# returns the return of CURL
function callCurl() {
  debug "Calling CURL.  Dumping headers to $2"
  md5=($(md5sum $1))
  debug "Backup file MD5: $md5"
  echo curl "$URL" -k -F uid="$SERVER_UID" -F uploadedfile=@$1 -F md5="$md5" --dump-header $2 --max-time $TIMEOUT
  curl "$URL" -k -F uid="$SERVER_UID" -F uploadedfile=@$1 -F md5="$md5" --dump-header $2 --max-time $TIMEOUT > /dev/null 2>&1
  return $?
}

####################################
# "Main" logic starts here

while getopts "ht:k:u:f:v" opt; do
  case $opt in
    h) doHelp;exit 0;;
    f) BACKUP_FILE=$OPTARG;;
    u) URL=$OPTARG;;
    k) SERVER_UID=$OPTARG;;
    t) TIMEOUT=$OPTARG;;
    v) VERBOSE=true;;
  esac
done

if [ "INVALID" == "$URL" ]; then
  echo "Please provide a URL";
  exit 1;
fi

if [ "INVALID" == "$SERVER_UID" ]; then
  echo "Please provide a UID";
  exit 1;
fi

if [ "INVALID" == "$BACKUP_FILE" ]; then
  echo "Please provide a file";
  exit 1;
fi

if [ ! -f $BACKUP_FILE ] ; then
  echo "file missing: $BACKUP_FILE"
  exit 1
fi
    
debug "File: " $BACKUP_FILE
debug "URL: " $URL
debug "UID:" $SERVER_UID
debug "Timeout: " $TIMEOUT

HEADER_FILE=`mktemp -t ut-remotebackup.XXXXXXXXXX`
callCurl $BACKUP_FILE $HEADER_FILE
CURL_RET=$?
debug "CURL returned $CURL_RET"

# Check CURL return codes
if [ $CURL_RET -eq 7 ]; then
  # A machine that exists, wrong port (e.g. http://localhost:800/)
  # or machine cannot be contacted then CURL returns 7
  err "CURL returned 7, indicating that the URL $URL could not be contacted"
  rm -f $HEADER_FILE
  exit 4
fi

if [ $CURL_RET -eq 28 ]; then
  # When CURL times out, it returns 28.
  err "CURL returned 28, indicating a timeout when contacting the URL $URL"
  rm -f $HEADER_FILE
  exit 5
fi


# Get the HTTP status code from the CURL header file
RETURN_CODE=`getHTTPStatus $HEADER_FILE`
debug "HTTP status code $RETURN_CODE"

# Remove the header file
debug "Remove header file $HEADER_FILE"
rm -f $HEADER_FILE

if [ ! -z "$RETURN_CODE" ] ; then
    # Evaluate HTTP status code
    if [ $RETURN_CODE -eq 401 ];then
        err "Remote server at URL $URL returned 401"
        exit 3
    fi
    if [ $RETURN_CODE -eq 403 ];then
        err "Remote server at URL $URL returned 403"
        exit 3
    fi
fi

if [ $RETURN_CODE -gt 200 ]
then
  # Web server exists, "page" not found.  CURL returns 0
  # and we parse for the 404.  This would be for a bad URL which
  # happened to point at a real web server
  err "Remote server at URL $URL returned $RETURN_CODE which is too high.  Assume failure"
  exit 2
else
  debug "Backup to remote URL complete"
  exit 0
fi



