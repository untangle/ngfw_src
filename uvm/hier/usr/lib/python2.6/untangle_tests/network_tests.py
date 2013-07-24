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
# ATS Radius server
external_client = "10.5.6.71" 
dogfood = "10.0.0.1"
dogfood_alt = "10.0.0.2"
orig_netsettings = None

def createPortForwardLocalMatcherRule( matcherType, value, destinationIP):
    matcherTypeStr = str(matcherType)
    valueStr = str(value)
    return {
        "description": "forward " + matcherTypeStr + value + " test", 
        "enabled": True, 
        "javaClass": "com.untangle.uvm.network.PortForwardRule", 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": "DST_LOCAL", 
                    "value": "true"
                }, 
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": matcherTypeStr,
                    "value": value
                }, 
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": "PROTOCOL", 
                    "value": "TCP,UDP"
                }
            ]
        }, 
        "newDestination": destinationIP,
        "ruleId": 1
    } 

def createPortForwardNewPortMatcherRule( matcherType, value, destinationIP, destinationPort):
    matcherTypeStr = str(matcherType)
    valueStr = str(value)
    return {
        "description": "forward " + matcherTypeStr + " " + value + " test", 
        "enabled": True, 
        "javaClass": "com.untangle.uvm.network.PortForwardRule", 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": "DST_LOCAL", 
                    "value": "true"
                }, 
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": matcherTypeStr,
                    "value": value
                }, 
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": "PROTOCOL", 
                    "value": "TCP,UDP"
                }
            ]
        }, 
        "newDestination": destinationIP,
        "newPort": destinationPort,
        "ruleId": 1
    } 

def createBypassMatcherRule( matcherType, value):
    matcherTypeStr = str(matcherType)
    valueStr = str(value)
    return {
        "bypass": True, 
        "description": "test bypass", 
        "enabled": True, 
        "javaClass": "com.untangle.uvm.network.BypassRule", 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.BypassRuleMatcher", 
                    "matcherType": matcherTypeStr, 
                    "value": value
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
    matcherTypeStr = str(matcherType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + matcherTypeStr + " = " + valueStr, 
        "flag": flagged, 
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
    
def nukeFWRules():
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

def nukeRouteRules():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['staticRoutes']['list'][:] = []
    uvmContext.networkManager().setNetworkSettings(netsettings)    

def isBridgeMode(clientIPAdress):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    for interface in netsettings['interfaces']['list']:
        if interface['isWan']:
            if interface['v4StaticGateway']:
                wanIP = interface['v4StaticGateway']
                wanNetmask = interface['v4StaticNetmask']
                systemProperties = system_props.SystemProperties()
                wanNetSize = systemProperties.get_net_size(wanNetmask)
                wanRange = wanIP + '/' + wanNetSize
                wanNet = ipaddr.IPNetwork(wanRange)
                wanAddr = ipaddr.IPAddress(clientIPAdress)
            elif (interface['v4ConfigType'] in ['AUTO','PPPOE']):
                # is this a dynamic IP w/o PPPOE
                nicDevice = str(interface['symbolicDev'])
                systemProperties = system_props.SystemProperties()
                wanIP = systemProperties.get_ip_address(nicDevice)
                wanNetmask =  systemProperties.get_netmask(nicDevice)
                wanRange = wanIP + '/' + systemProperties.get_net_size(wanNetmask)
                wanNet = ipaddr.IPNetwork(wanRange)
                wanAddr = ipaddr.IPAddress(clientIPAdress)
            else:
                raise unittest2.SkipTest("Unable to determine WAN IP")
            if wanAddr in wanNet:
                return True
            else:
                pass
        else:
            pass
    return False

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
        global orig_netsettings, utBridged, externalClientResult
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        # print "orig_netsettings <%s>" % orig_netsettings
        utBridged = isBridgeMode(ClientControl.hostIP)
        clientControl.runCommand("kill $(ps aux | grep SimpleHTTPServer | grep -v grep | awk '{print $2}') 2>/dev/null")
        externalClientResult = subprocess.call(["ping","-c","1",external_client],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        print "externalClientResult <%s>" % externalClientResult
        
    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        assert (result == 0)

    def test_020_enableQoS(self):
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Record average speed with QoS at 10M configured
        # Download file and record the average speed in which the file was download
        # remove previous test file and log
        netsettings['qosSettings']['qosEnabled'] = False
        uvmContext.networkManager().setNetworkSettings(netsettings)            
        wget_speed_pre_QoSLimit = getDownloadSpeed()
        
        netsettings['qosSettings']['qosEnabled'] = True
        i = 0
        for interface in netsettings['interfaces']['list']:
            if interface['isWan']:
                netsettings['interfaces']['list'][i]['downloadBandwidthKbps']=10000
                netsettings['interfaces']['list'][i]['uploadBandwidthKbps']=10000
            i += 1
        uvmContext.networkManager().setNetworkSettings(netsettings)
        wget_speed_post_QoSLimit= getDownloadSpeed()
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        print "Result of wget_speed_pre_QoSLimit <%s> wget_speed_post_QoSLimit <%s>" % (wget_speed_pre_QoSLimit,wget_speed_post_QoSLimit)
        assert ((wget_speed_pre_QoSLimit) and (wget_speed_post_QoSLimit))
        assert (wget_speed_pre_QoSLimit >  wget_speed_post_QoSLimit)

    def test_030_port80Forward(self):
        nukeFWRules()
        netstatResult = int(clientControl.runCommand("netstat -an | grep '0.0.0.0:80 ' | wc -l",True))
        print "netstatResult <%s>" % netstatResult
        if (netstatResult == 0):
            raise unittest2.SkipTest("No web server running on client, skipping port 80 forwarding test")
        if (externalClientResult != 0):
            raise unittest2.SkipTest("External test client unreachable, skipping port 80 forwarding test")
        clientControl.runCommand("rm -f /tmp/network_test_030*")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        # print "wan_IP <%s>" % wan_IP
        # port forward 80 to client box
        appendForward(createPortForwardLocalMatcherRule("DST_PORT","80",ClientControl.hostIP))
        tmp_hostIP = clientControl.hostIP
        # switch client to external box
        clientControl.hostIP = external_client
        resultUntangle = clientControl.runCommand("wget -SO- -T 1 -t 1 http://" + wan_IP + " 2>&1 | egrep -i welcome.do >/dev/null")
        resultWeb = clientControl.runCommand("wget -SO- -T 1 -t 1 http://" + wan_IP + " 2>&1 | egrep -i '200 OK' >/dev/null")
        # print resultUntangle
        # print resultWeb
        clientControl.hostIP = tmp_hostIP
        # Check that the Untangle page is not shown
        assert (resultUntangle != 0)
        # Check for HTTP code is 200 not the 302 (Untangle)
        assert (resultWeb == 0)
        # check if hairpin works only on non bridge setups
        if not utBridged:
            resultUntangle = clientControl.runCommand("wget -SO- -T 1 -t 1 http://" + wan_IP + " 2>&1 | egrep -i welcome.do >/dev/null")
            resultWeb = clientControl.runCommand("wget -SO- -T 1 -t 1 http://" + wan_IP + " 2>&1 | egrep -i '200 OK' >/dev/null")
            # Check that the Untangle page is not shown
            assert (resultUntangle != 0)
            # Check for HTTP code is 200 not the 302 (Untangle)
            assert (resultWeb == 0)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

    def test_040_port443Forward(self):
        netstatResult = int(clientControl.runCommand("netstat -an | grep 0.0.0.0:443 | wc -l",True))
        # print "netstatResult <%s>" % netstatResult
        if (netstatResult == 0):
            raise unittest2.SkipTest("No ssl web server running on client, skipping port 443 forwarding test")
        if (externalClientResult != 0):
            raise unittest2.SkipTest("External test client unreachable, skipping port 443 forwarding test")
        nukeFWRules()            
        clientControl.runCommand("rm -f /tmp/network_test_040*")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        # Move Admin port 443 to 4443
        netsettings['httpsPort'] = 4443
        uvmContext.networkManager().setNetworkSettings(netsettings)
        # port forward 443 to client box
        appendForward(createPortForwardLocalMatcherRule("DST_PORT","443",ClientControl.hostIP))
        tmp_hostIP = ClientControl.hostIP
        # switch client to external box
        ClientControl.hostIP = external_client
        resultUntangle = clientControl.runCommand("wget -SO- --no-check-certificate -T 1 -t 1 https://" + wan_IP + " 2>&1 | egrep -i welcome.do >/dev/null")
        resultWeb = clientControl.runCommand("wget -SO- --no-check-certificate -T 1 -t 1 https://" + wan_IP + " 2>&1 | egrep -i '200 OK' >/dev/null")
        ClientControl.hostIP = tmp_hostIP
        # Check that the Untangle page is not shown
        assert (resultUntangle != 0)
        # Check for HTTP code is 200 not the 302 (Untangle)
        assert (resultWeb == 0)
        clientControl.runCommand("rm -f /tmp/network_test_040*")
        # check if hairpin works
        if not utBridged:
            resultUntangle = clientControl.runCommand("wget -SO- --no-check-certificate -T 1 -t 1 https://" + wan_IP + " 2>&1 | egrep -i welcome.do >/dev/null")
            resultWeb = clientControl.runCommand("wget -SO- --no-check-certificate -T 1 -t 1 https://" + wan_IP + " 2>&1 | egrep -i '200 OK' >/dev/null")
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        # Move Admin port back to 443
        netsettings['httpsPort'] = 443
        uvmContext.networkManager().setNetworkSettings(netsettings)
        nukeFWRules()
        # Check that the Untangle page is not shown
        assert (resultUntangle != 0)
        # Check for HTTP code is 200 not the 302 (Untangle)
        assert (resultWeb == 0)

    def test_050_portForwardAlt(self):
        if (externalClientResult != 0):
            raise unittest2.SkipTest("External test client unreachable, skipping alternate port forwarding test")
        clientControl.runCommand("rm -f /tmp/network_test_050*")
        # port forward to a different port that the incoming port.
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        # Start remote web server on 8080
        clientControl.runCommand("rm -f index.html")
        clientControl.runCommand("nohup python -m SimpleHTTPServer 8080 2> /dev/null < /dev/null &",False,True)

        # port forward 80 to client box port 8080
        appendForward(createPortForwardNewPortMatcherRule("DST_PORT","80",ClientControl.hostIP,"8080"))
        tmp_hostIP = ClientControl.hostIP
        # switch client to external box
        ClientControl.hostIP = external_client
        clientControl.runCommand("rm -f /tmp/network_test_050*")
        result = clientControl.runCommand("wget -a /tmp/network_test_050a.log -O /tmp/network_test_050a.out -t 4 -T 20 \'http://" + wan_IP + "\'" ,True)
        # Check listing from python HTTP server
        search = clientControl.runCommand("grep -q 'Directory listing' /tmp/network_test_050a.out")  
        ClientControl.hostIP = tmp_hostIP
        assert (search == 0)

        # check if hairpin works
        # hairpin is not a valid test if on port 80 and in bridge mode
        if not utBridged:
            result = clientControl.runCommand("wget -a /tmp/network_test_050b.log -O /tmp/network_test_050b.out -t 4 -T 20 \'http://" + wan_IP + "\'" ,True)
            # Check listing from python HTTP server
            search = clientControl.runCommand("grep -q 'Directory listing' /tmp/network_test_050b.out")  # check for default apache web page
            assert (search == 0)
        # kill the 8080 web server
        clientControl.runCommand("kill $(ps aux | grep SimpleHTTPServer | grep -v grep | awk '{print $2}') 2>/dev/null")
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

    def test_060_bypassRules(self):
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

    def test_070_routes(self):        
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
        if utBridged:
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
        
    def test_999_finalTearDown(self):
        global node,nodeFW
        # Restore original settings to return to initial settings
        # print "orig_netsettings <%s>" % orig_netsettings
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        # In case firewall is still installed.
        if (uvmContext.nodeManager().isInstantiated(self.nodeNameFW())):
            uvmContext.nodeManager().destroy( nodeFW.getNodeSettings()["id"] )
        # In case test_050_portForwardAlt fails and leaves the python web server running
        clientControl.runCommand("kill $(ps aux | grep SimpleHTTPServer | grep -v grep | awk '{print $2}') 2>/dev/null")
        node = None
        nodeFW = None


TestDict.registerNode("network", NetworkTests)
