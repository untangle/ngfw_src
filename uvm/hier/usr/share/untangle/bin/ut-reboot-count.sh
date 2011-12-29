#!/bin/sh

THIS_MONTH_REBOOTS="`last -f /var/log/wtmp reboot | grep '^reboot' | wc -l`"
THIS_MONTH_CRASHS="`last -f /var/log/wtmp reboot | grep 'crash' | wc -l`"
LAST_MONTH_REBOOTS="0"
LAST_MONTH_CRASHS="0"
if [ -f /var/log/wtmp.1 ] ; then
    LAST_MONTH_REBOOTS="`last -f /var/log/wtmp.1 reboot | grep '^reboot' | wc -l`"
    LAST_MONTH_CRASHS="`last -f /var/log/wtmp.1 reboot | grep 'crash' | wc -l`"
fi

echo $(($THIS_MONTH_REBOOTS + $LAST_MONTH_REBOOTS)) "("$(($THIS_MONTH_CRASHS + $LAST_MONTH_CRASHS))")"
