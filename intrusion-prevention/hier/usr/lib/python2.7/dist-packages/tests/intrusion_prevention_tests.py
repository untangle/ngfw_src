import unittest2
import time
import sys
import pdb
import os
import subprocess

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
from datetime import datetime
import remote_control
import test_registry
import global_functions

default_policy_id = 1
appSettings = None
app = None

#pdb.set_trace()

def create_signature( gid = "1", sid = "1999999", classtype="attempted-admin", category="app-detect",  msg="Msg", log=True, block=False, 
    action="alert", type="tcp", source_ip="any", source_port="any", dest_ip="any", dest_port="any"):
    if block:
        action = "drop"
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
    
def create_rule(desc="ATS rule", action="blocklog", rule_type="CLASSTYPE", type_value="attempted-admin", enable_rule = True):
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
    

class IntrusionPreventionTests(unittest2.TestCase):

    @staticmethod
    def appName():
        return "intrusion-prevention"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global app, appSettings
        if (uvmContext.appManager().isInstantiated(self.appName())):
            raise Exception('app %s already instantiated' % self.appName())
        app = uvmContext.appManager().instantiate(self.appName(), default_policy_id)
        app.start()
        appSettings = app.getSettings()

    def setUp(self):
        pass
            

    def test_009_IsRunning(self):
        result = subprocess.call("ps aux | grep suricata | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result == 0)

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.appName()))

    def test_050_functional_tcp_log(self):
        global app, appSettings
        appSettings['signatures']['list'].append(create_signature( gid = "1", 
                                                sid = "1999999", 
                                                classtype="attempted-admin", 
                                                category="app-detect",  
                                                msg="CompanySecret", 
                                                log=True, 
                                                block=False, 
                                                action="alert", 
                                                type="tcp"))
        appSettings['rules']['list'].append(create_rule(rule_type="CATEGORY",type_value="app-detect"))
        app.setSettings(appSettings)

    	time.sleep(60)  # It can take a minute for sessions to start scanning

        startTime = datetime.now()
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
        assert(found)

def test_080_nmap_log(self):
        wan_ip = uvmContext.networkManager().getFirstWanAddress()
        iperf_avail = global_functions.verify_iperf_configuration(wan_ip)
        device_in_office = global_functions.is_in_office_network(wan_ip)
        # Also test that it can probably reach us (we're on a 10.x network)
        if not device_in_office:
            raise unittest2.SkipTest("Not on office network, skipping")
        if (not iperf_avail):
            raise unittest2.SkipTest("IperfServer test client unreachable, skipping alternate port forwarding test")

        startTime = datetime.now()
        # start nmap on client
        remote_control.run_command("nmap " + wan_ip + " >/dev/null 2>&1",host=global_functions.iperf_server)
        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'msg', "NMAP",
                                               'blocked', False,
                                               min_date=startTime)

    @staticmethod
    def finalTearDown(self):
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None

test_registry.registerApp("intrusion-prevention", IntrusionPreventionTests)
