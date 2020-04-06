#! /bin/bash

#=============================================================
# Script which takes the output of "ut-backup" and
# restores it to a system.  
#==============================================================

VERBOSE="false"
CHECK_ONLY="false"
WORKING_DIR=""
TARBALL_FILE=""
VERSION_FILE=""
#ACCEPTED_PREVIOUS_VERSION="10.1|10.2"
#ACCEPTED_PREVIOUS_VERSION="14.2"
ACCEPTED_PREVIOUS_VERSION="15.0"

function debug() {
  if [ "true" == $VERBOSE ]; then
    echo $*
  fi
}

function err() {
  echo $* >> /dev/stderr
}

function doHelp() {
  echo "$0 [options]"
  echo "required options: "
  echo " -i input_file   (restore file)"
  echo "optional options: "
  echo " -m regex        (files in the current configuration to maintain - regex)"            
  echo " -h              (help)"
  echo " -c              (check file)"
  echo " -v              (verbose)"
}

INST_OPTS=" -o DPkg::Options::=--force-confnew --yes --force-yes --fix-broken "
UPGD_OPTS=" -o DPkg::Options::=--force-confnew --yes --force-yes --fix-broken "

function doRestore() 
{
    # save old settings & delete the current settings
    temp=`mktemp -d -t ut-backup-restore.XXXXXXXXXX`
    cp -rL @PREFIX@/usr/share/untangle/settings/* $temp/
    rm -rf @PREFIX@/usr/share/untangle/settings/*

    @PREFIX@/usr/share/untangle/bin/ut-show-upgrade-splash start 'Restore in progress. Do not reboot or power off the server!'

    # stop the untangle-vm
    if [ -x @PREFIX@/etc/init.d/untangle-vm ] ; then
        @PREFIX@/etc/init.d/untangle-vm stop
    fi

    # restore the files, both system and the /usr/share/untangle important stuf
    debug "Restoring files..."

    if [ "true" == $VERBOSE ]; then
      tar zxhfv $WORKING_DIR/$TARBALL_FILE -C /
    else
      tar zxhf $WORKING_DIR/$TARBALL_FILE -C /
    fi

    # update date on all files
    find @PREFIX@/usr/share/untangle/settings -type f -exec touch {} \;

    debug "Restoring files...done"

    # restore "maintain files"
    if [ ! -z "$MAINTAIN_REGEX" ] ; then
        echo "Maintaining files..."

        pushd > /dev/null 2>&1
        cd $temp
        find . -regextype sed -regex "$MAINTAIN_REGEX" -exec echo Keeping original @PREFIX@/usr/share/untangle/settings/{} \;
        find . -regextype sed -regex "$MAINTAIN_REGEX" -exec cp -f --parents --remove-destination {} @PREFIX@/usr/share/untangle/settings/ \;
        popd > /dev/null 2>&1

        echo "Maintaining files... done"
    fi

    # start the UVM, depending on circumstances (menu driven restore) may need to be restopped
    if [ -x @PREFIX@/etc/init.d/untangle-vm ] ; then
        @PREFIX@/etc/init.d/untangle-vm restart
    fi

    debug "Completed.  Success"
}

function expandFile() 
{
    restore_file=$1

    if [ ! -f $restore_file ] ; then
        err "File does not seem to be a valid backup file. (does not exist)"
        return 1;
    fi
    if [ ! -s $restore_file ] ; then
        err "File does not seem to be a valid backup file. (zero size)"
        return 1;
    fi

    cp $restore_file $WORKING_DIR/restore_file.tar.gz
    EXIT_VAL=$?
    if [ $EXIT_VAL != 0 ]; then
        err "File does not seem to be a valid backup file. (file copy failed)"
        return 1
    fi

    gzip -t $WORKING_DIR/restore_file.tar.gz
    EXIT_VAL=$?
    if [ $EXIT_VAL != 0 ]; then
        err "File does not seem to be a valid backup file. (gzip failed)"
        return 1
    fi

    gunzip $WORKING_DIR/restore_file.tar.gz
    EXIT_VAL=$?
    if [ $EXIT_VAL != 0 ]; then
        err "File does not seem to be a valid backup file. (gunzip failed)"
        return 1
    fi

    pushd $WORKING_DIR > /dev/null 2>&1
    tar -xvf restore_file.tar  > /dev/null 2>&1
    EXIT_VAL=$?
    popd  > /dev/null 2>&1
    if [ $EXIT_VAL != 0 ]; then
        err "File does not seem to be a valid backup file. (tar failed)"
        return 1
    fi

    # Find the specfic files
    pushd $WORKING_DIR > /dev/null 2>&1
    TARBALL_FILE=`ls | grep files*.tar.gz`
    VERSION_FILE=`ls | grep PUBVERSION`
    popd  > /dev/null 2>&1

    # Verify files
    if [ -z "$TARBALL_FILE" ]; then
        err "File does not seem to be a valid backup file. (missing files tarball)"
        return 1
    fi
    if [ -z "$VERSION_FILE" ]; then
        err "File does not seem to be a valid backup file. (missing version file)"
        return 1
    fi

    # Check that the version of the backup file is supported
    CURRENT_VERSION="`cat @PREFIX@/usr/share/untangle/lib/untangle-libuvm-api/PUBVERSION`"
    BACKUP_VERSION="`cat $WORKING_DIR/$VERSION_FILE`"
    echo $BACKUP_VERSION | grep -qE "$ACCEPTED_PREVIOUS_VERSION"
    PREV_VERSION_CHECK=$?
    if [ "$BACKUP_VERSION" != "$CURRENT_VERSION" ] && [ $PREV_VERSION_CHECK != 0 ] ; then
        err "Backup file version not supported. ($BACKUP_VERSION)"
        return 1
    fi

    return 0
}

####################################
# "Main" logic starts here

while getopts "i:m:hvcf" opt; do
  case $opt in
    h) doHelp;exit 0;;
    i) RESTORE_FILE=$OPTARG;;
    m) MAINTAIN_REGEX=$OPTARG;;
    c) CHECK_ONLY="true";;
    v) VERBOSE="true";;
  esac
done

if [ -z "$RESTORE_FILE" ] ; then
    doHelp; 
    exit 1;
fi

# Create a working directory
WORKING_DIR=`mktemp -d -t ut-restore.XXXXXXXXXX`

# Expand and check the file
expandFile $RESTORE_FILE
RETURN_CODE=$?

# If the file doesn't check out or in check only mode exit with expandFile's return code
if [ "$CHECK_ONLY" == "true" ] || [ $RETURN_CODE != 0 ] ; then
    rm -rf ${WORKING_DIR}
    exit $RETURN_CODE
fi

# Do the restore
doRestore

rm -rf ${WORKING_DIR}



