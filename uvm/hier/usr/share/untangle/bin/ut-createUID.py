#!/usr/bin/python

# This file creates a UID 
# and writes the uid file  (default: /usr/share/untangle/conf/uid)
# and writes the sources file (default: /etc/apt/sources.list.d/untangle.list)

import getopt
import random
import sys
import os
import re
import datetime
import traceback

CURRENT_STABLE=None
UID_FILENAME=None
SOURCES_FILENAME="/etc/apt/sources.list.d/untangle.list"
UPDATE_SERVER="updates.untangle.com"
URI_REGEX=re.compile(r'^(https?:\/\/)([^?\/\s]+[?\/])(.*)')

class ArgumentParser(object):
    def __init__(self):
        pass

    def set_distro( self, arg ):
        global CURRENT_STABLE
        CURRENT_STABLE = arg

    def set_uidfile( self, arg ):
        global UID_FILENAME
        UID_FILENAME = arg

    def set_aptfile( self, arg ):
        global SOURCES_FILENAME
        SOURCES_FILENAME = arg

    def set_updatesrv( self, arg ):
        global UPDATE_SERVER
        UPDATE_SERVER = arg

    def parse_args( self ):
        handlers = {
            '-d' : self.set_distro,
            '-f' : self.set_uidfile,
            '-a' : self.set_aptfile,
            '-u' : self.set_updatesrv
        }

        try:
            (optlist, args) = getopt.getopt(sys.argv[1:], 'd:n:f:a:u:')
            for opt in optlist:
                handlers[opt[0]](opt[1])
            return args
        except getopt.GetoptError, exc:
            print(exc)
            printUsage()
            exit(1)

def printUsage():
    sys.stderr.write( """\
%s Usage:
  required args:
    -d <current_stable_distro> example: "stable-10"
    -f <uid_filename>          example: "/usr/share/untangle/conf/uid"
  optional args:
    -a <apt_sources_filename>  default: "/etc/apt/sources.list.d/untangle.list"
    -u <update_server>         default: "updates.untangle.com"
""" % (sys.argv[0]) )
    sys.exit(1)

parser = ArgumentParser()
parser.parse_args()

if CURRENT_STABLE == None or UID_FILENAME == None:
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
    else:
        print("Unknown debian Version %s. Assuming \"%s\"" % (ver.strip(), debian_distro))
except Exception,e:
    traceback.print_exc()
    print("Unknown debian Version %s. Assuming \"%s\"" % (ver.strip(), debian_distro))

# last two bytes in UID have special meaning 
platforms = { 'sarge':'0', 'etch':'1', 'sid':'2', 'lenny':'7', 'squeeze':'8', 'wheezy':'9', 'jessie':'3', 'stretch':'4' }
versions = { 'hardware':'1', 'iso':'2' }

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

update_uri_scheme = "http://"
update_uri_host = UPDATE_SERVER+"/"
match = re.search(URI_REGEX, UPDATE_SERVER)
if match:
    update_uri_scheme=match.group(1)
    update_uri_host=match.group(2)

# write sources file
file = open( SOURCES_FILENAME, "w+" )
file.write("## Auto Generated on %s\n" % datetime.datetime.now());
file.write("## DO NOT EDIT. Changes will be overwritten.\n" + "\n");
file.write("deb %s%s:untangle@%spublic/%s %s main non-free\n" % (update_uri_scheme, uid, update_uri_host, debian_distro, CURRENT_STABLE))
file.flush()
file.close()


sys.exit(0)
