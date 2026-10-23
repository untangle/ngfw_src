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

PG_VERSION=$(pg_lsclusters -h | awk '$2 == "main" && $3 == "5432" { print $1; exit }')
case "$PG_VERSION" in
    ''|*[!0-9]*)
        echo "ERROR: No valid PostgreSQL cluster is serving port 5432." >&2
        exit 1
        ;;
esac

PG_DUMP="/usr/lib/postgresql/${PG_VERSION}/bin/pg_dump"
if [[ ! -x "$PG_DUMP" ]]; then
    echo "ERROR: PostgreSQL ${PG_VERSION} pg_dump is unavailable." >&2
    exit 1
fi

umask 077
TEMP_FILE=$(mktemp -- "${FILE}.tmp.XXXXXX")
trap 'rm -f -- "$TEMP_FILE"' EXIT

"$PG_DUMP" \
    --username=postgres \
    --format=custom \
    --schema=reports \
    --table="reports.${REGEX}" \
    --file="$TEMP_FILE" \
    --dbname=uvm

mv -f -- "$TEMP_FILE" "$FILE"
trap - EXIT
