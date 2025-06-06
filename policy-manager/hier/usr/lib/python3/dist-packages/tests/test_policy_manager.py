"""policy_manager tests"""
import re
import socket
import base64
import unittest
import pytest

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
appData = None
app = None
secondRackId = None
secondRackFirewall = None
secondRackWebfilter = None
thirdRackId = None
thirdRackFirewall = None
defaultRackCaptivePortal = None

def createFirewallSingleConditionRule( conditionType, value, blocked=True ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.app.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + conditionTypeStr + " = " + valueStr, 
        "log": True, 
        "block": blocked, 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.app.firewall.FirewallRuleCondition", 
                    "conditionType": conditionTypeStr, 
                    "value": valueStr
                    }
                ]
            }
        }

def createLocalDirectoryUser():
    passwd_encoded = base64.b64encode("passwd".encode("utf-8"))
    return {'javaClass': 'java.util.LinkedList', 
        'list': [{
            'username': 'test20', 
            'firstName': '[firstName]', 
            'lastName': '[lastName]', 
            'javaClass': 'com.untangle.uvm.LocalDirectoryUser', 
            'expirationTime': 0, 
            'passwordBase64Hash': passwd_encoded.decode("utf-8"),
            'email': 'test20@example.com'
            },]
    }

def removeLocalDirectoryUser():
    return {'javaClass': 'java.util.LinkedList', 
        'list': []
    }

@pytest.mark.policy_manager
class PolicyManagerTests(NGFWTestCase):

    @staticmethod
    def module_name():
        return "policy-manager"

    @classmethod
    def initial_extra_setup(cls):
        global appData
        appData = cls._app.getSettings()
        remote_control.run_command("rm -f ./authpost*")

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))

    # add a rack
    def test_015_addRack(self):
        global secondRackId
        secondRackId = global_functions.addRack(self._app)
        result = remote_control.is_online()
        assert (result == 0)

    # remove a rack
    def test_016_removeRack(self):
        global secondRackId
        assert (global_functions.removeRack(self._app, secondRackId))
        result = remote_control.is_online()
        assert (result == 0)

    # add a rack
    def test_021_addSecondRack(self):
        global secondRackId
        secondRackId = global_functions.addRack(self._app, name="Second Rack")
        result = remote_control.is_online()
        assert (result == 0)

    # add firewall to second rack
    def test_022_addFirewallToSecondRack(self):
        global secondRackFirewall 
        secondRackFirewall = global_functions.uvmContext.appManager().instantiate("firewall", secondRackId)
        assert (secondRackFirewall != None)
        # add a block rule for the client IP
        rules = secondRackFirewall.getRules()
        rules["list"].append(createFirewallSingleConditionRule("SRC_ADDR",remote_control.client_ip))
        secondRackFirewall.setRules(rules)

    # verify client is online
    def test_023_childShouldNotEffectParent(self):
        # add a child that blocks everything
        blockRackId = global_functions.addRack(self._app, name="Block Rack", parentId=default_policy_id)
        blockRackFirewall = global_functions.uvmContext.appManager().instantiate("firewall", blockRackId)
        assert (blockRackFirewall != None)
        # add a block rule for the client IP
        rules = blockRackFirewall.getRules()
        rules["list"].append(createFirewallSingleConditionRule("SRC_ADDR",remote_control.client_ip))
        blockRackFirewall.setRules(rules)
        # client should still be online
        result = remote_control.is_online()
        assert (result == 0)
        global_functions.uvmContext.appManager().destroy( blockRackFirewall.getAppSettings()["id"] )
        assert (global_functions.removeRack(self._app, blockRackId))

        # Get the IP address of test.untangle.com
        test_untangle_com_ip = socket.gethostbyname("test.untangle.com")
        
        events = global_functions.get_events('Policy Manager','All Events',None,100)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 100, 
                                            "s_server_addr", str(test_untangle_com_ip),
                                            "policy_id", 1,
                                            "c_client_addr", remote_control.client_ip)
        assert( found )

    # send client's traffic to second rack - should now be blocked by firewall
    def test_024_addRuleForSecondRack(self):
        global secondRackId
        global_functions.appendRule(self._app, global_functions.createPolicySingleConditionRule("SRC_ADDR",remote_control.client_ip, secondRackId))
        # client should be offline
        result = remote_control.is_online(tries=1)
        assert (result != 0)
        
    # send client back to default rack
    def test_025_removeRuleForSecondRack(self):
        global_functions.nukeRules(self._app)
        result = remote_control.is_online()
        assert (result == 0)

    # add a third rack thats a child of second rack
    def test_026_addThirdRack(self):
        global thirdRackId
        thirdRackId = global_functions.addRack(self._app, name="Third Rack", parentId=secondRackId)
        result = remote_control.is_online()
        assert (result == 0)

    # send client's traffic to third rack - should now be blocked by firewall inherited from second rack
    def test_027_addRuleForThirdRack(self):
        global thirdRackId
        global_functions.appendRule(self._app, global_functions.createPolicySingleConditionRule("SRC_ADDR",remote_control.client_ip, thirdRackId))
        # client should be offline
        result = remote_control.is_online(tries=1)
        assert (result != 0)

    # add firewall to third rack - this should override the second rack's firewall with the block rule
    def test_028_addFirewallToThirdRack(self):
        global thirdRackFirewall
        thirdRackFirewall = global_functions.uvmContext.appManager().instantiate("firewall", thirdRackId)
        assert (thirdRackFirewall != None)
        result = remote_control.is_online()
        assert (result == 0)

    # disable firewall to third rack - even when disabled it should override the firewall in the second rack
    def test_029_stopFirewallToThirdRack(self):
        global thirdRackFirewall
        thirdRackFirewall.stop()
        assert (thirdRackFirewall != None)
        result = remote_control.is_online()
        assert (result == 0)
        thirdRackFirewall.start()

    # add a app that requires a casing to second rack to make sure casing is inherited
    def test_030_addWebFilterToSecondRack(self):
        global secondRackWebfilter
        secondRackWebfilter = global_functions.uvmContext.appManager().instantiate("web-filter", secondRackId)
        assert (secondRackWebfilter != None)
        result = remote_control.is_online()
        assert (result == 0)
        # add a block rule
        newRule = { "blocked": True, "description": "desc", "flagged": True, "javaClass": "com.untangle.uvm.app.GenericRule", "string": "test.untangle.com/test/testPage1.html" }
        rules = secondRackWebfilter.getBlockedUrls()
        rules["list"].append(newRule)
        secondRackWebfilter.setBlockedUrls(rules)
        # verify traffic is now blocked (third rack inherits web filter from second rack)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://test.untangle.com/test/testPage1.html") + "2>&1 | grep -q blockpage")
        assert (result == 0)

    # direct traffic to second rack after local authentication
    def test_040_localCaptivePortalToSecondRack(self):
        global defaultRackCaptivePortal
        remote_control.run_command("rm -f /tmp/policy_test_040*")
        defaultRackCaptivePortal = global_functions.uvmContext.appManager().instantiate("captive-portal", default_policy_id)
        assert (defaultRackCaptivePortal != None)
        defaultRackCaptivePortalData = defaultRackCaptivePortal.getSettings()
        # turn default capture rule on and basic login
        defaultRackCaptivePortalData['captureRules']['list'][0]['enabled'] = True
        defaultRackCaptivePortalData['authenticationType']="LOCAL_DIRECTORY"
        defaultRackCaptivePortalData['pageType'] = "BASIC_LOGIN"
        defaultRackCaptivePortal.setSettings(defaultRackCaptivePortalData)
        
        # Create local directory user 'test20'
        global_functions.uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        # check host table and remove username for host IP
        userHost = global_functions.uvmContext.hostTable().getHostTableEntry(remote_control.client_ip)
        userHost['username'] = ""
        userHost['usernameCaptivePortal'] = ""
        global_functions.uvmContext.hostTable().setHostTableEntry(remote_control.client_ip,userHost)
        # userHost = global_functions.uvmContext.hostTable().getHostTableEntry(remote_control.client_ip)
        # print(userHost)
        global_functions.nukeRules(self._app)
        global_functions.appendRule(self._app, global_functions.createPolicySingleConditionRule("USERNAME","[authenticated]", secondRackId))
        
        # check that basic captive page is shown
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/policy_test_040.out", log_file="/tmp/policy_test_040.log", uri="http://www.google.com/", quiet=False))
        assert (result == 0)
        search = remote_control.run_command("grep -q 'username and password' /tmp/policy_test_040.out")
        assert (search == 0)

        # check if local directory login and password works
        ipfind = remote_control.run_command("grep 'Location' /tmp/policy_test_040.log",stdout=True)
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        captureIP = ip[0]
        print('Capture IP address is %s' % captureIP)
        appid = str(defaultRackCaptivePortal.getAppSettings()["id"])
        # print('appid is %s' % appid  # debug line)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://{captureIP}/capture/handler.py/authpost?username=test20&password=passwd&nonce=9abd7f2eb5ecd82b&method=GET&appid={appid}&host={captureIP}&uri=/"))
        assert (result == 0)
        # verify the username is assigned to the IP
        userHost = global_functions.uvmContext.hostTable().getHostTableEntry(remote_control.client_ip)
        assert (userHost['username'] == "test20")
        userHost = global_functions.uvmContext.hostTable().getHostTableEntry(remote_control.client_ip)
        # firewall on rack 2 is blocking all, we should not get the test.untangle.com page
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/policy_test_040a.out", log_file="/tmp/policy_test_040a.log", uri="http://www.google.com/"))
        search = remote_control.run_command("grep -q 'Hi!' /tmp/policy_test_040a.out")
        assert (search != 0)
        # Or the captive page
        search = remote_control.run_command("grep -q 'username and password' /tmp/policy_test_040a.out")
        assert (search != 0)
        
        # Logout
        result = remote_control.run_command(global_functions.build_wget_command(output_file="/tmp/policy_test_040b.out", log_file="/tmp/policy_test_040b.log", uri=f"http://{captureIP}/capture/logout"))
        assert (result == 0)
        search = remote_control.run_command("grep -q 'logged out' /tmp/policy_test_040b.out")
        assert (search == 0)
        # remove captive portal and test user
        global_functions.uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        global_functions.uvmContext.appManager().destroy( defaultRackCaptivePortal.getAppSettings()["id"] )
        defaultRackCaptivePortal = None

    # remove apps from second rack
    def test_980_removeAppsFromSecondRack(self):
        global secondRackFirewall , secondRackWebfilter
        global_functions.uvmContext.appManager().destroy( secondRackFirewall.getAppSettings()["id"] )
        global_functions.uvmContext.appManager().destroy( secondRackWebfilter.getAppSettings()["id"] )

    # remove apps from third rack
    def test_981_removeAppsFromThirdRack(self):
        global thirdRackFirewall 
        global_functions.uvmContext.appManager().destroy( thirdRackFirewall.getAppSettings()["id"] )

    # remove third rack
    def test_982_removeThirdRack(self):
        global thirdRackId
        global_functions.nukeRules(self._app)
        assert (global_functions.removeRack(self._app, thirdRackId))
        result = remote_control.is_online()
        assert (result == 0)

    # remove second rack
    def test_983_removeSecondRack(self):
        global secondRackId
        global_functions.nukeRules(self._app)
        assert (global_functions.removeRack(self._app, secondRackId))
        result = remote_control.is_online()
        assert (result == 0)

    @classmethod
    def final_extra_tear_down(cls):
        global defaultRackCaptivePortal

        if defaultRackCaptivePortal != None:
            global_functions.uvmContext.appManager().destroy( defaultRackCaptivePortal.getAppSettings()["id"] )
            defaultRackCaptivePortal = None


test_registry.register_module("policy-manager", PolicyManagerTests)
