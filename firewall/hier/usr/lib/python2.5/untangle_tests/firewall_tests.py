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

ruleBlockPort80 = {
    "javaClass": "com.untangle.node.firewall.FirewallRule", 
    "id": 1, 
    "enabled": True, 
    "description": "Block Port 80", 
    "log": True, 
    "block": True, 
    "matchers": {
        "javaClass": "java.util.LinkedList", 
        "list": [
            {
                "invert": False, 
                "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                "matcherType": "DST_PORT", 
                "value": "80"
                }
            ]
        }
    };

def flushEvents():
    global uvmContext
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents(True)

def nukeRules():
    global node
    rules = node.getRules()
    rules["list"] = [];
    node.setRules(rules);

def appendRule(newRule):
    global node
    rules = node.getRules()
    rules["list"].append(newRule);
    node.setRules(rules);

class FirewallTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-firewall"

    @staticmethod
    def vendorName():
        return "Untangle"

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

    # verify client is online
    def test_011_defaultIsPass(self):
        result = clientControl.runCommand("wget -o /dev/null http://metaloft.com/")
        assert (result == 0)

    # verify a block port 80 rule works
    def test_020_blockPort80(self):
        global ruleBlockPort80;
        nukeRules();
        appendRule(ruleBlockPort80);
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)


    def test_999_finalTearDown(self):
        global nodeDesc
        global node
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        node = None
        nodeDesc = None
        








