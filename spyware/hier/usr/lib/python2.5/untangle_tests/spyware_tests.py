import unittest
import time
import sys
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
nodeProperties = None
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

def nukeCookies():
    rules = node.getCookies()
    rules["list"] = []
    node.setCookies(rules)

def flushEvents():
    global uvmContext
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()
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
        global nodeProperties, node
        if nodeProperties == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                raise Exception('node %s already instantiated' % self.nodeName())
            nodeProperties = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), defaultRackId)
            node = uvmContext.nodeManager().nodeContext(nodeProperties['nodeSettings']).node()

    # verify client is online
    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -o /dev/null http://google.com/")
        assert (result == 0)

    # verify gator site is blocked in default config
    def test_011_defaultGatorIsBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://gator.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)
    
    # verify there is a youtube cookie
    def test_012_youtubeCookie(self):
        # remove any previous instance of testcookie.txt
        clientControl.runCommand("/bin/rm -f testcookie.txt")
        # see if untangle cookie is downloaded.
        result = clientControl.runCommand("wget -q --save-cookies testcookie.txt -O - http://youtube.com/ >/dev/null 2>&1 ; grep -q youtube.com testcookie.txt")
        assert (result == 0)

    # verify a youtube cookie can be blocked
    def test_013_youtubeCookieBlocked(self):
        addCookieBlocked("youtube.com")
        # remove any previous instance of testcookie.txt
        clientControl.runCommand("/bin/rm -f testcookie.txt")
        # see if untangle cookie is downloaded.
        result = clientControl.runCommand("wget -q --save-cookies testcookie.txt -O - http://youtube.com/ >/dev/null 2>&1 ; grep -q youtube.com testcookie.txt")
        assert (result == 1)



    def test_999_finalTearDown(self):
        global nodeProperties
        uvmContext.nodeManager().destroy(nodeProperties['nodeSettings']['id']);
        








