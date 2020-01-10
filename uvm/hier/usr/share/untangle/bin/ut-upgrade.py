#!/usr/bin/python -u
# $Id$#!/usr/bin/python

import getopt
import sys
import os
import signal
import re
import subprocess
import tempfile
import time
import logging
import platform

# set noninteractive mode for all apt-get calls
os.environ['DEBIAN_FRONTEND'] = 'noninteractive'
# set the path (in case its run from cron)
os.environ['PATH'] = '/usr/local/bin:/usr/local/sbin:/usr/bin:/usr/sbin:/bin:/sbin:' + os.environ['PATH']

# apt-get options for various commands
INSTALL_OPTS = " -o DPkg::Options::=--force-confnew -o DPkg::Options::=--force-confmiss --yes --force-yes --fix-broken --purge "
UPGRADE_OPTS = " -o DPkg::Options::=--force-confnew -o DPkg::Options::=--force-confmiss --yes --force-yes --fix-broken --purge -o Debug::Pkgproblemresolver=1 -o Debug::Pkgpolicy=1 "
UPDATE_OPTS = " --yes --force-yes "
AUTOREMOVE_OPTS = " --yes --force-yes --purge "
QUIET=False

# Ignore SIGHUP from parent (this is in case we get launched by the UVM, and then it exits)
# This isn't enough because this doesn't modify sigprocmask so children of this process still get it
signal.signal( signal.SIGHUP, signal.SIG_IGN )
# Detach from parent (so it won't send us signals like SIGHUP)
# This should shield us and children from SIGHUPs
os.setpgrp()

def printUsage():
    sys.stderr.write( """\
%s Usage:
  optional args:
    -q   : quiet
""" % (sys.argv[0]) )
    sys.exit(1)


upgrade_log = open("/var/log/uvm/upgrade.log", "a")

try:
     opts, args = getopt.getopt(sys.argv[1:], "q", ['quiet'])
except getopt.GetoptError, err:
     print(str(err))
     printUsage()
     sys.exit(2)

for opt in opts:
     k, v = opt
     if k == '-q' or k == '--quiet':
         QUIET = True

def log(str):
    # wrap all attempts in try/except
    # if the parent dies, stdout might product a sigpipe
    # see we need to be careful with "print"
    try: 
        upgrade_log.write(str + "\n")
        upgrade_log.flush()
    except:
        pass
    try:
        if not QUIET:
            print(str)
    except: 
        pass

def log_date( cmd ):
    p = subprocess.Popen(["date","+%Y-%m-%d %H:%M"], stdout=subprocess.PIPE )
    for line in iter(p.stdout.readline, ''):
        log( line.strip() + " " + cmd)
    p.wait()
    return p.returncode

def cmd_to_log(cmd):
    stdin=open(os.devnull, 'rb')
    p = subprocess.Popen(["sh","-c","%s 2>&1" % (cmd)], stdout=subprocess.PIPE, stdin=stdin )
    for line in iter(p.stdout.readline, ''):
        log( line.strip() )
    p.wait()
    return p.returncode

def update():
    log("apt-get update %s" % UPDATE_OPTS)
    p = subprocess.Popen(["sh","-c","apt-get update %s 2>&1" % UPDATE_OPTS], stdout=subprocess.PIPE)
    for line in iter(p.stdout.readline, ''):
        if not re.search('^W: (Conflicting distribution|You may want to run apt-get update to correct these problems)', line):
            log( line.strip() )
    p.wait()
    return p.returncode

def check_upgrade():
    p = subprocess.Popen(["sh","-c","apt-get -s dist-upgrade %s 2>&1" % UPGRADE_OPTS], stdout=subprocess.PIPE)
    for line in iter(p.stdout.readline, ''):
        if re.search('.*been kept back.*', line):
            log( "Packages have been kept back.\n" )
            return 1
    p.wait()
    return p.returncode

def upgrade():
    log("apt-get dist-upgrade %s" % UPGRADE_OPTS)
    return cmd_to_log("apt-get dist-upgrade %s" % UPGRADE_OPTS)

def autoremove():
    log("apt-get autoremove %s" % AUTOREMOVE_OPTS)
    return cmd_to_log("apt-get autoremove %s" % AUTOREMOVE_OPTS)

def clean():
    log("apt-get clean")
    return cmd_to_log("apt-get clean")

log_date( os.path.basename( sys.argv[0]) )

log("")

update()

if "2.6.32" in platform.platform():
    log("Upgrade(s) are not allowed on the 2.6.32 kernel. Please reboot and select a newer kernel.")
    sys.exit(1)

log_date("")
log("")

r = check_upgrade();
if r != 0:
    log("apt-get -s dist-upgrade returned an error (%i). Abort." % r)
    sys.exit(1)

upgrade()

log_date("")
log("")

autoremove()

log_date("")
log("")

clean()

log_date("")
log("")

log_date( os.path.basename( sys.argv[0]) + " done." )

sys.exit(0)
    
