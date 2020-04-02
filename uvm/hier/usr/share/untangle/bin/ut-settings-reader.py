#!/usr/bin/python

import getopt
import sys

if "@PREFIX@" != '':
    sys.path.insert(0, '@PREFIX@/usr/lib/python2.7/dist-packages')

from uvm.settings_reader import get_uvm_settings_item
from uvm.settings_reader import get_app_settings_item


def usage():
    print("""\
usage: %s [options] uvm|app basename|app settings_name
Options:
  -l | --lower                  lower case output
  -d | --default                default value if setting is None/null
""" % sys.argv[0])

try:
     opts, args = getopt.getopt(sys.argv[1:], "ld:", ['lower','default'])
except getopt.GetoptError, err:
     print(str(err))
     usage()
     sys.exit(2)

if len(args) < 3:
    usage();
    sys.exit(2)

location = args[0]
base = args[1]
name = args[2]

option_to_lower = False
default_value = None

for opt in opts:
     k, v = opt
     if k == '-l' or k == '--lower':
         option_to_lower = True
     elif k == '-d' or k == '--default':
          default_value = str(v)

if location == "uvm":
    setting = get_uvm_settings_item(base, name)
elif location == "app":
    setting = get_app_settings_item(base, name)
else:
    print("usage: %s [uvm|app] [basename|app] settings_name" % sys.argv[0])
    sys.exit(1)

if setting == None:
    setting = default_value

if option_to_lower:
    setting  = str(setting).lower();

print(setting)

