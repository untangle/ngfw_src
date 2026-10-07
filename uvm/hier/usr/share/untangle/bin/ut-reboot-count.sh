#!/bin/bash

# Output format: "REBOOTS (CRASHES)" covering this month and last month

USE_WTMP=false
if command -v last >/dev/null 2>&1 && [ -f /var/log/wtmp ]; then
    last -f /var/log/wtmp reboot >/dev/null 2>&1 && USE_WTMP=true
fi

if $USE_WTMP; then
    THIS_MONTH_REBOOTS="$(last -f /var/log/wtmp reboot 2>/dev/null | grep -c '^reboot')"
    THIS_MONTH_CRASHS="$(last -f /var/log/wtmp 2>/dev/null | grep -c 'crash')"
    LAST_MONTH_REBOOTS="0"
    LAST_MONTH_CRASHS="0"
    if [ -f /var/log/wtmp.1 ]; then
        LAST_MONTH_REBOOTS="$(last -f /var/log/wtmp.1 reboot 2>/dev/null | grep -c '^reboot')"
        LAST_MONTH_CRASHS="$(last -f /var/log/wtmp.1 2>/dev/null | grep -c 'crash')"
    fi
    echo $(($THIS_MONTH_REBOOTS + $LAST_MONTH_REBOOTS)) "("$(($THIS_MONTH_CRASHS + $LAST_MONTH_CRASHS))")"
else
    SINCE_LAST_MONTH="$(date -d "$(date +%Y-%m-01) -1 month" +%Y-%m-%d)"
    REBOOTS="$(journalctl --list-boots --no-pager -q 2>/dev/null | awk -v since="$SINCE_LAST_MONTH" '$1 ~ /^-?[0-9]+$/ && $4 >= since' | wc -l)"
    CRASHS=0
    while read -r idx _rest; do
        [ -z "$idx" ] && continue
        if ! journalctl -b "$idx" -q --no-pager -o cat \
             MESSAGE_ID=98268866d1d54a499c4e98921d93bc40 2>/dev/null | grep -q .; then
            CRASHS=$((CRASHS + 1))
        fi
    done < <(journalctl --list-boots --no-pager -q 2>/dev/null | awk -v since="$SINCE_LAST_MONTH" '$1 ~ /^-?[0-9]+$/ && $4 >= since && $1 < 0 {print $1}')
    echo "${REBOOTS:-0} (${CRASHS})"
fi
