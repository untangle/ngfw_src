#!/usr/bin/env python3

# Sync Settings is takes the shield settings JSON file and "syncs" it to the 
# It reads through the settings and writes the appropriate files:
# /etc/untangle/iptables-rules.d/600-shield
#
# This script should be called after changing the settings file to "sync" the settings to the OS.

import sys
sys.path.insert(0, sys.path[0] + "/" + "../" + "../" + "../" + "lib/" + "python%s.%s/" % sys.version_info[:2])

import getopt
import signal
import os
import traceback
import json
import datetime

from sync import *

class ArgumentParser(object):
    def __init__(self):
        self.file = None
        self.networkFile = None
        self.prefix = ''
        self.verbosity = 0
        self.disable = False

    def set_file( self, arg ):
        self.file = arg

    def set_networkFile( self, arg ):
        self.networkFile = arg

    def set_prefix( self, arg ):
        self.prefix = arg

    def increase_verbosity( self, arg ):
        self.verbosity += 1

    def set_disable( self, arg ):
        self.disable = True

    def parse_args( self ):
        handlers = {
            '-f' : self.set_file,
            '-n' : self.set_networkFile,
            '-p' : self.set_prefix,
            '-v' : self.increase_verbosity,
            '-d' : self.set_disable
        }

        try:
            (optlist, args) = getopt.getopt(sys.argv[1:], 'f:n:p:vd')
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
    -f <file>   : shield settings file to sync to OS
    -n <file>   : network settings file 
  optional args:
    -d          : disable shield null ruleset)
    -p <prefix> : prefix to append to output files
    -v          : verbose (can be specified more than one time)
""" % sys.argv[0] )

# sanity check settings
def check_settings( settings ):
    return

# fix settings if they are weird
def fixup_settings( ):
    if 'requestPerSecondLimit' not in settings:
        print("ERROR: no action requestPerSecondLimit in settings")
    settings['action'] = 30

    return

def write_iptables_shield_rule( file, shield_rule, verbosity=0 ):
    if 'enabled' in shield_rule and not shield_rule['enabled']:
        return
    if 'conditions' not in shield_rule or 'list' not in shield_rule['conditions']:
        return
    if 'ruleId' not in shield_rule:
        return
    if 'action' not in shield_rule:
        print("ERROR: invalid shield rule - no action")
        return

    action = shield_rule['action'];
    
    if action == "PASS":
        target = " -j RETURN # PASS"
    else:
        target = " -g shield-process # SCAN"

    description = "Route Rule #%i" % int(shield_rule['ruleId'])
    commands = IptablesUtil.conditions_to_prep_commands( shield_rule['conditions']['list'], description, verbosity );
    iptables_conditions = IptablesUtil.conditions_to_iptables_string( shield_rule['conditions']['list'], description, verbosity );
    commands += [ "${IPTABLES} -t filter -A shield-rules " + ipt + target for ipt in iptables_conditions ]

    file.write("# %s\n" % description);
    for cmd in commands:
        file.write(cmd + "\n")
    return


def write_iptables_file( file, verbosity=0 ):
    global parser

    file.write("#!/bin/dash");
    file.write("\n\n");

    file.write("## Auto Generated on %s\n" % datetime.datetime.now());
    file.write("## DO NOT EDIT. Changes will be overwritten.\n");
    file.write("\n");

    file.write("TABLE_SIZE=65535" + "\n")
    file.write("LIST_SIZE=50" + "\n")
    file.write("MAX_RATE=" + str(settings.get('requestPerSecondLimit')) + "\n")
    
    file.write("""

iptables_debug_onerror()
{
    # Ignore -N errors
    /sbin/iptables "$@" || {
        [ "${3}x" != "-Nx" ] && echo "[`date`] Failed: /sbin/iptables $@"
    }

    true
}

if [ -z "${IPTABLES}" ] ; then
    IPTABLES="iptables_debug_onerror"
fi

${IPTABLES} -t filter -N shield-scan >/dev/null 2>&1
${IPTABLES} -t filter -F shield-scan

${IPTABLES} -t filter -N shield-rules >/dev/null 2>&1
${IPTABLES} -t filter -F shield-rules

${IPTABLES} -t filter -N shield-process >/dev/null 2>&1
${IPTABLES} -t filter -F shield-process

${IPTABLES} -t filter -N shield-block >/dev/null 2>&1
${IPTABLES} -t filter -F shield-block

# modprobe -r xt_recent
modprobe xt_recent ip_list_tot=${TABLE_SIZE} ip_pkt_list_tot=${LIST_SIZE}

""")

    if parser.disable:
        return
    
    file.write("""
    
${IPTABLES} -D FORWARD -t filter -p udp -m comment --comment "Shield scan" -m state --state NEW -j shield-scan >/dev/null 2>&1
${IPTABLES} -A FORWARD -t filter -p udp -m comment --comment "Shield scan" -m state --state NEW -j shield-scan
${IPTABLES} -D FORWARD -t filter -p tcp -m comment --comment "Shield scan" -m state --state NEW -j shield-scan >/dev/null 2>&1
${IPTABLES} -A FORWARD -t filter -p tcp -m comment --comment "Shield scan" -m state --state NEW -j shield-scan

# We do not want to rate limit UDP because DNS lookups to dnsmasq might be affected
# ${IPTABLES} -D INPUT -t filter -p udp -m comment --comment "Shield scan" -m state --state NEW -j shield-scan >/dev/null 2>&1
# ${IPTABLES} -A INPUT -t filter -p udp -m comment --comment "Shield scan" -m state --state NEW -j shield-scan
${IPTABLES} -D INPUT -t filter -p tcp -m comment --comment "Shield scan" -m state --state NEW -j shield-scan >/dev/null 2>&1
${IPTABLES} -A INPUT -t filter -p tcp -m comment --comment "Shield scan" -m state --state NEW -j shield-scan

${IPTABLES} -t filter -A shield-scan -m comment --comment "Only process new sessions" -m state ! --state NEW -j RETURN
${IPTABLES} -t filter -A shield-scan -m comment --comment "Do not process reinjected packets" -i utun -j RETURN
${IPTABLES} -t filter -A shield-scan -m comment --comment "Do not process bypassed sessions" -m connmark --mark 0x01000000/0x01000000 -j RETURN
${IPTABLES} -t filter -A shield-scan -m comment --comment "Process shield rules" -j shield-rules

${IPTABLES} -t filter -A shield-process -m comment --comment "Block sessions over limit" -m recent --name shield --rcheck --seconds 1 --reap --hitcount ${MAX_RATE} -j shield-block
${IPTABLES} -t filter -A shield-process -m comment --comment "Update shield table" -m recent --name shield --set -j RETURN

${IPTABLES} -t filter -A shield-block -m comment --comment "Log the shield_blocked" -j NFLOG --nflog-prefix "shield_blocked"
${IPTABLES} -t filter -A shield-block -m comment --comment "Drop the packet" -m recent --name shield --rcheck --seconds 1 --hitcount ${MAX_RATE} -j DROP

""")

    try:
        file.write("# Shield Rules" + "\n")
        shield_rules = settings.get('rules').get('list');
        for shield_rule in shield_rules:
            try:
                write_iptables_shield_rule( file, shield_rule, parser.verbosity );
            except Exception as e:
                traceback.print_exc(e)
    except Exception as e:
        traceback.print_exc(e)

    file.write("""

${IPTABLES} -t filter -A shield-rules -m comment --comment "No rules matched - process session normally" -j shield-process
        
""")



parser = ArgumentParser()
parser.parse_args()
settings = None

if parser.file == None or parser.networkFile == None:
    printUsage()
    sys.exit(1)

try:
    settingsFile = open(parser.file, 'r')
    settingsData = settingsFile.read()
    settingsFile.close()
    settings = json.loads(settingsData)
except IOError as e:
    print("Unable to read settings file: ",e)
    exit(1)

try:
    networkSettingsFile = open(parser.networkFile, 'r')
    networkSettingsData = networkSettingsFile.read()
    networkSettingsFile.close()
    networkSettings = json.loads(networkSettingsData)
except IOError as e:
    print("Unable to read network settings file: ",e)
    exit(1)

try:
    check_settings(settings)
except Exception as e:
    traceback.print_exc(e)
    exit(1)

IptablesUtil.settings = networkSettings
NetworkUtil.settings = networkSettings

fixup_settings()

if parser.verbosity > 0: print("Syncing %s to system..." % parser.file)

# Write shield iptables file
filename = parser.prefix + "/etc/untangle/iptables-rules.d/600-shield"
fileDir = os.path.dirname( filename )
if not os.path.exists( fileDir ):
    os.makedirs( fileDir )
    
file = open( filename, "w+" )

write_iptables_file( file, parser.verbosity )

file.flush()
file.close()
os.system("chmod a+x %s" % filename)

if parser.verbosity > 0: print("Wrote %s" % filename)


