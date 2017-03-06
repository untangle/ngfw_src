import unittest2
import time
import sys
import pdb
import os
import re
import socket
import subprocess
import base64
import copy
import platform

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions

defaultRackId = 1
nodeData = None
node = None
nodeDataAD = None
nodeAD = None
nodeWeb = None
AD_HOST = "10.112.56.47"
AD_ADMIN = "ATSadmin"
AD_PASSWORD = "passwd"
radiusHost = "10.112.56.71"
localUserName = 'test20'
adUserName = 'atsadmin'
captureIP = None
savedCookieFileName = "/tmp/capture_cookie.txt";

# pdb.set_trace()
def createCaptureNonWanNicRule():
    return {
        "capture": True,
        "description": "Test Rule - Capture all internal traffic",
        "enabled": True,
        "id": 1,
        "javaClass": "com.untangle.node.captive_portal.CaptureRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "invert": False,
                "javaClass": "com.untangle.node.captive_portal.CaptureRuleCondition",
                "conditionType": "SRC_INTF",
                "value": "non_wan"
                }]
            },
        "ruleId": 1
    };

def createLocalDirectoryUser():
    return {'javaClass': 'java.util.LinkedList',
        'list': [{
            'username': localUserName,
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

def createDirectoryConnectorSettings():
    # Need to send Radius setting even though it's not used in this case.
    return {
       "activeDirectorySettings": {
            "LDAPHost": AD_HOST,
            "LDAPPort": 389,
            "OUFilter": "",
            "domain": "adtest.adtesting.int",
            "enabled": True,
            "javaClass": "com.untangle.node.directory_connector.ActiveDirectorySettings",
            "superuser": AD_ADMIN,
            "superuserPass": AD_PASSWORD
       },
        "radiusSettings": {
            "port": 1812,
            "enabled": False,
            "authenticationMethod": "PAP",
            "javaClass": "com.untangle.node.directory_connector.RadiusSettings",
            "server": radiusHost,
            "sharedSecret": "mysharedsecret"
        },
        "googleSettings": {
            "javaClass": "com.untangle.node.directory_connector.GoogleSettings",
            "authenticationEnabled": True
        },
        "facebookSettings": {
            "javaClass": "com.untangle.node.directory_connector.FacebookSettings",
            "authenticationEnabled": True
        }
    }

def createRadiusSettings():
    return {
        "activeDirectorySettings": {
            "enabled": False,
            "superuserPass": "passwd",
            "LDAPPort": "389",
            "OUFilter": "",
            "domain": "adtest.metaloft.com",
            "javaClass": "com.untangle.node.directory_connector.ActiveDirectorySettings",
            "LDAPHost": AD_HOST,
            "superuser": AD_ADMIN
        },
        "radiusSettings": {
            "port": 1812,
            "enabled": True,
            "authenticationMethod": "PAP",
            "javaClass": "com.untangle.node.directory_connector.RadiusSettings",
            "server": radiusHost,
            "sharedSecret": "chakas"
        },
        "googleSettings": {
            "javaClass": "com.untangle.node.directory_connector.GoogleSettings"
        }
    }

def findNameInHostTable (hostname='test'):
    #  Test for username in session
    foundTestSession = False
    remote_control.isOnline()
    hostList = uvmContext.hostTable().getHosts()
    sessionList = hostList['list']
    # find session generated with netcat in session table.
    for i in range(len(sessionList)):
        print sessionList[i]
        # print "------------------------------"
        if (sessionList[i]['address'] == remote_control.clientIP) and (sessionList[i]['username'] == hostname):
            foundTestSession = True
            break
    remote_control.runCommand("pkill netcat")
    return foundTestSession

def timeOfClientOff (timediff=60):
    # Check the time differential betwen the Untangle and client is less than 1 min.
    client_time = int(remote_control.runCommand("date +%s",stdout=True))
    local_time = int(time.time())
    diff_time = abs(client_time - local_time)
    if diff_time > timediff:
        return True
    else:
        return False

class CaptivePortalTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-captive-portal"

    @staticmethod
    def nodeNameAD():
        return "untangle-node-directory-connector"

    @staticmethod
    def nodeNameWeb():
        return "untangle-node-web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global nodeData, node, nodeDataRD, nodeDataAD, nodeAD, nodeWeb, adResult, radiusResult, test_untangle_com_ip, captureIP
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            print "ERROR: Node %s already installed" % self.nodeName()
            raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        nodeData = node.getCaptivePortalSettings()
        if (uvmContext.nodeManager().isInstantiated(self.nodeNameAD())):
            print "ERROR: Node %s already installed" % self.nodeNameAD()
            raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
        nodeAD = uvmContext.nodeManager().instantiate(self.nodeNameAD(), defaultRackId)
        nodeDataAD = nodeAD.getSettings().get('activeDirectorySettings')
        nodeDataRD = nodeAD.getSettings().get('radiusSettings')
        if (uvmContext.nodeManager().isInstantiated(self.nodeNameWeb())):
            print "ERROR: Node %s already installed" % self.nodeNameWeb()
            raise unittest2.SkipTest('node %s already instantiated' % self.nodeNameWeb())
        nodeWeb = uvmContext.nodeManager().instantiate(self.nodeNameWeb(), defaultRackId)
        adResult = subprocess.call(["ping","-c","1",AD_HOST],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        radiusResult = subprocess.call(["ping","-c","1",radiusHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        # Create local directory user 'test20'
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        # Get the IP address of test.untangle.com
        test_untangle_com_ip = socket.gethostbyname("test.untangle.com")

        # remove previous temp files
        remote_control.runCommand("rm -f /tmp/capture_test_*")

    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_020_defaultTrafficCheck(self):
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 http://test.untangle.com/")
        assert (result == 0)

    def test_021_captureTrafficCheck(self):
        global node, nodeData
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        node.setSettings(nodeData)
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_021.log http://test.untangle.com/")
        assert (result == 0)

        events = global_functions.get_events('Captive Portal','All Session Events',None,100)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_server_addr', test_untangle_com_ip,
                                            'c_client_addr', remote_control.clientIP,
                                            'captive_portal_blocked', True )
        assert( found )
        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_021b.out http://" + global_functions.get_lan_ip() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_021b.out")
        assert (search == 0)

    def test_022_webFilterAffinityCheck(self):
        global node, nodeData, nodeWeb
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        node.setSettings(nodeData)

        newRule = { "blocked": True, "description": "test.untangle.com", "flagged": True, "javaClass": "com.untangle.uvm.node.GenericRule", "string": "test.untangle.com" }
        rules_orig = nodeWeb.getBlockedUrls()
        rules = copy.deepcopy(rules_orig)
        rules["list"].append(newRule)
        nodeWeb.setBlockedUrls(rules)

        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_022.out http://test.untangle.com/")
        assert (result == 0)
        # User should see captive portal page (not web filter block page)
        search = remote_control.runCommand("grep -q 'Captive Portal' /tmp/capture_test_022.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        nodeWeb.setBlockedUrls(rules_orig)
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_022b.out http://" + global_functions.get_lan_ip() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_022b.out")
        assert (search == 0)

    def test_023_loginAnonymous(self):
        global node, nodeData

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="NONE"
        nodeData['pageType'] = "BASIC_MESSAGE"
        nodeData['userTimeout'] = 3600  # default
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_023.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Captive Portal' /tmp/capture_test_023.out")
        assert (search == 0)

        # Verify anonymous works
        appid = str(node.getNodeSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_023a.out  \'" + global_functions.get_http_url() + "capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_023a.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_023b.out " + global_functions.get_http_url() + "capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_023b.out")
        assert (search == 0)

    def test_024_loginAnonymousTimeout(self):
        global node, nodeData
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="NONE"
        nodeData['pageType'] = "BASIC_MESSAGE"
        nodeData['userTimeout'] = 10
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_024.out http://test.untangle.com/")
        assert (result == 0)

        # Verify anonymous works
        appid = str(node.getNodeSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_024a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_024a.out")
        assert (search == 0)

        # Wait for captive timeout
        time.sleep(20)
        node.runCleanup() # run the periodic cleanup task to remove expired users

        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_024b.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Captive Portal' /tmp/capture_test_024b.out")
        assert (search == 0)

    def test_025_loginAnonymousHttps(self):
        global node, nodeData

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="NONE"
        nodeData['pageType'] = "BASIC_MESSAGE"
        nodeData['userTimeout'] = 3600  # default
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("curl -s --connect-timeout 10 -L -o /tmp/capture_test_025.out --insecure https://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Captive Portal' /tmp/capture_test_025.out")
        assert (search == 0)

        # Verify anonymous works
        appid = str(node.getNodeSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("curl -s --connect-timeout 10 -L -o /tmp/capture_test_025a.out --insecure  \'" + global_functions.get_http_url() + "/capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_025a.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_025b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_025b.out")
        assert (search == 0)

    def test_030_loginLocalDirectory(self):
        global node, nodeData

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="LOCAL_DIRECTORY"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['userTimeout'] = 3600  # default
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username and password' /tmp/capture_test_030.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_030a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + localUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_030a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_030b.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(not foundUsername)

    def test_031_loginAny(self):
        global node, nodeData

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="ANY"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['userTimeout'] = 3600  # default
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username and password' /tmp/capture_test_030.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_030a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + localUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_030a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_030b.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(not foundUsername)

    def test_032_loginGoogle(self):
        raise unittest2.SkipTest('Broken test - google keeps banning account')
        if platform.machine().startswith('arm'):
            raise unittest2.SkipTest('Not supported on ARM')
        global node, nodeData
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        device_in_office = global_functions.isInOfficeNetwork(wan_IP)
        if (device_in_office):
            raise unittest2.SkipTest('Google Login not working in office')
        googleUserName, googlePassword = global_functions.getLiveAccountInfo("Google")
        print "username: %s\n " % str(googleUserName)
        if googlePassword != None:
            print "password: %s\n" % (len(googlePassword)*"*")

        # account not found if message returned
        if googleUserName == "message":
            raise unittest2.SkipTest(googlePassword)
        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="GOOGLE"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['userTimeout'] = 3600  # default
        node.setSettings(nodeData)

        # Configure Directory Connector
        nodeAD.setSettings(createDirectoryConnectorSettings())

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_032.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username and password' /tmp/capture_test_032.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_032a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + googleUserName + "&password=" + googlePassword + "&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_032a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(googleUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_030b.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(googleUserName)
        assert(not foundUsername)

    def test_033_loginFacebook(self):
        if platform.machine().startswith('arm'):
            raise unittest2.SkipTest('Not supported on ARM')
        global node, nodeData
        facebookUserName, facebookPassword = global_functions.getLiveAccountInfo("Facebook")
        print "username: %s\n " % str(facebookUserName)
        if facebookPassword != None:
            print "password: %s\n" % (len(facebookPassword)*"*")

        # account not found if message returned
        if facebookUserName == "message":
            raise unittest2.SkipTest(facebookPassword)
        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="FACEBOOK"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['userTimeout'] = 3600  # default
        node.setSettings(nodeData)

        # Configure Directory Connector
        nodeAD.setSettings(createDirectoryConnectorSettings())

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_032.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username and password' /tmp/capture_test_032.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_032a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + facebookUserName + "&password=" + facebookPassword + "&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_032a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(facebookUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_030b.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(facebookUserName)
        assert(not foundUsername)

    def test_035_loginActiveDirectory(self):
        global nodeData, node, nodeDataAD, nodeAD
        if (adResult != 0):
            raise unittest2.SkipTest("No AD server available")
        # Configure AD settings
        testResultString = nodeAD.getActiveDirectoryManager().getActiveDirectoryStatusForSettings(createDirectoryConnectorSettings())
        # print 'testResultString %s' % testResultString  # debug line
        nodeAD.setSettings(createDirectoryConnectorSettings())
        assert ("success" in testResultString)
        # Create Internal NIC capture rule with basic AD login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="ACTIVE_DIRECTORY"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['userTimeout'] = 3600  # default
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_035.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username and password' /tmp/capture_test_035.out")
        assert (search == 0)

        # check if AD login and password
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_035a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + adUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_035a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(adUserName)
        assert(foundUsername)

        # logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_035b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_035b.out")
        assert (search == 0)
        # try second time to login,
        result = remote_control.runCommand("wget -O /tmp/capture_test_035c.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + adUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_035c.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_035d.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_035d.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(adUserName)
        assert(not foundUsername)

        # check extend ascii in login and password bug 10860
        result = remote_control.runCommand("wget -O /tmp/capture_test_035e.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=britishguy&password=passwd%C2%A3&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_035e.out")
        assert (search == 0)

        # logout user to clean up test.
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_035f.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_035f.out")
        assert (search == 0)


    def test_040_loginRadius(self):
        global nodeData, node, nodeDataRD, nodeDataAD, nodeAD
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
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="RADIUS"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['userTimeout'] = 3600  # default
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=5 -O /tmp/capture_test_040.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username' /tmp/capture_test_040.out")
        assert (search == 0)

        # check if RADIUS login and password
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_040a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=normal&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'",stdout=True)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_040a.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_040b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_040b.out")
        assert (search == 0)

        # check if RADIUS login and password a second time.
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_040c.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=normal&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'",stdout=True)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_040c.out")
        assert (search == 0)

        # logout user to clean up test a second time.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_040d.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_040d.out")
        assert (search == 0)

    def test_050_cookie(self):
        """
        Cookie test
        """
        global node, nodeData
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        # variable for local test
        capture_file_name = "/tmp/capture_test_050.out"
        cookie_file_name = "/tmp/capture_test_050_cookie.txt"

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())

        nodeData['authenticationType']="LOCAL_DIRECTORY"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['sessionCookiesEnabled'] = True
        nodeData['sessionCookiesTimeout'] = 86400
        nodeData['userTimeout'] = 10
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O " + capture_file_name + " http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username and password' " + capture_file_name)
        assert (search == 0)

        # check if local directory login and password
        appid = str(node.getNodeSettings()["id"])

        # connect and auth to get cookie
        result = remote_control.runCommand("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=test20&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\' --save-cookies " + cookie_file_name)
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' " + capture_file_name)
        assert (search == 0)

        # Wait for captive timeout
        time.sleep(20)
        node.runCleanup() # run the periodic cleanup task to remove expired users

        # try again without cookie (confirm session not active)
        result = remote_control.runCommand("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/?username=&password=&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' " + capture_file_name)
        assert (search == 1)

        # try again with cookie
        result = remote_control.runCommand("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/index?nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\' --load-cookies " + cookie_file_name)
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' " + capture_file_name)
        assert (search == 0)

        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # Wait for captive timeout
        time.sleep(20)
        node.runCleanup() # run the periodic cleanup task to remove expired users

    def test_051_cookieTimeout(self):
        """
        Cookie expiration
        """
        global node, nodeData
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if timeOfClientOff():
            raise unittest2.SkipTest('Client time different than Untangle server')

        # variable for local test
        capture_file_name = "/tmp/capture_test_051.out"
        cookie_file_name = "/tmp/capture_test_051_cookie.txt"
        cookie_timeout = 5

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())

        nodeData['authenticationType']="LOCAL_DIRECTORY"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['sessionCookiesEnabled'] = True
        nodeData['sessionCookiesTimeout'] = cookie_timeout
        nodeData['userTimeout'] = 10
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O " + capture_file_name + " http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username and password' " + capture_file_name)
        assert (search == 0)

        # check if local directory login and password
        appid = str(node.getNodeSettings()["id"])

        # connect and auth to get cookie
        result = remote_control.runCommand("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=test20&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\' --save-cookies " + cookie_file_name)
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' " + capture_file_name)
        assert (search == 0)

        # Wait for captive timeout
        time.sleep(20)
        node.runCleanup() # run the periodic cleanup task to remove expired users

        # Cookie expiration is handled by browser so check that after the cookie timeout,
        # the client side's expiration difference from current is greater than timeout.
        cookie_expires = remote_control.runCommand("tail -1 " + cookie_file_name + " | cut -f5",stdout=True)
        assert(cookie_expires) # verify there is a cookie time
        # Save the cookie file since it is used in the next test.
        remote_control.runCommand("cp " + cookie_file_name + " " + savedCookieFileName)
        second_difference = int(remote_control.runCommand("expr $(date +%s) - " + cookie_expires,stdout=True))
        print "second_difference: %i cookie_timeout: %i" %(second_difference, cookie_timeout)
        assert(second_difference > cookie_timeout)

    def test_052_cookieDisabled(self):
        """
        User has a cookie but cookies have been disabled
        """
        global node, nodeData

        # variable for local test
        capture_file_name = "/tmp/capture_test_052.out"
        cookieExistsResults = remote_control.runCommand("test -e " + savedCookieFileName)
        if (cookieExistsResults == 1):
            raise unittest2.SkipTest('Cookie file %s was was not create in test_051_captivePortalCookie_timeout' % savedCookieFileName)

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())

        nodeData['authenticationType']="LOCAL_DIRECTORY"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['sessionCookiesEnabled'] = False
        nodeData['sessionCookiesTimeout'] = 10
        nodeData['userTimeout'] = 3600
        node.setSettings(nodeData)

        # # check if local directory login and password
        appid = str(node.getNodeSettings()["id"])

        result = remote_control.runCommand("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/index?nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\' --load-cookies " + savedCookieFileName)
        assert (result == 0)
        remote_control.runCommand("rm " + savedCookieFileName)
        search = remote_control.runCommand("grep -q 'Hi!' " + capture_file_name)
        assert (search == 1)

        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername == False)

    def test_060_loginLocalDirectoryMacMode(self):
        global node, nodeData

        # Create Internal NIC capture rule with basic login page
        nodeData['captureRules']['list'] = []
        nodeData['captureRules']['list'].append(createCaptureNonWanNicRule())
        nodeData['authenticationType']="LOCAL_DIRECTORY"
        nodeData['pageType'] = "BASIC_LOGIN"
        nodeData['userTimeout'] = 3600  # default
        nodeData['useMacAddress'] = True
        node.setSettings(nodeData)

        # check that basic captive page is shown
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_060.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'username and password' /tmp/capture_test_060.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(node.getNodeSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.runCommand("wget -O /tmp/capture_test_060a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + localUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'Hi!' /tmp/capture_test_060a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_060b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.runCommand("grep -q 'logged out' /tmp/capture_test_060b.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(not foundUsername)

    @staticmethod
    def finalTearDown(self):
        global node, nodeAD, nodeWeb
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        if nodeAD != None:
            uvmContext.nodeManager().destroy( nodeAD.getNodeSettings()["id"] )
            nodeAD = None
        if nodeWeb != None:
            uvmContext.nodeManager().destroy( nodeWeb.getNodeSettings()["id"] )
            nodeWeb = None

test_registry.registerNode("captive-portal", CaptivePortalTests)
