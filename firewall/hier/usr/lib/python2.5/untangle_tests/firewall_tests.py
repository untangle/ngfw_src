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
metaloftIP = "74.123.29.145"
metaloftIPRange = "74.123.29.140-74.123.29.150"
metaloftIPRange2 = "74.123.27.140-74.123.30.150"

def createSingleMatcherRule( matcherType, value, blocked=True ):
    matcherTypeStr = str(matcherType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + matcherTypeStr + " = " + valueStr, 
        "log": True, 
        "block": blocked, 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                    "matcherType": matcherTypeStr, 
                    "value": valueStr
                    }
                ]
            }
        };

def createDualMatcherRule( matcherType, value, matcherType2, value2, blocked=True ):
    matcherTypeStr = str(matcherType)
    valueStr = str(value)
    matcherTypeStr2 = str(matcherType2)
    valueStr2 = str(value2)
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Dual Matcher: " + matcherTypeStr + " = " + valueStr + " && " + matcherTypeStr2 + " = " + valueStr2, 
        "log": True, 
        "block": blocked, 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                    "matcherType": matcherTypeStr, 
                    "value": valueStr
                    },
                {
                    "invert": False, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                    "matcherType": matcherTypeStr2, 
                    "value": valueStr2
                    }

                ]
            }
        };

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

def nukeRules():
    rules = node.getRules()
    rules["list"] = [];
    node.setRules(rules);

def appendRule(newRule):
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
                print "ERROR: Node %s already installed" % self.nodeName();
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
    def test_020_blockDstPort80(self):
        nukeRules();
        appendRule(createSingleMatcherRule("DST_PORT","80"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port 79-81 rule works
    def test_021_blockDstPort79to81(self):
        nukeRules();
        appendRule(createSingleMatcherRule("DST_PORT","79-81"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port 79,80,81 rule works
    def test_022_blockDstPort79comma80comma81(self):
        nukeRules();
        appendRule(createSingleMatcherRule("DST_PORT","79,80,81"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port 79,80,81 rule works
    def test_023_blockDstPort79comma81(self):
        nukeRules();
        appendRule(createSingleMatcherRule("DST_PORT","79,81"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify a block port 79,80,81 rule works
    def test_024_blockDstPortList(self):
        nukeRules();
        appendRule(createSingleMatcherRule("DST_PORT","1- 5,80, 90-100"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify a block port any rule works
    def test_025_blockDstPortAny(self):
        nukeRules();
        appendRule(createSingleMatcherRule("DST_PORT","any"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src addr rule with any works
    def test_030_blockSrcAddrAny(self):
        nukeRules();
        appendRule(createSingleMatcherRule("SRC_ADDR","any"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src addr rule with IP works
    def test_031_blockSrcAddrIP(self):
        nukeRules();
        appendRule(createSingleMatcherRule("SRC_ADDR",ClientControl.hostIP));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src addr rule with CIDR works
    def test_032_blockSrcAddrCIDR(self):
        nukeRules();
        appendRule(createSingleMatcherRule("SRC_ADDR",ClientControl.hostIP+"/24"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src addr rule with commas works
    def test_033_blockSrcAddrComma(self):
        nukeRules();
        appendRule(createSingleMatcherRule("SRC_ADDR","4.3.2.1, "+ ClientControl.hostIP + ",  1.2.3.4/31"));
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst addr rule with any works
    def test_040_blockDstAddrAny(self):
        nukeRules();
        appendRule( createSingleMatcherRule("DST_ADDR","Any", blocked=True) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst addr rule with IP works
    def test_041_blockDstAddr(self):
        nukeRules();
        appendRule( createSingleMatcherRule("DST_ADDR",metaloftIP, blocked=True) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst addr rule with CIDR works
    def test_042_blockDstAddrCIDR(self):
        nukeRules();
        appendRule( createSingleMatcherRule("DST_ADDR",metaloftIP+"/31", blocked=True) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_043_blockDstAddrComma(self):
        nukeRules();
        appendRule( createSingleMatcherRule("DST_ADDR","1.2.3.4/31," + metaloftIP+",5.6.7.8", blocked=True) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_044_blockDstAddrRange(self):
        nukeRules();
        appendRule( createSingleMatcherRule("DST_ADDR",metaloftIPRange, blocked=True) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_045_blockDstAddrRange2(self):
        nukeRules();
        appendRule( createSingleMatcherRule("DST_ADDR",metaloftIPRange2, blocked=True) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify protocol rule works
    def test_046_blockProtocolTCP(self):
        nukeRules();
        appendRule( createSingleMatcherRule("PROTOCOL","TCP", blocked=True) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src intf any rule works
    def test_050_blockDstIntfAny(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "DST_INTF", "any" ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst intf number rule works
    def test_051_blockDstIntf(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "DST_INTF", ClientControl.interfaceExternal ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst intf number rule doesnt match everythin
    def test_052_blockDstIntfWrongIntf(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "DST_INTF", ClientControl.interfaceExternal + 1 ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify dst intf with commas blocks
    def test_053_blockDstIntfCommas(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "DST_INTF", "99," + str(ClientControl.interfaceExternal) +  ", 100" ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst intf wan is blockde
    def test_054_blockDstIntfWan(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "DST_INTF", "wan" ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify dst intf non_wan not blocked
    def test_055_blockDstIntfNonWan(self):
        nukeRules();
        # specify TCP so the DNS UDP session doesn't get blocked (if it happens to be inbound)
        appendRule( createDualMatcherRule( "DST_INTF", "non_wan", "PROTOCOL", "tcp") );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify src intf any rule works
    def test_060_blockSrcIntfAny(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "SRC_INTF", "any" ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src intf number rule works
    def test_061_blockSrcIntf(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "SRC_INTF", ClientControl.interface ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src intf number rule doesnt match everythin
    def test_062_blockSrcIntfWrongIntf(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "SRC_INTF", ClientControl.interface + 1 ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify src intf with commas blocks
    def test_063_blockSrcIntfCommas(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "SRC_INTF", "99," + str(ClientControl.interface) +  ", 100" ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src intf non_wan is blocked
    def test_064_blockSrcIntfWan(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "SRC_INTF", "non_wan" ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify src intf wan not blocked
    def test_065_blockSrcIntfNonWan(self):
        nukeRules();
        appendRule( createSingleMatcherRule( "SRC_INTF", "wan" ) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify rules that a rule with two matching matchers works
    def test_080_dualMatcherRule(self):
        nukeRules();
        appendRule( createDualMatcherRule("SRC_ADDR", ClientControl.hostIP, "DST_PORT", 80) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    # verify rules that both MUST match for the session to be blocked
    def test_080_dualMatcherRuleAnd(self):
        nukeRules();
        appendRule( createDualMatcherRule("SRC_ADDR", ClientControl.hostIP, "DST_PORT", 79) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify rules evaluated in order
    def test_090_ruleOrder(self):
        nukeRules();
        appendRule( createSingleMatcherRule("SRC_ADDR", ClientControl.hostIP, blocked=False) );
        appendRule( createSingleMatcherRule("DST_PORT", "80") );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result == 0)

    # verify rules evaluated in order
    def test_091_ruleOrderReverse(self):
        nukeRules();
        appendRule( createSingleMatcherRule("DST_PORT", "80") );
        appendRule( createSingleMatcherRule("SRC_ADDR", ClientControl.hostIP, blocked=False) );
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://metaloft.com/")
        assert (result != 0)

    def test_999_finalTearDown(self):
        global nodeDesc
        global node
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        node = None
        nodeDesc = None
        








