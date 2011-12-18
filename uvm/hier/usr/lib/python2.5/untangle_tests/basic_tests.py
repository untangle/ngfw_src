
import unittest
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
clientControl = ClientControl()

class TestEnvironmentTests(unittest.TestCase):

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
        assert (uvmContext.nodeManager().isInstantiated('untangle-node-reporting'))

    # verify connectivity to client
    def test_10_clientConnectivity(self):
        result = clientControl.runCommand("/bin/true")
        assert (result == 0)

    # verify client can exec commands and return code
    def test_11_clientShellReturnCode(self):
        result = clientControl.runCommand("/bin/true")
        assert (result == 0)
        result = clientControl.runCommand("/bin/false")
        assert (result == 1)

    # verify client has necessary tools
    def test_12_clientHasNecessaryTools(self):
        result = clientControl.runCommand("which wget >/dev/null")
        assert (result == 0)
        result = clientControl.runCommand("which curl >/dev/null")
        assert (result == 0)
        result = clientControl.runCommand("which netcat >/dev/null")
        assert (result == 0)

    # verify client is online
    def test_13_clientIsOnline(self):
        result = clientControl.runCommand("wget -o /dev/null http://google.com/")
        assert (result == 0)



