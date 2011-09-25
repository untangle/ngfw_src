
import unittest
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm

uvmContext = Uvm().getUvmContext()

class TestEnvironmentTests(unittest.TestCase):

    def test_uvmConnectivity(self):
        global uvmContext
        try:
            version = uvmContext.version()
        except JSONRPCException, e:
            raise AssertionError("Failed to connect to untangle-vm")







