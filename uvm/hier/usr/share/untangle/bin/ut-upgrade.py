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

from uvm.settings_reader import get_uvm_settings_item

# set noninteractive mode for all apt-get calls
os.environ['DEBIAN_FRONTEND'] = 'noninteractive'
# set the path (in case its run from cron)
os.environ['PATH'] = '/usr/local/bin:/usr/local/sbin:/usr/bin:/usr/sbin:/bin:/sbin:' + os.environ['PATH']

# apt-get options for various commands
UPGRADE_OPTS = " -o DPkg::Options::=--force-confnew --yes --force-yes --fix-broken --purge "
UPDATE_OPTS = " --yes --force-yes --purge "
AUTOREMOVE_OPTS = " --yes --purge "

# Ignore SIGHUP from parent (this is in case we get launched by the UVM, and then it exits)
# This isn't enough because this doesn't modify sigprocmask so children of this process still get it
signal.signal( signal.SIGHUP, signal.SIG_IGN )
# Detach from parent (so it won't send us signals like SIGHUP)
# This should shield us and children from SIGHUPs
os.setpgrp()

apt_log = open("/var/log/uvm/apt.log", "a")

def log(str):
    # wrap all attempts in try/except
    # if the parent dies, stdout might product a sigpipe
    # see we need to be careful with "print"
    try: 
        apt_log.write(str + "\n")
        apt_log.flush()
    except:
        pass
    try:
        print str
    except: 
        pass

def log_date( cmd ):
    p = subprocess.Popen(["date","+%Y-%m-%d:%H:%m"], stdout=subprocess.PIPE )
    for line in iter(p.stdout.readline, ''):
        log( line.strip() + " " + cmd)
    p.wait()

def cmd_to_stderr(cmd):
    p = subprocess.Popen(["sh","-c","%s 2>&1" % (cmd)], stdout=subprocess.PIPE )
    for line in iter(p.stdout.readline, ''):
        log( line.strip() )
    p.wait()
    if p.returncode == 0:
        return 0
    else:
        return 1

def update():
    log("apt-get update %s" % UPDATE_OPTS)

    p = subprocess.Popen(["sh","-c","apt-get update %s 2>&1" % UPDATE_OPTS], stdout=subprocess.PIPE)
    for line in iter(p.stdout.readline, ''):
        if not re.search('^W: (Conflicting distribution|You may want to run apt-get update to correct these problems)', line):
            log( line.strip() )
    p.wait()
    return 0

def upgrade():
    log("apt-get dist-upgrade %s" % UPGRADE_OPTS)
    r = cmd_to_stderr("apt-get dist-upgrade %s" % UPGRADE_OPTS)
    return 0

def autoremove():
    log("apt-get autoremove %s" % AUTOREMOVE_OPTS)
    r = cmd_to_stderr("apt-get autoremove %s" % AUTOREMOVE_OPTS)
    return 0

log_date( os.path.basename( sys.argv[0]) )
log("")

update()
log("")

upgrade()
log("")

autoremove()
log("")

log_date( os.path.basename( sys.argv[0]) + " done." )

sys.exit(0)
    
