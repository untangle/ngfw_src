import unittest2
import os
import sys
reload(sys)
sys.setdefaultencoding("utf-8")
import re
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
# ATS Radius server
external_client = "10.5.6.71" 

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

def appendForward(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['portForwardRules']['list'].append(newRule);
    uvmContext.networkManager().setNetworkSettings(netsettings)

class NetworkTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "network"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node,orig_netstatings
        orig_netstatings = uvmContext.networkManager().getNetworkSettings()
        
    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_010.log -O /tmp/capture_test_010.out http://www.untangle.com/")
        assert (result == 0)

    def test_020_enableQoS(self):
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Record average speed with QoS at 10M configured
        # Download file and record the average speed in which the file was download
        # remove previous test file and log
        clientControl.runCommand("rm -f 5MB.zip /tmp/network_test_020a.log")
        result = clientControl.runCommand("wget -o /tmp/network_test_020a.log http://test.untangle.com/5MB.zip")
        result = clientControl.runCommand("tail -2 /tmp/network_test_020a.log", True)
        match = re.search(r'\d+\.\d{1,2}', result)
        wget_speed_pre_QoSLimit =  match.group()
        # cast string to float for comparsion.
        wget_speed_pre_QoSLimit = float(wget_speed_pre_QoSLimit)
        netsettings['qosSettings']['qosEnabled'] = True
        i = 0
        for interface in netsettings['interfaces']['list']:
            if interface['isWan']:
                netsettings['interfaces']['list'][i]['downloadBandwidthKbps']=1000
                netsettings['interfaces']['list'][i]['uploadBandwidthKbps']=1000
            i += 1
        uvmContext.networkManager().setNetworkSettings(netsettings)
        clientControl.runCommand("rm -f 5MB.zip /tmp/network_test_020b.log")
        result = clientControl.runCommand("wget -o /tmp/network_test_020b.log http://test.untangle.com/5MB.zip")
        result = clientControl.runCommand("tail -2 /tmp/network_test_020b.log", True)
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)
        # remove test file
        match = re.search(r'\d+\.\d{1,2}', result)
        wget_speed_post_QoSLimit =  match.group()
        wget_speed_post_QoSLimit = float(wget_speed_post_QoSLimit)
        print "Result of wget_speed_pre_QoSLimit <%s> wget_speed_post_QoSLimit <%s>" % (wget_speed_pre_QoSLimit,wget_speed_post_QoSLimit)
        assert ((wget_speed_post_QoSLimit != 0) and (wget_speed_post_QoSLimit < 500))
        assert (wget_speed_pre_QoSLimit <  wget_speed_post_QoSLimit)

    # webResult = clientControl.runCommand("netstat -an | grep -q :80")
    # @unittest2.skipIf(webResult != 0,  "No web server running on client")
        
    def test_030_port80Forward(self):
        clientControl.runCommand("rm -f /tmp/network_test_030*")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        # port forward 80 to client box
        appendForward(createPortForwardLocalMatcherRule("DST_PORT","80",ClientControl.hostIP))
        tmp_hostIP = clientControl.hostIP
        # switch client to external box
        clientControl.hostIP = external_client
        result = clientControl.runCommand("wget -a /tmp/network_test_030a.log -O /tmp/network_test_030a.out -t 1 \'http://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'works!' /tmp/network_test_030a.out")  # check for default apache web page
        assert (search == 0)
        clientControl.hostIP = tmp_hostIP
        # check if hairpin works
        result = clientControl.runCommand("wget -a /tmp/network_test_030b.log -O /tmp/network_test_030b.out -t 1 \'http://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'works!' /tmp/network_test_030b.out")  # check for default apache web page
        assert (search == 0)
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)

    # webResult = clientControl.runCommand("netstat -an | grep -q :443")
    # @unittest2.skipIf(webResult != 0,  "No ssl web server running on client")

    def test_040_port443Forward(self):
        clientControl.runCommand("rm -f /tmp/network_test_040*")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        # Move Admin port 443 to 4443
        adminsettings = uvmContext.systemManager().getSettings()
        adminsettings['httpsPort']="4443"
        uvmContext.systemManager().setSettings(adminsettings)
        # port forward 443 to client box
        appendForward(createPortForwardLocalMatcherRule("DST_PORT","443",ClientControl.hostIP))
        tmp_hostIP = ClientControl.hostIP
        # switch client to external box
        ClientControl.hostIP = external_client
        result = clientControl.runCommand("wget --no-check-certificate  -a /tmp/network_test_040a.log -O /tmp/network_test_040a.out -t 1 \'https://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'works!' /tmp/network_test_040a.out")  # check for default apache web page
        assert (search == 0)
        ClientControl.hostIP = tmp_hostIP
        clientControl.runCommand("rm -f /tmp/network_test_040*")
        # check if hairpin works
        result = clientControl.runCommand("wget --no-check-certificate  -a /tmp/network_test_040b.log -O /tmp/network_test_040b.out -t 1 \'https://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'works!' /tmp/network_test_040b.out")  # check for default apache web page
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)
        # Move Admin port back to 443
        adminsettings = uvmContext.systemManager().getSettings()
        adminsettings['httpsPort']="443"
        uvmContext.systemManager().setSettings(adminsettings)
        assert (search == 0)

    def test_050_portForwardAlt(self):
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
        ClientControl.hostIP
        ClientControl.hostIP = external_client
        clientControl.runCommand("rm -f /tmp/network_test_050*")
        result = clientControl.runCommand("wget -a /tmp/network_test_050a.log -O /tmp/network_test_050a.out -t 4 -T 20 \'http://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'Directory listing' /tmp/network_test_050a.out")  
        assert (search == 0)
        ClientControl.hostIP = tmp_hostIP

        # check if hairpin works
        result = clientControl.runCommand("wget -a /tmp/network_test_050b.log -O /tmp/network_test_050b.out -t 4 -T 20 \'http://" + wan_IP + "\'" ,True)
        search = clientControl.runCommand("grep -q 'Directory listing' /tmp/network_test_050b.out")  # check for default apache web page
        assert (search == 0)
        # kill the 8080 web server
        clientControl.runCommand("kill \$(pgrep python)")
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)

    def test_999_finalTearDown(self):
        global node,orig_netstatings
        # Restore original settings to return to initial settings
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)
        node = None
        return True


TestDict.registerNode("network", NetworkTests)
