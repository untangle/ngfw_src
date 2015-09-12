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
# As this file is maintained, note that its behavior is bound
# to com.untangle.node.configuration_backup.BoxBackupImpl.  Any changes
# this this script should be reflected in that Java code
#================================================================

# Example:
#./ut-remotebackup.sh -u http://localhost/boxtrack/configuration-backup/backup.php -k xyz678


# Constants
TIMEOUT=3
URL=INVALID
BOX_KEY=INVALID
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
  echo "$0 -t [timeout] -u [URL] -k [boxKey] -h (help) -v (verbose)"
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
  curl "$URL" -k -F boxkey="$BOX_KEY" -F uploadedfile=@$1 --dump-header $2 --max-time $TIMEOUT > /dev/null 2>&1
  return $?
}


#
# 1 = Directory to dump backup files
#
function createBackup() {
  @PREFIX@/usr/share/untangle/bin/ut-backup.sh -o $1
  DUMP_EXIT=$?
  debug "Done creating backup with return code $DUMP_EXIT"
  return $DUMP_EXIT
}

####################################
# "Main" logic starts here

while getopts "ht:k:u:v" opt; do
  case $opt in
    h) doHelp;exit 0;;
    u) URL=$OPTARG;;
    k) BOX_KEY=$OPTARG;;
    t) TIMEOUT=$OPTARG;;
    v) VERBOSE=true;;
  esac
done

if [ "INVALID" == "$URL" ]; then
  echo "Please provide a URL";
  exit 1;
fi

if [ "INVALID" == "$BOX_KEY" ]; then
  echo "Please provide a box key";
  exit 1;
fi

debug "Using URL -" $URL
debug "Using Box Key - " $BOX_KEY
debug "Using Timeout - " $TIMEOUT

# Tar the contents of the temp directory
BACKUP_FILE=`mktemp -t ut-remotebackup.XXXXXXXXXX`
createBackup $BACKUP_FILE


HEADER_FILE=`mktemp -t ut-remotebackup.XXXXXXXXXX`
callCurl $BACKUP_FILE $HEADER_FILE
CURL_RET=$?
debug "CURL returned $CURL_RET"

# Clean-up tar file
debug "Deleting Backup file $BACKUP_FILE"
rm -f $BACKUP_FILE

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

# Evaluate HTTP status code
if [ $RETURN_CODE -eq 401 ];then
  err "Remote server at URL $URL returned 401"
  exit 3
fi
if [ $RETURN_CODE -eq 403 ];then
  err "Remote server at URL $URL returned 403"
  exit 3
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



