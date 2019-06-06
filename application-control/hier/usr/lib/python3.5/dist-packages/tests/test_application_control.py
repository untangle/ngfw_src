"""application_control tests"""
import time
import subprocess
import unittest
import pytest
import runtests

from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
appSettings = None
app = None

#pdb.set_trace()

def touchProtoRule( protoGusername, flag = True, block =True ):
    global appSettings,app
    for rec in appSettings['protoRules']['list']:
        # print("appSettings: " + str(rec))
        if (rec['name'] == protoGusername):
            rec['flag'] = flag
            rec['block'] = block
    app.setSettings(appSettings)

def create2ConditionRule( matcher1Type, matcher1Value, matcher2Type, matcher2Value, blocked=True ):
    matcher1TypeStr = str(matcher1Type)
    matcher1ValueStr = str(matcher1Value)
    matcher2TypeStr = str(matcher2Type)
    matcher2ValueStr = str(matcher2Value)
    return {
        "javaClass": "com.untangle.app.application_control.ApplicationControlLogicRule",
        "description": "2-ConditionRule: " + matcher1TypeStr + " = " + matcher1ValueStr + " && " + matcher2TypeStr + " = " + matcher2ValueStr,
        "enabled": True,
        "id": 1,
        "action": {
            "javaClass": "com.untangle.app.application_control.ApplicationControlLogicRuleAction",
            "actionType": "BLOCK",
            "flag": True
            },
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.app.application_control.ApplicationControlLogicRuleCondition",
                    "conditionType": matcher1TypeStr,
                    "value": matcher1ValueStr
                    },
                {
                    "invert": False,
                    "javaClass": "com.untangle.app.application_control.ApplicationControlLogicRuleCondition",
                    "conditionType": matcher2TypeStr,
                    "value": matcher2ValueStr
                    }
                ]
            }
        };

def nukeLogicRules():
    global app, appSettings
    appSettings['logicRules']['list'] = []
    app.setSettings(appSettings)

def appendLogicRule(newRule):
    global app, appSettings
    appSettings['logicRules']['list'].append(newRule)
    app.setSettings(appSettings)

@pytest.mark.application_control
class ApplicationControlTests(unittest.TestCase):

    @staticmethod
    def module_name():
        return "application-control"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initial_setup(self):
        global appSettings, app
        if (uvmContext.appManager().isInstantiated(self.module_name())):
            raise Exception('app %s already instantiated' % self.module_name())
        app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)
        appSettings = app.getSettings()
        # run a few sessions so that the classd daemon starts classifying
        for i in range(2): remote_control.is_online()

    def setUp(self):
        pass
            
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_012_classdIsRunning(self):
        result = subprocess.call("ps aux | grep classd | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result == 0)

    def test_020_protoRule_Default_Google(self):
        result = remote_control.run_command("wget -q -O /dev/null -4 -t 2 --timeout=5 http://www.google.com/")
        assert (result == 0)

    def test_021_protoRule_Block_Google(self):
        touchProtoRule("Google",False,False)
        result1 = remote_control.run_command("wget -4 -q -O /dev/null -t 2 --timeout=5 http://www.google.com/")
        touchProtoRule("Google",True,True)
        result2 = remote_control.run_command("wget -q -O /dev/null -4 -t 2 --timeout=5 http://www.google.com/")
        touchProtoRule("Google",False,False)
        assert (result1 == 0)
        assert (result2 != 0)

    def test_023_protoRule_Facebook(self):
        touchProtoRule("Facebook",False,False)
        result1 = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://facebook.com/")
        touchProtoRule("Facebook",True,True)
        result2 = remote_control.run_command("wget --no-check-certificate -4 -q -O /dev/null -t 2 --timeout=5 https://facebook.com/")
        touchProtoRule("Facebook",False,False)
        assert (result1 == 0)
        assert (result2 != 0)

    def test_024_protoRule_Dns(self):
        raise unittest.SkipTest("Test not consistent, disabling.")
        touchProtoRule("DNS",False,False)
        result1 = remote_control.run_command("host -4 -W3 test.untangle.com 8.8.8.8")
        touchProtoRule("DNS",True,True)
        result2 = remote_control.run_command("host -4 -W3 test.untangle.com 8.8.8.8")
        touchProtoRule("DNS",False,False)
        assert (result1 == 0)
        assert (result2 != 0)

    def test_025_protoRule_Ftp(self):
        touchProtoRule("FTP",False,False)
        pingResult = subprocess.call(["ping","-c","1",global_functions.ftp_server],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if pingResult:
            raise unittest.SkipTest(global_functions.ftp_server + " not reachable")
        ftpUserName, ftpPassword = global_functions.get_live_account_info("ftp")            
        result1 = remote_control.run_command("wget --user=" + ftpUserName + " --password='" + ftpPassword + "' -q -O /dev/null -4 -t 2 -o /dev/null --timeout=5 ftp://" + global_functions.ftp_server + "/")
        touchProtoRule("FTP",True,True)
        result2 = remote_control.run_command("wget --user=" + ftpUserName + " --password='" + ftpPassword + "' -q -O /dev/null -4 -t 2 -o /dev/null --timeout=5 ftp://" + global_functions.ftp_server + "/")
        touchProtoRule("FTP",False,False)
        assert (result1 == 0)
        assert (result2 != 0)

    def test_026_protoRule_Pandora(self):
        pre_count = global_functions.get_app_metric_value(app,"pass")

        touchProtoRule("Pandora",False,False)
        result1 = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://pandora.com/")
        touchProtoRule("Pandora",True,True)
        result2 = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://pandora.com/")
        touchProtoRule("Pandora",False,False)
        assert (result1 == 0)
        assert (result2 != 0)

        # Check to see if the faceplate counters have incremented. 
        post_count = global_functions.get_app_metric_value(app,"pass")
        assert(pre_count < post_count)

    def test_030_logicRule_Allow_Gmail(self):
        result = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://mail.google.com/")
        assert (result == 0)
        
    def test_031_logicRule_Block_Gmail(self):
        nukeLogicRules()
        appendLogicRule(create2ConditionRule("PROTOCOL", "TCP", "APPLICATION_CONTROL_APPLICATION", "GMAIL"))
        result = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://mail.google.com/")
        assert (result != 0)

    def test_031_logicRule_Block_Gmail_by_ProtoChain(self):
        nukeLogicRules()
        appendLogicRule(create2ConditionRule("PROTOCOL", "TCP", "APPLICATION_CONTROL_PROTOCHAIN", "*/SSL*"))
        result = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://mail.google.com/")
        assert (result != 0)

    def test_032_logicRule_Block_Gmail_by_Category(self):
        nukeLogicRules()
        appendLogicRule(create2ConditionRule("PROTOCOL", "TCP", "APPLICATION_CONTROL_CATEGORY", "Mail"))
        result = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://mail.google.com/")
        assert (result != 0)

    def test_033_logicRule_Block_Gmail_by_Productivity(self):
        nukeLogicRules()
        appendLogicRule(create2ConditionRule("PROTOCOL", "TCP", "APPLICATION_CONTROL_PRODUCTIVITY", ">2"))
        result = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://mail.google.com/")
        assert (result != 0)

    def test_034_logicRule_Block_Gmail_by_Risk(self):
        nukeLogicRules()
        appendLogicRule(create2ConditionRule("PROTOCOL", "TCP", "APPLICATION_CONTROL_RISK", "<5"))
        result = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://mail.google.com/")
        assert (result != 0)

    def test_034_logicRule_Block_Gmail_by_Confidence(self):
        nukeLogicRules()
        appendLogicRule(create2ConditionRule("PROTOCOL", "TCP", "APPLICATION_CONTROL_CONFIDENCE", ">50"))
        result = remote_control.run_command("wget --no-check-certificate -q -O /dev/null -4 -t 2 --timeout=5 https://mail.google.com/")
        assert (result != 0)

    def test_100_eventlog_Block_Google(self):
        touchProtoRule("Google",True,True)
        result = remote_control.run_command("wget -O /dev/null -4 -t 2 --timeout=5 http://www.google.com/")
        assert (result != 0)
        time.sleep(1)

        events = global_functions.get_events('Application Control','Blocked Sessions',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "application_control_application", "GOOGLE", 
                                            "application_control_category", "Web Services", 
                                            "application_control_blocked", True,
                                            "application_control_flagged", True)
        assert( found )

    def test_500_classdDaemonReconnect(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        for i in range(10):
            print("Test %i" % i)
            result = subprocess.call("systemctl restart untangle-classd >/dev/null 2>&1", shell=True)
            assert (result == 0)
            result = remote_control.is_online()
            assert (result == 0)
            # delay so we don't trigger systemd throttling of 5 restarts in 10 seconds
            time.sleep(3)
        # give it some time to recover for future tests
        for i in range(5):
            result = remote_control.is_online()
            time.sleep(1)

    @staticmethod
    def final_tear_down(self):
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None

test_registry.register_module("application-control", ApplicationControlTests)

