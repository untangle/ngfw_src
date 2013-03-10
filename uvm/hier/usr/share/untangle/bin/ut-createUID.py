#!/usr/bin/python

# This file creates a UID 
# and writes the username file  (default: /usr/share/untangle/conf/username)
# and writes the sources file (default: /etc/apt/sources.list.d/untangle.list)

import getopt
import random
import sys
import os
import re
import datetime
import traceback

CURRENT_STABLE=None
CURRENT_NAME=None
UID_FILENAME=None
SOURCES_FILENAME="/etc/apt/sources.list.d/untangle.list"
UPDATE_SERVER="updates.untangle.com"

class ArgumentParser(object):
    def __init__(self):
        pass

    def set_distro( self, arg ):
        global CURRENT_STABLE
        CURRENT_STABLE = arg

    def set_name( self, arg ):
        global CURRENT_NAME
        CURRENT_NAME = arg

    def set_usernamefile( self, arg ):
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
            '-n' : self.set_name,
            '-f' : self.set_usernamefile,
            '-a' : self.set_aptfile,
            '-u' : self.set_updatesrv
        }

        try:
            (optlist, args) = getopt.getopt(sys.argv[1:], 'd:n:f:a:u:')
            for opt in optlist:
                handlers[opt[0]](opt[1])
            return args
        except getopt.GetoptError, exc:
            print exc
            printUsage()
            exit(1)

def printUsage():
    sys.stderr.write( """\
%s Usage:
  required args:
    -d <current_stable_distro> example: "stable-10"
    -n <current_stable_name>   example: "focus"
    -f <username_filename>          example: "/usr/share/untangle/conf/username"
  optional args:
    -a <apt_sources_filename>  default: "/etc/apt/sources.list.d/untangle.list"
    -u <update_server>         default: "updates.untangle.com"
""" % (sys.argv[0]) )
    sys.exit(1)

parser = ArgumentParser()
parser.parse_args()

if CURRENT_STABLE == None or CURRENT_NAME == None or UID_FILENAME == None:
    printUsage()

if os.path.exists( UID_FILENAME ):
    print "UID already exists"
    sys.exit(1)

# find debian version
debian_distro='squeeze'
try:
    debian_version_file = open( "/etc/debian_version", "r+" )
    ver = debian_version_file.read()
    if re.match(r'^6\.*',ver) != None:
        debian_distro='squeeze'
    elif re.match(r'^7\.*',ver) != None:
        debian_distro='wheezy'
    else:
        print "Unknown debian Version %s. Assuming \"squeeze\"" % ver.strip()
        debian_distro='squeeze'
except Exception,e:
    traceback.print_exc()
    print "Exception finding debian Version. Assuming \"squeeze\""
    debian_distro='squeeze'

# last two bytes in UID have special meaning 
platforms = { 'sarge':'0', 'etch':'1', 'sid':'2', 'lenny':'7', 'squeeze':'8' }
versions = { 'hardware':'1', 'iso':'2' }

# generate UID (mostly random bytes)
username_quad1 = ''.join(random.choice('0123456789abcdef') for i in range(4))
username_quad2 = ''.join(random.choice('0123456789abcdef') for i in range(4))
username_quad3 = ''.join(random.choice('0123456789abcdef') for i in range(4))
username_quad4 = ''.join(random.choice('0123456789abcdef') for i in range(2)) + platforms[debian_distro] + versions['iso']
username = username_quad1 + "-" + username_quad2 + "-" + username_quad3 + "-" + username_quad4

# write UID file
file = open( UID_FILENAME, "w+" )
file.write(username + "\n")
file.flush()
file.close()

# write sources file
file = open( SOURCES_FILENAME, "w+" )
file.write("## Auto Generated on %s\n" % datetime.datetime.now());
file.write("## DO NOT EDIT. Changes will be overwritten.\n" + "\n");
file.write("deb http://%s:untangle@%s/public/%s %s main premium upstream" % (username, UPDATE_SERVER, debian_distro, CURRENT_STABLE)+ "\n")
file.write("deb http://%s:untangle@%s/public/%s %s main premium upstream" % (username, UPDATE_SERVER, debian_distro, CURRENT_NAME)+ "\n")
file.flush()
file.close()


sys.exit(0)
