#!/usr/bin/env python3

# Sync Settings is takes the tunnel-vpn settings JSON file and "syncs" it to the 
# It reads through the settings and writes the appropriate files:
#
# /etc/untangle/iptables-rules.d/350-tunnel-vpn
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
import stat

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
    -d          : disable tunnel-vpn (writes null ruleset)
    -p <prefix> : prefix to append to output files
    -v          : verbose (can be specified more than one time)
""" % sys.argv[0] )

# sanity check settings
def check_settings( settings ):
    if settings.get('rules') == None or settings.get('rules').get('list') == None:
        raise Exception("Missing Rules")
    return

# Fix settings if anything is odd/weird about them.
# If all weights are 0, then change to all one so there is no divide by zero
def fixup_settings( ):
    return


def write_iptables_rule( file, rule, verbosity=0 ):
    if 'enabled' in rule and not rule['enabled']:
        return
    if 'conditions' not in rule or 'list' not in rule['conditions']:
        return
    if 'ruleId' not in rule:
        return
    if 'tunnelId' not in rule:
        return
    tunnelId = rule.get('tunnelId')
    if tunnelId <= -2:
        print("Invalid tunnelId: " + tunnelId )
        return

    if tunnelId == 0:
        target = " -j ACCEPT"
    elif tunnelId == -1:
        target = " -j tunnel-vpn-any"
    else:
        target = " -j tunnel-vpn-%s " % str(tunnelId)
        
    description = "Route Rule #%i" % int(rule['ruleId'])
    commands = IptablesUtil.conditions_to_prep_commands( rule['conditions']['list'], description, verbosity );
    iptables_conditions = IptablesUtil.conditions_to_iptables_string( rule['conditions']['list'], description, verbosity );
    commands += [ "${IPTABLES} -t mangle -A tunnel-vpn-rules " + ipt + target for ipt in iptables_conditions ]

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

    file.write("# Create (if needed) and flush" + "\n");
    file.write("${IPTABLES} -t mangle -N tunnel-vpn-rules 2>/dev/null" + "\n");
    file.write("${IPTABLES} -t mangle -F tunnel-vpn-rules >/dev/null 2>&1" + "\n");
    file.write("\n");

    file.write("${IPTABLES} -t mangle -N tunnel-vpn-any 2>/dev/null" + "\n");
    file.write("${IPTABLES} -t mangle -F tunnel-vpn-any >/dev/null 2>&1" + "\n");
    file.write("${IPTABLES} -t mangle -A tunnel-vpn-any -j RETURN" + "\n");
    file.write("\n");

    for tunnel in settings.get('tunnels').get('list'):
        file.write("# Create target for tunnel-%i %s" % (tunnel.get('tunnelId'),tunnel.get('name')) + "\n");
        file.write("${IPTABLES} -t mangle -N tunnel-vpn-%i 2>/dev/null"%tunnel.get('tunnelId') + "\n");
        file.write("${IPTABLES} -t mangle -F tunnel-vpn-%i >/dev/null 2>&1"%tunnel.get('tunnelId') + "\n");
        file.write("${IPTABLES} -t mangle -A tunnel-vpn-%i -j RETURN"%tunnel.get('tunnelId') + "\n");
        file.write("\n");
    
    file.write("# Call chains from prerouting-tunnel-vpn in mangle" + "\n");
    file.write("${IPTABLES} -t mangle -D prerouting-tunnel-vpn -m conntrack --ctstate NEW -m comment --comment \"Run route rules\" -j tunnel-vpn-rules >/dev/null 2>&1" + "\n");

    if parser.disable:
        return

    file.write("${IPTABLES} -t mangle -I prerouting-tunnel-vpn -m conntrack --ctstate NEW -m comment --comment \"Run route rules\" -j tunnel-vpn-rules" + "\n");
    file.write("\n");

    for tunnel in settings.get('tunnels').get('list'):
        file.write("${IPTABLES} -t mangle -D mark-src-intf -i tun%i -j MARK --set-mark %i/0x00ff -m comment --comment \"Set src interface mark for tunnel vpn\" >/dev/null 2>&1"%(tunnel.get('tunnelId'),tunnel.get('tunnelId')) + "\n");
        file.write("${IPTABLES} -t mangle -D mark-dst-intf -o tun%i -j MARK --set-mark %i/0xff00 -m comment --comment \"Set dst interface mark for tunnel vpn\" >/dev/null 2>&1"%(tunnel.get('tunnelId'),tunnel.get('tunnelId')<<8) + "\n");
        file.write("${IPTABLES} -t mangle -I mark-src-intf 3 -i tun%i -j MARK --set-mark %i/0x00ff -m comment --comment \"Set src interface mark for tunnel vpn\""%(tunnel.get('tunnelId'),tunnel.get('tunnelId')) + "\n");
        file.write("${IPTABLES} -t mangle -I mark-dst-intf 3 -o tun%i -j MARK --set-mark %i/0xff00 -m comment --comment \"Set dst interface mark for tunnel vpn\""%(tunnel.get('tunnelId'),tunnel.get('tunnelId')<<8) + "\n");
        file.write("\n");

        # If the nat settings is true, or its missing
        # Add rules to NAT traffic exiting the tunnel
        if tunnel.get('nat') == None or tunnel.get('nat') == True:
            file.write("${IPTABLES} -t nat -D nat-rules -o tun%i -j MASQUERADE -m comment --comment \"NAT tunnel vpn sessions\" >/dev/null 2>&1"%(tunnel.get('tunnelId')) + "\n");
            file.write("${IPTABLES} -t nat -I nat-rules -o tun%i -j MASQUERADE -m comment --comment \"NAT tunnel vpn sessions\""%(tunnel.get('tunnelId')) + "\n");
            file.write("\n");
        
    try:
        file.write("\n")
        file.write("# Rules" + "\n")
        rules = settings.get('rules').get('list');
        for rule in rules:
            try:
                write_iptables_rule( file, rule, parser.verbosity );
            except Exception as e:
                traceback.print_exc(e)
    except Exception as e:
        traceback.print_exc(e)

parser = ArgumentParser()
parser.parse_args()
settings = None

if parser.file == None:
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

# Write 350-tunnel-vpn iptables file
filename = parser.prefix + "/etc/untangle/iptables-rules.d/350-tunnel-vpn"
fileDir = os.path.dirname( filename )
if not os.path.exists( fileDir ):
    os.makedirs( fileDir )
    
file = open( filename, "w+" )

write_iptables_file( file, parser.verbosity )

file.flush()
file.close()
os.system("chmod a+x %s" % filename)
if parser.verbosity > 0: print("Wrote %s" % filename)

# Write the auth.txt files
for tunnel in settings.get('tunnels').get('list'):
    if tunnel.get('tunnelId') == None:
        continue
    username = 'username'
    password = 'password'
    if tunnel.get('username') != None:
        username = tunnel.get('username')
    if tunnel.get('password') != None:
        password = tunnel.get('password')
    filename = parser.prefix + "@PREFIX@/usr/share/untangle/settings/tunnel-vpn/tunnel-%i/auth.txt" % tunnel.get('tunnelId')
    try: os.makedirs(os.path.dirname(filename))
    except: pass
    file = open( filename, "w+" )
    file.write("%s\n%s\n" % (username,password));
    file.flush()
    file.close()
    os.chmod(filename, stat.S_IWRITE | stat.S_IREAD)
    if parser.verbosity > 0: print("Wrote %s" % filename)



