#!/usr/bin/python

import os
import getopt
import sys
import subprocess
import re

def usage():
    print """\
usage: %s filename
example: %s test.po 
""" % (sys.argv[0], sys.argv[0])
    sys.exit(1)

try:
     opts, args = getopt.getopt(sys.argv[1:], "", [])
except getopt.GetoptError, err:
     print str(err)
     usage()
     sys.exit(2)

if len(args) < 1:
    usage();

filename = args[0]

txt = open(filename)

for line in txt.readlines():
    if bool( re.search( '^msgid ', line )):
        key = line
        key = re.sub('^msgid \"','',key)
        key = re.sub('\"$','',key)
        key = re.sub('\n','',key)
        if key == "date_fmt":
            value = 'Y|m|d'
        elif key == "thousand_sep":
            value = "."
        elif key == "decimal_sep":
            value = ","
        elif key == "timestamp_fmt":
            value = "Y|m|d g:i:s a"
        else:
            value = "X" + key + "X"
        sys.stdout.write(line)
    else:
        if bool( re.search( '^msgstr ', line )):
            sys.stdout.write('msgstr "' + value + '"\n')
        else:
            sys.stdout.write(line)

sys.stdout.flush()
txt.close()
