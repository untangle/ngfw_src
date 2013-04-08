import unittest2
import os
import sys
reload(sys)
sys.setdefaultencoding("utf-8")
import subprocess
import ipaddr
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests import TestDict
from untangle_tests import SystemProperties
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
systemProperties = SystemProperties()

external_client = "10.5.6.71"

def createPortForwardLocalMatcherRule( matcherType, value):
    matcherTypeStr = str(matcherType)
    valueStr = str(value)
    return {
        "description": "forward to" + matcherTypeStr + value + " test", 
        "enabled": true, 
        "javaClass": "com.untangle.uvm.network.PortForwardRule", 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": false, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": "DST_LOCAL", 
                    "value": "true"
                }, 
                {
                    "invert": false, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": matcherTypeStr,
                    "value": value
                }, 
                {
                    "invert": false, 
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleMatcher", 
                    "matcherType": "PROTOCOL", 
                    "value": "TCP,UDP"
                }
            ]
        }, 
        "newDestination": ClientControl.hostIP, 
        "ruleId": 1
    } 

def appendForward(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['interfaces']['list'].append(newRule);
    uvmContext.networkManager().setNetworkSettings(netsettings)

class NetworkTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "network"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node
        
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

        
    def test_030_portForward(self):
        netsettings = uvmContext.networkManager().getNetworkSettings()
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        appendRule(createPortForwardLocalMatcherRule("DST_PORT","80"))
        tmp_hostIP = ClientControl.hostIP
        ClientControl.hostIP = external_client
        result = clientControl.runCommand("wget -a /tmp/network_test_030a.log -O /tmp/network_test_030a.out  \'http://" + wan_IP ,True)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/network_test_030a.out")

    def test_999_finalTearDown(self):
        global node,orig_netstatings
        # Restore original settings to return to initial settings
        uvmContext.networkManager().setNetworkSettings(orig_netstatings)
        node = None
        return True


TestDict.registerNode("network", NetworkTests)
        