import os
import sys
import subprocess
import time
import datetime

class ClientControl:

    # global variables
    clientIP = None
    hostUsername = "testshell"
    hostKeyFile = "/usr/lib/python2.7/untangle_tests/testShell.key"
    logfile = None
    verbosity = 0
    sshOptions = "-o StrictHostKeyChecking=no -o ConnectTimeout=300 -o ConnectionAttempts=15"
    quickTestsOnly = False
    interface = 0
    interfaceExternal = 0

    # set the key file permissions correctly just in case
    os.system("chmod 600 %s" % hostKeyFile)

    def __init__(self, host=None):
        self.hostIP = host

    def redirectOutput(self, logfile):
        self.orig_stdout = sys.stdout
        self.orig_stderr = sys.stderr
        sys.stdout = logfile
        sys.stderr = logfile

    def restoreOutput(self):
        sys.stdout = self.orig_stdout
        sys.stderr = self.orig_stderr

    # runs a given command on the client
    # returns the exit code of the command
    # if stdout=True returns the output of the command
    # if nowait=True returns the initial output if stdout=True, 0 otherwise
    def runCommand(self, command, stdout=False, nowait=False):
        if self.hostIP == None:
            self.hostIP = ClientControl.clientIP

        if (ClientControl.logfile != None):
            self.redirectOutput(ClientControl.logfile)

        result = 1
        try:
            sshCommand = "ssh %s -i %s %s@%s \"%s\"" % (ClientControl.sshOptions, ClientControl.hostKeyFile, ClientControl.hostUsername, self.hostIP, command)
            # if (ClientControl.verbosity > 1):
            #    print "\nSSH cmd : %s" % sshCommand
            if (ClientControl.verbosity > 0):
                print "\nClient  : %s" % self.hostIP
                print "Command : %s" % command
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
            print "Result  : %i"  % result
            print "Output  : %s"  % output
            sys.stdout.flush()
            if stdout:
                return output
            else:
                return result
        finally:
            if (ClientControl.logfile != None):
                self.restoreOutput()

    def isOnline(self):
        result = self.runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 http://test.untangle.com/")
        return result

    # FIXME this should be in a test util class
    def check_events(self, events, num_events, *args, **kwargs):
        if events == None:
            return False
        if num_events == 0:
            return False
        if kwargs.get('min_date') == None:
            min_date = datetime.datetime.now()-datetime.timedelta(minutes=10)
        else:
            min_date = kwargs.get('min_date')
        if (len(args) % 2) != 0:
            print "Invalid argument length"
            return False
        num_checked = 0
        while num_checked < num_events:
            if len(events) <= num_checked:
                break
            event = events[num_checked]
            num_checked += 1

            # if event has a date and its too old - ignore the event
            if event.get('time_stamp') != None and datetime.datetime.fromtimestamp((event['time_stamp']['time'])/1000) < min_date:
                continue

            # check each expected value
            # if one doesn't match continue to the next event
            # if all match, return True
            allMatched = True
            for i in range(0, len(args)/2):
                key=args[i*2]
                expectedValue=args[i*2+1]
                actualValue = event.get(key)
                #print "key %s expectedValue %s actualValue %s " % ( key, str(expectedValue), str(actualValue) )
                if str(expectedValue) != str(actualValue):
                    print "mismatch event[%s] expectedValue %s != actualValue %s " % ( key, str(expectedValue), str(actualValue) )
                    allMatched = False
                    break

            if allMatched:
                return True
        return False
