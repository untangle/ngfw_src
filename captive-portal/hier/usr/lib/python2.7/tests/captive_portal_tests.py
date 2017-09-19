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
appData = None
app = None
appDataAD = None
appAD = None
appWeb = None
AD_ADMIN = "ATSadmin"
AD_PASSWORD = "passwd"
localUserName = 'test20'
adUserName = 'atsadmin'
captureIP = None
savedCookieFileName = "/tmp/capture_cookie.txt";

# pdb.set_trace()
def createCaptureNonWanNicRule(idValue):
    return {
        "capture": True,
        "description": "Test Rule - Capture all internal traffic",
        "enabled": True,
        "id": 1,
        "javaClass": "com.untangle.app.captive_portal.CaptureRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "invert": False,
                "javaClass": "com.untangle.app.captive_portal.CaptureRuleCondition",
                "conditionType": "SRC_INTF",
                "value": "non_wan"
                }]
            },
        "ruleId": idValue
    };

def createCaptureAllowHttpRule(idValue):
    return {
        "capture": False,
        "description": "Test Rule - Allow HTTP to test server",
        "enabled": True,
        "id": 1,
        "javaClass": "com.untangle.app.captive_portal.CaptureRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "invert": False,
                "javaClass": "com.untangle.app.captive_portal.CaptureRuleCondition",
                "conditionType": "HTTP_HOST",
                "value": "test.untangle.com"
                }]
            },
        "ruleId": idValue
     };

def createCaptureAllowHttpsRule(idValue):
    return {
        "capture": False,
        "description": "Test Rule - Allow HTTPS to test server",
        "enabled": True,
        "id": 1,
        "javaClass": "com.untangle.app.captive_portal.CaptureRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "invert": False,
                "javaClass": "com.untangle.app.captive_portal.CaptureRuleCondition",
                "conditionType": "SSL_INSPECTOR_SNI_HOSTNAME",
                "value": "test.untangle.com"
                }]
            },
        "ruleId": idValue
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
            "LDAPHost": global_functions.adServer,
            "LDAPPort": 389,
            "OUFilter": "",
            "OUFilters": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "domain": "adtest.adtesting.int",
            "enabled": True,
            "javaClass": "com.untangle.app.directory_connector.ActiveDirectorySettings",
            "superuser": AD_ADMIN,
            "superuserPass": AD_PASSWORD
       },
        "radiusSettings": {
            "port": 1812,
            "enabled": False,
            "authenticationMethod": "PAP",
            "javaClass": "com.untangle.app.directory_connector.RadiusSettings",
            "server": global_functions.radiusServer,
            "sharedSecret": "mysharedsecret"
        },
        "googleSettings": {
            "javaClass": "com.untangle.app.directory_connector.GoogleSettings",
            "authenticationEnabled": True
        },
        "facebookSettings": {
            "javaClass": "com.untangle.app.directory_connector.FacebookSettings",
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
            "OUFilters": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "domain": "adtest.metaloft.com",
            "javaClass": "com.untangle.app.directory_connector.ActiveDirectorySettings",
            "LDAPHost": global_functions.adServer,
            "superuser": AD_ADMIN
        },
        "radiusSettings": {
            "port": 1812,
            "enabled": True,
            "authenticationMethod": "PAP",
            "javaClass": "com.untangle.app.directory_connector.RadiusSettings",
            "server": global_functions.radiusServer,
            "sharedSecret": "chakas"
        },
        "googleSettings": {
            "javaClass": "com.untangle.app.directory_connector.GoogleSettings"
        }
    }

def findNameInHostTable (hostname='test'):
    #  Test for username in session
    foundTestSession = False
    remote_control.is_online()
    hostList = uvmContext.hostTable().getHosts()
    sessionList = hostList['list']
    # find session generated with netcat in session table.
    for i in range(len(sessionList)):
        print sessionList[i]
        # print "------------------------------"
        if (sessionList[i]['address'] == remote_control.clientIP) and (sessionList[i]['username'] == hostname):
            foundTestSession = True
            break
    remote_control.run_command("pkill netcat")
    return foundTestSession

def timeOfClientOff (timediff=60):
    # Check the time differential betwen the Untangle and client is less than 1 min.
    client_time = int(remote_control.run_command("date +%s",stdout=True))
    local_time = int(time.time())
    diff_time = abs(client_time - local_time)
    if diff_time > timediff:
        return True
    else:
        return False

class CaptivePortalTests(unittest2.TestCase):

    @staticmethod
    def appName():
        return "captive-portal"

    @staticmethod
    def appNameAD():
        return "directory-connector"

    @staticmethod
    def appNameWeb():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global appData, app, appDataRD, appDataAD, appAD, appWeb, adResult, radiusResult, test_untangle_com_ip, captureIP
        if (uvmContext.appManager().isInstantiated(self.appName())):
            print "ERROR: App %s already installed" % self.appName()
            raise unittest2.SkipTest('app %s already instantiated' % self.appName())
        app = uvmContext.appManager().instantiate(self.appName(), defaultRackId)
        appData = app.getCaptivePortalSettings()
        if (uvmContext.appManager().isInstantiated(self.appNameAD())):
            print "ERROR: App %s already installed" % self.appNameAD()
            raise unittest2.SkipTest('app %s already instantiated' % self.appName())
        appAD = uvmContext.appManager().instantiate(self.appNameAD(), defaultRackId)
        appDataAD = appAD.getSettings().get('activeDirectorySettings')
        appDataRD = appAD.getSettings().get('radiusSettings')
        if (uvmContext.appManager().isInstantiated(self.appNameWeb())):
            print "ERROR: App %s already installed" % self.appNameWeb()
            raise unittest2.SkipTest('app %s already instantiated' % self.appNameWeb())
        appWeb = uvmContext.appManager().instantiate(self.appNameWeb(), defaultRackId)
        adResult = subprocess.call(["ping","-c","1",global_functions.adServer],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        radiusResult = subprocess.call(["ping","-c","1",global_functions.radiusServer],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        # Create local directory user 'test20'
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        # Get the IP address of test.untangle.com
        test_untangle_com_ip = socket.gethostbyname("test.untangle.com")

        # remove previous temp files
        remote_control.run_command("rm -f /tmp/capture_test_*")

    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_020_defaultTrafficCheck(self):
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 http://test.untangle.com/")
        assert (result == 0)

    def test_021_captureTrafficCheck(self):
        global app, appData
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        app.setSettings(appData)
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_021.log http://test.untangle.com/")
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
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_021b.out http://" + global_functions.get_lan_ip() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_021b.out")
        assert (search == 0)

    def test_022_webFilterAffinityCheck(self):
        global app, appData, appWeb
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        app.setSettings(appData)

        newRule = { "blocked": True, "description": "test.untangle.com", "flagged": True, "javaClass": "com.untangle.uvm.app.GenericRule", "string": "test.untangle.com" }
        rules_orig = appWeb.getBlockedUrls()
        rules = copy.deepcopy(rules_orig)
        rules["list"].append(newRule)
        appWeb.setBlockedUrls(rules)

        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_022.out http://test.untangle.com/")
        assert (result == 0)
        # User should see captive portal page (not web filter block page)
        search = remote_control.run_command("grep -q 'Captive Portal' /tmp/capture_test_022.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        appWeb.setBlockedUrls(rules_orig)
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_022b.out http://" + global_functions.get_lan_ip() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_022b.out")
        assert (search == 0)

    def test_023_loginAnonymous(self):
        global app, appData

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="NONE"
        appData['pageType'] = "BASIC_MESSAGE"
        appData['userTimeout'] = 3600  # default
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_023.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Captive Portal' /tmp/capture_test_023.out")
        assert (search == 0)

        # Verify anonymous works
        appid = str(app.getAppSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_023a.out  \'" + global_functions.get_http_url() + "capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_023a.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_023b.out " + global_functions.get_http_url() + "capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_023b.out")
        assert (search == 0)

    def test_024_loginAnonymousTimeout(self):
        global app, appData
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="NONE"
        appData['pageType'] = "BASIC_MESSAGE"
        appData['userTimeout'] = 10
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_024.out http://test.untangle.com/")
        assert (result == 0)

        # Verify anonymous works
        appid = str(app.getAppSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_024a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_024a.out")
        assert (search == 0)

        # Wait for captive timeout
        time.sleep(20)
        app.runCleanup() # run the periodic cleanup task to remove expired users

        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_024b.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Captive Portal' /tmp/capture_test_024b.out")
        assert (search == 0)

    def test_025_loginAnonymousHttps(self):
        global app, appData

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="NONE"
        appData['pageType'] = "BASIC_MESSAGE"
        appData['userTimeout'] = 3600  # default
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("curl -s --connect-timeout 10 -L -o /tmp/capture_test_025.out --insecure https://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Captive Portal' /tmp/capture_test_025.out")
        assert (search == 0)

        # Verify anonymous works
        appid = str(app.getAppSettings()["id"])
        print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("curl -s --connect-timeout 10 -L -o /tmp/capture_test_025a.out --insecure  \'" + global_functions.get_http_url() + "/capture/handler.py/infopost?method=GET&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&agree=agree&submit=Continue&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_025a.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_025b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_025b.out")
        assert (search == 0)

    def test_026_PassRuleHttp(self):
        global app, appData

        # Create pass rule for our HTTP test server and append Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureAllowHttpRule(1))
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(2))
        appData['authenticationType']="NONE"
        appData['pageType'] = "BASIC_MESSAGE"
        appData['userTimeout'] = 3600  # default
        app.setSettings(appData)

        # check that basic captive page is NOT show for host with HTTP hostname pass rule
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_026.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Captive Portal' /tmp/capture_test_027.out")
        assert (search != 0)

    def test_027_PassRuleHttps(self):
        global app, appData

        # Create pass rule for our HTTPS test server and append Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureAllowHttpsRule(1))
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(2))
        appData['authenticationType']="NONE"
        appData['pageType'] = "BASIC_MESSAGE"
        appData['userTimeout'] = 3600  # default
        app.setSettings(appData)

        # check that basic captive page is NOT show for host with SNI hostname pass rule
        result = remote_control.run_command("curl -s --connect-timeout 10 -L -o /tmp/capture_test_027.out --insecure https://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Captive Portal' /tmp/capture_test_027.out")
        assert (search != 0)

    def test_030_loginLocalDirectory(self):
        global app, appData

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="LOCAL_DIRECTORY"
        appData['pageType'] = "BASIC_LOGIN"
        appData['userTimeout'] = 3600  # default
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' /tmp/capture_test_030.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(app.getAppSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_030a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + localUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_030a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_030b.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(not foundUsername)

    def test_031_loginAny(self):
        global app, appData

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="ANY_DIRCON"
        appData['pageType'] = "BASIC_LOGIN"
        appData['userTimeout'] = 3600  # default
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' /tmp/capture_test_030.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(app.getAppSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_030a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + localUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_030a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_030b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_030b.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(not foundUsername)

    def test_035_loginActiveDirectory(self):
        global appData, app, appDataAD, appAD
        if (adResult != 0):
            raise unittest2.SkipTest("No AD server available")
        # Configure AD settings
        testResultString = appAD.getActiveDirectoryManager().getActiveDirectoryStatusForSettings(createDirectoryConnectorSettings())
        # print 'testResultString %s' % testResultString  # debug line
        appAD.setSettings(createDirectoryConnectorSettings())
        assert ("success" in testResultString)
        # Create Internal NIC capture rule with basic AD login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="ACTIVE_DIRECTORY"
        appData['pageType'] = "BASIC_LOGIN"
        appData['userTimeout'] = 3600  # default
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_035.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' /tmp/capture_test_035.out")
        assert (search == 0)

        # check if AD login and password
        appid = str(app.getAppSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_035a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + adUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_035a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(adUserName)
        assert(foundUsername)

        # logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_035b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_035b.out")
        assert (search == 0)
        # try second time to login,
        result = remote_control.run_command("wget -O /tmp/capture_test_035c.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + adUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_035c.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_035d.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_035d.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(adUserName)
        assert(not foundUsername)

        # check extend ascii in login and password bug 10860
        result = remote_control.run_command("wget -O /tmp/capture_test_035e.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=britishguy&password=passwd%C2%A3&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_035e.out")
        assert (search == 0)

        # logout user to clean up test.
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_035f.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_035f.out")
        assert (search == 0)


    def test_040_loginRadius(self):
        global appData, app, appDataRD, appDataAD, appAD
        if (radiusResult != 0):
            raise unittest2.SkipTest("No RADIUS server available")

        # Configure RADIUS settings
        appAD.setSettings(createRadiusSettings())
        attempts = 0
        while attempts < 3:
            testResultString = appAD.getRadiusManager().getRadiusStatusForSettings(createRadiusSettings(),"normal","passwd")
            if ("success" in testResultString):
                break
            else:
                attempts += 1
        print 'testResultString %s attempts %s' % (testResultString, attempts) # debug line
        assert ("success" in testResultString)
        # Create Internal NIC capture rule with basic AD login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="RADIUS"
        appData['pageType'] = "BASIC_LOGIN"
        appData['userTimeout'] = 3600  # default
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -q -4 -t 2 --timeout=5 -O /tmp/capture_test_040.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username' /tmp/capture_test_040.out")
        assert (search == 0)

        # check if RADIUS login and password
        appid = str(app.getAppSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_040a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=normal&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'",stdout=True)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_040a.out")
        assert (search == 0)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_040b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_040b.out")
        assert (search == 0)

        # check if RADIUS login and password a second time.
        appid = str(app.getAppSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_040c.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=normal&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'",stdout=True)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_040c.out")
        assert (search == 0)

        # logout user to clean up test a second time.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_040d.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_040d.out")
        assert (search == 0)

    def test_050_cookie(self):
        """
        Cookie test
        """
        global app, appData
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        # variable for local test
        capture_file_name = "/tmp/capture_test_050.out"
        cookie_file_name = "/tmp/capture_test_050_cookie.txt"

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))

        appData['authenticationType']="LOCAL_DIRECTORY"
        appData['pageType'] = "BASIC_LOGIN"
        appData['sessionCookiesEnabled'] = True
        appData['sessionCookiesTimeout'] = 86400
        appData['userTimeout'] = 10
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O " + capture_file_name + " http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' " + capture_file_name)
        assert (search == 0)

        # check if local directory login and password
        appid = str(app.getAppSettings()["id"])

        # connect and auth to get cookie
        result = remote_control.run_command("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=test20&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\' --save-cookies " + cookie_file_name)
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' " + capture_file_name)
        assert (search == 0)

        # Wait for captive timeout
        time.sleep(20)
        app.runCleanup() # run the periodic cleanup task to remove expired users

        # try again without cookie (confirm session not active)
        result = remote_control.run_command("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/?username=&password=&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' " + capture_file_name)
        assert (search == 1)

        # try again with cookie
        result = remote_control.run_command("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/index?nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\' --load-cookies " + cookie_file_name)
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' " + capture_file_name)
        assert (search == 0)

        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # Wait for captive timeout
        time.sleep(20)
        app.runCleanup() # run the periodic cleanup task to remove expired users

    def test_051_cookieTimeout(self):
        """
        Cookie expiration
        """
        global app, appData
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if timeOfClientOff():
            raise unittest2.SkipTest('Client time different than Untangle server')

        # variable for local test
        capture_file_name = "/tmp/capture_test_051.out"
        cookie_file_name = "/tmp/capture_test_051_cookie.txt"
        cookie_timeout = 5

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))

        appData['authenticationType']="LOCAL_DIRECTORY"
        appData['pageType'] = "BASIC_LOGIN"
        appData['sessionCookiesEnabled'] = True
        appData['sessionCookiesTimeout'] = cookie_timeout
        appData['userTimeout'] = 10
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O " + capture_file_name + " http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' " + capture_file_name)
        assert (search == 0)

        # check if local directory login and password
        appid = str(app.getAppSettings()["id"])

        # connect and auth to get cookie
        result = remote_control.run_command("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=test20&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\' --save-cookies " + cookie_file_name)
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' " + capture_file_name)
        assert (search == 0)

        # Wait for captive timeout
        time.sleep(20)
        app.runCleanup() # run the periodic cleanup task to remove expired users

        # Cookie expiration is handled by browser so check that after the cookie timeout,
        # the client side's expiration difference from current is greater than timeout.
        cookie_expires = remote_control.run_command("tail -1 " + cookie_file_name + " | cut -f5",stdout=True)
        assert(cookie_expires) # verify there is a cookie time
        # Save the cookie file since it is used in the next test.
        remote_control.run_command("cp " + cookie_file_name + " " + savedCookieFileName)
        second_difference = int(remote_control.run_command("expr $(date +%s) - " + cookie_expires,stdout=True))
        print "second_difference: %i cookie_timeout: %i" %(second_difference, cookie_timeout)
        assert(second_difference > cookie_timeout)

    def test_052_cookieDisabled(self):
        """
        User has a cookie but cookies have been disabled
        """
        global app, appData

        # variable for local test
        capture_file_name = "/tmp/capture_test_052.out"
        cookieExistsResults = remote_control.run_command("test -e " + savedCookieFileName)
        if (cookieExistsResults == 1):
            raise unittest2.SkipTest('Cookie file %s was was not create in test_051_captivePortalCookie_timeout' % savedCookieFileName)

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))

        appData['authenticationType']="LOCAL_DIRECTORY"
        appData['pageType'] = "BASIC_LOGIN"
        appData['sessionCookiesEnabled'] = False
        appData['sessionCookiesTimeout'] = 10
        appData['userTimeout'] = 3600
        app.setSettings(appData)

        # # check if local directory login and password
        appid = str(app.getAppSettings()["id"])

        result = remote_control.run_command("wget -O " + capture_file_name + "  \'" + global_functions.get_http_url() + "/capture/handler.py/index?nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\' --load-cookies " + savedCookieFileName)
        assert (result == 0)
        remote_control.run_command("rm " + savedCookieFileName)
        search = remote_control.run_command("grep -q 'Hi!' " + capture_file_name)
        assert (search == 1)

        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername == False)

    def test_060_loginLocalDirectoryMacMode(self):
        global app, appData

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="LOCAL_DIRECTORY"
        appData['pageType'] = "BASIC_LOGIN"
        appData['userTimeout'] = 3600  # default
        appData['useMacAddress'] = True
        app.setSettings(appData)

        # check that basic captive page is shown
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_060.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' /tmp/capture_test_060.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(app.getAppSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_060a.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + localUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_060a.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_060b.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_060b.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(not foundUsername)

    def test_070_loginRedirectUsingHostname(self):
        global app, appData

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="LOCAL_DIRECTORY"
        appData['pageType'] = "BASIC_LOGIN"
        appData['userTimeout'] = 3600  # default
        appData['redirectUsingHostname'] = True
        app.setSettings(appData)

        # check that basic captive page is shown using HTTP
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_070a.out http://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' /tmp/capture_test_070a.out")
        assert (search == 0)

        # check that basic captive page is shown using HTTPS
        result = remote_control.run_command("curl -s --connect-timeout 10 -L -o /tmp/capture_test_070b.out --insecure https://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' /tmp/capture_test_070b.out")
        assert (search == 0)

        # check if local directory login and password
        appid = str(app.getAppSettings()["id"])
        # print 'appid is %s' % appid  # debug line
        result = remote_control.run_command("wget -O /tmp/capture_test_070c.out  \'" + global_functions.get_http_url() + "/capture/handler.py/authpost?username=" + localUserName + "&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid=" + appid + "&host=test.untangle.com&uri=/\'")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Hi!' /tmp/capture_test_070c.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(foundUsername)

        # logout user to clean up test.
        # wget http://<internal IP>/capture/logout
        result = remote_control.run_command("wget -4 -t 2 --timeout=5 -O /tmp/capture_test_070d.out " + global_functions.get_http_url() + "/capture/logout")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/capture_test_070d.out")
        assert (search == 0)
        foundUsername = findNameInHostTable(localUserName)
        assert(not foundUsername)

    def test_071_checkSecureRedirectEnabled(self):
        global app, appData

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="NONE"
        appData['pageType'] = "BASIC_MESSAGE"
        appData['userTimeout'] = 3600  # default
        appData['disableSecureRedirect'] = False
        app.setSettings(appData)

        # check that basic captive page is show when secure redirection is enabled
        result = remote_control.run_command("curl -s --connect-timeout 10 -L -o /tmp/capture_test_071.out --insecure https://test.untangle.com/")
        assert (result == 0)
        search = remote_control.run_command("grep -q 'Captive Portal' /tmp/capture_test_071.out")
        assert (search == 0)

    def test_072_checkSecureRedirectDisabled(self):
        global app, appData

        # Create Internal NIC capture rule with basic login page
        appData['captureRules']['list'] = []
        appData['captureRules']['list'].append(createCaptureNonWanNicRule(1))
        appData['authenticationType']="NONE"
        appData['pageType'] = "BASIC_MESSAGE"
        appData['userTimeout'] = 3600  # default
        appData['disableSecureRedirect'] = True
        app.setSettings(appData)

        # check that the request times out when secure redirection is disabled
        result = remote_control.run_command("curl -s --connect-timeout 10 -L -o /tmp/capture_test_072.out --insecure https://test.untangle.com/")
        assert (result != 0)

    @staticmethod
    def finalTearDown(self):
        global app, appAD, appWeb
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        if appAD != None:
            uvmContext.appManager().destroy( appAD.getAppSettings()["id"] )
            appAD = None
        if appWeb != None:
            uvmContext.appManager().destroy( appWeb.getAppSettings()["id"] )
            appWeb = None

test_registry.registerApp("captive-portal", CaptivePortalTests)
