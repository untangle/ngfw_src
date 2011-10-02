
import unittest
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
clientControl = ClientControl()

node = None

class WebFilterTests(unittest.TestCase):

    def setUp(self):
        global node
        if node == None:
            if (uvmContext.nodeManager().isInstantiated("untangle-node-webfilter")):
                raise Exception('node webfilter already instantiated')
            node = uvmContext.nodeManager().instantiateAndStart("untangle-node-webfilter", uvmContext.policyManager().getDefaultPolicy())

    # verify client is online
    def test_10_clientIsOnline(self):
        result = clientControl.runCommand("wget -o /dev/null http://google.com/")
        assert (result == 0)

    # verify client is online
    def test_11_defaultPornIsBlocked(self):
        result = clientControl.runCommand("wget http://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)


    def test_999_finalTearDown(self):
        global node
        uvmContext.nodeManager().destroy(node['nodeId']);
        








