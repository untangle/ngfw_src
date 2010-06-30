#! /bin/bash

#=============================================================
# Little script which creates a backup of the system (like
# "backup-ut") then creates a single tar.gz file of the results.
#
# This script exists so a bunch of places don't releat this logic,
# as we may want to add some meta information (i.e. some manifest)
# to the single bundle later.
#
#==============================================================

#================================================================
#
# **************************************************
# ******************** WARNING  ********************
# **************************************************
#
# As this file is maintained, note that its behavior is bound
# to com.untangle.node.boxbackup.BoxBackupImpl.  Any changes
# this this script should be reflected in that Java code
#================================================================


OUT_FILE=INVALID
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
  echo "$0 -o (output file) -h (help) -v (verbose)"
}


#
# 1 = Directory to dump backup files
#
function createBackup() {
  debug "Creating Backup in " $1
  @UVM_HOME@/bin/backup-ut $1
#  pushd $1 > /dev/null 2>&1
#  datestamp=$(date '+%Y%m%d%H%M')
#  echo "FOO" > uvmdb-$datestamp.gz
#  echo "MOO" > files-$datestamp.tar.gz
#  echo "DOO" > installed-$datestamp
#  popd > /dev/null 2>&1
  DUMP_EXIT=$?
  debug "Done creating backup with return code $DUMP_EXIT"
  return $DUMP_EXIT
}


# 1 = tar file
# 2 = dir with backups
function tarBackupFiles() {
  debug "Taring files in $2 into tar $1"
  pushd $2 > /dev/null 2>&1
  tar -cf $1 .
  popd > /dev/null 2>&1
  TAR_EXIT=$?
  debug "Done creating tar with return code $TAR_EXIT"
}

####################################
# "Main" logic starts here

while getopts "ho:v" opt; do
  case $opt in
    h) doHelp;exit 0;;
    o) OUT_FILE=$OPTARG;;
    v) VERBOSE=true;;
  esac
done

if [ "INVALID" == $OUT_FILE ]; then
  err "Please provide an output file";
  exit 1;
fi

debug "Outputting to file -" $OUT_FILE

# Create the backups into a directory we provide
DUMP_DIR=`mktemp -d -t ut-backup-bundled.XXXXXXXXXX`
createBackup $DUMP_DIR

EXIT_CODE=$?

if [ $EXIT_CODE != 0 ]; then
  err "Unable to create backup,  Exit code $EXIT_CODE"
  exit 1;
fi




# Tar the contents of the temp directory
TAR_FILE=`mktemp -t ut-backup-bundled.XXXXXXXXXX`
tarBackupFiles $TAR_FILE $DUMP_DIR

debug "Remove dump dir"
rm -rf $DUMP_DIR

debug "Gzipping $TAR_FILE"
gzip $TAR_FILE

TAR_FILE=$TAR_FILE.gz
debug "Tar file renamed to $TAR_FILE"


# Delete the temp directory
debug "Delete dump directory $DUMP_DIR"
rm -rf $DUMP_DIR

debug "Copy bundle to $OUT_FILE"
mv $TAR_FILE $OUT_FILE


