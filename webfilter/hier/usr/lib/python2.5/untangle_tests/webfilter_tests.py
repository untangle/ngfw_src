import unittest
import time
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
clientControl = ClientControl()
nodeDesc = None
node = None

def addBlockedSite(site):
    global node
    newUrl =  { "blocked": True, "description": "description", "flagged": True, "javaClass": "com.untangle.uvm.node.GenericRule", "string": site }

    blockedUrls = node.getBlockedUrls()
    blockedUrls["list"].append(newUrl)
    node.setBlockedUrls(blockedUrls)


class WebFilterTests(unittest.TestCase):


    def setUp(self):
        global nodeDesc, node
        if nodeDesc == None:
            if (uvmContext.nodeManager().isInstantiated("untangle-node-webfilter")):
                raise Exception('node webfilter already instantiated')
            nodeDesc = uvmContext.nodeManager().instantiateAndStart("untangle-node-webfilter", uvmContext.policyManager().getDefaultPolicy())
            node = uvmContext.nodeManager().nodeContext(nodeDesc['nodeId']).node()

    # verify client is online
    def test_10_clientIsOnline(self):
        result = clientControl.runCommand("wget -o /dev/null http://google.com/")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_11_defaultPornIsBlocked(self):
        result = clientControl.runCommand("wget http://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify metaloft site is not blocked in default config
    def test_12_defaultMetaloftIsNotBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test.html 2>&1 | grep -q success")
        assert (result == 0)

    # verify blocked site url list works
    def test_13_blockedUrl(self):
        addBlockedSite("metaloft.com/test.html")
        # this test URL should now be blocked
        result = clientControl.runCommand("wget http://metaloft.com/test.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        # this test2 URL should NOT be blocked
        result = clientControl.runCommand("wget http://metaloft.com/test2.html 2>&1 | grep -q blockpage")
        assert (result != 0)


    def test_999_finalTearDown(self):
        global nodeDesc
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        








