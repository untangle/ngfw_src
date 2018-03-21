#!/bin/dash

# This script is launched by the untangle-vm.postinst
# It will run some jessie -> stretch conversion tasks after apt-get is complete


echo "`date -Iseconds` $0: $0 $@ $$"

# Wait for upgrade(apt-get) to complete
while true; do
    if ps -C apt-get,aptitude,dpkg,ut-upgrade.py >/dev/null 2>&1; then
        echo "`date -Iseconds` $0: Waiting for upgrade to complete..."
        sleep 10
    else
        /bin/rm -f $pidfile
        echo "`date -Iseconds` $0: Running conversion"

        echo "`date -Iseconds` $0: apt-get clean"
        apt-get clean

        echo "`date -Iseconds` $0: sync-settings"
        /usr/bin/sync-settings.py -n
        if [ $? != 0 ] ; then
            echo
            echo "sync-settings failed! Not rebooting!"
            echo "sync-settings failed!\nSee /var/log/uvm/stretch.log for more info" | wall
            echo
            exit
        fi

        echo "`date -Iseconds` $0: Rebooting in 30 seconds"
        echo "Rebooting in 30 seconds...\n\"touch /tmp/abort\" to stop" | wall
        sleep 10
        echo "Rebooting in 20 seconds...\n\"touch /tmp/abort\" to stop" | wall
        sleep 10
        echo "Rebooting in 10 seconds...\n\"touch /tmp/abort\" to stop" | wall
        sleep 10
        if [ -f /tmp/abort ] ; then
            exit 0
        else
            echo "Rebooting...\n" | wall
            echo "Rebooting..."
            reboot
            exit 0
        fi
    fi
done
