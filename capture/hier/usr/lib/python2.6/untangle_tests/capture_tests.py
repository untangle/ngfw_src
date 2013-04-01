import unittest2
import time
import sys
import pdb
import os
import re
import subprocess
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
adHost = "10.5.6.48"
radiusHost = "10.5.6.71"

# pdb.set_trace()

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
    # Need to send Radius setting even though it's not used in this case.
    return {
       "activeDirectorySettings": {
            "LDAPHost": adHost,
            "LDAPPort": 389,
            "OUFilter": "",
            "domain": "adtesting.int",
            "enabled": True,
            "javaClass": "com.untangle.node.adconnector.ActiveDirectorySettings",
            "superuser": "ATSadmin",
            "superuserPass": "passwd"},
        "radiusSettings": {
            "port": 1812, 
            "enabled": False, 
            "authenticationMethod": "PAP", 
            "javaClass": "com.untangle.node.adconnector.RadiusSettings", 
            "server": radiusHost, 
            "sharedSecret": "mysharedsecret"}
    }

def createRadiusSettings():
    return {
        "activeDirectorySettings": {
            "enabled": False, 
            "superuserPass": "passwd", 
            "LDAPPort": "389", 
            "OUFilter": "", 
            "domain": "adtest.metaloft.com", 
            "javaClass": "com.untangle.node.adconnector.ActiveDirectorySettings", 
            "LDAPHost": adHost, 
            "superuser": "Administrator"}, 
        "radiusSettings": {
            "port": 1812, 
            "enabled": True, 
            "authenticationMethod": "PAP", 
            "javaClass": "com.untangle.node.adconnector.RadiusSettings", 
            "server": radiusHost, 
            "sharedSecret": "chakas"}
        }

class CaptureTests(unittest2.TestCase):

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
        global nodeData, node, nodeDataRD, nodeDataAD, nodeAD
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), defaultRackId)
            nodeData = node.getCaptureSettings()
        if nodeAD == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeNameAD())):
                print "ERROR: Node %s already installed" % self.nodeNameAD()
                raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
            nodeAD = uvmContext.nodeManager().instantiateAndStart(self.nodeNameAD(), defaultRackId)
            nodeDataAD = nodeAD.getSettings().get('activeDirectorySettings')
            nodeDataRD = nodeAD.getSettings().get('radiusSettings')
        # remove previous temp files
        clientControl.runCommand("rm /tmp/capture_test_010.log /tmp/capture_test_010.out \
                                  /tmp/capture_test_020.log /tmp/capture_test_020.out \
                                  /tmp/capture_test_021.log /tmp/capture_test_021.out \
                                  /tmp/capture_test_030.log /tmp/capture_test_030.out \
                                  /tmp/capture_test_030a.log /tmp/capture_test_030a.out \
                                  /tmp/capture_test_030b.log /tmp/capture_test_030b.out \
                                  /tmp/capture_test_040.log /tmp/capture_test_040.out \
                                  /tmp/capture_test_040a.log /tmp/capture_test_040a.out \
                                  /tmp/capture_test_040b.log /tmp/capture_test_040b.out \
                                  ")

    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_010.log -O /tmp/capture_test_010.out http://www.untangle.com/")
        assert (result == 0)

    def test_020_defaultTrafficCheck(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_020.log -O /tmp/capture_test_020.out http://www.google.com/")
        assert (result == 0)

    def test_021_captureTrafficCheck(self):
        global node, nodeData, captureIP
        nodeData['captureRules']['list'].append(createCaptureInternalNicRule())
        node.setSettings(nodeData)
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_021.log -O /tmp/capture_test_021.out http://www.google.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Captive Portal' /tmp/capture_test_021.out")
        assert (search == 0)
        # get the IP address of the capture page 
        ipfind = clientControl.runCommand("grep 'Location' /tmp/capture_test_021.log",True)
        ip = re.findall( r'[0-9]+(?:\.[0-9]+){3}', ipfind )
        captureIP = ip[0]
        print 'Capture IP address is %s' % captureIP

    adResult = subprocess.call(["ping","-c","1",adHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    
    @unittest2.skipIf(adResult != 0,  "No AD server available")
    def test_030_captureADLogin(self):
        global nodeData, node, nodeDataAD, nodeAD, captureIP
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
        # Get the captive page IP address for logout use.
        
        
        
        # check if AD login and password 
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        # get the IP address of the capture page 
        result = clientControl.runCommand("wget -a /tmp/capture_test_030a.log -O /tmp/capture_test_030a.out  \'http://" + captureIP + "/capture/handler.py/authpost?username=atsadmin&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'",True)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_030a.out")
        assert (search == 0)
        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout  
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_030b.log -O /tmp/capture_test_030b.out http://" + captureIP + "/capture/logout")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'logout' /tmp/capture_test_030b.out")
        assert (search == 0)

    RadiusResult = subprocess.call(["ping","-c","1",radiusHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    #@unittest2.skipIf(RadiusResult != 0,  "No RADIUS server available")
    @unittest2.skipIf(True,  "Test Broken, hard-coded nonce.")
    def test_040_captureRadiusLogin(self):
        global nodeData, node, nodeDataRD, nodeDataAD, nodeAD, captureIP
        # Configure RADIUS settings
        nodeAD.setSettings(createRadiusSettings())
        testResultString = nodeAD.getRadiusManager().getRadiusStatusForSettings(createRadiusSettings(),"normal","passwd")
        print 'testResultString %s' % testResultString  # debug line
        assert ("success" in testResultString)
        # Create Internal NIC capture rule with basic AD login page
        nodeData['authenticationType']="RADIUS"
        nodeData['pageType'] = "BASIC_LOGIN"
        node.setSettings(nodeData)
        # check that basic captive page is shown
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_040.log -O /tmp/capture_test_040.out http://www.google.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'username and password' /tmp/capture_test_040.out")
        assert (search == 0)
        # check if RADIUS login and password 
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = clientControl.runCommand("wget -a /tmp/capture_test_040a.log -O /tmp/capture_test_040a.out  \'http://" + captureIP + "/capture/handler.py/authpost?username=normal&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'",True)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_040a.out")
        assert (search == 0)
        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout  
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_040b.log -O /tmp/capture_test_040b.out http://" + captureIP + "/capture/logout")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'logout' /tmp/capture_test_040b.out")
        assert (search == 0)


    def test_999_finalTearDown(self):
        global node, nodeAD
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        uvmContext.nodeManager().destroy( nodeAD.getNodeSettings()["id"] )
        node = None
        nodeAD = None

TestDict.registerNode("capture", CaptureTests)
