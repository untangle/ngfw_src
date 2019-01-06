import os
import sys
import subprocess
import time
import datetime
import re
from uvm import Manager
from uvm import Uvm

# exteral global variables
hostUsername = "testshell"
hostKeyFile = "@PREFIX@/usr/lib/python3.5/dist-packages/tests/test_shell.key"
logfile = None
verbosity = 0
sshOptions = "-o StrictHostKeyChecking=no -o ConnectTimeout=300 -o ConnectionAttempts=15"
quickTestsOnly = False
interface = 0
interfaceExternal = 0
hostname = None

__orig_stdout = None
__orig_stderr = None

# set the key file permissions correctly just in case
subprocess.call("chmod 600 %s" % hostKeyFile, shell=True)

netsettings = Uvm().getUvmContext(timeout=240).networkManager().getNetworkSettings()
for interface in netsettings['interfaces']['list']:
    if interface['name'] == "External":
        interfaceExternal = interface.get('interfaceId')
        break

def __redirect_output( logfile ):
    global __orig_stderr, __orig_stdout
    __orig_stdout = sys.stdout
    __orig_stderr = sys.stderr
    sys.stdout = logfile
    sys.stderr = logfile

def __restore_output():
    global __orig_stderr, __orig_stdout
    sys.stdout = __orig_stdout
    sys.stderr = __orig_stderr

# runs a given command on the specified host (or the default client IP if host = None)
# returns the exit code of the command
# if stdout=True returns the output of the command
# if nowait=True returns the initial output if stdout=True, 0 otherwise
def run_command( command, host=None, stdout=False, nowait=False):
    global client_ip, hostUsername, hostKeyFile, sshOptions, logfile, verbosity
    if host == None:
        host = client_ip

    if logfile != None:
        __redirect_output( logfile )

    result = 1
    try:
        sshCommand = "ssh %s -i %s %s@%s \"%s\"" % ( sshOptions, hostKeyFile, hostUsername, host, command )
        # if verbosity > 1:
        #   print("\nSSH cmd : %s" % sshCommand)
        if verbosity > 0:
            print(("\nClient  : %s" % host))
            print(("Command : %s" % command))
            sys.stdout.flush()
        if (nowait):
            sshCommand += " & " # don't wait for process to complete
        proc = subprocess.Popen(sshCommand, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

        # If nowait, sleep for a second to give time for the ssh to connect and run the command before returning
        if nowait:
            time.sleep(1)
            if stdout:
                return proc.communicate()[0].strip()
            else:
                return 0

        result = proc.wait()
        output = proc.communicate()[0].strip()
        print(("Result  : %i"  % result))
        print(("Output  : %s"  % output))
        sys.stdout.flush()
        if stdout:
            return output.decode('utf-8')
        else:
            return result
    finally:
        if logfile != None:
            __restore_output()

def is_online( tries=5, host=None ):
    onlineResults = -1
    while tries > 0 and onlineResults != 0:
        onlineResults = run_command("wget -q -O /dev/null -4 -t 2 --timeout=5 http://test.untangle.com/", host=host)
        if onlineResults != 0:
            # check DNS and pings if offline
            run_command("host test.untangle.com", host=host)
            run_command("ping -c 1 test.untangle.com", host=host)
        tries -= 1
    return onlineResults

def get_hostname():
    global hostname
    if hostname != None:
        return hostname
    hostname = run_command("hostname -s", stdout=True)
    return hostname
