#!/bin/bash

function doHelp() {
    echo "$0 [options]"
    echo "optional options: "
    echo " -d directory   (remote directory to hold file)"
    echo " -f filename    (the filename)"
    echo
}

while getopts "d:f:h" opt; do
  case $opt in
    h) doHelp;exit 0;;
    d) DIR=$OPTARG;;
    f) FILENAME=$OPTARG;;
  esac
done

YESTERDAY="`date --date='1 days ago' '+%Y_%m_%d'`"

if [ -z "$FILENAME" ] ; then
    HOSTNAME="`cat /etc/hostname | sed -e 's/\s+/_/g' | sed -e 's/\./_/g'`"
    FILENAME="${HOSTNAME}-reports_data-${YESTERDAY}.sql.gz"
fi

echo "Creating backup: /tmp/$FILENAME" 
@PREFIX@/usr/share/untangle/bin/reports-create-backup.sh -r "*$YESTERDAY*" -f /tmp/$FILENAME

DEST_DIR="/var/lib/google-drive/$DIR/"
mkdir -p "$DEST_DIR"
mv /tmp/$FILENAME "$DEST_DIR"

/usr/bin/drive push --no-prompt "$DEST_DIR/$FILENAME"
exit_code=$?

# remove the backup file
rm -f "$DEST_DIR/$FILENAME"
exit $exit_code
