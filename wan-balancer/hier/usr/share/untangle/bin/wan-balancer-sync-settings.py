#!/usr/bin/env python3

# Sync Settings is takes the wan-balancer settings JSON file and "syncs" it to the 
# It reads through the settings and writes the appropriate files:
#
# /etc/untangle/iptables-rules.d/330-wan-balancer-rules
# /etc/untangle/post-network-hook.d/040-wan-balancer
#
# This script should be called after changing the settings file to "sync" the settings to the OS.

import sys

if "@PREFIX@" != '':
    sys.path.insert(0, '@PREFIX@/usr/lib/python3/dist-packages')

import getopt
import signal
import os
import traceback
import json
import datetime
import itertools

from   sync import *

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
    -f <file>   : settings file to sync to OS
    -n <file>   : network settings file 
  optional args:
    -d          : disable wan-balancer (writes null ruleset)
    -p <prefix> : prefix to append to output files
    -v          : verbose (can be specified more than one time)
""" % sys.argv[0] )

# sanity check settings
def check_settings( settings ):
    if settings.get('routeRules') == None or settings.get('routeRules').get('list') == None:
        raise Exception("Missing Route Rules")
    if settings.get('weights') == None:
        raise Exception("Missing WAN weights")
    return

# Fix settings if anything is odd/weird about them.
# If all weights are 0, then change to all one so there is no divide by zero
def fixup_settings( ):
    global networkSettings, settings

    totalWeight = 0.0
    for intf in networkSettings.get('interfaces').get('list'):
        if intf.get('configType') == 'ADDRESSED' and intf.get('isWan'):
            totalWeight = totalWeight + float(settings.get('weights')[intf.get('interfaceId') - 1])

   # If total weight is zero, change all weights to 1
    if totalWeight == 0.0:
        for intf in networkSettings.get('interfaces').get('list'):
            if intf.get('configType') == 'ADDRESSED' and intf.get('isWan'):
                settings.get('weights')[intf.get('interfaceId') - 1] = 1;

def write_iptables_route_rule( file, route_rule, verbosity=0 ):
    if 'enabled' in route_rule and not route_rule['enabled']:
        return
    if 'conditions' not in route_rule or 'list' not in route_rule['conditions']:
        return
    if 'ruleId' not in route_rule:
        return

    if 'destinationWan' in route_rule:
        if int(route_rule.get('destinationWan')) == 0:
            target = " -j ACCEPT " 
        else:
            target = " -j MARK --set-mark 0x%04X/0x%04X " % ( int(route_rule.get('destinationWan'))<<8 ,0xff00) 
    else:
        print("ERROR: invalid route rule target: %s" + str(route_rule))
        return

    description = "Route Rule #%i" % int(route_rule['ruleId'])
    commands = IptablesUtil.conditions_to_prep_commands( route_rule['conditions']['list'], description, verbosity );
    iptables_conditions = IptablesUtil.conditions_to_iptables_string( route_rule['conditions']['list'], description, verbosity );
    iptables_commands = [ "${IPTABLES} -t mangle -A wan-balancer-route-rules " + ipt + target for ipt in iptables_conditions ]
    accept_commands = [ "${IPTABLES} -t mangle -A wan-balancer-route-rules " + ipt + " -j ACCEPT " for ipt in iptables_conditions ]
    all_commands = list(itertools.chain(*zip(iptables_commands,accept_commands)))
    commands += all_commands
    
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

""")

    file.write("# Create (if needed) and flush restore-interface-marks, mark-src-intf, mark-dst-intf chains" + "\n");
    file.write("${IPTABLES} -t mangle -N wan-balancer-route-rules 2>/dev/null" + "\n");
    file.write("${IPTABLES} -t mangle -F wan-balancer-route-rules >/dev/null 2>&1" + "\n");
    file.write("\n");

    file.write("# Call chains from prerouting-wan-balancer in mangle" + "\n");
    file.write("${IPTABLES} -t mangle -D prerouting-wan-balancer -m conntrack --ctstate NEW -m comment --comment \"Run route rules\" -j wan-balancer-route-rules >/dev/null 2>&1" + "\n");

    if parser.disable:
        return

    file.write("${IPTABLES} -t mangle -I prerouting-wan-balancer -m conntrack --ctstate NEW -m comment --comment \"Run route rules\" -j wan-balancer-route-rules" + "\n");
    file.write("\n");

    try:
        file.write("\n")
        file.write("# Route Rules" + "\n")
        route_rules = settings.get('routeRules').get('list');
        for route_rule in route_rules:
            try:
                write_iptables_route_rule( file, route_rule, parser.verbosity );
            except Exception as e:
                traceback.print_exc(e)
    except Exception as e:
        traceback.print_exc(e)

def write_route_file( file, verbosity=0 ):
    global networkSettings, settings
    global parser

    file.write("#!/bin/dash");
    file.write("\n\n");

    file.write("## Auto Generated on %s\n" % datetime.datetime.now());
    file.write("## DO NOT EDIT. Changes will be overwritten.\n");
    file.write("\n");

    file.write("UNTANGLE_PRIORITY_BALANCE=\"900000\"" + "\n")
    file.write("\n");

    if parser.disable:
        file.write("ip rule del priority ${UNTANGLE_PRIORITY_BALANCE} >/dev/null 2>&1" + "\n")
        file.write("# echo ip rule del priority ${UNTANGLE_PRIORITY_BALANCE}" + "\n")
        file.write("exit 0" + "\n")
        return

    file.write("# For each non-zero-weight WAN considered up (an ip rule exists) include it in balancing." + "\n")
    file.write("ROUTE_STR=\"\"" + "\n")
    for intf in networkSettings.get('interfaces').get('list'):
        if intf.get('configType') == 'ADDRESSED' and intf.get('isWan'):
            interfaceId = int(intf.get('interfaceId'))
            weight = int(settings.get('weights')[intf.get('interfaceId') - 1])
            if ( weight != 0 ):
                file.write("if [ ! -z  \"`ip rule ls | grep fwmark | egrep 'uplink.%i\s' 2>/dev/null`\" ] ; then " % (interfaceId) + "\n")
                file.write("    ROUTE_STR=\"$ROUTE_STR `ip route show table uplink.%i | sed -e 's/default/nexthop/'` weight %i\"" % (interfaceId, weight) + "\n")
                file.write("fi" + "\n")
    file.write("\n");

    file.write("# If there are no routes (no up WANs) dont balance at all" + "\n")
    file.write("if [ -z \"$ROUTE_STR\" ] ; then" + "\n")
    file.write("    ip rule del priority ${UNTANGLE_PRIORITY_BALANCE} >/dev/null 2>&1" + "\n")
    file.write("    exit 0" + "\n")
    file.write("fi" + "\n")
    file.write("\n");

    file.write("# echo ip route replace table balance default scope global $ROUTE_STR" + "\n")
    file.write("ip route replace table balance default scope global $ROUTE_STR" + "\n")
    file.write("\n");

    file.write("# delete old priority" + "\n")
    file.write("ip rule del priority 366800 >/dev/null 2>&1" + "\n")
    file.write("\n");

    file.write("ip rule del priority ${UNTANGLE_PRIORITY_BALANCE} >/dev/null 2>&1" + "\n")
    file.write("ip rule add priority ${UNTANGLE_PRIORITY_BALANCE} lookup balance" + "\n")
    
    return




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

# Write 330-wan-balancer iptables file
filename = parser.prefix + "/etc/untangle/iptables-rules.d/330-wan-balancer"
fileDir = os.path.dirname( filename )
if not os.path.exists( fileDir ):
    os.makedirs( fileDir )
    
file = open( filename, "w+" )

write_iptables_file( file, parser.verbosity )

file.flush()
file.close()
os.system("chmod a+x %s" % filename)

if parser.verbosity > 0: print("Wrote %s" % filename)


# Write 040-wan-balancer post network hook file
filename = parser.prefix + "/etc/untangle/post-network-hook.d/040-wan-balancer"
fileDir = os.path.dirname( filename )
if not os.path.exists( fileDir ):
    os.makedirs( fileDir )
    
file = open( filename, "w+" )

write_route_file( file, parser.verbosity )

file.flush()
file.close()
os.system("chmod a+x %s" % filename)

if parser.verbosity > 0: print("Wrote %s" % filename)

