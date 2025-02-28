"""intrusion_prevention tests"""
import time
import subprocess
import datetime
import unittest
import pytest
import runtests
import json

import glob
import os
import shutil

from pathlib import Path

from tests.common import NGFWTestCase
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
        global app, appSettings, app_id
        cls.ftp_user_name, cls.ftp_password = global_functions.get_live_account_info("ftp")
        if os.path.exists('/tmp/backup'):
            cls.restore_original_files()
        app = IntrusionPreventionTests._app
        appSettings = IntrusionPreventionTests._appSettings
        app_id = cls.get_app_id()

    @staticmethod
    def module_name():
        return "intrusion-prevention"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def backup_files():
        files_to_backup = [
            "/usr/share/untangle-suricata-config/current/templates/defaults.js",
            "/usr/share/untangle-suricata-config/current/rules/classification.config",
            "/etc/suricata/suricata.yaml"
        ]
        backup_dir = "/tmp/backup"
        os.makedirs(backup_dir, exist_ok=True)
        for file_path in files_to_backup:
            if os.path.exists(file_path):
                shutil.copy(file_path, os.path.join(backup_dir, os.path.basename(file_path)))   

    @staticmethod
    def modify_conf_value():   
        default_js_path = "/usr/share/untangle-suricata-config/current/templates/defaults.js"
        new_value = "111111111"
    
        # Read the content of default.js file
        with open(default_js_path, 'r') as file:
            data = json.load(file)

        # Modify the value where type is SYSTEM_MEMORY in rules list
        for rule in data['rules']['list']:
            for condition in rule['conditions']['list']:
                if condition['type'] == 'SYSTEM_MEMORY':
                    condition['value'] = new_value

        # Write the modified content back to the file
        with open(default_js_path, 'w') as file:
            json.dump(data, file, indent=4)
        
        # Add new entry to classification.config
        classification_config_path = "/usr/share/untangle-suricata-config/current/rules/classification.config"
        with open(classification_config_path, 'a') as file:
            file.write("config classification: test-test,Test classification,1\n")

        # Add new variable under vars in suricata.yaml
        suricata_yaml_path = "/etc/suricata/suricata.yaml"
        # Read the YAML file
        with open(suricata_yaml_path, 'r') as file:
            lines = file.readlines()

        # Find the line where vars section ends
        end_vars_index = lines.index('    ENIP_SERVER: "$HOME_NET"\n') + 1

        # Insert the new value after the last line of the vars section
        lines.insert(end_vars_index, '    TEST_PORTS: [80, 443]\n')

        # Write the updated lines back to the file
        with open(suricata_yaml_path, 'w') as file:
            file.writelines(lines)

    @staticmethod
    def restore_original_files():
        backup_dir = "/tmp/backup"
        files_to_restore = [
            "/usr/share/untangle-suricata-config/current/templates/defaults.js",
            "/usr/share/untangle-suricata-config/current/rules/classification.config",
            "/etc/suricata/suricata.yaml"
        ]
        for file_path in files_to_restore:
            backup_file_path = os.path.join(backup_dir, os.path.basename(file_path))
            if os.path.exists(backup_file_path):
                shutil.copy(backup_file_path, file_path)
                open(backup_file_path, 'w').close() 
        # Remove the backup directory
        if os.path.exists(backup_dir):
            shutil.rmtree(backup_dir)


    def test_009_IsRunning(self):
        result = subprocess.call("ps aux | grep suricata | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result == 0)

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))

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
        remote_control.run_command(global_functions.build_wget_command(uri="http://test.untangle.com/CompanySecret",))

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
        remote_control.run_command(global_functions.build_wget_command(uri="http://test.untangle.com/CompanySecret",))

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

        wan_ip = global_functions.uvmContext.networkManager().getFirstWanAddress()
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
                                               'protocol_name', "icmp",
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
            result = remote_control.run_command(global_functions.build_wget_command(uri="http://test.untangle.com/CompanySecret",))

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
        result = remote_control.run_command(global_functions.build_wget_command(user=self.ftp_user_name, password=self.ftp_password, uri=f"ftp://{global_functions.ftp_server}/test.zip"))
        assert (result == 0)

    @pytest.mark.slow
    def test_080_nmap_log(self):
        global app, appSettings
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        wan_ip = global_functions.uvmContext.networkManager().getFirstWanAddress()
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

        run_ips_updates(url="https://ids.edge.arista.com/last_suricatasignatures.tar.gz")

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

        run_ips_updates(url="https://ids.edge.arista.com/last_suricatasignatures.tar.gz")

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

        run_ips_updates(url="https://ids.edge.arista.com/last_suricatasignatures.tar.gz")

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

    @pytest.mark.slow
    def test_200_ui_download_signatures(self):
        """
        Replicate downloading signatures from UI
        """
        # get catalog
        post = {
            "type": "IntrusionPreventionSettings",
            "arg1": "signatures",
            "arg2": self.get_app_id(),
            "arg5": "catalog"
        }
        post_data = []
        for key in post.keys():
            post_data.append(f"{key}={post[key]}")

        catalog = subprocess.check_output(global_functions.build_wget_command(output_file='-', post_data="&".join(post_data), uri="http://localhost/admin/download"), shell=True, stderr=subprocess.STDOUT).decode('utf-8').split("\n")
        assert len(catalog) > 0, "non empty catalog"

        for file_name in catalog:
            post["arg5"] = file_name
            post_data = []
            for key in post.keys():
                post_data.append(f"{key}={post[key]}")
            print("&".join(post_data))
            signature_set = subprocess.check_output(global_functions.build_wget_command(output_file='-', post_data="&".join(post_data), uri="http://localhost/admin/download"), shell=True, stderr=subprocess.STDOUT).decode('utf-8').split("\n")
            assert len(signature_set) > 0, f"non empty signature set {file_name}"

    @pytest.mark.slow
    def test_201_settings_changes(self):
        global app, appSettings 
        original_file_path = "/usr/share/untangle/settings/intrusion-prevention/settings_"+str(app_id)+".js"
        #Read original settings file
        with open(original_file_path, "r") as original_file:
            original_content = original_file.readlines()
        # Take backup of original configuration file
        self.backup_files()
        # Update configurations files
        self.modify_conf_value()
        #Sync updated settings in current settings
        app.synchronizeSettings()
        # Read updated settings file
        with open(original_file_path, "r") as updated_file:
            updated_content = updated_file.readlines()
        #Verify content of update_file and original_file should not be identical
        assert original_content != updated_content, "Content of updated file matches original file"


    def test_300_flow_established_toggle(self):
        """
        Verify that with/without the file flag toggle, established is removed or not.
        """
        global app, appSettings
        flow_established_enabled_flag_filename = "/usr/share/untangle/conf/intrusion-prevention-signatures-flow-established"
        rules_filename = "/etc/suricata/ngfw.rules"
        # Add "|| true" because if grep doesn't find anything, it will exit with an error code causing an exception
        command = f"grep -v 'not_established' {rules_filename} | grep -c 'flow:.*established' || true"

        # Flag enabled
        Path(flow_established_enabled_flag_filename).touch()
        app.setSettings(appSettings, True, True)
        established_count = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        print(f"with {flow_established_enabled_flag_filename}, found {established_count}")
        assert established_count > 0, "established found in signatures"

        # Flag disabled (default)
        Path(flow_established_enabled_flag_filename).unlink()
        app.setSettings(appSettings, True, True)
        established_count = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        print(f"without {flow_established_enabled_flag_filename}, found {established_count}")
        assert established_count == 0, "established not found in signatures"
        empty_flow_count = int(subprocess.check_output(f"grep -c 'flow:;' {rules_filename}|| true", shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        print(f"empty flow count {flow_established_enabled_flag_filename}, found {empty_flow_count}")
        assert empty_flow_count == 0, "empty flow not found in signatures"

    @classmethod
    def final_extra_tear_down(cls):
        # Restore original settings to return to initial settings
        global app
        cls.restore_original_files()
        #Restoring default settings
        app.synchronizeSettings()

test_registry.register_module("intrusion-prevention", IntrusionPreventionTests)
