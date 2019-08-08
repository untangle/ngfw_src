"""firewall tests"""
import unittest
import pytest
import time
import sys
import traceback
import socket

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

default_policy_id = 1
app = None
testsite = "test.untangle.com"
testsiteIP = socket.gethostbyname("test.untangle.com")
dnsServer = "8.8.8.8" # Google DNS

def create_rule_single_condition( conditionType, value, blocked=True, flagged=True, invert=False ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.app.firewall.FirewallRule",
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
                    "javaClass": "com.untangle.app.firewall.FirewallRuleCondition",
                    "conditionType": conditionTypeStr,
                    "value": valueStr
                    }
                ]
            }
        };

def create_rule_dual_condition( conditionType, value, conditionType2, value2, blocked=True, flagged=True, invert=False ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    conditionTypeStr2 = str(conditionType2)
    valueStr2 = str(value2)
    return {
        "javaClass": "com.untangle.app.firewall.FirewallRule",
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
                    "javaClass": "com.untangle.app.firewall.FirewallRuleCondition",
                    "conditionType": conditionTypeStr,
                    "value": valueStr
                    },
                {
                    "invert": invert,
                    "javaClass": "com.untangle.app.firewall.FirewallRuleCondition",
                    "conditionType": conditionTypeStr2,
                    "value": valueStr2
                    }

                ]
            }
        };

def rules_clear():
    rules = app.getRules()
    rules["list"] = []
    app.setRules(rules)

def rule_append(newRule):
    rules = app.getRules()
    rules["list"].append(newRule)
    app.setRules(rules)

@pytest.mark.firewall
class FirewallTests(NGFWTestCase):

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = FirewallTests._app
        return "firewall"

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    # verify client is online
    def test_012_defaultIsPass(self):
        result = remote_control.run_command("wget -q -O /dev/null http://test.untangle.com/")
        assert (result == 0)

    # verify a block port 80 rule works
    def test_020_portDst(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","80"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port 79-81 rule works
    def test_021_portRange(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","79-81"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port 79,80,81 rule works
    def test_022_portComma(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","79,80,81"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port 79,81 rule doesnt match 80
    def test_023_portComma2(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","79,81"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify a block port 79,80,81 rule works
    def test_024_portMixed(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","1- 5,80, 90-100"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port any rule works
    def test_025_portAny(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","any"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port >79 rule blocks 80
    def test_026_portGreaterThan(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT",">79"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port <81 rule blocks 80
    def test_027_portLessThan(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","<81"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a block port <1 rule doesnt block 80
    def test_028_portLessThan2(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","<1"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify a block udp rule
    def test_028_portUdp(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","53"))
        result = remote_control.run_command("host test.untangle.com " + dnsServer)
        assert (result != 0)

    # verify src addr rule with any works
    def test_030_addressAny(self):
        rules_clear()
        rule_append(create_rule_single_condition("SRC_ADDR","any"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src addr rule with IP works
    def test_031_addressIp(self):
        rules_clear()
        rule_append(create_rule_single_condition("SRC_ADDR",remote_control.client_ip))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src addr rule with CIDR works
    def test_032_addressCidr(self):
        rules_clear()
        rule_append(create_rule_single_condition("SRC_ADDR",remote_control.client_ip+"/24"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src addr rule with commas works
    def test_033_addressComma(self):
        rules_clear()
        rule_append(create_rule_single_condition("SRC_ADDR","4.3.2.1, "+ remote_control.client_ip + ",  1.2.3.4/31"))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with any works
    def test_040_addressAnyDstCapital(self):
        rules_clear()
        rule_append( create_rule_single_condition("DST_ADDR","Any", blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with IP works
    def test_041_addressIpDst(self):
        rules_clear()
        rule_append( create_rule_single_condition("DST_ADDR",testsiteIP, blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with CIDR works
    def test_042_addressCidrDst(self):
        rules_clear()
        rule_append( create_rule_single_condition("DST_ADDR",testsiteIP+"/31", blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_043_addressDstComma(self):
        rules_clear()
        rule_append( create_rule_single_condition("DST_ADDR","1.2.3.4/31," + testsiteIP+",5.6.7.8", blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_044_addressDstRange(self):
        ipObj = ipaddr.IPAddress(testsiteIP)
        startRangeIP = ipObj - 1
        stopRangeIP = ipObj + 1
        testsiteIPRange = str(startRangeIP) + "-" + str(stopRangeIP)

        rules_clear()
        rule_append( create_rule_single_condition("DST_ADDR",testsiteIPRange, blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst addr rule with commas works
    def test_045_addressDstRange2(self):
        testsiteIP = socket.gethostbyname("test.untangle.com")
        ipObj = ipaddr.IPAddress(testsiteIP)
        startRangeIP2 = ipObj - 255
        stopRangeIP2 = ipObj + 255
        testsiteIPRange2 = str(startRangeIP2) + "-" + str(stopRangeIP2)

        rules_clear()
        rule_append( create_rule_single_condition("DST_ADDR",testsiteIPRange2, blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify protocol rule works
    def test_046_protocolTCP(self):
        rules_clear()
        rule_append( create_rule_single_condition("PROTOCOL","TCP", blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify protocol UDP not TCP block rule works
    def test_047_protocolUDPnotTCP(self):
        rules_clear()
        rule_append( create_rule_dual_condition("PROTOCOL","UDP", "DST_PORT", 53) )
        result = remote_control.run_command("host test.untangle.com " + dnsServer)
        assert (result != 0)
        # Use TCP version of DNS lookup.
        result = remote_control.run_command("host -T test.untangle.com " + dnsServer)
        assert (result == 0)

    # verify protocol TCP not UDP block rule works
    def test_048_protocolTCPnotUDP(self):
        rules_clear()
        rule_append( create_rule_dual_condition("PROTOCOL","TCP", "DST_PORT", 53) )
        result = remote_control.run_command("host test.untangle.com " + dnsServer)
        assert (result == 0)
        # Use TCP version of DNS lookup.
        result = remote_control.run_command("host -T test.untangle.com " + dnsServer)
        assert (result != 0)

    # verify src intf any rule works
    def test_050_intfDstAny(self):
        rules_clear()
        rule_append( create_rule_single_condition( "DST_INTF", "any" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst intf number rule works
    def test_051_intfDst(self):
        rules_clear()
        # check if a multi-wan box.
        indexOfWans = global_functions.get_wan_tuples()
        if (len(indexOfWans) < 2):
            rule_append( create_rule_single_condition( "DST_INTF", remote_control.interface_external ) )
        else:
            for wanIndexTup in indexOfWans:
                wanIndex = wanIndexTup[0]
                rule_append( create_rule_single_condition( "DST_INTF", wanIndex ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst intf number rule doesnt match everythin
    def test_052_intfWrongIntf(self):
        rules_clear()
        rule_append( create_rule_single_condition( "DST_INTF", int(remote_control.interface_external) + 1 ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify dst intf with commas blocks
    def test_053_intfCommas(self):
        rules_clear()
        # check if a multi-wan box.
        indexOfWans = global_functions.get_wan_tuples()
        if (len(indexOfWans) < 2):
            rule_append( create_rule_single_condition( "DST_INTF", "99," + str(remote_control.interface_external) +  ", 100" ) )
        else:
            interfaces_str = "99"
            for wanIndexTup in indexOfWans:
                interfaces_str += "," + str(wanIndexTup[0])
            interfaces_str += ",100"
            rule_append( create_rule_single_condition( "DST_INTF", interfaces_str ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst intf wan is blockde
    def test_054_intfWan(self):
        rules_clear()
        rule_append( create_rule_single_condition( "DST_INTF", "wan" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify dst intf non_wan not blocked
    def test_055_intfNonWan(self):
        rules_clear()
        # specify TCP so the DNS UDP session doesn't get blocked (if it happens to be inbound)
        rule_append( create_rule_dual_condition( "DST_INTF", "non_wan", "PROTOCOL", "tcp") )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify src intf any rule works
    def test_060_intfSrcAny(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SRC_INTF", "any" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src intf number rule works
    def test_061_intfSrc(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SRC_INTF", remote_control.interface ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src intf number rule doesnt match everythin
    def test_062_intfSrcWrongIntf(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SRC_INTF", int(remote_control.interface) + 1 ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify src intf with commas blocks
    def test_063_intfSrcCommas(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SRC_INTF", "99," + str(remote_control.interface) +  ", 100" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src intf non_wan is blocked
    def test_064_intfSrcNonWan(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SRC_INTF", "non_wan" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify src intf wan not blocked
    def test_065_intfSrcWan(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SRC_INTF", "wan" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify GeoiP blocking does block stuff that is blocked
    def test_070_geoipClientCountryBlock(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SERVER_COUNTRY", "CN,US,AU" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify GeoIP blocking doesn't block stuff that isn't blocked
    def test_071_geoipClientCountryMiss(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SERVER_COUNTRY", "CN,GB,AU" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify GeoiP blocking does block stuff that is blocked
    def test_072_geoipHostCountryBlock(self):
        rules_clear()
        rule_append( create_rule_single_condition( "REMOTE_HOST_COUNTRY", "CN,US,AU" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify GeoIP blocking doesn't block stuff that isn't blocked
    def test_073_geoipHostCountryMiss(self):
        rules_clear()
        rule_append( create_rule_single_condition( "REMOTE_HOST_COUNTRY", "CN,GB,AU" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    def test_080_tagHostCheckMissing(self):
        rules_clear()
        rule_append( create_rule_single_condition( "TAGGED", "NONEXISTANT-TAG" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    def test_081_tagHostBlock(self):
        rules_clear()
        global_functions.host_tags_add("testtag")
        rule_append( create_rule_single_condition( "TAGGED", "testtag" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        global_functions.host_tags_clear()

    def test_082_tagHostBlockGlob(self):
        rules_clear()
        global_functions.host_tags_add("testtag")
        rule_append( create_rule_single_condition( "TAGGED", "*test*" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        global_functions.host_tags_clear()
        assert (result != 0)

    def test_082_tagHostBlockMultiple(self):
        rules_clear()
        global_functions.host_tags_add("foobar1")
        global_functions.host_tags_add("foobar2")
        global_functions.host_tags_add("testtag")
        global_functions.host_tags_add("foobar3")
        global_functions.host_tags_add("foobar4")
        rule_append( create_rule_single_condition( "TAGGED", "*test*" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        global_functions.host_tags_clear()
        assert (result != 0)
        
    def test_083_tagUserBlock(self):
        rules_clear()
        username = remote_control.get_hostname()
        global_functions.host_username_set( username )
        global_functions.user_tags_add(username,"testtag")
        rule_append( create_rule_single_condition( "TAGGED", "testtag" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        global_functions.user_tags_clear(username)
        global_functions.host_username_clear()
        assert (result != 0)
        
    # verify server penalty box wan not blocked
    def test_100_serverPenaltyBox(self):
        rules_clear()
        rule_append( create_rule_single_condition( "SERVER_IN_PENALTY_BOX", None ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        # verify client penalty box not blocked
    def test_101_clientPenaltyBox(self):
        rules_clear()
        rule_append( create_rule_single_condition( "CLIENT_IN_PENALTY_BOX", None ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify client penalty box is blocked when client in penalty box
    def test_102_clientPenaltyBox2(self):
        rules_clear()
        global_functions.host_tags_add("penalty-box")
        rule_append( create_rule_single_condition( "CLIENT_IN_PENALTY_BOX", None ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        global_functions.host_tags_clear()

    # verify client penalty box not blocked
    def test_103_hostPenaltyBox(self):
        rules_clear()
        rule_append( create_rule_single_condition( "HOST_IN_PENALTY_BOX", None ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify client penalty box is blocked when client in penalty box
    def test_104_hostPenaltyBox2(self):
        rules_clear()
        global_functions.host_tags_add("penalty-box")
        rule_append( create_rule_single_condition( "HOST_IN_PENALTY_BOX", None ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        global_functions.host_tags_clear()
        
    # verify client quota attainment condition
    def test_110_clientQuotaAttainment(self):
        rules_clear()
        rule_append( create_rule_single_condition("CLIENT_QUOTA_ATTAINMENT", "<1.3", blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify client quota attainment condition
    def test_111_hostQuotaAttainment(self):
        rules_clear()
        rule_append( create_rule_single_condition("HOST_QUOTA_ATTAINMENT", "<1.3", blocked=True) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        
    # verify bogus user agent match not blocked
    def test_120_clientUserAgent(self):
        rules_clear()
        rule_append( create_rule_single_condition( "HTTP_USER_AGENT", "*testtesttesttesttesttesttest*" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify bogus user agent match is blocked after setting agent
    def test_121_clientUserAgent2(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        entry['httpUserAgent'] = "Mozilla foo bar"
        uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )

        rules_clear()
        rule_append( create_rule_single_condition( "HTTP_USER_AGENT", "*Mozilla*" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        entry['httpUserAgent'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )

    # verify bogus hostname match not blocked
    def test_130_clientHostnameBogus(self):
        rules_clear()
        rule_append( create_rule_single_condition( "CLIENT_HOSTNAME", "*testtesttesttesttesttesttest*" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify hostname match blocked after setting hostname
    def test_131_clientHostname(self):
        global_functions.host_hostname_set( remote_control.get_hostname() )
        rules_clear()
        rule_append( create_rule_single_condition( "CLIENT_HOSTNAME", remote_control.get_hostname() ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        global_functions.host_hostname_clear()
        assert (result != 0)

    # verify hostname match blocked after setting hostname
    def test_132_hostHostname(self):
        global_functions.host_hostname_set( remote_control.get_hostname() )
        rules_clear()
        rule_append( create_rule_single_condition( "HOST_HOSTNAME", remote_control.get_hostname() ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        global_functions.host_hostname_clear()
        assert (result != 0)

    # verify hostname match blocked after setting hostname
    def test_133_hostHostnameMultiple(self):
        global_functions.host_hostname_set( remote_control.get_hostname() )

        rules_clear()
        rule_append( create_rule_single_condition( "HOST_HOSTNAME", remote_control.get_hostname() + ",foobar") )
        result1 = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        rules_clear()
        rule_append( create_rule_single_condition( "HOST_HOSTNAME", "foobar," + remote_control.get_hostname() ) )
        result2 = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        global_functions.host_hostname_clear()
        assert (result1 != 0)
        assert (result2 != 0)
        
    # verify bogus username match not blocked
    def test_140_clientUsername(self):
        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", "*testtesttesttesttesttesttest*" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify bogus username match not blocked
    def test_141_clientUsernameUnauthenticated(self):
        # make sure no username is known for this IP
        global_functions.host_username_clear()
        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", "[unauthenticated]" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify username matcher works
    def test_142_clientUsernameManual(self):
        username = remote_control.get_hostname()
        global_functions.host_username_set( remote_control.get_hostname() )

        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", username ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        global_functions.host_username_clear()
        assert (result != 0)

    # verify username matcher works with comma
    def test_143_clientUsernameMultiple(self):
        username = remote_control.get_hostname()
        global_functions.host_username_set( username )

        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", username + ",foobar" ) )
        result1 = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", "foobar," + username ) )
        result2 = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        global_functions.host_username_clear()
        assert (result1 != 0)
        assert (result2 != 0)

    # verify username matcher works despite rule & username being different case
    def test_144_clientUsernameWrongCase1(self):
        username = remote_control.get_hostname()
        global_functions.host_username_set( username )

        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", username.lower() ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        global_functions.host_username_clear()

    # verify username matcher works despite rule & username being different case
    def test_145_clientUsernameWrongCase1(self):
        username = remote_control.get_hostname()
        global_functions.host_username_set( username )

        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", username.upper() ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        global_functions.host_username_clear()
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify "[authenticated]" matches any username
    def test_146_clientUsernameAuthenticated(self):
        username = remote_control.get_hostname()
        global_functions.host_username_set( username )

        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", '[authenticated]' ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        global_functions.host_username_clear()
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)
        
    # verify "A*" matches "abc"
    def test_147_clientUsernameLetterStar(self):
        username = remote_control.get_hostname()
        global_functions.host_username_set( username )

        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", username[:1].upper()+'*' ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        global_functions.host_username_clear()
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify "*" matches any username but not null
    def test_148_clientUsernameStarOnly(self):
        username = remote_control.get_hostname()
        global_functions.host_username_set( username )

        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", '*' ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        global_functions.host_username_clear()
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify '' username matches null username (but not all usernames)
    def test_149_clientUsernameBlank(self):
        username = remote_control.get_hostname()
        global_functions.host_username_set( username )

        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", '' ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        global_functions.host_username_clear()

    # verify username is NOT '*' matches null username
    def test_150_clientUsernameBlank2(self):
        rules_clear()
        rule_append( create_rule_single_condition( "USERNAME", '*', invert=True ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify block by client MAC
    def test_160_clientMacAddress(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        if entry.get('macAddress') == None:
            raise unittest.SkipTest('MAC not known')
        mac = entry.get('macAddress')

        rules_clear()
        rule_append( create_rule_single_condition( "SRC_MAC", mac ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify block client MAC by *
    def test_161_clientMacAddressStar(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        if entry.get('macAddress') == None:
            raise unittest.SkipTest('MAC not known')

        rules_clear()
        rule_append( create_rule_single_condition( "SRC_MAC", "*" ) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify block by client MAC in list
    def test_162_clientMacAddressMultiple(self):
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        if entry.get('macAddress') == None:
            raise unittest.SkipTest('MAC not known')
        mac = entry.get('macAddress')

        rules_clear()
        rule_append( create_rule_single_condition( "SRC_MAC", "11:22:33:44:55:66," + mac ) )
        result1 = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result1 != 0)

        rules_clear()
        rule_append( create_rule_single_condition( "SRC_MAC", mac + ",11:22:33:44:55:66" ) )
        result2 = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result2 != 0)

    # verify rules that a rule with two matching matchers works
    def test_700_ruleConditionDual(self):
        rules_clear()
        rule_append( create_rule_dual_condition("SRC_ADDR", remote_control.client_ip, "DST_PORT", 80) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify rules that both MUST match for the session to be blocked
    def test_701_ruleConditionDualAnd(self):
        rules_clear()
        rule_append( create_rule_dual_condition("SRC_ADDR", remote_control.client_ip, "DST_PORT", 79) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify rules evaluated in order
    def test_702_ruleOrder(self):
        rules_clear()
        rule_append( create_rule_single_condition("SRC_ADDR", remote_control.client_ip, blocked=False) )
        rule_append( create_rule_single_condition("DST_PORT", "80") )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

    # verify rules evaluated in order
    def test_703_ruleOrderReverse(self):
        rules_clear()
        rule_append( create_rule_single_condition("DST_PORT", "80") )
        rule_append( create_rule_single_condition("SRC_ADDR", remote_control.client_ip, blocked=False) )
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

    # verify a session event
    def test_900_logEventLog(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","80",blocked=False,flagged=False))
        result = remote_control.is_online()
        assert (result == 0)

        events = global_functions.get_events('Firewall','All Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            's_server_port', 80,
                                            'firewall_blocked', False,
                                            'firewall_flagged', False)

    # verify a blocked session event
    def test_901_blockEventLog(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","80"))
        pre_events_block = global_functions.get_app_metric_value(app,"block")

        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        events = global_functions.get_events('Firewall','Blocked Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            's_server_port', 80,
                                            'firewall_blocked', True,
                                            'firewall_flagged', True)
        assert( found )

        # Check to see if the faceplate counters have incremented.
        post_events_block = global_functions.get_app_metric_value(app,"block")
        assert(pre_events_block < post_events_block)

    # verify a flagged sesion event
    def test_902_flagEventLog(self):
        rules_clear()
        rule_append(create_rule_single_condition("DST_PORT","80",blocked=False,flagged=True))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        events = global_functions.get_events('Firewall','Flagged Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            's_server_port', 80,
                                            'firewall_blocked', False,
                                            'firewall_flagged', True)
        assert( found )

    # verify a blocked local session event
    def test_903_blockLocalEventLog(self):
        rules_clear()
        rule_append(create_rule_single_condition("CLIENT_COUNTRY","XL"))
        pre_events_block = global_functions.get_app_metric_value(app,"block")

        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)

        events = global_functions.get_events('Firewall','Blocked Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            'client_country', "XL",
                                            'firewall_blocked', True,
                                            'firewall_flagged', True)
        assert( found )

        # Check to see if the faceplate counters have incremented.
        post_events_block = global_functions.get_app_metric_value(app,"block")
        assert(pre_events_block < post_events_block)

    # verify a flagged local sesion event
    def test_904_flagLocalEventLog(self):
        rules_clear()
        rule_append(create_rule_single_condition("CLIENT_COUNTRY","XL",blocked=False,flagged=True))
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)

        events = global_functions.get_events('Firewall','Flagged Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            'client_country', "XL",
                                            'firewall_blocked', False,
                                            'firewall_flagged', True)
        assert( found )

test_registry.register_module("firewall", FirewallTests)

