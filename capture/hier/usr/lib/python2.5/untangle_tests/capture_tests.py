import unittest
import time
import sys
import pdb
import os
import re
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests import TestDict
from untangle_tests import SystemProperties

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
systemProperties = SystemProperties()
nodeData = None
node = None
nodeDataAD = None
nodeAD = None

#  /usr/share/untangle/web/webui/script/untangle-node-adconnector/settings.js
#pdb.set_trace()

def createCaptureInternalNicRule():
    return {
        "capture": True,
        "description": "Test Rule - Capture all internal traffic",
        "enabled": True,
        "id": 1,
        "javaClass": "com.untangle.node.capture.CaptureRule",
        "matchers": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "invert": False,
                "javaClass": "com.untangle.node.capture.CaptureRuleMatcher",
                "matcherType": "SRC_INTF",
                "value": "2"
                }]
            },
        "ruleId": 1
    };


def createCaptureLoginADSettings():
    return {
    "authenticationType": "ACTIVE_DIRECTORY",
    "basicLoginFooter": "If you have any questions, please contact your network administrator.",
    "basicLoginMessageText": "Please enter your username and password to connect to the internet.",
    "basicLoginPageTitle": "Captive Portal",
    "basicLoginPageWelcome": "Welcome to the Untangle Captive Portal",
    "basicLoginPassword": "Password:",
    "basicLoginUsername": "Username:",
    "pageType": "BASIC_LOGIN",
    };

def createADSettings():
    return {
       "activeDirectorySettings": {
            "LDAPHost": "10.5.6.48",
            "LDAPPort": 389,
            "OUFilter": "",
            "domain": "adtesting.int",
            "enabled": True,
            "javaClass": "com.untangle.node.adconnector.ActiveDirectorySettings",
            "superuser": "ATSadmin",
            "superuserPass": "passwd"
        },
        "radiusSettings": {
            "port": 1812, 
            "enabled": False, 
            "authenticationMethod": "PAP", 
            "javaClass": "com.untangle.node.adconnector.RadiusSettings", 
            "server": "1.2.3.4", 
            "sharedSecret": "mysharedsecret"
        }
    }

def nukeRules():
    rules = node.getRules()
    rules["list"] = [];
    node.setRules(rules);

class CaptureTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-capture"

    @staticmethod
    def nodeNameAD():
        return "untangle-node-adconnector"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global nodeData, node, nodeDataAD, nodeAD
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), defaultRackId)
            nodeData = node.getCaptureSettings()
        if nodeAD == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeNameAD())):
                print "ERROR: Node %s already installed" % self.nodeNameAD()
                raise Exception('node %s already instantiated' % self.nodeNameAD())
            nodeAD = uvmContext.nodeManager().instantiateAndStart(self.nodeNameAD(), defaultRackId)
            nodeDataAD = nodeAD.getActiveDirectorySettings()

    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_010.log -O /tmp/capture_test_010.out http://www.untangle.com/")
        assert (result == 0)

    def test_020_defaultTrafficCheck(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_020.log -O /tmp/capture_test_020.out http://www.google.com/")
        assert (result == 0)

    def test_021_captureTrafficCheck(self):
        global node, nodeData
        nodeData['captureRules']['list'].append(createCaptureInternalNicRule())
        node.setSettings(nodeData)
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_021.log -O /tmp/capture_test_021.out http://www.google.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Captive Portal' /tmp/capture_test_021.out")
        assert (search == 0)

    def test_030_captureADLogin(self):
        global nodeData, node, nodeDataAD, nodeAD
        # Configure AD settings
        testResultString = nodeAD.getActiveDirectoryManager().getActiveDirectoryStatusForSettings(createADSettings())
        # print 'testResultString %s' % testResultString  # debug line
        nodeAD.setSettings(createADSettings())
        assert ("success" in testResultString)
        # Create Internal NIC capture rule with basic AD login page
        nodeData['authenticationType']="ACTIVE_DIRECTORY"
        nodeData['pageType'] = "BASIC_LOGIN"
        node.setSettings(nodeData)
        # check that basic captive page is shown
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_030.log -O /tmp/capture_test_030.out http://www.google.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'username and password' /tmp/capture_test_030.out")
        assert (search == 0)
        # print 'Login page found'  # debug line
        # check if AD login and password 
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        # get the IP address of the capture page 
        gatewayIPAddress = systemProperties.internalInterfaceIP()
        # print 'gatewayIPAddress is %s' % gatewayIPAddress
        result = clientControl.runCommand("wget -a /tmp/capture_test_030a.log -O /tmp/capture_test_030a.out  \'http://" + gatewayIPAddress + "/capture/handler.py/authpost?username=atsadmin&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'",True)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_030a.out")
        assert (search == 0)

    def test_999_finalTearDown(self):
        global node, nodeAD
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        uvmContext.nodeManager().destroy( nodeAD.getNodeSettings()["id"] )
        node = None
        nodeAD = None

TestDict.registerNode("capture", CaptureTests)

