import unittest2
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control

uvmContext = Uvm().getUvmContext()

class TestEnvironmentTests(unittest2.TestCase):

    # verify connectivity to untangle-vm
    def test_01_uvmConnectivity(self):
        global uvmContext
        try:
            version = uvmContext.version()
        except JSONRPCException, e:
            raise AssertionError("Failed to connect to untangle-vm")

    # verify reports is installed (needed for event log tests)
    def test_02_reportsIsInstalled(self):
        global uvmContext
        assert (uvmContext.nodeManager().isInstantiated('untangle-node-reports'))

    # verify reports flush events works
    def test_03_reportsFlushEvents(self):
        reports = uvmContext.nodeManager().node("untangle-node-reports")
        assert (reports != None)
        reports.flushEvents()

    # verify connectivity to client
    def test_10_clientConnectivity(self):
        assert ( remote_control.runCommand("/bin/true") == 0 )

    # verify client can exec commands and return code
    def test_11_clientShellReturnCode(self):
        assert ( remote_control.runCommand("/bin/false") == 1 )

    # verify client can exec commands and return code
    def test_12_clientShellOutput(self):
        result = remote_control.runCommand("echo yay", stdout=True)
        assert (result == "yay")

    # verify client has necessary tools
    def test_13_clientHasNecessaryTools(self):
        # on jessie:
        #   apt-get install host netcat-openbsd python curl wget nmap mime-construct sysvinit-utils
        assert ( remote_control.runCommand("which wget") == 0 )
        assert ( remote_control.runCommand("which curl") == 0 )
        assert ( remote_control.runCommand("which netcat") == 0 )
        assert ( remote_control.runCommand("which nmap") == 0 )
        assert ( remote_control.runCommand("which python") == 0 )
        assert ( remote_control.runCommand("which mime-construct") == 0 )
        assert ( remote_control.runCommand("which pidof") == 0 )
        assert ( remote_control.runCommand("which host") == 0 )
        # check for netcat options
        assert ( remote_control.runCommand("netcat -h 2>&1 | grep -q '\-d\s'") == 0 )
        assert ( remote_control.runCommand("netcat -h 2>&1 | grep -q '\-z\s'") == 0 )
        assert ( remote_control.runCommand("netcat -h 2>&1 | grep -q '\-w\s'") == 0 )
        assert ( remote_control.runCommand("netcat -h 2>&1 | grep -q '\-l\s'") == 0 )
        assert ( remote_control.runCommand("netcat -h 2>&1 | grep -q '\-4\s'") == 0 )
        assert ( remote_control.runCommand("netcat -h 2>&1 | grep -q '\-p\s'") == 0 )

    # verify client is online
    def test_14_clientIsOnline(self):
        assert ( remote_control.isOnline() == 0 )
        assert ( remote_control.runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 http://google.com/") == 0 )

    # verify client can pass UDP
    def test_20_clientCanPassUDP(self):
        assert ( remote_control.runCommand("host cnn.com 8.8.8.8") == 0 )
        assert ( remote_control.runCommand("host google.com 8.8.8.8") == 0 )

    # verify client is online
    def test_30_clientNotRunningOpenvpn(self):
        assert ( remote_control.runCommand("pidof openvpn") != 0 )
