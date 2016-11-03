import unittest2
import time
import sys
import traceback
import ipaddr
import socket

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions

defaultRackId = 1
node = None
testsite = "test.untangle.com"
testsiteIP = socket.gethostbyname("test.untangle.com")
dnsServer = "74.123.28.4"

def createSingleConditionRule( conditionType, value, blocked=True, flagged=True, invert=False ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + conditionTypeStr + " = " + valueStr, 
        "flag": flagged, 
        "block": blocked, 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": invert, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleCondition", 
                    "conditionType": conditionTypeStr, 
                    "value": valueStr
                    }
                ]
            }
        };

def createDualConditionRule( conditionType, value, conditionType2, value2, blocked=True, flagged=True, invert=False ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    conditionTypeStr2 = str(conditionType2)
    valueStr2 = str(value2)
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Dual Matcher: " + conditionTypeStr + " = " + valueStr + " && " + conditionTypeStr2 + " = " + valueStr2, 
        "flag": flagged, 
        "block": blocked, 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": invert, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleCondition", 
                    "conditionType": conditionTypeStr, 
                    "value": valueStr
                    },
                {
                    "invert": invert, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleCondition", 
                    "conditionType": conditionTypeStr2, 
                    "value": valueStr2
                    }

                ]
            }
        };

def nukeRules():
    rules = node.getRules()
    rules["list"] = []
    node.setRules(rules)

def appendRule(newRule):
    rules = node.getRules()
    rules["list"].append(newRule)
    node.setRules(rules)

class FirewallTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-firewall"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        
    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    # verify client is online
    def test_011_defaultIsPass(self):
        result = remote_control.runCommand("wget -q -O /dev/null http://test.untangle.com/")
        assert (result == 0)

    # verify a block port 80 rule works
    def test_020_portDst(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","80"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port 79-81 rule works
    def test_021_portRange(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","79-81"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port 79,80,81 rule works
    def test_022_portComma(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","79,80,81"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port 79,81 rule doesnt match 80
    def test_023_portComma2(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","79,81"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify a block port 79,80,81 rule works
    def test_024_portMixed(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","1- 5,80, 90-100"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port any rule works
    def test_025_portAny(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","any"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port >79 rule blocks 80
    def test_026_portGreaterThan(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT",">79"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port <81 rule blocks 80
    def test_027_portLessThan(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","<81"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port <1 rule doesnt block 80
    def test_028_portLessThan2(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","<1"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify a block udp rule
    def test_028_portUdp(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","53"))
        result = remote_control.runCommand("host test.untangle.com " + dnsServer)
        assert (result != 0)

    # verify src addr rule with any works
    def test_030_addressAny(self):
        nukeRules()
        appendRule(createSingleConditionRule("SRC_ADDR","any"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src addr rule with IP works
    def test_031_addressIp(self):
        nukeRules()
        appendRule(createSingleConditionRule("SRC_ADDR",remote_control.clientIP))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src addr rule with CIDR works
    def test_032_addressCidr(self):
        nukeRules()
        appendRule(createSingleConditionRule("SRC_ADDR",remote_control.clientIP+"/24"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src addr rule with commas works
    def test_033_addressComma(self):
        nukeRules()
        appendRule(createSingleConditionRule("SRC_ADDR","4.3.2.1, "+ remote_control.clientIP + ",  1.2.3.4/31"))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with any works
    def test_040_addressAnyDstCapital(self):
        nukeRules()
        appendRule( createSingleConditionRule("DST_ADDR","Any", blocked=True) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with IP works
    def test_041_addressIpDst(self):
        nukeRules()
        appendRule( createSingleConditionRule("DST_ADDR",testsiteIP, blocked=True) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with CIDR works
    def test_042_addressCidrDst(self):
        nukeRules()
        appendRule( createSingleConditionRule("DST_ADDR",testsiteIP+"/31", blocked=True) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_043_addressDstComma(self):
        nukeRules()
        appendRule( createSingleConditionRule("DST_ADDR","1.2.3.4/31," + testsiteIP+",5.6.7.8", blocked=True) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_044_addressDstRange(self):
        ipObj = ipaddr.IPAddress(testsiteIP)
        startRangeIP = ipObj - 1
        stopRangeIP = ipObj + 1
        testsiteIPRange = str(startRangeIP) + "-" + str(stopRangeIP)

        nukeRules()
        appendRule( createSingleConditionRule("DST_ADDR",testsiteIPRange, blocked=True) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_045_addressDstRange2(self):
        testsiteIP = socket.gethostbyname("test.untangle.com")
        ipObj = ipaddr.IPAddress(testsiteIP)
        startRangeIP2 = ipObj - 255
        stopRangeIP2 = ipObj + 255
        testsiteIPRange2 = str(startRangeIP2) + "-" + str(stopRangeIP2)

        nukeRules()
        appendRule( createSingleConditionRule("DST_ADDR",testsiteIPRange2, blocked=True) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify protocol rule works
    def test_046_protocolTCP(self):
        nukeRules()
        appendRule( createSingleConditionRule("PROTOCOL","TCP", blocked=True) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify protocol UDP not TCP block rule works
    def test_047_protocolUDPnotTCP(self):
        nukeRules()
        appendRule( createDualConditionRule("PROTOCOL","UDP", "DST_PORT", 53) )
        result = remote_control.runCommand("host test.untangle.com " + dnsServer)
        assert (result != 0)
        # Use TCP version of DNS lookup.
        result = remote_control.runCommand("host -T test.untangle.com " + dnsServer)
        assert (result == 0)

    # verify protocol TCP not UDP block rule works
    def test_048_protocolTCPnotUDP(self):
        nukeRules()
        appendRule( createDualConditionRule("PROTOCOL","TCP", "DST_PORT", 53) )
        result = remote_control.runCommand("host test.untangle.com " + dnsServer)
        assert (result == 0)
        # Use TCP version of DNS lookup.
        result = remote_control.runCommand("host -T test.untangle.com " + dnsServer)
        assert (result != 0)

    # verify src intf any rule works
    def test_050_intfDstAny(self):
        nukeRules()
        appendRule( createSingleConditionRule( "DST_INTF", "any" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst intf number rule works
    def test_051_intfDst(self):
        nukeRules()
        # check if a multi-wan box.
        indexOfWans = global_functions.foundWans()
        if (len(indexOfWans) < 2):
            appendRule( createSingleConditionRule( "DST_INTF", remote_control.interfaceExternal ) )
        else:
            for wanIndexTup in indexOfWans:
                wanIndex = wanIndexTup[0]
                appendRule( createSingleConditionRule( "DST_INTF", wanIndex ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst intf number rule doesnt match everythin
    def test_052_intfWrongIntf(self):
        nukeRules()
        appendRule( createSingleConditionRule( "DST_INTF", int(remote_control.interfaceExternal) + 1 ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify dst intf with commas blocks
    def test_053_intfCommas(self):
        nukeRules()
        # check if a multi-wan box.
        indexOfWans = global_functions.foundWans()
        if (len(indexOfWans) < 2):
            appendRule( createSingleConditionRule( "DST_INTF", "99," + str(remote_control.interfaceExternal) +  ", 100" ) )
        else:
            interfaces_str = "99"
            for wanIndexTup in indexOfWans:
                interfaces_str += "," + str(wanIndexTup[0])
            interfaces_str += ",100"
            appendRule( createSingleConditionRule( "DST_INTF", interfaces_str ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst intf wan is blockde
    def test_054_intfWan(self):
        nukeRules()
        appendRule( createSingleConditionRule( "DST_INTF", "wan" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst intf non_wan not blocked
    def test_055_intfNonWan(self):
        nukeRules()
        # specify TCP so the DNS UDP session doesn't get blocked (if it happens to be inbound)
        appendRule( createDualConditionRule( "DST_INTF", "non_wan", "PROTOCOL", "tcp") )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify src intf any rule works
    def test_060_intfSrcAny(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SRC_INTF", "any" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src intf number rule works
    def test_061_intfSrc(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SRC_INTF", remote_control.interface ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src intf number rule doesnt match everythin
    def test_062_intfSrcWrongIntf(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SRC_INTF", int(remote_control.interface) + 1 ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify src intf with commas blocks
    def test_063_intfSrcCommas(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SRC_INTF", "99," + str(remote_control.interface) +  ", 100" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src intf non_wan is blocked
    def test_064_intfSrcNonWan(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SRC_INTF", "non_wan" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src intf wan not blocked
    def test_065_intfSrcWan(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SRC_INTF", "wan" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify GeoiP blocking does block stuff that is blocked
    def test_070_geoipTrumpCountry(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SERVER_COUNTRY", "CN,US,AU" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify GeoIP blocking doesn't block stuff that isn't blocked
    def test_071_geoipOtherCountry(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SERVER_COUNTRY", "CN,GB,AU" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify server penalty box wan not blocked
    def test_100_serverPenaltyBox(self):
        nukeRules()
        appendRule( createSingleConditionRule( "SERVER_IN_PENALTY_BOX", None ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        # verify client penalty box not blocked
    def test_101_clientPenaltyBox(self):
        nukeRules()
        appendRule( createSingleConditionRule( "CLIENT_IN_PENALTY_BOX", None ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify client penalty box is blocked when client in penalty box
    def test_102_clientPenaltyBox2(self):
        fname = sys._getframe().f_code.co_name
        nukeRules()
        uvmContext.hostTable().addHostToPenaltyBox( remote_control.clientIP, 60, fname )
        appendRule( createSingleConditionRule( "CLIENT_IN_PENALTY_BOX", None ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        uvmContext.hostTable().releaseHostFromPenaltyBox( remote_control.clientIP )

    # verify client quota attainment condition
    def test_110_clientQuotaAttainment(self):
        nukeRules()
        appendRule( createSingleConditionRule("CLIENT_QUOTA_ATTAINMENT", "<1.3", blocked=True) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify bogus user agent match not blocked
    def test_120_clientUserAgent(self):
        nukeRules()
        appendRule( createSingleConditionRule( "HTTP_USER_AGENT", "*testtesttesttesttesttesttest*" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify bogus user agent match is blocked after setting agent
    def test_121_clientUserAgent2(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['httpUserAgent'] = "Mozilla foo bar"
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "HTTP_USER_AGENT", "*Mozilla*" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        entry['httpUserAgent'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

    # verify bogus hostname match not blocked
    def test_130_clientHostnameBogus(self):
        nukeRules()
        appendRule( createSingleConditionRule( "CLIENT_HOSTNAME", "*testtesttesttesttesttesttest*" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify hostname match blocked after setting hostname
    def test_131_clientHostname(self):
        hostname = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['hostname'] = hostname
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "CLIENT_HOSTNAME", hostname ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        entry['hostname'] = None 
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        assert (result != 0)

    # verify hostname match blocked after setting hostname
    def test_132_clientHostnameMultiple(self):
        hostname = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['hostname'] = hostname
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "CLIENT_HOSTNAME", hostname + ",foobar") )
        result1 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        nukeRules()
        appendRule( createSingleConditionRule( "CLIENT_HOSTNAME", "foobar," + hostname ) )
        result2 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        
        entry['hostname'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        assert (result1 != 0)
        assert (result2 != 0)
        
    # verify bogus username match not blocked
    def test_140_clientUsername(self):
        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", "*testtesttesttesttesttesttest*" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify bogus username match not blocked
    def test_141_clientUsernameUnauthenticated(self):
        # make sure no username is known for this IP
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = None
        entry['usernameCaptive'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", "[unauthenticated]" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify username matcher works
    def test_142_clientUsernameManual(self):
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = username
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", username ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        assert (result != 0)

    # verify username matcher works with comma
    def test_143_clientUsernameMultiple(self):
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = username
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", username + ",foobar" ) )
        result1 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", "foobar," + username ) )
        result2 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        assert (result1 != 0)
        assert (result2 != 0)
        
    # verify username matcher works despite rule & username being different case
    def test_144_clientUsernameWrongCase1(self):
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = username.upper()
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", username.lower() ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

    # verify username matcher works despite rule & username being different case
    def test_145_clientUsernameWrongCase1(self):
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = username.lower()
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", username.upper() ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

    # verify "[authenticated]" matches any username
    def test_146_clientUsernameAuthenticated(self):
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = username
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", '[authenticated]' ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

    # verify "A*" matches "abc"
    def test_147_clientUsernameLetterStar(self):
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = username.lower()
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", username[:1].upper()+'*' ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

    # verify "*" matches any username but not null
    def test_148_clientUsernameStarOnly(self):
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = username
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", '*' ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

    # verify '' username matches null username (but not all usernames)
    def test_149_clientUsernameBlank(self):
        username = remote_control.runCommand("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = username
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )

        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", '' ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameAdConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify username is NOT '*' matches null username
    def test_150_clientUsernameBlank2(self):
        nukeRules()
        appendRule( createSingleConditionRule( "USERNAME", '*', invert=True ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify block by client MAC
    def test_160_clientMacAddress(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        if entry.get('macAddress') == None:
            raise unittest2.SkipTest('MAC not known')
        mac = entry.get('macAddress')

        nukeRules()
        appendRule( createSingleConditionRule( "SRC_MAC", mac ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify block client MAC by *
    def test_161_clientMacAddressStar(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        if entry.get('macAddress') == None:
            raise unittest2.SkipTest('MAC not known')

        nukeRules()
        appendRule( createSingleConditionRule( "SRC_MAC", "*" ) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify block by client MAC in list
    def test_162_clientMacAddressMultiple(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        if entry.get('macAddress') == None:
            raise unittest2.SkipTest('MAC not known')
        mac = entry.get('macAddress')

        nukeRules()
        appendRule( createSingleConditionRule( "SRC_MAC", "11:22:33:44:55:66," + mac ) )
        result1 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result1 != 0)

        nukeRules()
        appendRule( createSingleConditionRule( "SRC_MAC", mac + ",11:22:33:44:55:66" ) )
        result2 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result2 != 0)
        
    # verify rules that a rule with two matching matchers works
    def test_700_ruleConditionDual(self):
        nukeRules()
        appendRule( createDualConditionRule("SRC_ADDR", remote_control.clientIP, "DST_PORT", 80) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify rules that both MUST match for the session to be blocked
    def test_701_ruleConditionDualAnd(self):
        nukeRules()
        appendRule( createDualConditionRule("SRC_ADDR", remote_control.clientIP, "DST_PORT", 79) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify rules evaluated in order
    def test_702_ruleOrder(self):
        nukeRules()
        appendRule( createSingleConditionRule("SRC_ADDR", remote_control.clientIP, blocked=False) )
        appendRule( createSingleConditionRule("DST_PORT", "80") )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify rules evaluated in order
    def test_703_ruleOrderReverse(self):
        nukeRules()
        appendRule( createSingleConditionRule("DST_PORT", "80") )
        appendRule( createSingleConditionRule("SRC_ADDR", remote_control.clientIP, blocked=False) )
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a session event
    def test_900_logEventLog(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","80",blocked=False,flagged=False))
        result = remote_control.isOnline()
        assert (result == 0)

        events = global_functions.get_events('Firewall','All Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.clientIP,
                                            's_server_port', 80,
                                            'firewall_blocked', False,
                                            'firewall_flagged', False)

    # verify a blocked session event
    def test_901_blockEventLog(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","80"))
        pre_events_block = global_functions.getStatusValue(node,"block")

        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        events = global_functions.get_events('Firewall','Blocked Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.clientIP,
                                            's_server_port', 80,
                                            'firewall_blocked', True,
                                            'firewall_flagged', True)
        assert( found )

        # Check to see if the faceplate counters have incremented. 
        post_events_block = global_functions.getStatusValue(node,"block")
        assert(pre_events_block < post_events_block)

    # verify a flagged sesion event
    def test_902_flagEventLog(self):
        nukeRules()
        appendRule(createSingleConditionRule("DST_PORT","80",blocked=False,flagged=True))
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        events = global_functions.get_events('Firewall','Flagged Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.clientIP,
                                            's_server_port', 80,
                                            'firewall_blocked', False,
                                            'firewall_flagged', True)
        assert( found )
        
    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None
        
test_registry.registerNode("firewall", FirewallTests)

