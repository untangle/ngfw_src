#!/bin/bash

set -euo pipefail

function doHelp() {
    echo "$0 [options]"
    echo "required options:"
    echo " -f input_file    (authenticated .rbackup Reports backup to restore)"
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

umask 077
RESTORE_DIR=$(mktemp -d /tmp/reports-restore.XXXXXX)
chmod 700 "$RESTORE_DIR"
trap 'rm -rf -- "$RESTORE_DIR"' EXIT
VERIFIED_ARCHIVE="$RESTORE_DIR/reports.pgdump"

mkdir -p "$(dirname "$RESTORE_LOG")"
printf '\n[%s] Starting Reports data restore from %s\n' "$(date --iso-8601=seconds)" "$FILE" | tee -a "$RESTORE_LOG"

BACKUP_INTEGRITY_HELPER="/usr/share/untangle/bin/reports-backup-integrity.py"
if [[ ! -x "$BACKUP_INTEGRITY_HELPER" ]]; then
    echo "ERROR: Reports backup integrity verifier is unavailable." >&2
    printf '[%s] Reports backup integrity verifier is unavailable.\n' "$(date --iso-8601=seconds)" | tee -a "$RESTORE_LOG"
    exit 1
fi

if ! "$BACKUP_INTEGRITY_HELPER" verify --package "$FILE" --output "$VERIFIED_ARCHIVE" 2>>"$RESTORE_LOG"; then
    echo "ERROR: Reports backup authentication or integrity check failed." >&2
    printf '[%s] Reports backup authentication or integrity check failed.\n' "$(date --iso-8601=seconds)" | tee -a "$RESTORE_LOG"
    exit 2
fi

# Only the authenticated, digest-checked archive reaches PostgreSQL tools.
if ! pg_restore --list "$VERIFIED_ARCHIVE" >/dev/null 2>>"$RESTORE_LOG"; then
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
pg_restore \
    --verbose \
    --username=reports_restore \
    --role=ngfw_reports \
    --dbname=uvm \
    --no-owner \
    --no-privileges \
    --schema=reports \
    "$VERIFIED_ARCHIVE" 2>&1 | tee -a "$RESTORE_LOG"
RESTORE_STATUS=${PIPESTATUS[0]}

printf '[%s] Reports data restore finished with exit code %s\n' "$(date --iso-8601=seconds)" "$RESTORE_STATUS" | tee -a "$RESTORE_LOG"
exit "$RESTORE_STATUS"
