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

def createDstPortRule(port, blocked=True):
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Block Port 80", 
        "log": True, 
        "block": blocked, 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                    "matcherType": "DST_PORT", 
                    "value": port
                    }
                ]
            }
        };

def createDstAddrRule(addr, blocked=True):
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Block Port 80", 
        "log": True, 
        "block": blocked, 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                    "matcherType": "DST_ADDR", 
                    "value": addr
                    }
                ]
            }
        };

def createSrcAddrRule(addr, blocked=True):
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Block Port 80", 
        "log": True, 
        "block": blocked, 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                    "matcherType": "SRC_ADDR", 
                    "value": addr
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
        nukeRules();
        appendRule(createDstPortRule("80"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port 79-81 rule works
    def test_021_blockPort79to81(self):
        nukeRules();
        appendRule(createDstPortRule("79-81"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port 79,80,81 rule works
    def test_022_blockPort79comma80comma81(self):
        nukeRules();
        appendRule(createDstPortRule("79,80,81"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port 79,80,81 rule works
    def test_023_blockPort79comma81(self):
        nukeRules();
        appendRule(createDstPortRule("79,81"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify a block port 79,80,81 rule works
    def test_024_blockPortList(self):
        nukeRules();
        appendRule(createDstPortRule("1-5,80,90-100"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port any rule works
    def test_025_blockPortAny(self):
        nukeRules();
        appendRule(createDstPortRule("any"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block src_addr any rule works
    def test_030_blockSourceAddrAny(self):
        nukeRules();
        appendRule(createSrcAddrRule("any"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port any rule works
    def test_031_blockSourceAddrIP(self):
        nukeRules();
        appendRule(createSrcAddrRule(ClientControl.hostIP));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port any rule works
    def test_032_blockSourceAddrCIDR(self):
        nukeRules();
        appendRule(createSrcAddrRule(ClientControl.hostIP+"/24"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port any rule works
    def test_033_blockSourceAddrComma(self):
        nukeRules();
        appendRule(createSrcAddrRule(ClientControl.hostIP+",1.2.3.4"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify rules evaluated in order
    def test_040_ruleOrder(self):
        nukeRules();
        appendRule( createSrcAddrRule(ClientControl.hostIP, blocked=False) );
        appendRule( createDstPortRule("80") );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify rules evaluated in order
    def test_041_ruleOrderReveres(self):
        nukeRules();
        appendRule( createDstPortRule("80") );
        appendRule( createSrcAddrRule(ClientControl.hostIP, blocked=False) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    def test_999_finalTearDown(self):
        global nodeDesc
        global node
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        node = None
        nodeDesc = None
        








