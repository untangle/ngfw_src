#!/bin/bash

function doHelp() {
    echo "$0 [options]"
    echo "required options: "
    echo " -f input_file   (file to write)"
    echo "optional options: "
    echo " -h              (help)"
    echo
}

while getopts "f:h" opt; do
  case $opt in
    h) doHelp;exit 0;;
    f) FILE=$OPTARG;;
  esac
done

if [ -z "$FILE" ] ; then
    doHelp; 
    exit 1;
fi

SCANNER="@PREFIX@/usr/share/untangle/bin/reports-sql-scanner.py"
RESTORE_LOG="/var/log/uvm/restore-reports.log"

if ! python3 "$SCANNER" "$FILE" >> "$RESTORE_LOG" 2>&1; then
    echo "ERROR: Dangerous SQL detected. Restore aborted."
    exit 2
fi

echo "SQL scan passed. Running restore..."

zcat $FILE | psql -U postgres uvm 2>&1 | tee -a $RESTORE_LOG | grep -v 'already exists'

psql_rc=${PIPESTATUS[1]}
if [ "$psql_rc" -eq 0 ]; then
    : > "$RESTORE_LOG"
fi

exit "$psql_rc"
