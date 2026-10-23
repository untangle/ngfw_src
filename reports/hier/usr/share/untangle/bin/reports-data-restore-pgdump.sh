#!/bin/bash

set -euo pipefail

function doHelp() {
    echo "$0 [options]"
    echo "required options:"
    echo " -f input_file    (custom-format Reports backup to restore)"
    echo "optional options:"
    echo " -h               (help)"
    echo
}

FILE=""
RESTORE_LOG="/var/log/uvm/restore-reports.log"

while getopts "f:h" opt; do
    case "$opt" in
        h) doHelp; exit 0 ;;
        f) FILE=$OPTARG ;;
        \?) doHelp >&2; exit 2 ;;
    esac
done

if [[ -z "$FILE" || ! -f "$FILE" || ! -r "$FILE" ]]; then
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

PG_RESTORE="/usr/lib/postgresql/${PG_VERSION}/bin/pg_restore"
if [[ ! -x "$PG_RESTORE" ]]; then
    echo "ERROR: PostgreSQL ${PG_VERSION} pg_restore is unavailable." >&2
    exit 1
fi

mkdir -p "$(dirname "$RESTORE_LOG")"
printf '\n[%s] Starting Reports data restore from %s\n' "$(date --iso-8601=seconds)" "$FILE" | tee -a "$RESTORE_LOG"

if ! "$PG_RESTORE" --list "$FILE" >/dev/null 2>>"$RESTORE_LOG"; then
    echo "ERROR: Invalid Reports backup archive." >&2
    printf '[%s] Invalid Reports backup archive.\n' "$(date --iso-8601=seconds)" | tee -a "$RESTORE_LOG"
    exit 2
fi

# Provision the owner/restore roles and migrate existing Reports objects
# before pg_restore attempts SET ROLE or creates inherited child tables.
REPORTS_TABLE_GENERATOR="/usr/share/untangle/bin/reports-generate-tables.py"
printf '[%s] Provisioning Reports restore roles and object ownership.\n' "$(date --iso-8601=seconds)" | tee -a "$RESTORE_LOG"
if [[ ! -x "$REPORTS_TABLE_GENERATOR" ]]; then
    echo "ERROR: Reports table generator is unavailable." >&2
    printf '[%s] Reports role provisioning failed: table generator is unavailable.\n' "$(date --iso-8601=seconds)" | tee -a "$RESTORE_LOG"
    exit 1
fi

if ! "$REPORTS_TABLE_GENERATOR" --roles-only >>"$RESTORE_LOG" 2>&1; then
    echo "ERROR: Could not provision Reports restore roles or ownership." >&2
    printf '[%s] Reports role provisioning failed; restore was not started.\n' "$(date --iso-8601=seconds)" | tee -a "$RESTORE_LOG"
    tail -n 80 "$RESTORE_LOG" >&2
    exit 1
fi

set +e
"$PG_RESTORE" \
    --verbose \
    --username=reports_restore \
    --role=ngfw_reports \
    --dbname=uvm \
    --no-owner \
    --no-privileges \
    --schema=reports \
    "$FILE" 2>&1 | tee -a "$RESTORE_LOG"
RESTORE_STATUS=${PIPESTATUS[0]}
set -e

printf '[%s] Reports data restore finished with exit code %s\n' "$(date --iso-8601=seconds)" "$RESTORE_STATUS" | tee -a "$RESTORE_LOG"
exit "$RESTORE_STATUS"
