import os
import sys
import subprocess

class ClientControl:

    # global variables
    hostIP = "192.168.2.100"
    hostUsername = "testshell"
    hostKeyFile = "@PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key"
    logfile = None
    verbosity = 0

    # set the key file permissions correctly just in case
    os.system("chmod 600 %s" % hostKeyFile)

    def redirectOutput(self, logfile):
        self.orig_stdout = sys.stdout
        self.orig_stderr = sys.stderr
        sys.stdout = logfile
        sys.stderr = logfile

    def restoreOutput(self):
        sys.stdout = self.orig_stdout
        sys.stderr = self.orig_stderr

    # runs a given command
    # returns 0 if the process returned 0, 1 otherwise
    def runCommand(self, command, stdout=False, nowait=False):

        if (ClientControl.verbosity <= 0) and not stdout:
            shellRedirect = " >/dev/null 2>&1 "
        else:
            shellRedirect = ""

        if (ClientControl.logfile != None):
            self.redirectOutput(ClientControl.logfile)

        result = 1
        try:
            sshCommand = "ssh -i %s %s@%s \"%s %s\" %s" % (ClientControl.hostKeyFile, ClientControl.hostUsername, ClientControl.hostIP, command, shellRedirect, shellRedirect)
            if (ClientControl.verbosity > 1):
                print "\nRunning command          : %s" % sshCommand
            if (ClientControl.verbosity > 0):
                print "\nRunning command on client: %s" % command
            if (nowait):
                sshCommand += " & " # don't wait for process to complete
            if (not stdout):
                result = os.system(sshCommand)
                if result == 0:
                    return 0
                else:
                    return 1
            else:
                # send command and read stdout
                rtn_cmd = subprocess.Popen(sshCommand, shell=True, stdout=subprocess.PIPE)
                rtn_stdout = rtn_cmd.communicate()[0].strip()
                return rtn_stdout 
        finally:
            if (ClientControl.logfile != None):
                self.restoreOutput()

        
