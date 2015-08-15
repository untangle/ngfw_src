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
in_msgid_phase = False
in_msgstr_phase = False

for line in txt.readlines():
    if bool( re.search( '^msgid ', line )):
        in_msgid_phase = True
        in_msgstr_phase = False
        key = line
        key = re.sub('^msgid \"','',key)
        key = re.sub('\"\s$','',key)
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
            in_msgid_phase = False
            in_msgstr_phase = True
            sys.stdout.write('msgstr "' + value + '"\n')
            value = None
        else:
            if line.strip() == "":
                in_msgstr_phase = False
                in_msgid_phase = False

            if in_msgid_phase:
                str = line
                str = re.sub('^\"','',str)
                str = re.sub('\"\s$','',str)
                if value.endswith("X"):
                    value = value[:-1]
                value = value + str + "X"

            elif in_msgstr_phase:
                # dont print the extra lines
                continue
                
            sys.stdout.write(line)


sys.stdout.flush()
txt.close()
