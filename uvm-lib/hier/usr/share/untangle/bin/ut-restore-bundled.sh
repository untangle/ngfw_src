#! /bin/bash

#=============================================================
# Script which takes the output of "ut-backup-bundled" and
# restores it to a system.  This is a wrapper around "ut-restore"
# which deals with single .tar.gz files rather than the three
# files from the "old-style" backups.
#
#
# 1 - Not a valid gzip file
# 2 - Not a tar file
# 3 - Missing content from file
# 4 - Error from restore file
# 5 - Restore file too old
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


IN_FILE=INVALID
VERBOSE=false
NOHUPPED=false


function debug() {
  if [ "true" == $VERBOSE ]; then
    echo $*
  fi
}

function err() {
  echo $* >> /dev/stderr
}

function doHelp() {
  echo "$0 -i (input bundle file) -h (help) -v (verbose)"
}


function doIt() {
    if [ "INVALID" == $IN_FILE ]; then
        err "Please provide an input file";
        exit 1;
    fi

    debug "Restoring from file -" $IN_FILE


# Create a working directory
    WORKING_DIR=`mktemp -d -t ut-restore-bundled.XXXXXXXXXX`
    debug "Working in directory $WORKING_DIR"

# Copy our file to the working directory
    cp $IN_FILE $WORKING_DIR/x.tar.gz

# Unzip
    gzip -t $WORKING_DIR/x.tar.gz
    EXIT_VAL=$?

    if [ $EXIT_VAL != 0 ]; then
        err "$IN_FILE Does not seem to be a valid gzip file"
        rm -rf $WORKING_DIR
        exit 1
    fi

    debug "Gunzip"
    gunzip $WORKING_DIR/x.tar.gz


# Now, untar
    pushd $WORKING_DIR > /dev/null 2>&1
    debug "Untar"
    tar -xvf x.tar  > /dev/null 2>&1
    EXIT_VAL=$?
    popd  > /dev/null 2>&1

    if [ $EXIT_VAL != 0 ]; then
        err "$IN_FILE Does not seem to be a valid gzip tar file"
        rm -rf $WORKING_DIR
        exit 2
    fi


# Find the specfic files
    pushd $WORKING_DIR > /dev/null 2>&1

    DB_FILE=`ls | grep uvmdb*.gz`
    FILES_FILE=`ls | grep files*.tar.gz`
    INSTALLED_FILE=`ls | grep installed*`
    OLD_DB_FILE=`ls | grep mvvmdb*.gz`

    debug "DB file $DB_FILE"
    debug "Files file $FILES_FILE"
    debug "Installed file $INSTALLED_FILE"

    popd  > /dev/null 2>&1


# Check version
    if [ -n "$OLD_DB_FILE" ]; then
        err "Restore file too old"
        exit 5
    fi


# Verify files
    if [ -z "$INSTALLED_FILE" ]; then
        err "Unable to find installed packages file"
        rm -rf $WORKING_DIR
        exit 3
    fi

    if [ -z "$FILES_FILE" ]; then
        err "Unable to find system files file"
        rm -rf $WORKING_DIR
        exit 3
    fi

    if [ -z "$DB_FILE" ]; then
        err "Unable to find database file"
        rm -rf $WORKING_DIR
        exit 3
    fi

# Invoke ut-restore ("Usage: $0 dumpfile tarfile instfile")

    @UVM_HOME@/bin/ut-restore $WORKING_DIR/$DB_FILE $WORKING_DIR/$FILES_FILE $WORKING_DIR/$INSTALLED_FILE

    EXIT_VAL=$?

    rm -rf $WORKING_DIR

    if [ $EXIT_VAL != 0 ]; then
        err "Error $EXIT_VAL returned from untangle-restore"
        exit 4
    fi

    debug "Completed.  Success"
}

####################################
# "Main" logic starts here

while getopts "hi:vQ" opt; do
  case $opt in
    h) doHelp;exit 0;;
    i) IN_FILE=$OPTARG;;
    v) VERBOSE=true;;
    Q) NOHUPPED=true;;
  esac
done

## Execute these functions in a separate detached process, this way
## when uvm gets killed this process doesn't exit.
if [ $NOHUPPED != "true" ]; then
    ## Just append any arguments, they don't matter
    nohup bash @UVM_HOME@/bin/ut-restore-bundled.sh "$@" -Q > @PREFIX@/var/log/uvm/restore.log 2>&1 &
else
    doIt
fi

