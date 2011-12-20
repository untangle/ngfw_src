import unittest
import time
import sys
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
clientControl = ClientControl()
nodeDesc = None
node = None


def addSubnetsBlocked(url, enabled=True, description="description"):
    global node
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getCookies()
    rules["list"].append(newRule)
    node.setCookies(rules)


def addCookieBlocked(url, enabled=True, description="description"):
    global node
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getCookies()
    rules["list"].append(newRule)
    node.setCookies(rules)


def flushEvents():
    global uvmContext
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents(True)
#
# TESTS TO ADD:
# eventlog (events are logged properly)
# passed client IP (overrides other blocks)
#
class SpywareTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-spyware"

    def setUp(self):
        global nodeDesc, node
        if nodeDesc == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                raise Exception('node %s already instantiated' % self.nodeName())
            nodeDesc = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), uvmContext.policyManager().getDefaultPolicy())
            node = uvmContext.nodeManager().nodeContext(nodeDesc['nodeId']).node()

    # verify client is online
    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -o /dev/null http://google.com/")
        assert (result == 0)

    # verify gator site is blocked in default config
    def test_011_defaultGatorIsBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://gator.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)
    
    # verify gator cookie is blocked in default config
    def test_012_addCookieBlocked(self):
        addCookieBlocked("untangle.com")
        # remove any previous instance of testcookie.txt
        clientControl.runCommand("rm testcookie.txt")
        # see if untangle cookie is downloaded.
        result = clientControl.runCommand("wget -q --save-cookies file testcookie.txt -O - http://untange.com/ 2>&1 ; grep -q untangle testcookie.txt")
        assert (result == 0)



    def test_999_finalTearDown(self):
        global nodeDesc
        # uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        








