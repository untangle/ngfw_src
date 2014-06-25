import unittest2
import time
import sys
import pdb
import os
import re
import socket
import subprocess
import base64
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
adHost = "10.111.56.48"
radiusHost = "10.111.56.71"

# pdb.set_trace()

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

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

def createLocalDirectoryUser():
    return {'javaClass': 'java.util.LinkedList', 
        'list': [{
            'username': 'test20', 
            'firstName': '[firstName]', 
            'lastName': '[lastName]', 
            'javaClass': 'com.untangle.uvm.LocalDirectoryUser', 
            'expirationTime': 0, 
            'passwordBase64Hash': base64.b64encode('passwd'), 
            'email': 'test20@example.com'
            },]
    }

def removeLocalDirectoryUser():
    return {'javaClass': 'java.util.LinkedList', 
        'list': []
    }

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
        global nodeData, node, nodeDataRD, nodeDataAD, nodeAD, adResult, radiusResult, test_untangle_com_ip
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            nodeData = node.getCaptureSettings()
        if nodeAD == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeNameAD())):
                print "ERROR: Node %s already installed" % self.nodeNameAD()
                raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
            nodeAD = uvmContext.nodeManager().instantiate(self.nodeNameAD(), defaultRackId)
            nodeDataAD = nodeAD.getSettings().get('activeDirectorySettings')
            nodeDataRD = nodeAD.getSettings().get('radiusSettings')
        adResult = subprocess.call(["ping","-c","1",adHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        radiusResult = subprocess.call(["ping","-c","1",radiusHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        # Get the IP address of test.untangle.com
        test_untangle_com_ip = socket.gethostbyname("test.untangle.com")   

        # remove previous temp files
        clientControl.runCommand("rm -f /tmp/capture_test_* >/dev/null 2>&1")

    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        assert (result == 0)

    def test_020_defaultTrafficCheck(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_020.log -O /tmp/capture_test_020.out http://test.untangle.com/")
        assert (result == 0)

    def test_021_captureTrafficCheck(self):
        global node, nodeData, captureIP
        nodeData['captureRules']['list'].append(createCaptureInternalNicRule())
        node.setSettings(nodeData)
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_021.log -O /tmp/capture_test_021.out http://test.untangle.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Captive Portal' /tmp/capture_test_021.out")
        assert (search == 0)
        # get the IP address of the capture page 
        ipfind = clientControl.runCommand("grep 'Location' /tmp/capture_test_021.log",True)
        print 'ipFind %s' % ipfind
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        captureIP = ip[0]
        print 'Capture IP address is %s' % captureIP

        # check event log
        flushEvents()
        query = None;
        for q in node.getRuleEventQueries():
            if q['name'] == 'All Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,100)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        for event in events['list']:
            if event['c_server_addr'] == test_untangle_com_ip:
                assert(event['c_client_addr'] == ClientControl.hostIP)
                assert(event['capture_blocked'] == True)
                return
        assert(False) # event not found

    def test_023_captureAnonymousLogin(self):
        global node, nodeData

        # Create Internal NIC capture rule with basic login page
        nodeData['authenticationType']="NONE"
        nodeData['pageType'] = "BASIC_MESSAGE"
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_023.log -O /tmp/capture_test_023.out http://test.untangle.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Captive Portal' /tmp/capture_test_023.out")
        assert (search == 0)

        # Verify anonymous works
        appid = str(node.getNodeSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = clientControl.runCommand("wget -a /tmp/capture_test_023a.log -O /tmp/capture_test_023a.out  \'http://" + captureIP + "/capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_023a.out")
        assert (search == 0)
        
        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout  
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_023b.log -O /tmp/capture_test_023b.out http://" + captureIP + "/capture/logout")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'logged out' /tmp/capture_test_023b.out")
        assert (search == 0)

    def test_024_captureAnonymousLoginTimeout(self):
        global node, nodeData

        # Create Internal NIC capture rule with basic login page
        nodeData['authenticationType']="NONE"
        nodeData['pageType'] = "BASIC_MESSAGE"
        nodeData['userTimeout'] = 120
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_024.log -O /tmp/capture_test_024.out http://test.untangle.com/")
        assert (result == 0)

        # Verify anonymous works
        appid = str(node.getNodeSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = clientControl.runCommand("wget -a /tmp/capture_test_024a.log -O /tmp/capture_test_024a.out  \'http://" + captureIP + "/capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_024a.out")
        assert (search == 0)
        
        # Wait for captive timeout
        time.sleep(180)
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_024b.log -O /tmp/capture_test_024b.out http://test.untangle.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Captive Portal' /tmp/capture_test_024b.out")
        assert (search == 0)

    def test_025_captureAnonymousLoginHTTPS(self):
        global node, nodeData

        # Create Internal NIC capture rule with basic login page
        nodeData['authenticationType']="NONE"
        nodeData['pageType'] = "BASIC_MESSAGE"
        nodeData['userTimeout'] = 3600  # back to default setting
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = clientControl.runCommand("curl -s --connect-timeout 2 -L -o /tmp/capture_test_025.out --insecure https://test.untangle.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Captive Portal' /tmp/capture_test_025.out")
        assert (search == 0)

        # Verify anonymous works
        appid = str(node.getNodeSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = clientControl.runCommand("curl -s --connect-timeout 2 -L -o /tmp/capture_test_025a.out --insecure  \'https://" + captureIP + "/capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_025a.out")
        assert (search == 0)
        
    def test_030_captureLocalDirLogin(self):
        global node, nodeData
        # Create local directory user 'test20'
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        # results = uvmContext.localDirectory().getUsers()
        # print results

        # Create Internal NIC capture rule with basic login page
        nodeData['authenticationType']="LOCAL_DIRECTORY"
        nodeData['pageType'] = "BASIC_LOGIN"
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_030.log -O /tmp/capture_test_025.out http://test.untangle.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'username and password' /tmp/capture_test_030.out")
        assert (search == 0)

        # check if local directory login and password 
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = clientControl.runCommand("wget -a /tmp/capture_test_030a.log -O /tmp/capture_test_030a.out  \'http://" + captureIP + "/capture/handler.py/authpost?username=test20&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_030a.out")
        assert (search == 0)
        
        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout  
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_030b.log -O /tmp/capture_test_030b.out http://" + captureIP + "/capture/logout")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'logged out' /tmp/capture_test_030b.out")
        assert (search == 0)


    def test_035_captureADLogin(self):
        global nodeData, node, nodeDataAD, nodeAD, captureIP
        if (adResult != 0):
            raise unittest2.SkipTest("No AD server available")
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
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_030.log -O /tmp/capture_test_030.out http://test.untangle.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'username and password' /tmp/capture_test_030.out")
        assert (search == 0)

        # check if AD login and password 
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = clientControl.runCommand("wget -a /tmp/capture_test_030a.log -O /tmp/capture_test_030a.out  \'http://" + captureIP + "/capture/handler.py/authpost?username=atsadmin&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_030a.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout  
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_030b.log -O /tmp/capture_test_030b.out http://" + captureIP + "/capture/logout")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'logged out' /tmp/capture_test_030b.out")
        assert (search == 0)

        # check extend ascii in login and password 
        result = clientControl.runCommand("wget -a /tmp/capture_test_030c.log -O /tmp/capture_test_030c.out  \'http://" + captureIP + "/capture/handler.py/authpost?username=britishguy&password=passwd%C2%A3&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_030c.out")
        assert (search == 0)

        # logout user to clean up test.
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_030d.log -O /tmp/capture_test_030d.out http://" + captureIP + "/capture/logout")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'logged out' /tmp/capture_test_030d.out")
        assert (search == 0)

    def test_040_captureRadiusLogin(self):
        global nodeData, node, nodeDataRD, nodeDataAD, nodeAD, captureIP
        if (radiusResult != 0):
            raise unittest2.SkipTest("No RADIUS server available")

        # Configure RADIUS settings
        nodeAD.setSettings(createRadiusSettings())
        attempts = 0
        while attempts < 3:
            testResultString = nodeAD.getRadiusManager().getRadiusStatusForSettings(createRadiusSettings(),"normal","passwd")
            if ("success" in testResultString):
                break
            else:
                attempts += 1
        print 'testResultString %s attempts %s' % (testResultString, attempts) # debug line
        assert ("success" in testResultString)
        # Create Internal NIC capture rule with basic AD login page
        nodeData['authenticationType']="RADIUS"
        nodeData['pageType'] = "BASIC_LOGIN"
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_040.log -O /tmp/capture_test_040.out http://test.untangle.com/")
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
        search = clientControl.runCommand("grep -q 'logged out' /tmp/capture_test_040b.out")
        assert (search == 0)

        # check if RADIUS login and password a second time.
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = clientControl.runCommand("wget -a /tmp/capture_test_040a.log -O /tmp/capture_test_040a.out  \'http://" + captureIP + "/capture/handler.py/authpost?username=normal&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'",True)
        search = clientControl.runCommand("grep -q 'Hi!' /tmp/capture_test_040a.out")
        assert (search == 0)

        # logout user to clean up test a second time.
        # wget http://<internal IP>/capture/logout  
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_040b.log -O /tmp/capture_test_040b.out http://" + captureIP + "/capture/logout")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'logged out' /tmp/capture_test_040b.out")
        assert (search == 0)

    def test_999_finalTearDown(self):
        global node, nodeAD
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        uvmContext.nodeManager().destroy( nodeAD.getNodeSettings()["id"] )
        node = None
        nodeAD = None

TestDict.registerNode("capture", CaptureTests)
