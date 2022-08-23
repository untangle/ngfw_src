"""intrusion_prevention tests"""
import time
import subprocess
import datetime
import unittest
import pytest
import runtests

import glob
import os
import shutil

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

def run_ips_updates(url=None):
    url_argument = ""
    if url is not None:
        url_argument = "--url {url}".format(url=url) 
    result = subprocess.call("/usr/share/untangle/bin/intrusion-prevention-get-updates --debug {url_argument} >/dev/null 2>&1".format(url_argument=url_argument), shell=True)

@pytest.mark.intrusion_prevention
class IntrusionPreventionTests(NGFWTestCase):

    force_start = True
    wait_for_daemon_ready = True

    updates_path = "/usr/share/untangle-suricata-config"

    @classmethod
    def initial_extra_setup(cls):
        cls.ftp_user_name, cls.ftp_password = global_functions.get_live_account_info("ftp")

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

    @pytest.mark.slow
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
                                                category="compromised",
                                                msg="CompanySecret", 
                                                log=True, 
                                                block=False, 
                                                action="alert", 
                                                type="tcp"))
        # insert rule at the beginning of the list so other rules do not interfere. 
        appSettings['rules']['list'].insert(0,create_rule(action="block", rule_type="CATEGORY", type_value="compromised"))
        app.setSettings(appSettings, True, True)

        self.do_wait_for_daemon_ready()

        startTime = datetime.datetime.now()
        remote_control.run_command("wget --no-hsts --no-hsts -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'msg', "CompanySecret",
                                               'blocked', True,
                                               min_date=startTime)
        del appSettings['rules']['list'][0] # delete the first rule just added
        app.setSettings(appSettings, True, True)
        assert(found)

    @pytest.mark.slow
    def test_031_rule_modify(self):
        """
        Modify existing rule and rule to enable it
        """
        global app, appSettings
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        appSettings['rules']['list'][0]['action'] = "log"
        app.setSettings(appSettings, True, True)

        self.do_wait_for_daemon_ready()

        startTime = datetime.datetime.now()
        remote_control.run_command("wget --no-hsts -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'msg', "CompanySecret",
                                               'blocked', False,
                                               min_date=startTime)
        del appSettings['rules']['list'][0] # delete the first rule just added
        app.setSettings(appSettings, True, True)
        assert(found)

    @pytest.mark.slow
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
        app.setSettings(appSettings, True, True)

        self.do_wait_for_daemon_ready()

        startTime = datetime.datetime.now()

        # Ensure the icmp request includes the string ISSPNGRQ in it so we are sure to trigger a detected scan event
        # see sid: 2100465 "GPL SCAN ISS Pinger"
        remote_control.run_command("ping -c 1 -p 495353504e475251 " + wan_ip + " >/dev/null 2>&1",host=global_functions.iperf_server)

        app.forceUpdateStats()
        events = global_functions.get_events('Intrusion Prevention','All Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,
                                               'protocol', "1",
                                               'blocked', False,
                                               min_date=startTime)
        del appSettings['rules']['list'][0] # delete the first rule just added
        app.setSettings(appSettings, True, True)
        assert(found)

    @pytest.mark.slow
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
        app.setSettings(appSettings, True, True)

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
        app.setSettings(appSettings, True, True)
        assert(found)

    @pytest.mark.slow
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
        app.setSettings(appSettings, True, True)
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
            result = remote_control.run_command("wget --no-hsts -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

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
        app.setSettings(appSettings, True, True)
        assert(found)

        print("pre_events_scan: %s post_events_scan: %s"%(str(pre_events_scan),str(post_events_scan)))
        print("pre_events_detect: %s post_events_detect: %s"%(str(pre_events_detect),str(post_events_detect)))
        print("pre_events_block: %s post_events_block: %s"%(str(pre_events_block),str(post_events_block)))
        # assert(pre_events_scan < post_events_scan)
        assert(pre_events_detect < post_events_detect)
        assert(pre_events_block < post_events_block)    

    def test_070_ftp_traffic_doesnt_crash(self):
        """
        Verify that FTP does not cause system to crash
        """
        if self.ftp_user_name is None:
            raise unittest.SkipTest("Unable to obtain FTP credentials")
        ftp_result = subprocess.call(["ping","-c","1",global_functions.ftp_server ],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (ftp_result != 0):
            raise unittest.SkipTest("FTP server not available")
        result = remote_control.run_command("wget --no-hsts --user=" + self.ftp_user_name + " --password='" + self.ftp_password + "' -q -O /dev/null ftp://" + global_functions.ftp_server + "/test.zip")
        assert (result == 0)

    @pytest.mark.slow
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
    @pytest.mark.slow
    def test_100_update_patch(self):
        """
        Get last full update then try to apply differential patch.
        """
        global app, appSettings

        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # Remove existing tarballs
        existing_update_file_names = glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path))
        for file_name in existing_update_file_names:
            try:
                os.remove(file_name)
            except:
                print("cannot remove existing file_name = {file_name}".format(file_name=file_name))
                return False

        # Remove existing path
        current_path = "{updates_path}/current".format(updates_path=IntrusionPreventionTests.updates_path)
        if os.path.isdir(current_path) is True:
            try:
                shutil.rmtree(current_path)
            except:
                print("cannot remove existing current_path = {current_path}".format(current_path=current_path))
                return False

        run_ips_updates(url="https://ids.untangle.com/last_suricatasignatures.tar.gz")

        # Remove previously saved update tarballs.
        for file_name in glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path)):
            try:
                os.remove(file_name)
            except:
                print("cannot remove existing update file_name = {file_name}".format(file_name=file_name))
                return False

        run_ips_updates()

        # make sure file_size != previous_file_size
        update_file_names = glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path))
        is_patch_in_updates = False
        for file_name in update_file_names:
            if ".patch." in file_name:
                is_patch_in_updates = True

        assert(len(update_file_names) == 1)
        assert(is_patch_in_updates == True)

    @pytest.mark.slow
    def test_101_update_md5_mismatch_fallback(self):
        """
        Get last full update then add an expected file casuing the patch update to fail and
        fall back to full set install.
        """
        global app, appSettings

        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # Remove existing tarballs
        existing_update_file_names = glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path))
        for file_name in existing_update_file_names:
            try:
                os.remove(file_name)
            except:
                print("cannot remove existing file_name = {file_name}".format(file_name=file_name))
                return False

        # Remove existing path
        current_path = "{updates_path}/current".format(updates_path=IntrusionPreventionTests.updates_path)
        if os.path.isdir(current_path) is True:
            try:
                shutil.rmtree(current_path)
            except:
                print("cannot remove existing current_path = {current_path}".format(current_path=current_path))
                return False

        run_ips_updates(url="https://ids.untangle.com/last_suricatasignatures.tar.gz")

        # Remove previously saved update tarballs.
        for file_name in glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path)):
            try:
                os.remove(file_name)
            except:
                print("cannot remove existing update file_name = {file_name}".format(file_name=file_name))
                return False

        # Make applying patch fail by adding a file to current that will fail md5 compare
        unexpected_file_name = "{current_path}/what_am_i_doing_here".format(current_path=current_path)
        try:
            unexpected_file = open( unexpected_file_name, "w" )
        except:
            print("unable to open unexpeceted file = {unexpected_file_name}".format(unexpected_file_name=unexpected_file_name))
            return False
        unexpected_file.write("not supposed to be here\n")
        try:
            unexpected_file.close()
        except:
            print("unable to close unexpected file = {unexpected_file_name}".format(unexpected_file_name=unexpected_file_name))
            return False

        run_ips_updates()

        update_file_names = glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path))
        is_patch_in_updates = False
        for file_name in update_file_names:
            if ".patch." in file_name:
                is_patch_in_updates = True

        assert(len(update_file_names) == 1)
        assert(is_patch_in_updates == False)

    @pytest.mark.slow
    def test_102_update_patch_fail_fallback(self):
        """
        Get last full update then add an expected file casuing the patch update to fail and
        fall back to full set install.
        """
        global app, appSettings

        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # Remove existing tarballs
        existing_update_file_names = glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path))
        for file_name in existing_update_file_names:
            try:
                os.remove(file_name)
            except:
                print("cannot remove existing file_name = {file_name}".format(file_name=file_name))
                return False

        # Remove existing path
        current_path = "{updates_path}/current".format(updates_path=IntrusionPreventionTests.updates_path)
        if os.path.isdir(current_path) is True:
            try:
                shutil.rmtree(current_path)
            except:
                print("cannot remove existing current_path = {current_path}".format(current_path=current_path))
                return False

        run_ips_updates(url="https://ids.untangle.com/last_suricatasignatures.tar.gz")

        # Remove previously saved update tarballs.
        for file_name in glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path)):
            try:
                os.remove(file_name)
            except:
                print("cannot remove existing update file_name = {file_name}".format(file_name=file_name))
                return False

        # Make applying patch fail by deleting a file that will cause patch to fail.
        for file_name in glob.glob( "{current_path}/rules/*".format(current_path=current_path)):
            try:
                os.remove(file_name)
            except:
                print("cannot remove existing update file_name = {file_name}".format(file_name=file_name))
                return False
            break

        run_ips_updates()

        # make sure file_size != previous_file_size
        update_file_names = glob.glob( "{updates_path}/*.tar.gz".format(updates_path=IntrusionPreventionTests.updates_path))
        is_patch_in_updates = False
        for file_name in update_file_names:
            if ".patch." in file_name:
                is_patch_in_updates = True

        assert(len(update_file_names) == 1)
        assert(is_patch_in_updates == False)


test_registry.register_module("intrusion-prevention", IntrusionPreventionTests)
