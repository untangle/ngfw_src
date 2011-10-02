import os
import sys

class ClientControl:

    # global variables
    hostIP = "192.168.2.100"
    hostUsername = "testshell"
    hostKeyFile = "@PREFIX@/usr/lib/python2.5/untangle_tests/testShell.key"
    logfile = None
    verbosity = 0


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
    def runCommand(self, command):

        if (ClientControl.verbosity <= 0):
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
            result = os.system(sshCommand)
            #child_stdin, child_stdout, child_stderr = os.popen3(sshCommand)
            #child_stdin.close()
            #print child_stdout.read()
            #print child_stderr.read()
            #result = os.wait()
        finally:
            if (ClientControl.logfile != None):
                self.restoreOutput()

        if result == 0:
            return 0
        else:
            return 1
        
