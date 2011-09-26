
import unittest
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
clientControl = ClientControl()

class TestEnvironmentTests(unittest.TestCase):

    # tests that the test suite can connect to the untangle-vm
    def test_uvmConnectivity(self):
        global uvmContext
        try:
            version = uvmContext.version()
        except JSONRPCException, e:
            raise AssertionError("Failed to connect to untangle-vm")

    # tests that the test suite can connect to the client and run a command
    def test_clientConnectivity(self):
        result = clientControl.runCommand("/bin/true")
        assert (result == 0)

    # tests that the test suite can connect to the client and run a command and get return codes
    def test_shellReturnCode(self):
        result = clientControl.runCommand("/bin/true")
        assert (result == 0)
        result = clientControl.runCommand("/bin/false")
        assert (result == 1)







