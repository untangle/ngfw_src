#!/usr/bin/python -u
"""
Provision system as follows:
-   Restore settings.
-   Mark the Setup Wizard as completed.
-   Set registered flag.
-   If time zone specified:
        -   Set timezone.
        -   Force time sync.
        -   Reboot
"""
import getopt
import os
import sys
import subprocess

sys.path.insert(0,'@PREFIX@/usr/lib/python%d.%d/' % sys.version_info[:2])
import uvm


def main(argv):
    restore_arguments = None
    time_zone = None

    try:
        opts, args = getopt.getopt(argv, "hdr:s:t:n", ["help", "restore_arguments=", "time_zone="] )
    except getopt.GetoptError:
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            sys.exit()
        if opt in ( "--restore_arguments"):
            restore_arguments = arg
        if opt in ( "--time_zone"):
            time_zone = arg

    restore_command=['@PREFIX@/usr/share/untangle/bin/ut-restore.sh', restore_arguments]
    result = subprocess.call(restore_command)

    if result is 0:
        Uvm = uvm.Uvm().getUvmContext()
        Uvm.wizardComplete()
        Uvm.setRegistered()
        if time_zone is not None:
            Uvm.systemManager().setTimeZone(time_zone)
            Uvm.forceTimeSync()
            Uvm.rebootBox()

if __name__ == "__main__":
    main( sys.argv[1:] )