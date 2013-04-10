import unittest2
import os
import sys
reload(sys)
sys.setdefaultencoding("utf-8")
import subprocess
import ipaddr
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import TestDict
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
savedHostIP = clientControl.hostIP

external_client = "10.5.6.71" # ATS Radius server

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
                    "value": "True"
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
                    "value": "True"
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

def appendForward(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['portForwardRules']['list'].append(newRule);
    uvmContext.networkManager().setNetworkSettings(netsettings)

class NetworkTests(unittest2.TestCase):
    print "top client %s" % (savedHostIP)

    @staticmethod
    def nodeName():
        return "network"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node,savedHostIP
        savedHostIP = clientControl.hostIP
        print "setUp client %s" % (ClientControl.hostIP)
            
    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_010.log -O /tmp/capture_test_010.out http://www.untangle.com/")
        assert (result == 0)

    def test_020_enableQoS(self):
        global orig_netstatings
        orig_netstatings = netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['qosSettings']['qosEnabled'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)
        # wan_interface = uvmContext.networkManager().findInterfaceFirstWan()
        # print wan_interface['interfaceId']
        i = 0;
        for interface in netsettings['interfaces']['list']:
            if interface['isWan']:
                netsettings['interfaces']['list'][i]['downloadBandwidthKbps']=10000
                netsettings['interfaces']['list'][i]['uploadBandwidthKbps']=10000
            i += 1
        uvmContext.networkManager().setNetworkSettings(netsettings)
        netsettings['qosSettings']['qosEnabled'] = True
        print "webResult client %s" % (clientControl.hostIP)

    webResult = clientControl.runCommand("netstat -an | grep -q :80")
    @unittest2.skipIf(webResult != 0,  "No web server running on client")
        
    def test_030_port80Forward(self):
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        # port forward 80 to client box
        appendForward(createPortForwardLocalMatcherRule("DST_PORT","80",ClientControl.hostIP))
        tmp_hostIP = clientControl.hostIP
        # switch client to external box
        clientControl.hostIP = external_client
        result = clientControl.runCommand("wget -a /tmp/network_test_030a.log -O /tmp/network_test_030a.out  \'http://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'works!' /tmp/network_test_030a.out")  # check for default apache web page
        assert (search == 0)
        clientControl.hostIP = tmp_hostIP
        # check if hairpin works
        result = clientControl.runCommand("wget -a /tmp/network_test_030b.log -O /tmp/network_test_030b.out  \'http://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'works!' /tmp/network_test_030b.out")  # check for default apache web page
        assert (search == 0)
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)

    webResult = clientControl.runCommand("netstat -an | grep -q :443")
    @unittest2.skipIf(webResult != 0,  "No ssl web server running on client")

    def test_040_port443Forward(self):
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        # Move Admin port 443 to 4443
        adminsettings = uvmContext.settingsManager().getSettings()
        adminsettings['httpsPort']="4443"
        uvmContext.settingsManager().setSettings(adminsettings)
        # port forward 443 to client box
        appendForward(createPortForwardLocalMatcherRule("DST_PORT","443",ClientControl.hostIP))
        tmp_hostIP = ClientControl.hostIP
        # switch client to external box
        ClientControl.hostIP = external_client
        result = clientControl.runCommand("wget --no-check-certificate  -a /tmp/network_test_040a.log -O /tmp/network_test_040a.out  \'https://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'works!' /tmp/network_test_040a.out")  # check for default apache web page
        assert (search == 0)
        ClientControl.hostIP = tmp_hostIP
        # check if hairpin works
        result = clientControl.runCommand("wget --no-check-certificate  -a /tmp/network_test_040b.log -O /tmp/network_test_040b.out  \'https://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'works!' /tmp/network_test_040b.out")  # check for default apache web page
        assert (search == 0)
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)
        # Move Admin port back to 443
        adminsettings = uvmContext.settingsManager().getSettings()
        adminsettings['httpsPort']="443"
        uvmContext.settingsManager().setSettings(adminsettings)

    def test_050_portForwardAlt(self):
        # port forward to a different port that the incoming port.
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        # port forward 80 to client box
        appendForward(createPortForwardNewPortMatcherRule("DST_PORT","80",ClientControl.hostIP,"8080"))
        # Start remote web server on 8080
        clientControl.runCommand("python -m SimpleHTTPServer 8080 &")
        tmp_hostIP = ClientControl.hostIP
        # switch client to external box
        ClientControl.hostIP = external_client
        result = clientControl.runCommand("wget -a /tmp/network_test_050a.log -O /tmp/network_test_050a.out  \'http://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'Directory listing' /tmp/network_test_050a.out")  
        assert (search == 0)
        ClientControl.hostIP = tmp_hostIP
        # check if hairpin works
        result = clientControl.runCommand("wget -a /tmp/network_test_050b.log -O /tmp/network_test_050b.out  \'http://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'Directory listing' /tmp/network_test_050b.out")  # check for default apache web page
        assert (search == 0)
        # kill the 8080 web server
        clientControl.runCommand(" kill $!" ,True)
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)

    def test_999_finalTearDown(self):
        global node,orig_netstatings
        # Restore original settings to return to initial settings
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)
        node = None
        return True


TestDict.registerNode("network", NetworkTests)
