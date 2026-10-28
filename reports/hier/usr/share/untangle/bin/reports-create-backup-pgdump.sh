#!/bin/bash

set -euo pipefail

function doHelp() {
    echo "$0 [options]"
    echo "required options:"
    echo " -f output_file   (file to write)"
    echo " -r pattern       (pattern of Reports table names to back up)"
    echo "optional options:"
    echo " -h               (help)"
    echo
}

FILE=""
REGEX=""

while getopts "r:f:h" opt; do
    case "$opt" in
        h) doHelp; exit 0 ;;
        f) FILE=$OPTARG ;;
        r) REGEX=$OPTARG ;;
        \?) doHelp >&2; exit 2 ;;
    esac
done

if [[ -z "$FILE" || -z "$REGEX" ]]; then
    doHelp >&2
    exit 2
fi

umask 077
TEMP_FILE=$(mktemp -- "${FILE}.tmp.XXXXXX")
trap 'rm -f -- "$TEMP_FILE"' EXIT

pg_dump \
    --username=postgres \
    --format=custom \
    --schema=reports \
    --table="reports.${REGEX}" \
    --file="$TEMP_FILE" \
    --dbname=uvm

mv -f -- "$TEMP_FILE" "$FILE"
trap - EXIT
