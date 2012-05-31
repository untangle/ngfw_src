#!/usr/bin/python

import getopt
import sys
from uvm.settings_reader import get_uvm_settings_item
from uvm.settings_reader import get_node_settings_item


def usage():
    print """\
usage: %s [options] uvm|node basename|node settings_name
Options:
  -l | --lower                  lower case output
""" % sys.argv[0]

try:
     opts, args = getopt.getopt(sys.argv[1:], "l", ['lower',])
except getopt.GetoptError, err:
     print str(err)
     usage()
     sys.exit(2)

if len(args) < 3:
    usage();
    sys.exit(2)

location = args[0]
base = args[1]
name = args[2]

option_to_lower = False

for opt in opts:
     k, v = opt
     if k == '-l' or k == '--lower':
         option_to_lower = True

if location == "uvm":
    setting = get_uvm_settings_item(base, name)
elif location == "node":
    setting = get_node_settings_item(base, name)
else:
    print "usage: %s [uvm|node] [basename|node] settings_name" % sys.argv[0]
    sys.exit(1)

if option_to_lower:
    setting  = str(setting).lower();
print setting

