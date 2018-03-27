#!/usr/bin/python
"""
Synchronize settings
"""
import json
import os
import getopt
import sys
import re

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)

## need classes

def main(argv):
    """
    Main loop
    """
    global _debug
    _debug = False

    try:
        opts, args = getopt.getopt(argv, ":hd", ["help", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-d", "--debug"):
            _debug = True

# find settings-synchronize.js files and load
# find all settings files
# process

if __name__ == "__main__":
    main(sys.argv[1:])