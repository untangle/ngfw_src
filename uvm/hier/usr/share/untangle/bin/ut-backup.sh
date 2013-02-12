#! /bin/bash

#=============================================================
# Little script which creates a backup of the system (like
# "ut-backup") then creates a single tar.gz file of the results.
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

function tar_files()
{
    tarfile=$1

    tar zcf $tarfile --ignore-failed-read -C / \
        etc/hostname \
        etc/network/interfaces \
        etc/openvpn \
        etc/resolv.conf \
        etc/apache2/ssl \
        usr/share/untangle/conf/keystore \
        usr/share/untangle/conf/openvpn/misc \
        usr/share/untangle/conf/openvpn/pki \
        usr/share/untangle/conf/dirbackup.ldif \
        usr/share/untangle/conf/wizard-complete-flag \
        usr/share/untangle/settings 

    # Tar exits with non-zero when some files don't exist, which can happen.
    return 0
}

function dump_installed()
{
    instlist=$1

    /usr/share/untangle/bin/ut-apt installed > $instlist
}

function dump_db()
{
    outfile=$1
    # nothing of use is store in the DB
    # we just echo nothing to a zip file, so we don't break the restore process
    echo "" | gzip > $outfile 2>/dev/null
}

function backupToDir()
{
    outdir=$1

    datestamp=$(date '+%Y%m%d%H%M')
    dumpfile=$outdir/uvmdb-$datestamp.gz
    tarfile=$outdir/files-$datestamp.tar.gz
    instlist=$outdir/installed-$datestamp

    dump_db $dumpfile && tar_files $tarfile && dump_installed $instlist
}

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
  backupToDir $1
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
DUMP_DIR=`mktemp -d -t ut-backup.XXXXXXXXXX`
createBackup $DUMP_DIR

EXIT_CODE=$?

if [ $EXIT_CODE != 0 ]; then
  err "Unable to create backup,  Exit code $EXIT_CODE"
  exit 1;
fi



# Tar the contents of the temp directory
TAR_FILE=`mktemp -t ut-backup.XXXXXXXXXX`
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


