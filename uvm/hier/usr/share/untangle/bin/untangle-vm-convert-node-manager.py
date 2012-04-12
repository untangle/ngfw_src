#!/usr/bin/python

#
# This script can be removed in 10.x or after
#

import sys
import os
import re
import base64

def get_settings(debug=False):
    str = "{\n"
    str += '\t"javaClass": "com.untangle.uvm.NodeManagerSettings"\n'
    str += '}\n'

    return str

filename = None
if len(sys.argv) < 1:
    print "usage: %s [filename]" % sys.argv[0]
    sys.exit(1)

if len(sys.argv) > 1:
    filename = sys.argv[1]

try:
    dir = "/usr/share/untangle/settings/untangle-vm/"
    if not os.path.exists(dir):
        os.makedirs(dir)

    settings_str = get_settings()
    print settings_str
    if filename == None:
        filename = "/usr/share/untangle/settings/untangle-vm/node_manager.js"
    file = open(filename, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("could not get result",e);
    sys.exit(1)

sys.exit(0)
