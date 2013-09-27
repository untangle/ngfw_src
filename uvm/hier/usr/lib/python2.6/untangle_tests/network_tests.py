import socket
import unittest2
import os
import sys
reload(sys)
sys.setdefaultencoding("utf-8")
import re
import subprocess
import ipaddr
import system_props
import time
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import TestDict
from untangle_tests import ClientControl

node = None
nodeFW = None

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
orig_netsettings = None
test_untangle_com_ip = socket.gethostbyname("test.untangle.com")

def createPortForwardTripleCondition( matcherType1, value1, matcherType2, value2, matcherType3, value3, destinationIP, destinationPort):
    return {
        "description": "port forward  -> " + str(destinationIP) + ":" + str(destinationPort) + " test", 
        "enabled": True, 
        "javaClass": "com.untangle.uvm.network.PortForwardRule", 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": str(matcherType1), 
                    "value": str(value1)
                }, 
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": str(matcherType2),
                    "value": str(value2)
                }, 
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": str(matcherType3), 
                    "value": str(value3)
                }
            ]
        }, 
        "newDestination": destinationIP,
        "newPort": destinationPort,
        "ruleId": 1
    } 

def createBypassMatcherRule( matcherType, value):
    return {
        "bypass": True, 
        "description": "test bypass " + str(matcherType) + " " + str(value), 
        "enabled": True, 
        "javaClass": "com.untangle.uvm.network.BypassRule", 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.BypassRuleMatcher", 
                    "matcherType": str(matcherType), 
                    "value": str(value)
                }, 
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.BypassRuleMatcher", 
                    "matcherType": "PROTOCOL", 
                    "value": "TCP,UDP"
                }
            ]
        }, 
        "ruleId": 1
    } 

def createSingleMatcherRule( matcherType, value, blocked=True, flagged=True ):
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + str(matcherType) + " = " + str(value), 
        "flag": flagged, 
        "block": blocked, 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                    "matcherType": str(matcherType), 
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

def createDNSRule( networkAddr, name):
    return {
        "address": networkAddr, 
        "javaClass": "com.untangle.uvm.network.DnsStaticEntry", 
        "name": name
         }

def getHttpHttpsPorts():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    return (netsettings['httpPort'], netsettings['httpsPort'])

def setHttpHttpsPorts(httpPort, httpsPort):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['httpPort'] = httpPort
    netsettings['httpsPort'] = httpsPort
    uvmContext.networkManager().setNetworkSettings(netsettings)

def appendForward(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['portForwardRules']['list'].append(newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)

def appendBypass(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['bypassRules']['list'].append(newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)

def appendFWRule(newRule):
    rules = nodeFW.getRules()
    rules["list"].append(newRule)
    nodeFW.setRules(rules)

def appendRouteRule(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['staticRoutes']['list'].append(newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)

def appendDNSRule(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'].append(newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)
    
def nukePortForwardRules():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['portForwardRules']['list'][:] = []
    uvmContext.networkManager().setNetworkSettings(netsettings)
    
def nukeDNSRules():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'][:] = []
    uvmContext.networkManager().setNetworkSettings(netsettings)    
    
def nukeBypassRules():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['bypassRules']['list'][:] = []
    uvmContext.networkManager().setNetworkSettings(netsettings)    

def nukeRoutes():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['staticRoutes']['list'][:] = []
    uvmContext.networkManager().setNetworkSettings(netsettings)
    
def setDynDNS():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dynamicDnsServiceEnabled'] = True
    netsettings['dynamicDnsServiceHostnames'] = "testuntangle.dyndns-pics.com"
    netsettings['dynamicDnsServiceName'] = "dyndns"
    netsettings['dynamicDnsServicePassword'] = "untangledyn"
    netsettings['dynamicDnsServiceUsername'] = "testuntangle"
    uvmContext.networkManager().setNetworkSettings(netsettings)

def nukeDynDNS():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dynamicDnsServiceEnabled'] = False
    uvmContext.networkManager().setNetworkSettings(netsettings)

# def isBridgeMode(clientIPAdress):
#     netsettings = uvmContext.networkManager().getNetworkSettings()
#     for interface in netsettings['interfaces']['list']:
#         if interface['isWan']:
#             if interface['v4StaticGateway']:
#                 wanIP = interface['v4StaticGateway']
#                 wanNetmask = interface['v4StaticNetmask']
#                 systemProperties = system_props.SystemProperties()
#                 wanNetSize = systemProperties.get_net_size(wanNetmask)
#                 wanRange = wanIP + '/' + wanNetSize
#                 wanNet = ipaddr.IPNetwork(wanRange)
#                 wanAddr = ipaddr.IPAddress(clientIPAdress)
#             elif (interface['v4ConfigType'] in ['AUTO','PPPOE']):
#                 # is this a dynamic IP w/o PPPOE
#                 nicDevice = str(interface['symbolicDev'])
#                 systemProperties = system_props.SystemProperties()
#                 wanIP = systemProperties.get_ip_address(nicDevice)
#                 wanNetmask =  systemProperties.get_netmask(nicDevice)
#                 wanRange = wanIP + '/' + systemProperties.get_net_size(wanNetmask)
#                 wanNet = ipaddr.IPNetwork(wanRange)
#                 wanAddr = ipaddr.IPAddress(clientIPAdress)
#             else:
#                 raise unittest2.SkipTest("Unable to determine WAN IP")
#             if wanAddr in wanNet:
#                 return True
#             else:
#                 pass
#         else:
#             pass
#     return False

def getDownloadSpeed():
    # Download file and record the average speed in which the file was download
    clientControl.runCommand("rm /tmp/test.txt >/dev/null 2>&1")
    result = clientControl.runCommand("wget -o /tmp/test.txt http://test.untangle.com/5MB.zip")
    ClientControl.verbosity = 1
    result = clientControl.runCommand("tail -2 /tmp/test.txt", True)
    # remove test file
    match = re.search(r'([0-9.]+) [KM]B\/s', result)
    bandwidth_speed =  match.group(1)
    # cast string to float for comparsion.
    bandwidth_speed = float(bandwidth_speed)
    # adjust value if MB or KB
    if "MB/s" in result:
        bandwidth_speed *= 1000
    # print "bandwidth_speed <%s>" % bandwidth_speed
    return bandwidth_speed
    

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

    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        # save original network settings
        global orig_netsettings
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        assert (result == 0)

    # test basic port forward (tcp port 80)
    def test_020_portForward80(self):
        nukePortForwardRules()
        appendForward(createPortForwardTripleCondition("DST_PORT","80","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,80))
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://1.2.3.4/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)

    # test basic port forward (tcp port 443)
    def test_021_portForward443(self):
        nukePortForwardRules()
        appendForward(createPortForwardTripleCondition("DST_PORT","443","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,443))
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -q --no-check-certificate -O - https://1.2.3.4/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)

    # test port forward (changing the port 80 -> 81)
    def test_022_portForwardNewPort(self):
        nukePortForwardRules()
        appendForward(createPortForwardTripleCondition("DST_PORT","81","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,80))
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://1.2.3.4:81/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)

    # test port forward using DST_LOCAL condition
    def test_023_portForwardDstLocal(self):
        nukePortForwardRules()
        appendForward(createPortForwardTripleCondition("DST_PORT","81","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,80))
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://%s:81/test/testPage1.html 2>&1 | grep -q text123" % uvmContext.networkManager().getFirstWanAddress())
        assert(result == 0)

    # test port forward that uses the http port (move http to different port)
    def test_024_portForwardPort80LocalHttpPort(self):
        orig_ports = getHttpHttpsPorts()
        setHttpHttpsPorts( 8080, 4343 )
        nukePortForwardRules()
        appendForward(createPortForwardTripleCondition("DST_PORT","80","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,80))
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://%s/test/testPage1.html 2>&1 | grep -q text123" % uvmContext.networkManager().getFirstWanAddress())
        setHttpHttpsPorts( orig_ports[0], orig_ports[1])
        assert(result == 0)

    # test port forward that uses the https port (move https to different port)
    def test_025_portForwardPort443LocalHttpsPort(self):
        orig_ports = getHttpHttpsPorts()
        setHttpHttpsPorts( 8080, 4343 )
        nukePortForwardRules()
        appendForward(createPortForwardTripleCondition("DST_PORT","443","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,443))
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -q --no-check-certificate -O - https://%s/test/testPage1.html 2>&1 | grep -q text123" % uvmContext.networkManager().getFirstWanAddress())
        setHttpHttpsPorts( orig_ports[0], orig_ports[1])
        assert(result == 0)

    # test hairpin port forward (back to original client)
    def test_026_portForwardHairPin(self):
        nukePortForwardRules()
        appendForward(createPortForwardTripleCondition("DST_PORT","11234","DST_LOCAL","true","PROTOCOL","TCP",ClientControl.hostIP,11234))
        clientControl.runCommand("nohup netcat -l -p 11234 >/dev/null 2>&1",False,True)
        result = clientControl.runCommand("echo test | netcat -q0 %s 11234" % uvmContext.networkManager().getFirstWanAddress())
        print "result: %s" % str(result) 
        assert(result == 0)

    # test port forward to multiple ports (tcp port 80,443)
    def test_027_portForwardMultiport(self):
        nukePortForwardRules()
        appendForward(createPortForwardTripleCondition("DST_PORT","80,443","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,None))
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -q -O - http://1.2.3.4/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -q --no-check-certificate -O - https://1.2.3.4/test/testPage1.html 2>&1 | grep -q text123")
        assert(result == 0)

    # test a port forward from outside if possibel
    def test_030_portForwardInbound(self):
        # We will use 10.5.6.71 for this test. Test to see if we can reach it.
        externalClientResult = subprocess.call(["ping","-c","1","10.5.6.71"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (externalClientResult != 0):
            raise unittest2.SkipTest("External test client unreachable, skipping alternate port forwarding test")
        # Also test that it can probably reach us (we're on a 10.x network)
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (wan_IP.split(".")[0] != "10"):
            raise unittest2.SkipTest("Not on 10.x network, skipping")

        # start netcat on client
        clientControl.runCommand("nohup netcat -l -p 11245 >/dev/null 2>&1",False,True)

        # port forward 11245 to client box
        appendForward(createPortForwardTripleCondition("DST_PORT","11245","DST_LOCAL","true","PROTOCOL","TCP",ClientControl.hostIP,"11245"))

        # try connecting to netcat on client from "outside" box
        tmp_hostIP = ClientControl.hostIP
        ClientControl.hostIP = "10.5.6.71"
        result = clientControl.runCommand("echo test | netcat -q0 %s 11245" % uvmContext.networkManager().getFirstWanAddress())
        ClientControl.hostIP = tmp_hostIP
        assert (result == 0)

    # Test that QoS limits speed
    def test_050_enableQoS(self):
        netsettings = uvmContext.networkManager().getNetworkSettings()
        if netsettings['qosSettings']['qosEnabled']:
            netsettings['qosSettings']['qosEnabled'] = False
            uvmContext.networkManager().setNetworkSettings(netsettings)            

        wget_speed_pre_QoSLimit = getDownloadSpeed()
        # set limit to 80% of measured speed
        wanLimit = int((wget_speed_pre_QoSLimit*8) * .8)
        
        netsettings['qosSettings']['qosEnabled'] = True
        i = 0
        for interface in netsettings['interfaces']['list']:
            if interface['isWan']:
                netsettings['interfaces']['list'][i]['downloadBandwidthKbps']=wanLimit
                netsettings['interfaces']['list'][i]['uploadBandwidthKbps']=wanLimit
            i += 1
        uvmContext.networkManager().setNetworkSettings(netsettings)
        wget_speed_post_QoSLimit= getDownloadSpeed()
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        print "Result of wget_speed_pre_QoSLimit <%s> wget_speed_post_QoSLimit <%s>" % (wget_speed_pre_QoSLimit,wget_speed_post_QoSLimit)
        assert ((wget_speed_pre_QoSLimit) and (wget_speed_post_QoSLimit))
        # since the limit is 80% of first measure, check that second measure is < 90% of first measure
        assert (wget_speed_pre_QoSLimit * .9 >  wget_speed_post_QoSLimit)

    # Test that bypass rules bypass apps
    def test_060_bypassRules(self):
        nukeBypassRules()
        global nodeFW
        if nodeFW == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeNameFW())):
                print "ERROR: Node %s already installed" % self.nodeNameFW()
                raise Exception('node %s already instantiated' % self.nodeNameFW())
            nodeFW = uvmContext.nodeManager().instantiateAndStart(self.nodeNameFW(), defaultRackId)
        # verify port 80 is open
        result = clientControl.runCommand("wget -o /dev/null http://test.untangle.com/")
        assert (result == 0)
        # Block port 80 and verify it's closed
        appendFWRule(createSingleMatcherRule("DST_PORT","80"))
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        # bypass the client and verify the client can bypass the firewall
        appendBypass(createBypassMatcherRule("SRC_ADDR",ClientControl.hostIP))
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result == 0)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        uvmContext.nodeManager().destroy( nodeFW.getNodeSettings()["id"] )

    # Test static route that routing playboy.com to 127.0.0.1 makes it unreachable
    def test_070_routes(self):        
        nukeRoutes()
        clientControl.runCommand("rm -f /tmp/network_test_070a.log")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        result = clientControl.runCommand("host www.playboy.com", True)
        # print "result <%s>" % result
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_playboy = (match.group()).replace('address ','')
        appendRouteRule(createRouteRule(ip_address_playboy,32,"127.0.0.1"))
        # verify other sites are still available.
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://test.untangle.com")
        assert (result == 0)
        # Verify playboy is not accessible 
        result = clientControl.runCommand("wget -o /dev/null -t 1 --timeout=3 http://www.playboy.com")
        assert (result != 0)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

    # Test static DNS entry
    def test_080_DNS(self):        
        # Test static entries in Config -> Networking -> Advanced -> DNS
        nukeDNSRules()
        result = clientControl.runCommand("host test.untangle.com", True)
        # print "result <%s>" % result
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_testuntangle = (match.group()).replace('address ','')
        # print "IP address of test.untangle.com <%s>" % ip_address_testuntangle
        appendDNSRule(createDNSRule(ip_address_testuntangle,"www.foobar.com"))
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        print "wan_IP <%s>" % wan_IP

        # allow DNS on the WAN
        netsettings = uvmContext.networkManager().getNetworkSettings()
        i = 0
        for packetFilter in netsettings['inputFilterRules']['list']:
            if packetFilter['description'] == "Allow DNS on non-WANs":
                j = 0
                for pktRule in packetFilter['matchers']['list']:
                    if pktRule["matcherType"] == "SRC_INTF":
                        netsettings['inputFilterRules']['list'][i]['matchers']['list'][j]["value"] = "non_wan,wan"
                    j += 1
            i += 1
        uvmContext.networkManager().setNetworkSettings(netsettings)

        result = clientControl.runCommand("host -4 www.foobar.com " + wan_IP, True)
        # print "Results of www.foobar.com <%s>" % result
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        ip_address_foobar = (match.group()).replace('address ','')
        # print "IP address of www.foobar.com <%s>" % ip_address_foobar
        # print "IP address of test.untangle.com <%s>" % ip_address_testuntangle
        print "Result DNS lookup 1:\"%s\" 2:\"%s\"" % (str(ip_address_testuntangle),str(ip_address_foobar))
        assert(ip_address_testuntangle == ip_address_foobar)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        
    # Test dynamic hostname
    def test_090_DynamicDns(self):
        # Set DynDNS info
        setDynDNS()
        time.sleep(60) # wait a max of 1 minute for dyndns to update.
        outsideIP = clientControl.runCommand("wget -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",True)
        result = clientControl.runCommand("host testuntangle.dyndns-pics.com", True)
        match = re.search(r'\d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result)
        dynIP = (match.group()).replace('address ','')
        # print "IP address of outsideIP <%s> dynIP <%s> " % (outsideIP,dynIP)
        assert(outsideIP == dynIP)
        nukeDynDNS()
        
    def test_999_finalTearDown(self):
        global node,nodeFW
        # Restore original settings to return to initial settings
        # print "orig_netsettings <%s>" % orig_netsettings
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        # In case firewall is still installed.
        if (uvmContext.nodeManager().isInstantiated(self.nodeNameFW())):
            uvmContext.nodeManager().destroy( nodeFW.getNodeSettings()["id"] )
        node = None
        nodeFW = None


TestDict.registerNode("network", NetworkTests)
