#!/bin/bash
#
# fetch-settings.sh — pull latest NGFW settings tree to local disk for use
# with `test_safecheck_sweep.py --settings-dir`.
#
# REMOTE-SIDE GUARANTEE: this script is READ-ONLY on the NGFW box.
# It performs only: `cd`, `ls`, `[ -f ]` (file existence test), invokes
# `python3` to parse apps.js in-memory (stdin only — no files written),
# and `tar cf -` (which streams archive bytes to STDOUT — no file is
# written on the remote disk). No `rm`, `mv`, `cp`, `chmod`, `mkdir`,
# `>` redirect, or temp file is created on the remote.
#
# What it grabs from /usr/share/untangle/:
#   * settings/untangle-vm/*.js   (core UVM settings — single hardcoded file each)
#   * settings/<app-name>/settings_<id>.js — the settings file UVM is
#     actually running, where <id> comes from apps.js's {appName, id}
#     mapping. Falls back to "most recently modified settings_*.js" only
#     when apps.js is missing or unparseable (degraded-appliance case).
#     Silently skips dirs without settings_*.js (e.g. untangle-certificates/).
#   * conf/uid — appliance UID (small text file). Used by the sweep tool
#     to key per-appliance rejections in its persistent CSV without the
#     operator having to pass --uid manually. Read-only; non-fatal if
#     missing (sweep tool will demand --uid in that case).
#
# Output layout matches --settings-dir's expectation:
#   <target-dir>/usr/share/untangle/settings/
#       untangle-vm/*.js
#       <app-name>/settings_<latest>.js
#   <target-dir>/usr/share/untangle/conf/uid
#
# Usage:
#   ./fetch-settings.sh <target-dir> <ssh-args...>
#
#   <target-dir>   where to land the settings tree on the LOCAL machine.
#                  The tool creates <target-dir>/usr/share/untangle/settings/
#                  inside it so --settings-dir points at exactly that path.
#
#   <ssh-args...>  passed verbatim to ssh. Use the same arguments you'd type
#                  at the ssh command line — host, -p PORT, -i KEY, etc.
#
# Examples:
#
#   # Standard SSH (port 22):
#   ./fetch-settings.sh /tmp/cust42 root@192.168.56.5
#
#   # NGFW behind a jump host with custom port:
#   ./fetch-settings.sh /tmp/cust42 -p 17467 root@supssh.edge.arista.com
#
#   # With a specific key file:
#   ./fetch-settings.sh /tmp/cust42 -i ~/.ssh/lab_key -p 22 admin@10.1.2.3
#
# Then sweep against the pulled tree (no .backup round-trip; --ngfw is still
# required because the validate RPC runs on the actual NGFW):
#
#   python3 tools/safecheck-sweep/test_safecheck_sweep.py \
#       --settings-dir /tmp/cust42/usr/share/untangle/settings \
#       --ngfw 192.168.56.5 --username admin --password '<pass>'

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <target-dir> <ssh-args...>" >&2
    echo "  e.g.  $0 /tmp/cust42 -p 17467 root@supssh.edge.arista.com" >&2
    exit 1
fi

TARGET_DIR="$1"; shift
UNTANGLE_ROOT="${TARGET_DIR}/usr/share/untangle"
SETTINGS_ROOT="${UNTANGLE_ROOT}/settings"
mkdir -p "${UNTANGLE_ROOT}"

echo "Fetching latest settings via: ssh $* → ${UNTANGLE_ROOT}"
echo

# Capture the remote bash's stderr separately so any error message is
# visible after the run (otherwise it interleaves with the tar pipe and
# we lose context when something goes wrong).
REMOTE_STDERR=$(mktemp -t fetch-settings.remote.XXXXXX.log)
trap "if [ -s '${REMOTE_STDERR}' ]; then echo; echo '--- remote stderr ---'; cat '${REMOTE_STDERR}'; fi; rm -f '${REMOTE_STDERR}'" EXIT

# One SSH connection, one tar pipe — no per-file scp overhead.
# Heredoc body is sent verbatim to the remote shell; $vars escape so they
# expand REMOTELY (not locally).
ssh "$@" "bash -s" 2>"${REMOTE_STDERR}" <<'REMOTE_SCRIPT' | tar xf - -C "${UNTANGLE_ROOT}"
set -e
cd /usr/share/untangle

files=""

# Core UVM settings — always live, take all of them. EXCLUDES the
# *-version-YYYY-MM-DD-*.js historical-version files that UVM writes
# on every save (matches the exclusion ut-backup.sh:48 uses).
for f in settings/untangle-vm/*.js; do
    [ -f "$f" ] || continue
    case "$f" in
        *-version-[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-*) continue ;;
    esac
    files="$files $f"
done

# Per-app subdirs — pick the LIVE settings file per untangle-vm/apps.js
# (which is the install-ID UVM is actually using). Falls back to "most
# recently modified" only if apps.js is missing or unparseable, which
# would mean the appliance is in a broken state anyway.
APPS_LIST=$(python3 -c 'import json,sys
try:
 d=json.load(open("settings/untangle-vm/apps.js"))
 for e in d.get("apps",{}).get("list",[]):
  n=e.get("appName"); i=e.get("id")
  if n and i is not None: print(n,i)
except: pass' 2>/dev/null || true)

if [ -n "$APPS_LIST" ]; then
    # apps.js path: pull exactly the (appName, id) the UVM is running.
    while read -r name id; do
        [ -z "$name" ] && continue
        f="settings/${name}/settings_${id}.js"
        [ -f "$f" ] && files="$files $f"
    done <<< "$APPS_LIST"
else
    # Fallback: mtime-based selection. Dirs without any settings_*.js
    # (e.g. untangle-certificates/) get skipped because `latest` is empty.
    echo "WARN: apps.js missing or unparseable; falling back to mtime-based file selection" >&2
    for d in settings/*/; do
        [ "$d" = "settings/untangle-vm/" ] && continue
        latest=$(ls -t "${d}"settings_*.js 2>/dev/null | head -1 || true)
        [ -n "$latest" ] && files="$files $latest"
    done
fi

if [ -z "$files" ]; then
    echo "ERROR: no settings files found under /usr/share/untangle/settings" >&2
    exit 1
fi

# conf/uid — small text file with the appliance UID. Used by the sweep
# tool to key per-appliance rejections. Optional: if absent (rare), the
# sweep tool will demand the operator pass --uid explicitly.
[ -f conf/uid ] && files="$files conf/uid"

# -h / --dereference: follow symlinks and archive the TARGET file's
# content under the symlink's name. UVM symlinks live-settings files
# like 'apps.js' to the latest 'apps.js-version-YYYY-MM-DD-*.js'; we
# excluded the version targets from $files, so without -h the local
# extraction would land broken symlinks pointing to missing files.
tar cfh - $files
REMOTE_SCRIPT

# Summary
COUNT=$(find "${SETTINGS_ROOT}" -name "*.js" | wc -l)
UVM_COUNT=$(find "${SETTINGS_ROOT}/untangle-vm" -maxdepth 1 -name "*.js" 2>/dev/null | wc -l)
APP_COUNT=$(( COUNT - UVM_COUNT ))
UID_VALUE=$(cat "${UNTANGLE_ROOT}/conf/uid" 2>/dev/null || echo "<missing>")

echo "Fetched ${COUNT} settings files (${UVM_COUNT} core UVM + ${APP_COUNT} per-app latest); uid=${UID_VALUE}"
echo
echo "Next: run sweep against the pulled tree."
echo "  python3 $(dirname "$0")/test_safecheck_sweep.py \\"
echo "      --settings-dir ${SETTINGS_ROOT} \\"
echo "      --ngfw <ngfw-host> --username admin --password '<pass>'"
