#! /bin/bash

#=============================================================
# Little script which creates a backup of the system settings state
# Excluding "config" information, like UID, etc.
#==============================================================

VERBOSE=false

function debug() 
{
  if [ "true" == $VERBOSE ]; then
    echo $*
  fi
}

function err() 
{
  echo $* >> /dev/stderr
}

function doHelp() 
{
  echo "$0 -o (output file) -h (help) -v (verbose)"
}

# $1 = tar file
# $2 = dir with backups files
function tarBackupFiles() 
{
  debug "Taring files in $2 into tar $1"
  pushd $2 > /dev/null 2>&1
  tar -cf $1 .
  popd > /dev/null 2>&1
  TAR_EXIT=$?
  debug "Done creating tar with return code $TAR_EXIT"
}

# $1 = dir to put backup files
function backupToDir()
{
    outdir=$1

    datestamp=$(date '+%Y%m%d%H%M')

    # tar up important files
    tar zcf $outdir/files-$datestamp.tar.gz --ignore-failed-read -C / \
        etc/apache2/ssl \
        usr/share/untangle/conf/wizard-complete-flag \
        usr/share/untangle/settings 

    # save the list of important packages
    /usr/share/untangle/bin/ut-apt installed > $outdir/installed-$datestamp

    # save the version of this backup
    cp /usr/share/untangle/lib/untangle-libuvm-api/PUBVERSION $outdir/
}




while getopts "ho:v" opt; do
  case $opt in
    h) doHelp;exit 0;;
    o) OUT_FILE=$OPTARG;;
    v) VERBOSE=true;;
  esac
done

if [ -z "$OUT_FILE" ]; then
  err "Please provide an output file";
  doHelp; exit 1;
fi

debug "Outputting to file: " $OUT_FILE

# Create the backups into a directory we provide
DUMP_DIR=`mktemp -d -t ut-backup.XXXXXXXXXX`
backupToDir $DUMP_DIR

# Tar the contents of the temp directory
TAR_FILE=`mktemp -t ut-backup.XXXXXXXXXX`
tarBackupFiles $TAR_FILE $DUMP_DIR

debug "Remove dump dir"
rm -rf $DUMP_DIR

debug "Gzipping $TAR_FILE"
gzip $TAR_FILE

debug "Copy bundle to $OUT_FILE"
mv $TAR_FILE.gz $OUT_FILE


