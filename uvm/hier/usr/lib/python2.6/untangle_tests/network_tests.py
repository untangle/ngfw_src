import socket
import unittest2
import os
import sys
reload(sys)
sys.setdefaultencoding("utf-8")
import re
import subprocess
import ipaddr
import time
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import TestDict
from untangle_tests import ClientControl

node = None
nodeFW = None
radiusServer = "10.111.56.71"
ftp_server = "test.untangle.com"
ftp_file_name = "test.zip"

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

def createQoSMatcherRule( matcherType, value, priority):
    return {
        "bypass": True, 
        "description": "test QoS " + str(matcherType) + " " + str(value), 
        "enabled": True, 
        "javaClass": "com.untangle.uvm.network.QosRule", 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.QosRuleMatcher", 
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
        "priority": priority,
        "ruleId": 3
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

def appendQoSRule(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['qosSettings']['qosRules']['list'].append(newRule)
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
    
def getUDPSpeed():
    # Use mgen to get UDP speed.  Returns number of packets received.
    # start mgen receiver on radius server.
    os.system("rm mgen_recv.dat >/dev/null 2>&1")
    os.system("ssh -o 'StrictHostKeyChecking=no' -i /usr/lib/python2.6/untangle_tests/testShell.key testshell@" + radiusServer + " \"rm mgen_recv.dat >/dev/null 2>&1\"")
    os.system("ssh -o 'StrictHostKeyChecking=no' -i /usr/lib/python2.6/untangle_tests/testShell.key testshell@" + radiusServer + " \"/home/fnsadmin/MGEN/mgen output mgen_recv.dat port 5000 >/dev/null 2>&1 &\"")
    # start the UDP generator on the client behind the Untangle.
    clientControl.runCommand("mgen input /home/testshell/udp-load-ats.mgn txlog log mgen_snd.log >/dev/null 2>&1")
    # wait for UDP to finish
    time.sleep(70)
    # kill mgen receiver    
    os.system("ssh -o 'StrictHostKeyChecking=no' -i /usr/lib/python2.6/untangle_tests/testShell.key testshell@" + radiusServer + " \"pkill mgen >/dev/null 2>&1\"")
    os.system("scp -o 'StrictHostKeyChecking=no' -i /usr/lib/python2.6/untangle_tests/testShell.key testshell@" + radiusServer + ":mgen_recv.dat ./ >/dev/null 2>&1")
    wcResults = subprocess.Popen(["wc","-l","mgen_recv.dat"], stdout=subprocess.PIPE).communicate()[0]
    # print "wcResults " + str(wcResults)
    numOfPackets = wcResults.split(' ')[0]
    return numOfPackets
    

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

    # test a port forward from outside if possible
    def test_030_portForwardInbound(self):
        # We will use radiusServer for this test. Test to see if we can reach it.
        externalClientResult = subprocess.call(["ping","-c","1",radiusServer],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
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
        ClientControl.hostIP = radiusServer
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

    # Test UDP QoS limits speed
    def test_053_testUDPwithQoS(self):
        externalClientResult = subprocess.call(["ping","-c","1",radiusServer],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (externalClientResult != 0):
            raise unittest2.SkipTest("External test client unreachable, skipping UDP with QoS test")
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (wan_IP.split(".")[0] != "10"):
            raise unittest2.SkipTest("Not on 10.x network, skipping")            
        mgenResult = clientControl.runCommand("test -x /usr/bin/mgen")
        if mgenResult:
            # http://www.nrl.navy.mil/itd/ncs/products/mgen
            raise unittest2.SkipTest("Mgen app needs to be installed on client")

        netsettings = uvmContext.networkManager().getNetworkSettings()
        if not netsettings['qosSettings']['qosEnabled']:
            netsettings['qosSettings']['qosEnabled'] = True
            uvmContext.networkManager().setNetworkSettings(netsettings)
        appendBypass(createBypassMatcherRule("DST_PORT","5000"))
        appendQoSRule(createQoSMatcherRule("DST_PORT","5000", 1))
        pre_UDP_packets = getUDPSpeed()

        # Change UDP priority to limited
        netsettings = uvmContext.networkManager().getNetworkSettings()
        i = 0
        for qosCustomRules in netsettings['qosSettings']['qosRules']['list']:
            if qosCustomRules['description'] == "test QoS DST_PORT 5000":
                for qosRule in qosCustomRules['matchers']['list']:
                    if qosRule["value"] == "5000":
                        netsettings['qosSettings']['qosRules']['list'][i]['priority'] = 7
            i += 1
        uvmContext.networkManager().setNetworkSettings(netsettings)
        post_UDP_packets = getUDPSpeed()
        # print "Pre UDP packets " + str(pre_UDP_packets) + " post_UDP_packets " + str(post_UDP_packets)
        
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert (pre_UDP_packets >  post_UDP_packets)

    # Test that bypass rules bypass apps
    def test_060_bypassRules(self):
        global nodeFW
        if nodeFW == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeNameFW())):
                print "ERROR: Node %s already installed" % self.nodeNameFW()
                raise Exception('node %s already instantiated' % self.nodeNameFW())
            nodeFW = uvmContext.nodeManager().instantiate(self.nodeNameFW(), defaultRackId)
        nukeBypassRules()
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

    # Test FTP in active and passive modes
    def test_065_ftpModes(self):
        nukeBypassRules()
        clientControl.runCommand("rm /tmp/network_065a_ftp_file /tmp/network_065b_ftp_file >/dev/null 2>&1")
        # passive
        result = clientControl.runCommand("wget --timeout=30 -q -O /tmp/network_065a_ftp_file ftp://" + ftp_server + "/" + ftp_file_name)
        assert (result == 0)
        # active
        result = clientControl.runCommand("wget --timeout=30 --no-passive-ftp -q -O /tmp/network_065b_ftp_file ftp://" + ftp_server + "/" + ftp_file_name)
        assert (result == 0)

    # Test FTP in active and passive modes with bypass
    def test_066_bypassFtpModes(self):
        nukeBypassRules()
        appendBypass(createBypassMatcherRule("SRC_ADDR",ClientControl.hostIP))
        # --no-passive-ftp
        clientControl.runCommand("rm /tmp/network_066a_ftp_file /tmp/network_066b_ftp_file >/dev/null 2>&1")
        # passive
        result = clientControl.runCommand("wget --timeout=30 -q -O /tmp/network_066_ftp_file ftp://" + ftp_server + "/" + ftp_file_name)
        assert (result == 0)
        # active
        result = clientControl.runCommand("wget --timeout=30 --no-passive-ftp -q -O /tmp/network_066_ftp_file ftp://" + ftp_server + "/" + ftp_file_name)
        assert (result == 0)
        nukeBypassRules()

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
        print "IP address of outsideIP <%s> dynIP <%s> " % (outsideIP,dynIP)
        nukeDynDNS()
        # assert(outsideIP == dynIP)

    # Test VRRP is active
    def test_100_VRRP(self):
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Find a static interface
        i=0
        for interface in netsettings['interfaces']['list']:
            if interface['v4ConfigType'] == "STATIC":
                break
            i += 1
        # Verify interface is found
        if (netsettings['interfaces']['list'][i]['v4ConfigType'] != "STATIC"):
            raise unittest2.SkipTest("No static interface found")
        interfaceIP = netsettings['interfaces']['list'][i]['v4StaticAddress']
        interfacePrefix = netsettings['interfaces']['list'][i]['v4StaticPrefix']
        interfaceNet = interfaceIP + "/" + str(interfacePrefix)
        # get next IP not used
        ipStep = 1
        loopCounter = 10
        vrrpIP = None
        while vrrpIP == None and loopCounter:
            # get next IP and test that it is unused
            ip = ipaddr.IPAddress(interfaceIP)
            newip = ip + ipStep
            # check to see if the IP is in network range
            if newip in ipaddr.IPv4Network(interfaceNet):
                pingResult = clientControl.runCommand("ping -c 1 %s >/dev/null 2>&1" % str(newip))
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
        netsettings['interfaces']['list'][i]['vrrpAddress'] = str(vrrpIP)
        netsettings['interfaces']['list'][i]['vrrpEnabled'] = True
        netsettings['interfaces']['list'][i]['vrrpId'] = 2
        netsettings['interfaces']['list'][i]['vrrpPriority'] = 1
        uvmContext.networkManager().setNetworkSettings(netsettings)
        time.sleep(60)
        # Test that the VRRP is pingable
        pingResult = clientControl.runCommand("ping -c 1 %s >/dev/null 2>&1" % str(vrrpIP))
        # check if still online
        onlineResults = clientControl.runCommand("wget -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        # Return to default network state
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert (pingResult == 0)
        assert (onlineResults == 0)
        
    # Test MTU settings
    def test_110_MTU(self):
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
        
    # Verify SNMP
    def test_120_SNMP(self):
        snmpwalkResult = clientControl.runCommand("test -x /usr/bin/snmpwalk")  
        if snmpwalkResult:
            raise unittest2.SkipTest("Snmpwalk app needs to be installed on client")
        origsystemSettings = uvmContext.systemManager().getSettings()
        systemSettings = uvmContext.systemManager().getSettings()
        systemSettings['snmpSettings']['communityString'] = "atstest"
        systemSettings['snmpSettings']['enabled'] = True
        systemSettings['snmpSettings']['port'] = 161
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['sysContact'] = "qa@untangle.com"
        systemSettings['snmpSettings']['sendTraps'] = True
        systemSettings['snmpSettings']['trapHost'] = ClientControl.hostIP
        uvmContext.systemManager().setSettings(systemSettings)
        result = clientControl.runCommand("snmpwalk -v 2c -c atstest 192.168.10.61 | grep untangle >/dev/null 2>&1")
        uvmContext.systemManager().setSettings(origsystemSettings)
        assert(result == 0)

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
