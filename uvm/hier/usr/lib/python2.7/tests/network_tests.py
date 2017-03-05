import socket
import unittest2
import os
import sys
reload(sys)
sys.setdefaultencoding("utf-8")
import re
import subprocess
import pprint
import ipaddr
import time

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import test_registry
import remote_control
import system_properties
import global_functions


ftp_file_name = ""
dyn_hostname = ""
dyn_names = ['atstest.dnsalias.com', 'atstest2.dyndns-ip.com', 'atstest3.dnsalias.com', 'atstest4.dnsalias.com'];

defaultRackId = 1
orig_netsettings = None
test_untangle_com_ip = socket.gethostbyname("test.untangle.com")
run_ftp_inbound_tests = None
wan_IP = None
device_in_office = False
dyndns_resolver = "216.146.35.35"
#dyndns_resolver = "resolver1.dyndnsinternetguide.com"

def getUsableName(dyn_checkip):
    selected_name = ""
    for hostname in dyn_names:
        result = subprocess.check_output("host -R3 -4 " + hostname + " " + dyndns_resolver, shell=True)
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        hostname_ip = (match.group()).replace('address ','')
        if dyn_checkip != hostname_ip:
            selected_name = hostname
            break
    return selected_name
    
def createPortForwardTripleCondition( conditionType1, value1, conditionType2, value2, conditionType3, value3, destinationIP, destinationPort):
    return {
        "description": "port forward  -> " + str(destinationIP) + ":" + str(destinationPort) + " test",
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.PortForwardRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleCondition",
                    "conditionType": str(conditionType1),
                    "value": str(value1)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleCondition",
                    "conditionType": str(conditionType2),
                    "value": str(value2)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleCondition",
                    "conditionType": str(conditionType3),
                    "value": str(value3)
                }
            ]
        },
        "newDestination": destinationIP,
        "newPort": destinationPort,
        "ruleId": 1
    }

def createFilterRule( conditionType1, value1, conditionType2, value2, blocked ):
    return {
        "bypass": True,
        "description": "test rule " + str(conditionType1) + " " + str(value1) + " " + str(conditionType2) + " " + str(value2),
        "enabled": True,
        "blocked": blocked,
        "javaClass": "com.untangle.uvm.network.FilterRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                    "conditionType": str(conditionType1),
                    "value": str(value1)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                    "conditionType": str(conditionType2),
                    "value": str(value2)
                }
            ]
        },
        "ruleId": 1
    }

def createBypassConditionRule( conditionType, value ):
    return {
        "bypass": True,
        "description": "test bypass " + str(conditionType) + " " + str(value),
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.BypassRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.BypassRuleCondition",
                    "conditionType": str(conditionType),
                    "value": str(value)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.BypassRuleCondition",
                    "conditionType": "PROTOCOL",
                    "value": "TCP,UDP"
                }
            ]
        },
        "ruleId": 1
    }

def createQoSConditionRule( conditionType, value, priority):
    return {
        "description": "test QoS " + str(conditionType) + " " + str(value),
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.QosRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.QosRuleCondition",
                    "conditionType": str(conditionType),
                    "value": str(value)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.QosRuleCondition",
                    "conditionType": "PROTOCOL",
                    "value": "TCP,UDP"
                }
            ]
        },
        "priority": priority,
        "ruleId": 3
    }

def createSingleConditionFirewallRule( conditionType, value, blocked=True, flagged=True ):
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule",
        "id": 1,
        "enabled": True,
        "description": "Single Condition: " + str(conditionType) + " = " + str(value),
        "flag": flagged,
        "block": blocked,
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.node.firewall.FirewallRuleCondition",
                    "conditionType": str(conditionType),
                    "value": str(value)
                    }
                ]
            }
        }

def createRouteRule( networkAddr, netmask, gateway):
    return {
        "description": "test route",
        "javaClass": "com.untangle.uvm.network.StaticRoute",
        "network": networkAddr,
        "nextHop": gateway,
        "prefix": netmask,
        "ruleId": 1,
        "toAddr": True,
        "toDev": False
        }

def createNATRule( name, conditionType, value, source):
    return {
        "auto": False,
        "description": name,
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.NatRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.NatRuleCondition",
                    "conditionType": str(conditionType),
                    "value": value
                }
            ]
        },
        "newSource": source,
        "ruleId": 1
    }

def createDNSRule( networkAddr, name):
    return {
        "address": networkAddr,
        "javaClass": "com.untangle.uvm.network.DnsStaticEntry",
        "name": name
         }

def createVLANInterface( physicalInterface, symInterface, sysInterface, ipV4address):
    return {
            "addressed": True,
            "bridged": False,
            "configType": "ADDRESSED",
            "dhcpEnabled": False,
            "dhcpOptions": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "disabled": False,
            "interfaceId": 100,
            "isVlanInterface": True,
            "isWan": False,
            "javaClass": "com.untangle.uvm.network.InterfaceSettings",
            "name": "network_tests_010",
            "physicalDev": physicalInterface, #"eth1",
            "raEnabled": False,
            "symbolicDev": symInterface, #"eth1.1",
            "systemDev": sysInterface, #"eth1.1",
            "v4Aliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "v4ConfigType": "STATIC",
            "v4NatEgressTraffic": False,
            "v4NatIngressTraffic": False,
            "v4PPPoEPassword": "",
            "v4PPPoEUsePeerDns": False,
            "v4PPPoEUsername": "",
            "v4StaticAddress": ipV4address, #"192.168.14.1",
            "v4StaticNetmask": "255.255.255.0",
            "v4StaticPrefix": 24,
            "v6Aliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "v6ConfigType": "STATIC",
            "vlanParent": 2,
            "vlanTag": 1,
            "vrrpAliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "vrrpEnabled": False
        }

def createAlias(ipAddress,ipNetmask,ipPrefix):
    return {
            "javaClass": "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
            "staticAddress": ipAddress,
            "staticNetmask": ipNetmask,
            "staticPrefix": ipPrefix
        }


def getHttpHttpsPorts():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    return (netsettings['httpPort'], netsettings['httpsPort'])

def setHttpHttpsPorts(httpPort, httpsPort):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['httpPort'] = httpPort
    netsettings['httpsPort'] = httpsPort
    uvmContext.networkManager().setNetworkSettings(netsettings)

def setFirstLevelRule(newRule,ruleGroup):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings[ruleGroup]['list'].insert(0,newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)

def appendFWRule(node, newRule):
    rules = node.getRules()
    rules["list"].append(newRule)
    node.setRules(rules)

def addDNSRule(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'].insert(0,newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)

def findUsedIP(startIP):
    # Find an IP that is not currently used.
    loopLimit = 20
    testIP = ipaddr.IPAddress(startIP)
    ipUsed = True
    while (ipUsed and (loopLimit > 0)):
        loopLimit -= 1
        testIP += 1
        testIPResult = subprocess.call(["ping","-c","1",str(testIP)],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if testIPResult != 0:
            ipUsed = False

    if ipUsed:
        # no unused IP found
        return False
    else:
        return str(testIP)

def appendVLAN(parentInterfaceID):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    # find the physicalDev of the interface passed in.
    physicalDev = None
    for interface in netsettings['interfaces']['list']:
        if interface['interfaceId'] == parentInterfaceID:
            if interface['configType'] != "ADDRESSED":
                # only use if interface is addressed
                return False
            physicalDev = interface['physicalDev']
            break

    testVLANIP = findUsedIP("1.2.3.4")
    if testVLANIP:
        # no unused IP found
        return False

    # Check thast VLAN ID is not used
    loopLimit = 20
    testVLANID = 100
    vlanIdUsed = True
    while (vlanIdUsed and (loopLimit > 0)):
        testVLANID += 1
        loopLimit -= 1
        vlanIdUsed = False
        testVlanIdDev = physicalDev + "." + str(testVLANID)
        for interface in netsettings['interfaces']['list']:
            if interface['symbolicDev'] == testVlanIdDev:
                # found duplicate VLAN ID
                vlanIdUsed = True
                break

    if vlanIdUsed:
        # no unused VLAN ID found
        return False

    # if valid VLAN interface and IP is available, create a VLAN
    netsettings['interfaces']['list'].append(createVLANInterface(physicalDev,testVlanIdDev,testVlanIdDev,str(testVLANIP)))
    uvmContext.networkManager().setNetworkSettings(netsettings)
    return testVLANIP

def appendAliases(parentInterfaceID):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    for i in range(len(netsettings['interfaces']['list'])):
        if netsettings['interfaces']['list'][i]['interfaceId'] == parentInterfaceID:
            if netsettings['interfaces']['list'][i]['configType'] == "ADDRESSED" and netsettings['interfaces']['list'][i]['v4ConfigType'] == "STATIC":
                testStartIP = netsettings['interfaces']['list'][i]['v4StaticAddress']
                ipFound = findUsedIP(testStartIP)
                break;
            else:
                # only use if interface is addressed
                return False
    if ipFound:
        testAliasIP = findUsedIP(ipFound)
        if testAliasIP:
            netsettings['interfaces']['list'][i]['v4Aliases']['list'].append(createAlias(testAliasIP,
                                                                             netsettings['interfaces']['list'][i]['v4StaticNetmask'],
                                                                             netsettings['interfaces']['list'][i]['v4StaticPrefix']))
            uvmContext.networkManager().setNetworkSettings(netsettings)
        else:
            return False

    return testAliasIP

def nukeFirstLevelRule(ruleGroup):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings[ruleGroup]['list'][:] = []
    uvmContext.networkManager().setNetworkSettings(netsettings)

def nukeDNSRules():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'][:] = []
    uvmContext.networkManager().setNetworkSettings(netsettings)

def setDynDNS(login,password,hostname):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dynamicDnsServiceEnabled'] = True
    netsettings['dynamicDnsServiceHostnames'] = hostname
    netsettings['dynamicDnsServiceName'] = "dyndns"
    netsettings['dynamicDnsServiceUsername'] = login
    netsettings['dynamicDnsServicePassword'] = password
    uvmContext.networkManager().setNetworkSettings(netsettings)

def verifySnmpWalk():
    snmpwalkResult = remote_control.runCommand("test -x /usr/bin/snmpwalk")
    if snmpwalkResult:
        raise unittest2.SkipTest("Snmpwalk app needs to be installed on client")

def setSnmpV3Settings( settings, v3Enabled, v3Username, v3AuthenticationProtocol, v3AuthenticationPassphrase, v3PrivacyProtocol, v3PrivacyPassphrase, v3Required ):
    settings['v3Enabled'] = v3Enabled
    settings['v3Username'] = v3Username
    settings['v3AuthenticationProtocol'] = v3AuthenticationProtocol
    settings['v3AuthenticationPassphrase'] = v3AuthenticationPassphrase
    settings['v3PrivacyProtocol'] = v3PrivacyProtocol
    settings['v3PrivacyPassphrase'] = v3PrivacyPassphrase
    settings['v3Required'] = v3Required

    v1v2command = "snmpwalk -v 2c -c atstest " +  system_properties.get_lan_ip() + " | grep untangle"
    v3command = "snmpwalk -v 3 " + " -u " + v3Username + " -l authNoPriv " + " -a " + v3AuthenticationProtocol + " -A " + v3AuthenticationPassphrase + " -x " + v3PrivacyProtocol
    if v3PrivacyPassphrase != "":
        v3command += " -X " + v3PrivacyPassphrase
    v3command += " " +  system_properties.get_lan_ip() + " | grep untangle"

    print "v1v2command = " + v1v2command
    return( v1v2command, v3command )

def trySnmpCommand(command):
    result = remote_control.runCommand( command )
    if (result == 1):
        # there might be a delay in snmp restarting
        time.sleep(5)
        result = remote_control.runCommand( command )
    return result

class NetworkTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "network"

    @staticmethod
    def nodeNameFW():
        return "untangle-node-firewall"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global orig_netsettings, run_ftp_inbound_tests, wan_IP, device_in_office
        if orig_netsettings == None:
            orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        device_in_office = global_functions.isInOfficeNetwork(wan_IP)

        if run_ftp_inbound_tests == None:
            try:
                s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                s.connect( ( remote_control.clientIP, 21 ))
                s.close()
                pingResult = subprocess.call(["ping","-c","1",global_functions.ftpServer],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
                if pingResult == 0:
                    run_ftp_inbound_tests = True
                else:
                    run_ftp_inbound_tests = False
            except:
                run_ftp_inbound_tests = False

    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_015_addVLAN(self):
        raise unittest2.SkipTest("Review changes in test")
        # Add a test static VLAN
        testVLANIP = appendVLAN(remote_control.interface)
        if testVLANIP:
            result = subprocess.call(["ping","-c","1",str(testVLANIP)],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            uvmContext.networkManager().setNetworkSettings(orig_netsettings)
            assert(result == 0)
        else:
            # no VLAN was created so skip test
            unittest2.SkipTest("No VLAN or IP address available")


    def test_016_addAlias(self):
        raise unittest2.SkipTest("Review changes in test")
        # Add Alias IP
        AliasIP = appendAliases(remote_control.interface)
        if AliasIP:
            # print "AliasIP <%s>" % AliasIP
            result = remote_control.runCommand("ping -c 1 %s" % AliasIP)
            uvmContext.networkManager().setNetworkSettings(orig_netsettings)
            assert (result == 0)
        else:
            # No alias IP added so just skip
            unittest2.SkipTest("No alias address available")

    # test basic port forward (tcp port 80)
    def test_020_portForward80(self):
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","80","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,80),'portForwardRules')
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://1.2.3.4/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)

        events = global_functions.get_events('Network','Port Forwarded Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                               "s_server_addr", test_untangle_com_ip,
                                               "c_client_addr", remote_control.clientIP,
                                               "local_addr", remote_control.clientIP,
                                               "remote_addr", test_untangle_com_ip,
                                               "s_server_port", 80)
        assert(found)

    # test basic port forward (tcp port 443)
    def test_021_portForward443(self):
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","443","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,443),'portForwardRules')
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -q --no-check-certificate -O - https://1.2.3.4/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)

    # test port forward (changing the port 80 -> 81)
    def test_022_portForwardNewPort(self):
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","81","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,80),'portForwardRules')
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://1.2.3.4:81/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)

    # test port forward using DST_LOCAL condition
    def test_023_portForwardDstLocal(self):
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","81","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,80),'portForwardRules')
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://%s:81/test/testPage1.html 2>&1 | grep -q text123" % uvmContext.networkManager().getFirstWanAddress())
        assert(result == 0)

    # test port forward that uses the http port (move http to different port)
    def test_024_portForwardPort80LocalHttpPort(self):
        orig_ports = getHttpHttpsPorts()
        setHttpHttpsPorts( 8080, 4343 )
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","80","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,80),'portForwardRules')
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://%s/test/testPage1.html 2>&1 | grep -q text123" % uvmContext.networkManager().getFirstWanAddress())
        setHttpHttpsPorts( orig_ports[0], orig_ports[1])
        assert(result == 0)

    # test port forward that uses the https port (move https to different port)
    def test_025_portForwardPort443LocalHttpsPort(self):
        orig_ports = getHttpHttpsPorts()
        setHttpHttpsPorts( 8080, 4343 )
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","443","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,443),'portForwardRules')
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -q --no-check-certificate -O - https://%s/test/testPage1.html 2>&1 | grep -q text123" % uvmContext.networkManager().getFirstWanAddress())
        setHttpHttpsPorts( orig_ports[0], orig_ports[1])
        assert(result == 0)

    # test hairpin port forward (back to original client)
    def test_026_portForwardHairPin(self):
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","11234","DST_LOCAL","true","PROTOCOL","TCP",remote_control.clientIP,11234),'portForwardRules')
        remote_control.runCommand("nohup netcat -l -p 11234 >/dev/null 2>&1",stdout=False,nowait=True)
        result = remote_control.runCommand("echo test | netcat -q0 %s 11234" % uvmContext.networkManager().getFirstWanAddress())
        print "result: %s" % str(result)
        assert(result == 0)

    # test port forward to multiple ports (tcp port 80,443)
    def test_027_portForwardMultiport(self):
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","80,443","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,None),'portForwardRules')
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://1.2.3.4/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -q --no-check-certificate -O - https://1.2.3.4/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)

    # test a port forward from outside if possible
    def test_030_portForwardInbound(self):
        # We will use iperfServer for this test. Test to see if we can reach it.
        iperfAvail = global_functions.verifyIperf(wan_IP)
        if (not iperfAvail):
            raise unittest2.SkipTest("IperfServer test client unreachable, skipping alternate port forwarding test")
        # Also test that it can probably reach us (we're on a 10.x network)
        if not device_in_office:
            raise unittest2.SkipTest("Not on office network, skipping")

        # start netcat on client
        remote_control.runCommand("nohup netcat -l -p 11245 >/dev/null 2>&1",stdout=False,nowait=True)

        # port forward 11245 to client box
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","11245","DST_LOCAL","true","PROTOCOL","TCP",remote_control.clientIP,"11245"),'portForwardRules')

        # try connecting to netcat on client from "outside" box
        result = remote_control.runCommand("echo test | netcat -q0 " + wan_IP + " 11245", host=global_functions.iperfServer)
        assert (result == 0)

    # test a port forward from outside if possible
    def test_040_portForwardUDPInbound(self):
        # We will use iperf server and iperf for this test.
        # Also test that it can probably reach us (we're on a 10.x network)
        if not device_in_office:
            raise unittest2.SkipTest("Not on office network, skipping")
        iperfAvail = global_functions.verifyIperf(wan_IP)
        if (not iperfAvail):
            raise unittest2.SkipTest("iperfServer " + global_functions.iperfServer + " is unreachable, skipping")
        # Only if iperf is used
        # if not iperfResult:
        #     raise unittest2.SkipTest("Iperf server not reachable")

        # port forward UDP 5000 to client box
        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","5000","DST_LOCAL","true","PROTOCOL","UDP",remote_control.clientIP,"5000"),'portForwardRules')

        # start netcat on client
        remote_control.runCommand("rm -f /tmp/netcat.udp.recv.txt")
        remote_control.runCommand("nohup netcat -l -u -p 5000 >/tmp/netcat.udp.recv.txt",stdout=False,nowait=True)

        remote_control.runCommand("echo test| netcat -q0 -w1 -u " + wan_IP + " 5000",host=global_functions.iperfServer)

        result = remote_control.runCommand("grep test /tmp/netcat.udp.recv.txt")

        # send UDP packets through the port forward
        # UDP_speed = global_functions.getUDPSpeed( receiverIP=remote_control.clientIP, senderIP=global_functions.iperfServer, targetIP=wan_IP )
        # assert (UDP_speed >  0.0)

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert ( result == 0 )

    # test a NAT rules
    def test_050_natRule(self):
        # check if more than one WAN
        myWANs = {}
        netsettings = uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            # if its not a static WAN its not testable
            detectedIPlist =[]
            if interface['isWan'] and interface['v4ConfigType'] == "STATIC" and interface['v4StaticAddress'] != None:
                addr = interface['v4StaticAddress']
                # Check if WAN address is recognized by test.untangle.com
                detectedIP = global_functions.getIpAddress(extra_options="--bind-address=" + addr,localcall=True)
                detectedIP = detectedIP.rstrip()  # strip return character
                if detectedIP not in detectedIPlist:
                    detectedIPlist.append(detectedIP)
                    myWANs[addr] = detectedIP
        if (len(myWANs) < 2):
            raise unittest2.SkipTest("Need at least two public static WANS for test_050_natRule")
        for wanIP in myWANs:
            nukeFirstLevelRule("natRules")
            # Create NAT rule for port 80
            setFirstLevelRule(createNATRule("test out " + wanIP, "DST_PORT","80",wanIP),'natRules')
            # Determine current outgoing IP
            result = global_functions.getIpAddress()
            # print "result " + result + " wanIP " + myWANs[wanIP]
            assert (result == myWANs[wanIP])

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

    # Test that bypass rules bypass apps
    def test_060_bypassRules(self):
        nodeFW = None
        if (uvmContext.nodeManager().isInstantiated(self.nodeNameFW())):
            print "ERROR: Node %s already installed" % self.nodeNameFW()
            raise Exception('node %s already instantiated' % self.nodeNameFW())
        nodeFW = uvmContext.nodeManager().instantiate(self.nodeNameFW(), defaultRackId)
        nukeFirstLevelRule('bypassRules')
        # verify port 80 is open
        result1 = remote_control.runCommand("wget -q -O /dev/null http://test.untangle.com/")
        # Block port 80 and verify it's closed
        appendFWRule(nodeFW, createSingleConditionFirewallRule("DST_PORT","80"))
        result2 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        # add bypass rule for the client and enable bypass logging
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['bypassRules']['list'].append( createBypassConditionRule("SRC_ADDR",remote_control.clientIP) )
        netsettings['logBypassedSessions'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)

        # verify the client can still get out (and that the traffic is bypassed)
        result3 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

        events = global_functions.get_events('Network','Bypassed Sessions',None,100)

        uvmContext.nodeManager().destroy( nodeFW.getNodeSettings()["id"] )
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert (result1 == 0)
        assert (result2 != 0)
        assert (result3 == 0)

        assert(events != None)
        found = global_functions.check_events( events.get('list'), 100,
                                               "s_server_addr", test_untangle_com_ip,
                                               "c_client_addr", remote_control.clientIP,
                                               "local_addr", remote_control.clientIP,
                                               "remote_addr", test_untangle_com_ip,
                                               "s_server_port", 80)
        assert(found)

    # Test FTP (outbound) in active and passive modes
    def test_070_ftpModes(self):
        nukeFirstLevelRule('bypassRules')

        pasvResult = remote_control.runCommand("wget -t2 --timeout=10 -q -O /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        portResult = remote_control.runCommand("wget -t2 --timeout=10 --no-passive-ftp -q -O /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        epsvResult = remote_control.runCommand("curl --epsv -s -o /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        eprtResult = remote_control.runCommand("curl --eprt -P - -s -o /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        print "portResult: %i eprtResult: %i pasvResult: %i epsvResult: %i" % (portResult,eprtResult,pasvResult,epsvResult)
        assert (pasvResult == 0)
        assert (portResult == 0)
        assert (epsvResult == 0)
        assert (eprtResult == 0)

    # Test FTP (outbound) in active and passive modes with a firewall block all rule (firewall should pass related sessions without special rules)
    def test_071_ftpModesFirewalled(self):
        nodeFW = None
        if (uvmContext.nodeManager().isInstantiated(self.nodeNameFW())):
            print "ERROR: Node %s already installed" % self.nodeNameFW()
            raise Exception('node %s already instantiated' % self.nodeNameFW())
        nodeFW = uvmContext.nodeManager().instantiate(self.nodeNameFW(), defaultRackId)

        nukeFirstLevelRule('bypassRules')

        appendFWRule(nodeFW, createSingleConditionFirewallRule("DST_PORT","21", blocked=False))
        appendFWRule(nodeFW, createSingleConditionFirewallRule("PROTOCOL","TCP", blocked=True))

        pasvResult = remote_control.runCommand("wget -t2 --timeout=10 -q -O /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        portResult = remote_control.runCommand("wget -t2 --timeout=10 --no-passive-ftp -q -O /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        epsvResult = remote_control.runCommand("curl --epsv -s -o /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        eprtResult = remote_control.runCommand("curl --eprt -P - -s -o /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)

        uvmContext.nodeManager().destroy( nodeFW.getNodeSettings()["id"] )
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print "portResult: %i eprtResult: %i pasvResult: %i epsvResult: %i" % (portResult,eprtResult,pasvResult,epsvResult)
        assert (pasvResult == 0)
        assert (portResult == 0)
        assert (epsvResult == 0)
        assert (eprtResult == 0)

    # Test FTP (outbound) in active and passive modes with bypass
    def test_072_ftpModesBypassed(self):
        setFirstLevelRule(createBypassConditionRule("DST_PORT","21"),'bypassRules')

        pasvResult = remote_control.runCommand("wget -t2 --timeout=10 -q -O /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        portResult = remote_control.runCommand("wget -t2 --timeout=10 --no-passive-ftp -q -O /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        epsvResult = remote_control.runCommand("curl --epsv -s -o /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        eprtResult = remote_control.runCommand("curl --eprt -P - -s -o /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print "portResult: %i eprtResult: %i pasvResult: %i epsvResult: %i" % (portResult,eprtResult,pasvResult,epsvResult)
        assert (pasvResult == 0)
        assert (portResult == 0)
        assert (epsvResult == 0)
        assert (eprtResult == 0)

    # Test FTP (outbound) in active and passive modes with bypass with a block all rule in forward filter rules. It should pass RELATED session automatically
    def test_073_ftpModesBypassedFiltered(self):
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['bypassRules']['list'] = [ createBypassConditionRule("DST_PORT","21") ]
        netsettings['forwardFilterRules']['list'] = [ createFilterRule("DST_PORT","21","PROTOCOL","TCP",False), createFilterRule("DST_PORT","1-65535","PROTOCOL","TCP",True) ]
        uvmContext.networkManager().setNetworkSettings(netsettings)

        pasvResult = remote_control.runCommand("wget -t2 --timeout=10 -q -O /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        portResult = remote_control.runCommand("wget -t2 --timeout=10 --no-passive-ftp -q -O /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        epsvResult = remote_control.runCommand("curl --epsv -s -o /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)
        eprtResult = remote_control.runCommand("curl --eprt -P - -s -o /dev/null ftp://" + global_functions.ftpServer + "/" + ftp_file_name)

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print "portResult: %i eprtResult: %i pasvResult: %i epsvResult: %i" % (portResult,eprtResult,pasvResult,epsvResult)
        assert (pasvResult == 0)
        assert (portResult == 0)
        assert (epsvResult == 0)
        assert (eprtResult == 0)

    # Test FTP (inbound) in active and passive modes (untangle-vm should add port forwards for RELATED session)
    def test_074_ftpModesIncoming(self):
        if not run_ftp_inbound_tests:
            raise unittest2.SkipTest("remote client does not have ftp server")
        if not device_in_office:
            raise unittest2.SkipTest("Not on office network, skipping")

        setFirstLevelRule(createPortForwardTripleCondition("DST_PORT","21","DST_LOCAL","true","PROTOCOL","TCP",remote_control.clientIP,""),'portForwardRules')

        wan_IP = uvmContext.networkManager().getFirstWanAddress()

        pasvResult = remote_control.runCommand("wget -t2 --timeout=10 -q -O /dev/null ftp://" +  wan_IP + "/" + ftp_file_name,host=global_functions.ftpServer)
        portResult = remote_control.runCommand("wget -t2 --timeout=10 --no-passive-ftp -q -O /dev/null ftp://" + wan_IP + "/" + ftp_file_name,host=global_functions.ftpServer)
        epsvResult = remote_control.runCommand("curl --epsv -s -o /dev/null ftp://" + wan_IP + "/" + ftp_file_name,host=global_functions.ftpServer)
        eprtResult = remote_control.runCommand("curl --eprt -P - -s -o /dev/null ftp://" + wan_IP + "/" + ftp_file_name,host=global_functions.ftpServer)

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print "portResult: %i eprtResult: %i pasvResult: %i epsvResult: %i" % (portResult,eprtResult,pasvResult,epsvResult)
        assert (pasvResult == 0)
        assert (portResult == 0)
        assert (epsvResult == 0)
        assert (eprtResult == 0)

    # Test FTP (inbound) in active and passive modes with bypass (nf_nat_ftp should add port forwards for RELATED session, nat filters should allow RELATED)
    def test_075_ftpModesIncomingBypassed(self):
        if not run_ftp_inbound_tests:
            raise unittest2.SkipTest("remote client does not have ftp server")
        if not device_in_office:
            raise unittest2.SkipTest("Not on office network, skipping")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['bypassRules']['list'] = [ createBypassConditionRule("DST_PORT","21") ]
        netsettings['portForwardRules']['list'] = [ createPortForwardTripleCondition("DST_PORT","21","DST_LOCAL","true","PROTOCOL","TCP",remote_control.clientIP,"") ]
        uvmContext.networkManager().setNetworkSettings(netsettings)

        wan_IP = uvmContext.networkManager().getFirstWanAddress()

        pasvResult = remote_control.runCommand("wget -t2 --timeout=10 -q -O /dev/null ftp://" +  wan_IP + "/" + ftp_file_name,host=global_functions.ftpServer)
        portResult = remote_control.runCommand("wget -t2 --timeout=10 --no-passive-ftp -q -O /dev/null ftp://" + wan_IP + "/" + ftp_file_name,host=global_functions.ftpServer)
        epsvResult = remote_control.runCommand("curl --epsv -s -o /dev/null ftp://" + wan_IP + "/" + ftp_file_name,host=global_functions.ftpServer)
        eprtResult = remote_control.runCommand("curl --eprt -P - -s -o /dev/null ftp://" + wan_IP + "/" + ftp_file_name,host=global_functions.ftpServer)

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print "portResult: %i eprtResult: %i pasvResult: %i epsvResult: %i" % (portResult,eprtResult,pasvResult,epsvResult)
        assert (pasvResult == 0)
        assert (portResult == 0)
        assert (epsvResult == 0)
        assert (eprtResult == 0)

    # Test static route that routing test.untangle.com to 127.0.0.1 makes it unreachable
    def test_080_routes(self):
        preResult = remote_control.isOnline()

        # add a route to 127.0.0.1 to blackhole that IP
        setFirstLevelRule(createRouteRule(test_untangle_com_ip,32,"127.0.0.1"),'staticRoutes')

        postResult = remote_control.runCommand("wget -t 1 --timeout=3 http://test.untangle.com")

        # restore setting before validating results
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (preResult == 0)
        assert (postResult != 0)

    # Test static DNS entry
    def test_090_DNS(self):
        # Test static entries in Config -> Networking -> Advanced -> DNS
        global wan_IP
        nukeDNSRules()
        result = remote_control.runCommand("host -R3 -4 test.untangle.com " + wan_IP, stdout=True)
        # print "result <%s>" % result
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = (match.group()).replace('address ','')
        # print "IP address of test.untangle.com <%s>" % ip_address_testuntangle
        addDNSRule(createDNSRule(ip_address_testuntangle,"www.foobar.com"))
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        print "wan_IP <%s>" % wan_IP

        result = remote_control.runCommand("host -R3 -4 www.foobar.com " + wan_IP, stdout=True)
        # print "Results of www.foobar.com <%s>" % result
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_foobar = (match.group()).replace('address ','')
        # print "IP address of www.foobar.com <%s>" % ip_address_foobar
        # print "IP address of test.untangle.com <%s>" % ip_address_testuntangle
        print "Result expected:\"%s\" actual:\"%s\"" % (str(ip_address_testuntangle),str(ip_address_foobar))
        assert(ip_address_testuntangle == ip_address_foobar)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

    # Test dynamic hostname
    def test_100_DynamicDns(self):
        global dyn_hostname
        # raise unittest2.SkipTest('Broken test')
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        wan_count = 0
        netsettings = uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface['isWan'] and interface['disabled'] == False:
                wan_count += 1
        
        if (wan_count > 1):
            raise unittest2.SkipTest("More than 1 WAN does not work with Dynamic DNS NGFW-5543")
            
        # if dynamic name is already in the ddclient cache with the same IP, dyndns is never updates
        # we need a name never used or name with cache IP different than in the cache
        result = subprocess.check_output("wget --timeout=4 -q -O - \"$@\" checkip.dyndns.com", shell=True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        outsideIP = match.group()
        dyn_hostname = getUsableName(outsideIP)
        if dyn_hostname == "":
            raise unittest2.SkipTest('Skipping since all dyndns names already used')
        else:
            print "Using name: %s" % dyn_hostname
        dynDNSUserName, dynDNSPassword = global_functions.getLiveAccountInfo("Dyndns")
        # account not found if message returned
        if dynDNSUserName == "message":
            raise unittest2.SkipTest('no dyn user')

        # Clear the ddclient cache and set DynDNS info
        ddclientCacheFile = "/var/cache/ddclient/ddclient.cache"
        if os.path.isfile(ddclientCacheFile):
            os.remove(ddclientCacheFile)        
        setDynDNS(dynDNSUserName, dynDNSPassword, dyn_hostname)
        
        # since Untangle uses our own servers for ddclient, test boxes will show the office IP addresses so lookup up internal IP
        outsideIP2 = global_functions.getIpAddress(base_URL=global_functions.tlsSmtpServerHost,localcall=True)
        outsideIP2 = outsideIP2.rstrip()  # strip return character

        loopCounter = 60
        dynIpFound = False
        while loopCounter > 0 and not dynIpFound:
            # run force to get it to run now
            subprocess.call(["ddclient","--force"],stdout=subprocess.PIPE,stderr=subprocess.PIPE) # force it to run faster
            # time.sleep(10)
            loopCounter -= 1
            result = remote_control.runCommand("host " + dyn_hostname + " " + dyndns_resolver, stdout=True)
            match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
            dynIP = (match.group()).replace('address ','')
            print "IP address of outsideIP <%s> outsideIP2 <%s> dynIP <%s> " % (outsideIP,outsideIP2,dynIP)
            dynIpFound = False
            if outsideIP == dynIP or outsideIP2 == dynIP:
                dynIpFound = True

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert(dynIpFound)

    # Test VRRP is active
    def test_110_VRRP(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Find a static interface
        i=0
        interfaceNotFound = True
        for interface in netsettings['interfaces']['list']:
            if (interface['v4ConfigType'] == "STATIC" and not netsettings['interfaces']['list'][i]['disabled']):
                interfaceNotFound = False
                break
            i += 1
        # Verify interface is found
        if interfaceNotFound:
            raise unittest2.SkipTest("No static enabled interface found")
        interfaceIP = netsettings['interfaces']['list'][i]['v4StaticAddress']
        interfacePrefix = netsettings['interfaces']['list'][i]['v4StaticPrefix']
        interfaceNet = interfaceIP + "/" + str(interfacePrefix)
        # get next IP not used
        ipStep = 1
        loopCounter = 10
        vrrpIP = None
        ip = ipaddr.IPAddress(interfaceIP)
        while vrrpIP == None and loopCounter:
            # get next IP and test that it is unused
            newip = ip + ipStep
            # check to see if the IP is in network range
            if newip in ipaddr.IPv4Network(interfaceNet):
                pingResult = remote_control.runCommand("ping -c 1 %s" % str(newip))
                if pingResult:
                    # new IP found
                    vrrpIP = newip
            else:
                # The IP is beyond the range of the network, go backward through the IPs
                ipStep = -1
            loopCounter -= 1
            ip = newip
        if (vrrpIP == None):
            raise unittest2.SkipTest("No IP found for VRRP")
        # Set VRRP values

        netsettings['interfaces']['list'][i]['vrrpAliases'] = {
            "javaClass": "java.util.LinkedList",
            "list": [{
                    "javaClass": "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
                    "staticAddress": str(vrrpIP),
                    "staticPrefix": 24
                    }]
            }
        netsettings['interfaces']['list'][i]['vrrpEnabled'] = True
        netsettings['interfaces']['list'][i]['vrrpId'] = 2
        netsettings['interfaces']['list'][i]['vrrpPriority'] = 1
        uvmContext.networkManager().setNetworkSettings(netsettings)
        timeout = 12
        pingResult = 1
        onlineResults = 1
        while timeout > 0 and (pingResult != 0 or onlineResults != 0):
            time.sleep(10) # wait for settings to take affect
            timeout -= 1
            # Test that the VRRP is pingable
            pingResult = remote_control.runCommand("ping -c 1 %s" % str(vrrpIP))
            # check if still online
            onlineResults = remote_control.isOnline()
        print "Timeout: %d" % timeout
        # Return to default network state
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert (pingResult == 0)
        assert (onlineResults == 0)

    # Test MTU settings
    def test_120_MTU(self):
        mtuSetValue = '1460'
        targetDevice = 'eth0'
        mtuAutoValue = None
        # Get current MTU value due to bug 11599
        ifconfigResults = subprocess.Popen(["ifconfig", targetDevice], stdout=subprocess.PIPE).communicate()[0]
        # print ifconfigResults
        reValue = re.search(r'MTU:(\S+)', ifconfigResults)
        mtuValue = None
        if reValue:
             mtuAutoValue = reValue.group(1)
        # print "mtuValue " + mtuValue
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Set eth0 to 1480
        for i in range(len(netsettings['devices']['list'])):
            if netsettings['devices']['list'][i]['deviceName'] == targetDevice:
                netsettings['devices']['list'][i]['mtu'] = mtuSetValue
                break
        uvmContext.networkManager().setNetworkSettings(netsettings)
        # Verify the MTU is set
        ifconfigResults = subprocess.Popen(["ifconfig", targetDevice], stdout=subprocess.PIPE).communicate()[0]
        # print ifconfigResults
        reValue = re.search(r'MTU:(\S+)', ifconfigResults)
        mtuValue = None
        if reValue:
             mtuValue = reValue.group(1)
        # print "mtuValue " + mtuValue
        # manually set MTU back to original value due to bug 11599
        netsettings['devices']['list'][i]['mtu'] = mtuAutoValue
        uvmContext.networkManager().setNetworkSettings(netsettings)
        # Set MTU back to auto
        del netsettings['devices']['list'][i]['mtu']
        uvmContext.networkManager().setNetworkSettings(netsettings)
        ifconfigResults = subprocess.Popen(["ifconfig", targetDevice], stdout=subprocess.PIPE).communicate()[0]
        # print ifconfigResults
        reValue = re.search(r'MTU:(\S+)', ifconfigResults)
        mtu2Value = None
        if reValue:
             mtu2Value = reValue.group(1)
        # print "mtu2Value " + mtu2Value
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert (mtuValue == mtuSetValue)
        assert (mtu2Value == mtuAutoValue)

    # SNMP, v1/v2enabled, v3 disabled
    def test_130_SNMP_Enabled_V1V2Only(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = remote_control.clientIP
        systemSettings['snmpSettings']['port'] = 161
        systemSettings['snmpSettings']['v3Enabled'] = False
        uvmContext.systemManager().setSettings(systemSettings)
        v2cResult = remote_control.runCommand("snmpwalk -v 2c -c atstest " +  system_properties.get_lan_ip() + " | grep untangle")
        v3Result = remote_control.runCommand("snmpwalk -v 3 -u testuser -l authPriv -a sha -A password -x des -X drowssap " +  system_properties.get_lan_ip() + " | grep untangle")
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert( v2cResult == 0 )
        assert( v3Result == 1 )

    def test_131_SNMP_Enabled_V3ShaDesNoPrivacyPassphrase(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = remote_control.clientIP
        systemSettings['snmpSettings']['port'] = 161
        commands = setSnmpV3Settings( systemSettings['snmpSettings'], True, "testuser", "sha", "shapassword", "des", "", False )
        uvmContext.systemManager().setSettings(systemSettings)
        v2cResult = trySnmpCommand( commands[0] )
        v3Result = trySnmpCommand( commands[1] )
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_132_SNMP_Enabled_V3Md5DesNoPrivacyPassphrase(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = remote_control.clientIP
        systemSettings['snmpSettings']['port'] = 161
        commands = setSnmpV3Settings( systemSettings['snmpSettings'], True, "testuser", "md5", "md5password", "des", "", False )
        uvmContext.systemManager().setSettings(systemSettings)
        v2cResult = trySnmpCommand( commands[0] )
        v3Result = trySnmpCommand( commands[1] )
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_133_SNMP_Enabled_V3ShaDes(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = remote_control.clientIP
        systemSettings['snmpSettings']['port'] = 161
        commands = setSnmpV3Settings( systemSettings['snmpSettings'], True, "testuser", "sha", "shapassword", "des", "despassword", False )
        uvmContext.systemManager().setSettings(systemSettings)
        v2cResult = trySnmpCommand( commands[0] )
        v3Result = trySnmpCommand( commands[1] )
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_134_SNMP_Enabled_V3ShaAes(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = remote_control.clientIP
        systemSettings['snmpSettings']['port'] = 161
        commands = setSnmpV3Settings( systemSettings['snmpSettings'], True, "testuser", "sha", "shapassword", "aes", "aespassword", False )
        uvmContext.systemManager().setSettings(systemSettings)
        v2cResult = trySnmpCommand( commands[0] )
        v3Result = trySnmpCommand( commands[1] )
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_135_SNMP_Enabled_V3Md5Des(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = remote_control.clientIP
        systemSettings['snmpSettings']['port'] = 161
        commands = setSnmpV3Settings( systemSettings['snmpSettings'], True, "testuser", "md5", "md5password", "des", "despassword", False )
        uvmContext.systemManager().setSettings(systemSettings)
        v2cResult = trySnmpCommand( commands[0] )
        v3Result = trySnmpCommand( commands[1] )
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_136_SNMP_Enabled_V3Md5Aes(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = remote_control.clientIP
        systemSettings['snmpSettings']['port'] = 161
        commands = setSnmpV3Settings( systemSettings['snmpSettings'], True, "testuser", "md5", "md5password", "aes", "aespassword", False )
        uvmContext.systemManager().setSettings(systemSettings)
        v2cResult = trySnmpCommand( commands[0] )
        v3Result = trySnmpCommand( commands[1] )
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_137_SNMP_Enabled_V3Required(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = remote_control.clientIP
        systemSettings['snmpSettings']['port'] = 161
        commands = setSnmpV3Settings( systemSettings['snmpSettings'], True, "testuser", "sha", "shapassword", "aes", "aespassword", True )
        uvmContext.systemManager().setSettings(systemSettings)
        v2cResult = trySnmpCommand( commands[0] )
        v3Result = trySnmpCommand( commands[1] )
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert( v2cResult == 1 )
        assert( v3Result == 0 )

    def test_138_SNMP_Disabled(self):
        verifySnmpWalk()
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['enabled'] = False
        uvmContext.systemManager().setSettings(systemSettings)
        result = remote_control.runCommand("snmpwalk -v 2c -c atstest " + system_properties.get_lan_ip() + " | grep untangle")
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert(result == 1)

    def test_140_sessions(self):
        foundTestSession = False
        remote_control.runCommand("nohup netcat -d -4 test.untangle.com 80 >/dev/null 2>&1",stdout=False,nowait=True)
        loopLimit = 5
        while ((not foundTestSession) and (loopLimit > 0)):
            loopLimit -= 1
            time.sleep(1)
            result = uvmContext.sessionMonitor().getMergedSessions()
            sessionList = result['list']
            # find session generated with netcat in session table.
            for i in range(len(sessionList)):
                # print sessionList[i]
                # print "------------------------------"
                if (sessionList[i]['preNatClient'] == remote_control.clientIP) and \
                   (sessionList[i]['postNatServer'] == test_untangle_com_ip) and \
                   (sessionList[i]['postNatServerPort'] == 80) and \
                   (not sessionList[i]['bypassed']):
                    foundTestSession = True
                    break
        remote_control.runCommand("pkill netcat")
        assert(foundTestSession)

    def test_141_host(self):
        foundTestSession = False
        remote_control.runCommand("nohup netcat -d -4 test.untangle.com 80 >/dev/null 2>&1",stdout=False,nowait=True)
        time.sleep(2) # since we launched netcat in background, give it a second to establish connection
        result = uvmContext.hostTable().getHosts()
        sessionList = result['list']
        # find session generated with netcat in session table.
        for i in range(len(sessionList)):
            # print sessionList[i]
            # print "------------------------------"
            if (sessionList[i]['address'] == remote_control.clientIP):
                foundTestSession = True
                break
        remote_control.runCommand("pkill netcat")
        assert(foundTestSession)

    # Test logging of blocked sessions via untangle-nflogd
    def test_150_filterRulesBlockedEventLog(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        # verify port 80 is open
        result1 = remote_control.runCommand("wget -q -O /dev/null http://test.untangle.com/")

        # Add a block rule for port 80 and enabled blocked session logging
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['forwardFilterRules']['list'] = [ createFilterRule("DST_PORT","80","PROTOCOL","TCP",True) ]
        netsettings['logBlockedSessions'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)

        for i in range(0, 10):
            # make the request again which should now be blocked and logged
            result2 = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")

            # grab all of the blocked events for checking later
            events = global_functions.get_events('Network','Blocked Sessions',None,100)
            found = global_functions.check_events( events.get('list'), 100,
                                                   "s_server_addr", test_untangle_com_ip,
                                                   "c_client_addr", remote_control.clientIP,
                                                   "local_addr", remote_control.clientIP,
                                                   "remote_addr", test_untangle_com_ip,
                                                   "s_server_port", 80)
            if found:
                break

            # give the NetFilterLogger time to receive and write the event
            # This is necessary because we have no way to "flush" events
            # Sleep 10 before trying again
            time.sleep(10)

        # put the network settings back the way we found them
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        # make sure all of our tests were successful
        assert (result1 == 0)
        assert (result2 != 0)

        assert(events != None)
        assert(found)

    # Test UDP traceroute bug 12663 
    def test_160_tracerouteUDP(self):
        tracerouteExists = remote_control.runCommand("test -x /usr/sbin/traceroute")
        if tracerouteExists != 0:
            raise unittest2.SkipTest("Traceroute app needs to be installed on client")
        result = remote_control.runCommand("/usr/sbin/traceroute test.untangle.com", stdout=True)
        # 3 occurances of ms per line so check for at least two lines of ms times.
        assert(result.count('ms') > 4) 

    # UPnP - Disabled
    def test_170_upnp_disabled(self):
        upnpcExists = remote_control.runCommand("test -x /usr/bin/upnpc")
        if upnpcExists != 0:
            raise unittest2.SkipTest("Upnpc app needs to be installed on client")
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if global_functions.isBridged(wan_IP):
            raise unittest2.SkipTest("Unable to disable upnp on bridged configurations")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = False
        uvmContext.networkManager().setNetworkSettings(netsettings)
        result = remote_control.runCommand("/usr/bin/upnpc -a %s 5559 5559 tcp >/dev/null 2>&1" % (remote_control.clientIP),stdout=False)
        assert(result != 0)

    # UPnP - Enabled
    def test_171_upnp_enabled_defaults(self):
        upnpcExists = remote_control.runCommand("test -x /usr/bin/upnpc")
        if upnpcExists != 0:
            raise unittest2.SkipTest("Upnpc app needs to be installed on client")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)
        result = remote_control.runCommand("/usr/bin/upnpc -a %s 5559 5559 tcp >/dev/null 2>&1" % (remote_control.clientIP),stdout=False)
        assert(result == 0)

    # UPnP - Secure mode enabled
    def test_172_upnp_secure_mode_enabled(self):
        upnpcExists = remote_control.runCommand("test -x /usr/bin/upnpc")
        if upnpcExists != 0:
            raise unittest2.SkipTest("Upnpc app needs to be installed on client")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = True
        netsettings['upnpSettings']['secureMode'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)
        result1 = remote_control.runCommand("/usr/bin/upnpc -a %s 5559 5559 tcp >/dev/null 2>&1" % (remote_control.clientIP),stdout=False)
        result2 = remote_control.runCommand("/usr/bin/upnpc -a %s 5558 5558 tcp 2>&1 | grep ConflictInMappingEntry" % ("1.2.3.4"),stdout=False)
        assert(result1 == 0)
        assert(result2 == 0)

    # UPnP - Secure mode disabled
    def test_173_upnp_secure_mode_disabled(self):
        upnpcExists = remote_control.runCommand("test -x /usr/bin/upnpc")
        if upnpcExists != 0:
            raise unittest2.SkipTest("Upnpc app needs to be installed on client")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = True
        netsettings['upnpSettings']['secureMode'] = False
        uvmContext.networkManager().setNetworkSettings(netsettings)
        result1 = remote_control.runCommand("/usr/bin/upnpc -a %s 5559 5559 tcp >/dev/null 2>&1" % (remote_control.clientIP),stdout=False)
        result2 = remote_control.runCommand("/usr/bin/upnpc -a %s 5558 5558 tcp 2>&1 | grep ConflictInMappingEntry" % ("1.2.3.4"),stdout=False)
        assert(result1 == 0)
        assert(result2 == 1)

    # UPnP - Enabled, Deny rule
    def test_174_upnp_rules_deny_all(self):
        upnpcExists = remote_control.runCommand("test -x /usr/bin/upnpc")
        if upnpcExists != 0:
            raise unittest2.SkipTest("Upnpc app needs to be installed on client")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = True
        netsettings['upnpSettings']['upnpRules'] = {
            "javaClass": "java.util.LinkedList",
            "list": [{
                    "allow": False,
                    "conditions": {
                        "javaClass": "java.util.LinkedList",
                        "list": [
                            {
                                "conditionType": "SRC_ADDR",
                                "invert": False,
                                "javaClass": "com.untangle.uvm.network.UpnpRuleCondition",
                                "value": "0.0.0.0/0"
                            },
                            {
                                "conditionType": "DST_PORT",
                                "invert": False,
                                "javaClass": "com.untangle.uvm.network.UpnpRuleCondition",
                                "value": "0-65535"
                            },
                            {
                                "conditionType": "SRC_PORT",
                                "invert": False,
                                "javaClass": "com.untangle.uvm.network.UpnpRuleCondition",
                                "value": "0-65535"
                            }
                        ]
                    },
                    "description": "Deny all",
                    "enabled": True,
                    "javaClass": "com.untangle.uvm.network.UpnpRule",
                    "ruleId": 2
                }                
            ]
        }
        uvmContext.networkManager().setNetworkSettings(netsettings)
        result = remote_control.runCommand("/usr/bin/upnpc -a %s 5559 5559 tcp 2>&1 | grep failed" % (remote_control.clientIP),stdout=False)
        assert(result == 0)

    @staticmethod
    def finalTearDown(self):
        # Restore original settings to return to initial settings
        # print "orig_netsettings <%s>" % orig_netsettings
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)


test_registry.registerNode("network", NetworkTests)
