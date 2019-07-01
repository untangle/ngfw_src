"""intrusion_prevention tests"""
import time
import subprocess
import datetime
import unittest
import pytest
import runtests

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
from uvm import Uvm


appSettings = None
app = None

#pdb.set_trace()

def create_signature( gid = "1", sid = "1999999", classtype="attempted-admin", category="app-detect",  msg="Msg", log=True, block=False, 
    action="alert", type="tcp", source_ip="any", source_port="any", dest_ip="any", dest_port="any"):
    if block:
        action = "reject"
    else:
        action = "alert"
    signature =   action + " " + type + " " + source_ip + " " + source_port + " -> " + dest_ip + " " + dest_port + " (" + \
            "msg:\"" + msg + "\";" + \
            "classtype:" + classtype + ";" + \
            "sid:" + sid + ";" + \
            "gid:" + gid + ";" + \
            "content:\"" + msg + "\";nocase;)"
    return  {
        "category": category,
        "javaClass": "com.untangle.app.intrusion_prevention.IntrusionPreventionSignature",
        "signature": signature
    };
    
def create_rule(desc="ATS rule", action="blocklog", rule_type="CLASSTYPE", type_value="attempted-admin", enable_rule=True):
    return {
        "action": action,
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "comparator": "=",
                    "javaClass": "com.untangle.app.intrusion_prevention.IntrusionPreventionRuleCondition",
                    "type": rule_type,
                    "value": type_value
                }
            ]
        },
        "description": desc,
        "enabled": enable_rule,
        "id": "1",
        "javaClass": "com.untangle.app.intrusion_prevention.IntrusionPreventionRule"
    };


@pytest.mark.intrusion_prevention
class IntrusionPreventionTests(NGFWTestCase):

    force_start = True
    wait_for_daemon_ready = True

    @staticmethod
    def module_name():
        # cheap trick to force class variables _app and _appSettings into
        # global namespace as app and appSettings
        global app, appSettings
        app = IntrusionPreventionTests._app
        appSettings = IntrusionPreventionTests._appSettings
        return "intrusion-prevention"

    @staticmethod
    def vendorName():
        return "Untangle"

    def test_009_IsRunning(self):
        result = subprocess.call("ps aux | grep suricata | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result == 0)

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_030_rule_add(self):
        """
        Custom rule and rule to enable it
        """
        global app, appSettings
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        appSettings['signatures']['list'].append(create_signature( gid = "1", 
                                                sid = "1999999", 
                                                classtype="attempted-admin", 
                                                category="app-detect",  
                                                msg="CompanySecret", 
                                                log=True, 
                                                block=False, 
                                                action="alert", 
                                                type="tcp"))
        # insert rule at the beginning of the list so other rules do not interfere. 
        appSettings['rules']['list'].insert(0,create_rule(action="block", rule_type="CATEGORY", type_value="app-detect"))
        app.setSettings(appSettings, True)

        self.do_wait_for_daemon_ready()

        startTime = datetime.datetime.now()
        loopLimit = 4
        # Send four requests for test rebustnewss 
        while (loopLimit > 0):
            time.sleep(1)
            loopLimit -= 1
            result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'msg', "CompanySecret",
                                               'blocked', True,
                                               min_date=startTime)
        # del appSettings['rules']['list'][0] # delete the first rule just added
        # app.setSettings(appSettings, True)
        assert(found)

    def test_031_rule_modify(self):
        """
        Modify existing rule and rule to enable it
        """
        global app, appSettings
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        rule_desc = appSettings['rules']['list'][0]['description']
        if rule_desc != "ATS rule":
            raise unittest.SkipTest('Skipping as test test_030_rule_add is needed')
        else:
            appSettings['rules']['list'][0]['action'] = "log"
            app.setSettings(appSettings, True)

        self.do_wait_for_daemon_ready()

        startTime = datetime.datetime.now()
        loopLimit = 4
        # Send four requests for test rebustnewss 
        while (loopLimit > 0):
            time.sleep(1)
            loopLimit -= 1
            result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'msg', "CompanySecret",
                                               'blocked', False,
                                               min_date=startTime)
        del appSettings['rules']['list'][0] # delete the first rule just added
        app.setSettings(appSettings, True)
        assert(found)

    def test_052_functional_icmp_log(self):
        """
        Check for ICMP (ping)
        """
        global app, appSettings
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        wan_ip = uvmContext.networkManager().getFirstWanAddress()
        iperf_avail = global_functions.verify_iperf_configuration(wan_ip)
        device_in_office = global_functions.is_in_office_network(wan_ip)
        # Also test that it can probably reach us (we're on a 10.x network)
        if not device_in_office:
            raise unittest.SkipTest("Not on office network, skipping")
        if (not iperf_avail):
            raise unittest.SkipTest("IperfServer test client unreachable, skipping alternate port forwarding test")

        # insert rule at the beginning of the list so other rules do not interfere. 
        appSettings['rules']['list'].insert(0,create_rule(action="log", rule_type="CATEGORY", type_value="scan"))
        app.setSettings(appSettings, True)

        self.do_wait_for_daemon_ready()

        startTime = datetime.datetime.now()
        remote_control.run_command("nmap -sP " + wan_ip + " >/dev/null 2>&1",host=global_functions.iperf_server)

        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'protocol', "1",
                                               'blocked', False,
                                               min_date=startTime)
        del appSettings['rules']['list'][0] # delete the first rule just added
        app.setSettings(appSettings, True)
        assert(found)

    def test_054_functional_udp_block(self):
        global app, appSettings
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        appSettings['signatures']['list'].append(create_signature( gid = "1", 
                                                sid = "1999998", 
                                                classtype="attempted-admin", 
                                                category="app-detect",  
                                                msg="CompanySecret", 
                                                log=True, 
                                                block=True,
                                                action="alert", 
                                                type="udp"))
                                                
        # insert rule at the beginning of the list so other rules do not interfere. 
        appSettings['rules']['list'].insert(0,create_rule(action="block", rule_type="CATEGORY", type_value="app-detect"))
        app.setSettings(appSettings, True)

        self.do_wait_for_daemon_ready()

        startTime = datetime.datetime.now()
        result = remote_control.run_command("echo 'companysecret' | nc -w1 -q1 -u 4.2.2.1 2020 > /dev/null")

        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'msg', "CompanySecret",
                                               'blocked', True,
                                               min_date=startTime)

        del appSettings['rules']['list'][0] # delete the first rule just added
        app.setSettings(appSettings, True)
        assert(found)

    def test_060_app_stats(self):
        """
        Checks that the scan, detect, and block stats are properly incremented
        """
        global app, appSettings
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # clear out the signature list
        appSettings['signatures']['list'] = [] 
        appSettings['signatures']['list'].append(create_signature( gid = "1", 
                                                sid = "1999999", 
                                                classtype="attempted-admin", 
                                                category="app-detect",  
                                                msg="CompanySecret", 
                                                log=True, 
                                                block=False, 
                                                action="alert", 
                                                type="tcp"))
        # insert rule at the beginning of the list so other rules do not interfere. 
        appSettings['rules']['list'].insert(0,create_rule(action="block", rule_type="CATEGORY", type_value="app-detect"))
        app.setSettings(appSettings, True)
        self.do_wait_for_daemon_ready()

        app.forceUpdateStats()
        pre_events_scan = global_functions.get_app_metric_value(app,"scan")
        pre_events_detect = global_functions.get_app_metric_value(app,"detect")
        pre_events_block = global_functions.get_app_metric_value(app,"block")

        startTime = datetime.datetime.now()
        loopLimit = 4
        # Send four requests for test rebustnewss 
        while (loopLimit > 0):
            time.sleep(1)
            loopLimit -= 1
            result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        time.sleep(10)
        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'msg', "CompanySecret",
                                               'blocked', True,
                                               min_date=startTime)

        post_events_scan = global_functions.get_app_metric_value(app,"scan")
        post_events_detect = global_functions.get_app_metric_value(app,"detect")
        post_events_block = global_functions.get_app_metric_value(app,"block")

        del appSettings['rules']['list'][0] # delete the first rule just added
        app.setSettings(appSettings, True)
        assert(found)

        print("pre_events_scan: %s post_events_scan: %s"%(str(pre_events_scan),str(post_events_scan)))
        print("pre_events_detect: %s post_events_detect: %s"%(str(pre_events_detect),str(post_events_detect)))
        print("pre_events_block: %s post_events_block: %s"%(str(pre_events_block),str(post_events_block)))
        # assert(pre_events_scan < post_events_scan)
        assert(pre_events_detect < post_events_detect)
        assert(pre_events_block < post_events_block)    

    def test_080_nmap_log(self):
        global app, appSettings
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        wan_ip = uvmContext.networkManager().getFirstWanAddress()
        iperf_avail = global_functions.verify_iperf_configuration(wan_ip)
        device_in_office = global_functions.is_in_office_network(wan_ip)
        # Also test that it can probably reach us (we're on a 10.x network)
        if not device_in_office:
            raise unittest.SkipTest("Not on office network, skipping")
        if (not iperf_avail):
            raise unittest.SkipTest("IperfServer test client unreachable, skipping alternate port forwarding test")

        startTime = datetime.datetime.now()
        # start nmap on client
        remote_control.run_command("nmap " + wan_ip + " >/dev/null 2>&1",host=global_functions.iperf_server)
        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'msg', "NMAP",
                                               'blocked', False,
                                               min_date=startTime)

test_registry.register_module("intrusion-prevention", IntrusionPreventionTests)
