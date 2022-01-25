#!/usr/bin/python3

# This file creates a UID 
# and writes the uid file  (default: /usr/share/untangle/conf/uid)

import getopt
import random
import sys
import os
import re
import datetime
import traceback

UID_FILENAME=None
class ArgumentParser(object):
    def __init__(self):
        pass

    def set_uidfile( self, arg ):
        global UID_FILENAME
        UID_FILENAME = arg

    def parse_args( self ):
        handlers = {
            '-f' : self.set_uidfile
        }

        try:
            (optlist, args) = getopt.getopt(sys.argv[1:], 'd:n:f:a:u:')
            for opt in optlist:
                handlers[opt[0]](opt[1])
            return args
        except getopt.GetoptError as exc:
            print(exc)
            printUsage()
            exit(1)

def printUsage():
    sys.stderr.write( """\
%s Usage:
  required args:
    -f <uid_filename>          example: "/usr/share/untangle/conf/uid"
""" % (sys.argv[0]) )
    sys.exit(1)

parser = ArgumentParser()
parser.parse_args()

if UID_FILENAME == None:
    printUsage()

# find debian version
debian_distro='wheezy'
try:
    debian_version_file = open( "/etc/debian_version", "r+" )
    ver = debian_version_file.read()
    if re.match(r'^7\.*',ver) != None:
        debian_distro='wheezy'
    elif re.match(r'^8\.*',ver) != None:
        debian_distro='jessie'
    elif re.match(r'^9\.*',ver) != None:
        debian_distro='stretch'
    elif re.match(r'^10\.*',ver) != None:
        debian_distro='buster'
    elif re.match(r'^11\.*',ver) != None:
        debian_distro='bullseye'
    else:
        print("Unknown debian Version %s. Assuming \"%s\"" % (ver.strip(), debian_distro))
except Exception as e:
    traceback.print_exc()
    print("Unknown debian Version %s. Assuming \"%s\"" % (ver.strip(), debian_distro))

# last two bytes in UID have special meaning 
platforms = {'sarge': '0',
             'etch': '1',
             'sid': '2',
             'jessie': '3',
             'stretch': '4',
             'buster': '5',
             'bullseye': '6',
             'lenny': '7',
             'squeeze': '8',
             'wheezy': '9'}
versions = {'hardware': '1', 'iso': '2'}

# generate UID (mostly random bytes)
uid_quad1 = ''.join(random.choice('0123456789abcdef') for i in range(4))
uid_quad2 = ''.join(random.choice('0123456789abcdef') for i in range(4))
uid_quad3 = ''.join(random.choice('0123456789abcdef') for i in range(4))
uid_quad4 = ''.join(random.choice('0123456789abcdef') for i in range(2)) + platforms[debian_distro] + versions['iso']
uid = uid_quad1 + "-" + uid_quad2 + "-" + uid_quad3 + "-" + uid_quad4

# write UID file
file = open( UID_FILENAME, "w+" )
file.write(uid + "\n")
file.flush()
file.close()

sys.exit(0)
