import os

class ClientControl:

    # global variables
    hostIP = "192.168.2.100"
    hostUsername = "testshell"
    hostKeyFile = "@PREFIX@/usr/lib/python2.5/untangle_tests/testShell.key"
    verbosity = 0

    # runs a given command
    # returns 0 if the process returned 0, 1 otherwise
    def runCommand(self, command):
        if (ClientControl.verbosity <= 1):
            redirectOutput = " >/dev/null 2>&1 "
        else:
            redirectOutput = ""

        sshCommand = "ssh -i %s %s@%s \"%s %s\" %s" % (ClientControl.hostKeyFile, ClientControl.hostUsername, ClientControl.hostIP, command, redirectOutput, redirectOutput)
        if (ClientControl.verbosity > 2):
            print "Running command on client: \"%s\"" % command
        result = os.system(sshCommand)
        if result == 0:
            return 0
        else:
            return 1
        
