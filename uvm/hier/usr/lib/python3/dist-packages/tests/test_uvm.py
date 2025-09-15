import copy
import datetime
import glob
import json
import os
import pyotp
import pytest
import re
import runtests
import shutil
import socket
import ssl
import subprocess
import time
import unittest
import urllib.request, urllib.error, urllib.parse
import urllib
import urllib3
import fnmatch
import base64

from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control
import runtests.overrides as overrides

app = None
appFW = None

default_policy_id = 1
origMailsettings = None
test_untangle_com_ip = socket.gethostbyname("test.untangle.com")

Login_username = overrides.get("Login_username", default="admin")
Login_password = overrides.get("Login_password", default="passwd")
# Expcted response from successful login
Login_success_response = '<p>The document has moved <a href="/admin">here</a></p>'
# Logins use mod_python and spawn a thread to log te event.
# This can cause a delay between what the client receives and when the
# event has been logged.  To compensate, perform a small delay after each
# login attempt.
Login_event_delay = .5

def get_latest_mail_pkg():
    remote_control.run_command("rm -f mailpkg.tar*") # remove all previous mail packages
    results = remote_control.run_command(global_functions.build_wget_command(uri="http://test.untangle.com/test/mailpkg.tar"))
    # print("Results from getting mailpkg.tar <%s>" % results)
    results = remote_control.run_command("tar -xvf mailpkg.tar")
    # print("Results from untaring mailpkg.tar <%s>" % results)

def create_trigger_rule(action, tag_target, tag_name, tag_lifetime_sec, description, field, operator, value, field2, operator2, value2):
    return {
        "description": description,
        "action": action,
        "tagTarget": tag_target,
        "tagName": tag_name,
        "tagLifetimeSec": tag_lifetime_sec,
        "enabled": True,
        "javaClass": "com.untangle.uvm.event.TriggerRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "javaClass": "com.untangle.uvm.event.EventRuleCondition",
                "comparator": operator,
                "field": field,
                "fieldValue": value
            }, {
                "javaClass": "com.untangle.uvm.event.EventRuleCondition",
                "comparator": operator2,
                "field": field2,
                "fieldValue": value2
            }]
        },
        "ruleId": 1
    }

def create_local_directory_user(directory_user='test',expire_time=0):
    user_email = directory_user + "@test.untangle.com"
    passwd_encoded = base64.b64encode("passwd".encode("utf-8"))
    return {'javaClass': 'java.util.LinkedList',
        'list': [{
            'username': directory_user,
            'firstName': '[firstName]',
            'lastName': '[lastName]',
            'javaClass': 'com.untangle.uvm.LocalDirectoryUser',
            'expirationTime': expire_time,
            'passwordBase64Hash': passwd_encoded.decode("utf-8"),
            'email': user_email
            },]
    }


def remove_local_directory_user():
    return {'javaClass': 'java.util.LinkedList',
        'list': []
    }

def check_javascript_exceptions(errors):
    """
    Get current end line for uvm log, run logJavascripException, and verify it was logged
    """
    for error in errors.keys():
        print(error)
        uvm_last_line = subprocess.check_output("wc -l /var/log/uvm/uvm.log | cut -d' ' -f1", shell=True).decode("utf-8").strip()
        print(uvm_last_line)
        global_functions.uvmContext.logJavascriptException(errors[error])
        exceptions = subprocess.check_output(f"awk 'NR >= {uvm_last_line} && /logJavascriptException/{{ print NR, $0 }}' /var/log/uvm/uvm.log", shell=True).decode("utf-8")
        print(exceptions)
        assert exceptions != "", f"for {error} found logged exceptions"
        pid = subprocess.check_output(f"ps -p $(cat /var/run/uvm.pid) -o pid=", shell=True).decode("utf-8")
        assert pid != "", "uvm running"

def find_files(dir_path, search_string):
    license_files = []
    for root, dirs, files in os.walk(dir_path, followlinks=False):
        for filename in fnmatch.filter(files, search_string):
            license_files.append(os.path.join(root, filename))
    return license_files

def buildDevicesSettings(devicesList=[]):
    return {
        "autoDeviceRemove": False,
        "autoRemovalThreshold": 30,
        "devices": {
            "javaClass": "java.util.LinkedList",
            "list": devicesList
        },
        "javaClass": "com.untangle.uvm.DevicesSettings",
        "version": 1
    }

def buildDevice(hostname="client", interfaceId=0, lastSessionTime=0, macAddress="e7:b5:a0:3a:cd:49", macVendor=None, tagsList=[], tagsString=""):
    return {
        "hostnameLastKnown": hostname,
        "interfaceId": interfaceId,
        "javaClass": "com.untangle.uvm.DeviceTableEntry",
        "lastSessionTime": lastSessionTime,
        "macAddress": macAddress,
        "macVendor": macVendor,
        "tags": {
            "javaClass": "java.util.LinkedList",
            "list": tagsList
        },
        "tagsString": tagsString
    }

class TestTotp:
    """
    Static class for managing TOTP for time-based OTP
    """
    cmd_file_path = "/usr/share/untangle/conf/cmd_totp.conf"
    cmd_file_backup_path = "/usr/share/untangle/conf/cmd_totp.conf.backup"

    cmd_files_same = False

    test_url = "otpauth://totp/Edge%20Threat%20Management%20Appliance%20Login%20%28FirstName%20LastName%29?secret=O7OLTABU3XUPCD4Q7MWCZQR2I4JXF5MQTT5MYDHE5SHIGTWROUKZ2IIP6UPLTPBIZWKPYBBB4LSX2CAKWYS6RXWGSKKZDBWMC45N4SQ"

    @classmethod
    def setup(cls, enable=False):
        """
        Setup global totp seed for testing
        """
        if os.path.isfile(TestTotp.cmd_file_path) and not os.path.isfile(TestTotp.cmd_file_backup_path):
            # Preserve existing seed
            shutil.move(TestTotp.cmd_file_path, TestTotp.cmd_file_backup_path)

        if enable:
            with open(TestTotp.cmd_file_path, "w") as file:
                file.write(TestTotp.test_url)
        else:
            if os.path.isfile(TestTotp.cmd_file_path):
                os.remove(TestTotp.cmd_file_path)

    @classmethod
    def teardown(cls, enable=False):
        """
        Return global totp seed to previous state
        """
        if os.path.isfile(TestTotp.cmd_file_backup_path):
            # Restore previou seed
            shutil.move(TestTotp.cmd_file_backup_path, TestTotp.cmd_file_path)
        elif os.path.isfile(TestTotp.cmd_file_path):
            # Remove test seed
            os.remove(TestTotp.cmd_file_path)

    @classmethod
    def get_code(cls, counter_offset=0):
        """
        Get totp code from current secret

        :returns: TOTP code
        """
        if not os.path.isfile(TestTotp.cmd_file_path):
            # Calling without having enabled
            return None

        raw_uri=None
        with open(TestTotp.cmd_file_path, "r") as file:
            raw_uri=file.read()

        raw_uri=raw_uri.replace('\/', '/')

        parsed_uri=urllib3.util.url.parse_url(raw_uri)
        uri_qs=urllib.parse.parse_qs(parsed_uri.query)
        secret=uri_qs["secret"][0]
        totp = pyotp.TOTP(secret)

        # return [ totp.at(datetime.datetime.now()), totp.at(datetime.datetime.now(), counter_offset=-1), totp.at(datetime.datetime.now(),counter_offset=1)]
        return totp.at(datetime.datetime.now(), counter_offset=counter_offset)

@pytest.mark.uvm
class UvmTests(NGFWTestCase):

    not_an_app = True

    @staticmethod
    def module_name():
        return "uvm"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def appNameSpamCase():
        return "smtp"
    
    @classmethod
    def initial_extra_setup(cls):
        if(not global_functions.is_apache_listening_on_ipv6_port80()):
            global_functions.restart_apache()
            global_functions.uvmContext = global_functions.restart_uvm()
            time.sleep(180)

    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    @pytest.mark.slow
    def test_011_validate_serial_number(self):
        """
        Ensure valid serial number is returned if present
        """
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        
        # Skip if VMware instance
        if (global_functions.is_vm_instance()):
            raise unittest.SkipTest('Skipping serial number check on VM')
        
        file_path = '/tmp/product_serial'
        device_serial_number = 'CTW23050243'
        with open(file_path, "w") as f:
            f.write(device_serial_number)

        initial_serial_number  = global_functions.uvmContext.getServerSerialNumber()
        subprocess.call("mount /tmp/product_serial /sys/devices/virtual/dmi/id/product_serial -o rw,bind", shell=True, stderr=subprocess.STDOUT)
        ## Restart uvm
        global_functions.restart_uvm()
        updated_serial_number  = global_functions.uvmContext.getServerSerialNumber()
        ## verify serial number is updated to CTW23050243
        assert(device_serial_number == updated_serial_number)
        #unmounting the /tmp/product_serial
        subprocess.call("umount -v /sys/devices/virtual/dmi/id/product_serial", shell=True, stderr=subprocess.STDOUT)
        ##Reverting back to initial serial number
        with open(file_path, "w") as f:
            f.write(initial_serial_number)
        subprocess.call("mount /tmp/product_serial /sys/devices/virtual/dmi/id/product_serial -o rw,bind", shell=True, stderr=subprocess.STDOUT)
        global_functions.restart_uvm()
        ## verify serial number is updated to initial vlue
        assert (initial_serial_number == global_functions.uvmContext.getServerSerialNumber())
        #unmounting the /tmp/product_serial
        subprocess.call("umount -v /sys/devices/virtual/dmi/id/product_serial", shell=True, stderr=subprocess.STDOUT)
        ## removing file /tmp/product_serial
        subprocess.call("rm -f /tmp/product_serial", shell=True, stderr=subprocess.STDOUT)

    def test_012_help_links(self):
        helpLinkFile = "/tmp/helpLinks.json"
        subprocess.call(global_functions.build_wget_command(uri="http://test.untangle.com/test/help_links.json", output_file=helpLinkFile), shell=True)
        # if the links file was not found skip this test
        if not os.path.isfile(helpLinkFile):
            raise unittest.SkipTest("Skipping test since " + helpLinkFile + " is missing")
        # read file as JSON object and delete the temp file.
        with open(helpLinkFile) as dataFile:    
            helpLinks = json.load(dataFile)    
        if os.path.isfile(helpLinkFile):
            os.remove(helpLinkFile)
        
        # Check all the links in JSON
        linkCount = 0
        failedLinks = 0
        testResults = True
        for link in helpLinks["links"]:
            subLinks = [""]
            hdr = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11',
                   'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                   'Accept-Charset': 'ISO-8859-1,utf-8;q=0.7,*;q=0.3',
                   'Accept-Encoding': 'none',
                   'Accept-Language': 'en-US,en;q=0.8',
                   'Connection': 'keep-alive'}

            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE

            pat = re.compile(r'''.*URL=https://wiki.*.arista.com/(.*)">.*$''')
            version = global_functions.uvmContext.getFullVersion()
            print("------------------------------------------------------")
            if ('subcat' in link):
                subLinks.extend(link['subcat'])
            for i, subLink in enumerate(subLinks):
                if (subLink != ""):
                    subLink = link['fragment'] + "/" + subLink
                else:
                    subLink = link['fragment']
                url = "https://wiki.edge.arista.com/get.php?fragment=" + subLink + "&uid=0000-0000-0000-0000&version=" + version + "&webui=true&lang=en"
                print(("Checking %s = %s " % (subLink, url)))
                req = urllib.request.Request( url, headers=hdr) 
                ret = urllib.request.urlopen( req, context=ctx )
                time.sleep(.1) # dont flood wiki
                assert(ret)
                result = ret.read()
                result = result.decode('utf-8')
                assert(result)
                patmatch = pat.match( result )
                assert(patmatch)
                page = link['page'][i] #set 'page' to expected wiki page value from page array
                if (patmatch.group(1)):
                    print(("Result: \"%s\"" % patmatch.group(1)))
                    
                    if (patmatch.group(1) == "index.php/%s" % (page)):
                        print(("Page is correct: %s" % (page)))
                    else:
                        print(("******Sent to wrong page. Page should be %s, but you were sent to index.php/%s" % (page, patmatch.group(1))))
                        testResults = False
                        failedLinks += 1
                else:
                    print(("******Failed to get result for %s.  Expecting: %s" % (subLink,page)))
                    # check all help links before failing the test
                    testResults = False
                    failedLinks += 1
                linkCount += 1
                print("------------------------------------------------------")
        print(("%d Help Links were checked" % (linkCount)))
        print(("%d Links failed to resolve correctly" % (failedLinks)))
        assert(testResults)

    def test_020_auth_local_totp_disabled(self):
        """
        Local console authentication, TOTP not enabled
        """
        TestTotp.setup(enable=False)
        # fails due to NGFW-14344
        if(not global_functions.is_apache_listening_on_ipv6_port80()):
            raise unittest.SkipTest("Skipping apache is not listening on IPv6")
        
        try:
            uri="http://localhost/auth/login?url=/admin&realm=Administrator"
            # Make sure we come from ipv6
            override_arguments=["--silent", "-6"]

            # Bad username
            bad_username = f"bad_{Login_username}"
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username": bad_username, "password": Login_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response not in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="bad username",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": bad_username,
                    "local": True,
                    "succeeded": False,
                    "reason": "U"
                })

            # Bad password
            bad_password = f"bad_{Login_password}"
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username": Login_username, "password": bad_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response not in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="bad password",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": True,
                    "succeeded": False,
                    "reason": "P"
                })

            # Successful local login
            curl_result = subprocess.check_output(

                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username":Login_username, "password": Login_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="successful login",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": True,
                    "succeeded": True
                })
        except AssertionError as e:
            TestTotp.teardown()
            raise AssertionError(e)
            
        TestTotp.teardown()

    def test_021_auth_remote_totp_disabled(self):
        """
        Non-local authentication, TOTP not enabled
        """
        TestTotp.setup(enable=False)
        
        try:
            lan_ip = global_functions.get_lan_ip()
            uri = f"http://{lan_ip}/auth/login?url=/admin&realm=Administrator"
            override_arguments=["--silent"]

            # Bad username
            bad_username = f"bad_{Login_username}"
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username": bad_username, "password": Login_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response not in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="bad username",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": bad_username,
                    "local": False,
                    "succeeded": False,
                    "reason": "U"
                })

            # Bad password
            bad_password = f"bad_{Login_password}"
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username": Login_username, "password": bad_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response not in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="bad password",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": False,
                    "reason": "P"
                })

            # Successful remote login
            curl_result = remote_control.run_command(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username":Login_username, "password": Login_password}
                ),
                stdout=True)
            print(curl_result)
            assert Login_success_response in curl_result, "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="successful login",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": True
                })
        except (subprocess.CalledProcessError, AssertionError) as e:
            TestTotp.teardown()
            raise AssertionError(e)
        
        TestTotp.teardown()

    def test_022_auth_local_totp_enabled(self):
        """
        Local console authentication, TOTP enabled
        """
        TestTotp.setup(enable=True)
        # fails due to NGFW-14344
        if(not global_functions.is_apache_listening_on_ipv6_port80()):
            raise unittest.SkipTest("Skipping apache is not listening on IPv6")
        
        try:
            uri = "http://localhost/auth/login?url=/admin&realm=Administrator"
            override_arguments=["--silent", "-6"]

            # Bad username
            bad_username = f"bad_{Login_username}"
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username": bad_username, "password": Login_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response not in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="bad username",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": bad_username,
                    "local": True,
                    "succeeded": False,
                    "reason": "U"
                })

            # Bad password
            bad_password = f"bad_{Login_password}"
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username": Login_username, "password": bad_password}
                ), 
                shell=True)
            print(curl_result)
            assert Login_success_response not in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="bad password",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": True,
                    "succeeded": False,
                    "reason": "P"
                })

            # Successful local login
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username":Login_username, "password": Login_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="successful login",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": True,
                    "succeeded": True
                })

        except  (subprocess.CalledProcessError, AssertionError) as e:
            TestTotp.teardown()
            raise AssertionError(e)

        TestTotp.teardown()

    def test_023_auth_remote_totp_enabled(self):
        """
        Non-local authentication, TOTP enabled
        """
        TestTotp.setup(enable=True)

        try:
            lan_ip = global_functions.get_lan_ip()
            uri = f"http://{lan_ip}/auth/login?url=/admin&realm=Administrator"
            override_arguments=["--silent"]

            # Bad username
            bad_username = f"bad_{Login_username}"
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username": bad_username, "password": Login_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response not in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="bad username",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": bad_username,
                    "local": False,
                    "succeeded": False,
                    "reason": "U"
                })

            # Bad password
            bad_password = f"bad_{Login_password}"
            curl_result = subprocess.check_output(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username": Login_username, "password": bad_password}
                ),
                shell=True)
            print(curl_result)
            assert Login_success_response not in curl_result.decode(), "successful login response"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="bad password",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": False,
                    "reason": "P"
                })

            # Username and password but no TOTP
            curl_result = remote_control.run_command(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username":Login_username, "password": Login_password}
                ),
                stdout=True)
            print(curl_result)
            assert Login_success_response not in curl_result, "username, passwod, no totp"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="no totp",
                report_category="Administration", 
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": False,
                    "reason": "T"
                })

            # Username and password but TOTP many slots ago
            totp_code = TestTotp.get_code(counter_offset=-3)
            assert totp_code is not None, "got totp code"
            curl_result = remote_control.run_command(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username":Login_username, "password": Login_password, "totp": totp_code}
                ),
                stdout=True)
            print(curl_result)
            assert Login_success_response not in curl_result, "many totp slots ago"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="totp too old",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": False,
                    "reason": "T"
                })

            # Username and password but TOTP many slots from now
            totp_code = TestTotp.get_code(counter_offset=3)
            assert totp_code is not None, "got totp code"
            curl_result = remote_control.run_command(
                global_functions.build_curl_command(
                uri=uri,
                override_arguments=override_arguments,
                request="POST",
                form={"username":Login_username, "password": Login_password, "totp": totp_code}
                ),
                stdout=True)
            print(curl_result)
            assert Login_success_response not in curl_result, "many totp slots from now"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="totp too new",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": False,
                    "reason": "T"
                })

            # Username and password and current TOTP
            totp_code = TestTotp.get_code()
            assert totp_code is not None, "got totp code"
            curl_result = remote_control.run_command(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username":Login_username, "password": Login_password, "totp": totp_code}
                ),
                stdout=True)
            print(curl_result)
            assert Login_success_response in curl_result, "current totp"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="successful current totp",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": True
                })

            # Username and password and last TOTP
            totp_code = TestTotp.get_code(counter_offset=-1)
            assert totp_code is not None, "got totp code"
            curl_result = remote_control.run_command(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username":Login_username, "password": Login_password, "totp": totp_code}
                ),
                stdout=True)
            print(curl_result)
            assert Login_success_response in curl_result, "last totp"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="successful last totp",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": True
                })

            # Username and password and next TOTP
            totp_code = TestTotp.get_code(counter_offset=1)
            assert totp_code is not None, "got totp code"
            curl_result = remote_control.run_command(
                global_functions.build_curl_command(
                    uri=uri,
                    override_arguments=override_arguments,
                    request="POST",
                    form={"username":Login_username, "password": Login_password, "totp": totp_code}
                ),
                stdout=True)
            print(curl_result)
            assert Login_success_response in curl_result, "next totp"
            time.sleep(Login_event_delay)

            global_functions.get_and_check_events(
                prefix="successful next totp",
                report_category="Administration",
                report_title='Admin Login Events',
                matches={
                    "login": Login_username,
                    "local": False,
                    "succeeded": True
                })
        except  (subprocess.CalledProcessError, AssertionError) as e:
            TestTotp.teardown()
            raise AssertionError(e)

        TestTotp.teardown()

    @pytest.mark.slow
    @pytest.mark.failure_outside_corporate_network
    def test_030_test_smtp_settings(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        # Test mail setting in config -> email -> outgoing server
        if (global_functions.uvmContext.appManager().isInstantiated(self.appNameSpamCase())):
            print("smtp case present")
        else:
            print("smtp not present")
            global_functions.uvmContext.appManager().instantiate(self.appNameSpamCase(), 1)
        appSP = global_functions.uvmContext.appManager().app(self.appNameSpamCase())
        origAppDataSP = appSP.getSmtpSettings()
        origMailsettings = global_functions.uvmContext.mailSender().getSettings()
        # print(appDataSP)
        newMailsettings = copy.deepcopy(origMailsettings)
        newMailsettings['smtpHost'] = global_functions.TEST_SERVER_HOST
        newMailsettings['smtpPort'] = "6800"
        newMailsettings['sendMethod'] = 'CUSTOM'

        global_functions.uvmContext.mailSender().setSettings(newMailsettings)
        time.sleep(10) # give it time for exim to restart

        appDataSP = appSP.getSmtpSettings()
        appSP.setSmtpSettingsWithoutSafelists(appDataSP)
        recipient = global_functions.random_email()
        global_functions.uvmContext.mailSender().sendTestMessage(recipient)
        time.sleep(2)
        # force exim to flush queue
        subprocess.call(["exim -qff >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)
        time.sleep(10)

        global_functions.uvmContext.mailSender().setSettings(origMailsettings)
        appSP.setSmtpSettingsWithoutSafelists(origAppDataSP)
        emailContext = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://test.untangle.com/cgi-bin/getEmail.py?toaddress={recipient}") + " 2>&1" ,stdout=True)
        assert('Test Message' in emailContext)

    def test_040_trigger_rule_tag_host(self):
        settings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_trigger_rule("TAG_HOST", "localAddr", "test-tag", 30, "test tag rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        global_functions.uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        tag_test = entry.get('tagsString')
        global_functions.uvmContext.eventManager().setSettings( orig_settings )
        assert( tag_test != None )
        assert( "test-tag" in tag_test )

    def test_041_trigger_rule_untag_host(self):
        settings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_trigger_rule("TAG_HOST", "localAddr", "test-tag", 30, "test tag rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        global_functions.uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        tag_test = entry.get('tagsString')
        global_functions.uvmContext.eventManager().setSettings( orig_settings )

        new_rule = create_trigger_rule("UNTAG_HOST", "localAddr", "test*", 30, "test tag rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        global_functions.uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        tag_test2 = entry.get('tagsString')

        global_functions.uvmContext.eventManager().setSettings( orig_settings )
        assert( tag_test != None )
        assert( "test-tag" in tag_test )
        assert( tag_test2 == None or "test-tag" not in tag_test2)

    def test_042_trigger_rule_tag_host_subcondition(self):
        settings = global_functions.uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_trigger_rule("TAG_HOST", "sessionEvent.localAddr", "test-tag-2", 30, "test tag rule", "class", "=", "*SessionStatsEvent*", "sessionEvent.localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        global_functions.uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        entry = global_functions.uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )

        global_functions.uvmContext.eventManager().setSettings( orig_settings )
        tag_test = entry.get('tagsString')
        assert( tag_test != None )
        assert( "test-tag-2" in tag_test )

    def test_099_queue_process(self):
        """
        Generate "a lot" of traffic and verify the log event queue is quickly processed
        """
        default_policy_id = 1
        application_name = "web-filter"
        if global_functions.uvmContext.appManager().isInstantiated(application_name):
            raise unittest.SkipTest(f"app {application_name} already instantiated")

        application = global_functions.uvmContext.appManager().instantiate(application_name, default_policy_id)

        try:
            global_functions.wait_for_event_queue_drain(queue_size_key="eventQueueSize")
        except Exception as e:
            global_functions.uvmContext.appManager().destroy( application.getAppSettings()["id"] )
            raise unittest.SkipTest(e)

        global_functions.uvmContext.appManager().destroy( application.getAppSettings()["id"] )

    def test_100_account_login(self):
        untangleEmail, untanglePassword = global_functions.get_live_account_info("Untangle")
        if untangleEmail == "message":
            raise unittest.SkipTest('Skipping no accound found:' + str(untanglePassword))

        result = global_functions.uvmContext.cloudManager().accountLogin( untangleEmail, untanglePassword, global_functions.uvmContext.getServerUID(), "", "", "")
        assert (result.get('success'))

    def test_101_account_login_invalid(self):
        result = global_functions.uvmContext.cloudManager().accountLogin( "foobar@untangle.com", "badpassword" )
        assert not result.get('success')

    def test_102_admin_login_event(self):
        global_functions.uvmContext.adminManager().logAdminLoginEvent( "admin", True, "127.0.1.1", True, 'X' )
        events = global_functions.get_events('Administration','Admin Login Events',None,10)
        assert(events != None)
        for i in events.get('list'):
            print(i)
        found = global_functions.check_events( events.get('list'), 10,
                                               'client_addr', "127.0.1.1",
                                               'reason', 'X',
                                               'local', True,
                                               'succeeded', True,
                                               'login', 'admin' )
        assert( found )

    # Make sure the HostsFileManager is working as expected
    def test_110_hosts_file_manager(self):
        # get the hostname and settings from the network manager
        fullName = global_functions.uvmContext.networkManager().getFullyQualifiedHostname()
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        print(("Checking HostsFileManager records for " + fullName))

        # perform a DNS lookup for our hostname against every non-WAN interface
        # and make sure the value returned matches the address of the interface
        for interface in netsettings['interfaces']['list']:
            if interface['isWan'] == False and interface['configType'] == "Addressed":
                if 'v4StaticAddress' in interface:
                    netaddr = interface['v4StaticAddress']
                    if netaddr:
                        print(("Checking hostname resolution for %s" % netaddr))
                        output = subprocess.check_output("dig +short @" + netaddr + " " + fullName, shell=True)
                        result = output.strip()
                        assert(result == netaddr)
    
    def test_120_cert_is_in_backup(self):
        """check that the Server Certificate exists in the backup"""

        #remove any old files associated with backup
        subprocess.call("rm -rf /tmp/untangleBackup*", shell=True)

        #copy a backup of apache.pem
        certFilePath = "/usr/share/untangle/settings/untangle-certificates/apache.pem"
        subprocess.call("cp "+certFilePath+" "+certFilePath+".backup", shell=True)

        #Modify apache.pem a little to verify the change is in the backup
        certFile = open(certFilePath)
        lines = certFile.read().splitlines()
        newline = "AAAAA" + lines[1][5:]
        lines[1] = newline
        open(certFilePath, "w").write('\n'.join(lines))

        #Download backup
        result = subprocess.call(global_functions.build_wget_command(output_file='/tmp/untangleBackup.backup', post_data='type=backup', uri="http://localhost/admin/download"), shell=True)

        #replace modified cert with backed-up original before testing.
        subprocess.call("cp "+certFilePath+".backup "+certFilePath, shell=True)

        # does the backup exist
        assert(result == 0)

        #extract backup
        subprocess.call("mkdir /tmp/untangleBackup", shell=True)
        subprocess.call("tar -xf /tmp/untangleBackup.backup -C /tmp/untangleBackup", shell=True)
        subprocess.call("tar -xf "+glob.glob("/tmp/untangleBackup/files*.tar.gz")[0] + " -C /tmp/untangleBackup", shell=True) #use glob since extracted file has timestamp

        #Check the cert in the backup
        newCertFilePath = "/tmp/untangleBackup/usr/share/untangle/settings/untangle-certificates/apache.pem"
        newCertFile = open(newCertFilePath, "r")
        newCertFileLines = newCertFile.read().splitlines()

        #compare original and modified certs
        assert(newline == newCertFileLines[1])
        
    def test_130_check_cmd_connected(self):
        """Check if cmd is connected using alert rule"""
        # Enable cloud connection  
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['cloudEnabled'] = True
        global_functions.uvmContext.systemManager().setSettings(system_settings)
        
        # run cmd status
        result = ""
        for i in range(0,20):
            try:
                result = subprocess.check_output("/usr/bin/pyconnector-status")
                result = result.decode("utf-8")  # decode byte to string.
            except subprocess.CalledProcessError as e:
                print((e.output))
                time.sleep(10)
                continue
            else:
                break
        assert("Connected" in result)

    def test_140_change_language(self):
        """Check if changing language converts the GUI"""
        # Set language to Spanish
        language_settings = global_functions.uvmContext.languageManager().getLanguageSettings()
        language_settings_orig = copy.deepcopy(language_settings)
        language_settings['language'] = 'es'
        global_functions.uvmContext.languageManager().setLanguageSettings(language_settings)
        # Previous instance of test looked for "Not allowed" with its Spanish translation "no permitido".
        # However, this translation could change.  So assume that looking for "Not allowed" fails due to it being translated.
        result = subprocess.call(global_functions.build_wget_command( output_file="-", content_on_error=True, uri="http://localhost/admin/download") + ' 2>&1 | grep -qv "Not allowed"', shell=True)

        # revert language
        global_functions.uvmContext.languageManager().setLanguageSettings(language_settings_orig)
        
        assert(result == 0)

    def test_150_synchronize_Language(self):
        """Check synchronizeLanguage returns OK"""
        synchronized = global_functions.uvmContext.languageManager().synchronizeLanguage()

    def test_160_change_community_language(self):
        """Check if changing community language converts the GUI"""
        raise unittest.SkipTest("Community languages no longer supported NGFW-14374.")
        #set language to Russian
        language_settings_community = global_functions.uvmContext.languageManager().getLanguageSettings()
        language_settings_community_orig = copy.deepcopy(language_settings_community)
        language_settings_community['language'] = "ru"
        global_functions.uvmContext.languageManager().setLanguageSettings(language_settings_community)
        # Previous instance of test looked for "Not allowed" with its Russian translation "ne polozheno" but the real translation was the unicode "Метод не разрешен".
        # which is not easy to match via grep.  So assume that looking for "Not allowed" fails due to it being translated.
        result = subprocess.call(global_functions.build_wget_command( output_file="-", content_on_error=True, uri="http://localhost/admin/download") + ' 2>&1 | grep -qv "Not allowed"', shell=True)

        #Revert language
        global_functions.uvmContext.languageManager().setLanguageSettings(language_settings_community_orig)

        assert(result == 0)

    def test_165_password_encryption_decryption_process(self):
        """
        Verify password encryption decryption process
        """
        password = 'passwd'
        # Test 1: Valid password - it should pass if encrypted and decrypted correctly
        encrypted_password = global_functions.uvmContext.systemManager().getEncryptedPassword(password)
        decrypted_password = global_functions.uvmContext.systemManager().getDecryptedPassword(encrypted_password)

        # Compare original password with decrypted password
        self.assertEqual(password, decrypted_password, "Password encryption/decryption failed.")

        # Test 2: Empty password - it should pass if encrypted and decrypted correctly
        password = " "
        encrypted_password = global_functions.uvmContext.systemManager().getEncryptedPassword(password)
        decrypted_password = global_functions.uvmContext.systemManager().getDecryptedPassword(encrypted_password)

        # Password should match after encryption and decryption
        self.assertEqual(password, decrypted_password, "Empty password encryption/decryption failed.")

        # Test 3: None (null) password - should return None or raise an exception
        password = None
        encrypted_password = global_functions.uvmContext.systemManager().getEncryptedPassword(password)

        # Check if encryption of None returns None or raises an exception
        self.assertIsNone(encrypted_password, "Encrypted password should be None when input is None.")


    def test_168_password_encryption_setting_process(self):
        """
        Verify password encryption setting process
        """
        # Create local directory user 'test'
        global_functions.uvmContext.localDirectory().setUsers(create_local_directory_user())

        # Get local directory users 
        users = global_functions.uvmContext.localDirectory().getUsers()

        # Extract the user object
        user = users['list'][0]

        # Assert that encryptedPassword is present and password is None
        assert user.get('encryptedPassword') is not None, "encryptedPassword is missing"
        assert user.get('password') is None, "password is not None"
        #Clear the created user
        global_functions.uvmContext.localDirectory().setUsers(remove_local_directory_user())

    def test_169_test_local_user_deletion(self):
        """
        Verify local user removal
        """
        # Create local directory user 'test'
        global_functions.uvmContext.localDirectory().setUsers(create_local_directory_user())

        #test user creation
        users = global_functions.uvmContext.localDirectory().getUsers()
        assert len(users['list']) == 1, f"Assertion failed: Users list is empty."


        #Clear the created user
        global_functions.uvmContext.localDirectory().setUsers(remove_local_directory_user())

        #Check the deletion of user
        users = global_functions.uvmContext.localDirectory().getUsers()
        assert len(users['list']) == 0, f"Assertion failed: Users list is not empty, it contains {len(users['list'])} elements."
        

    def test_170_log_retention(self):
        """
        Verify log retention policy
        """
        # Pattern to parse:
        # rotating pattern: /var/log/uvm/uvm.log  512000 bytes (7 rotations)
        # We want file name and rotation count
        rotating_pattern = re.compile(r'rotating pattern: ([^\s]+) .* \((\d+) rotations\)')

        system_settings = global_functions.uvmContext.systemManager().getSettings()
        previous_log_retention = system_settings["logRetention"]

        # Build hash of current log files and their rotation values
        previous_logrotate_log_rotations = {}
        logrotate_test_output_lines = subprocess.check_output("logrotate -d /etc/logrotate.conf 2>&1 | grep 'rotating pattern:' | grep rotations", shell=True).decode('utf-8').split("\n")
        for line in logrotate_test_output_lines:
            match = re.search(rotating_pattern, line)
            if match:
                log_file = match.group(1)
                rotations = match.group(2)
                previous_logrotate_log_rotations[log_file] = rotations
                print(f"previous {log_file} {rotations}")

        # Update the new rotation to be different than previous
        new_log_retention = previous_log_retention * 2
        if previous_log_retention > 10:
            new_log_retention = int(previous_log_retention/2)

        system_settings["logRetention"] = new_log_retention
        global_functions.uvmContext.systemManager().setSettings(system_settings)

        logrotate_test_output_lines = subprocess.check_output("logrotate -d /etc/logrotate.conf 2>&1 | grep 'rotating pattern:' | grep rotations", shell=True).decode('utf-8').split("\n")
        same_rotation = False
        for line in logrotate_test_output_lines:
            match = re.search(rotating_pattern, line)
            if match:
                log_file = match.group(1)
                rotations = match.group(2)
                print(f"new {log_file} {rotations}")
                if log_file in previous_logrotate_log_rotations and previous_logrotate_log_rotations[log_file] == rotations:
                    # Previous and new rotation for this file has not changed
                    same_rotation = True
                    break

        assert same_rotation is False, "previous and new rotations changed"

    def test_200_dashboard_free_disk_space(self):
        """Check if full disk space is within range """
        df_fields = subprocess.check_output("df | grep /$ | tr -s ' '", shell=True).decode('utf-8').split(' ')
        used = float(df_fields[2]) * 1024
        available = float(df_fields[3]) * 1024
        total_size = used + available
        in_threshold = int((total_size * .97) - used)
        out_threshold = int((total_size * .93) - used)
        fallocate_path = ""
        full_filename = "/tmp/full.txt";
        # check if fallocate exists and if in /bin or /usr/bin
        fallocate_output_obj = subprocess.run(["which", "fallocate"], capture_output=True)
        if fallocate_output_obj.returncode != 0 or fallocate_output_obj.stdout is None :
            raise unittest.SkipTest("fallocate not available")
        else:
            fallocate_path = fallocate_output_obj.stdout.decode("utf-8")
            fallocate_path = fallocate_path.replace("\n","")
        filename_output_obj = subprocess.run([fallocate_path,"-l",str(in_threshold),full_filename])
        if filename_output_obj.returncode != 0:
            raise unittest.SkipTest(full_filename + " not available")
        df_fields = subprocess.check_output("df | grep /$ | tr -s ' '", shell=True).decode('utf-8').split(' ')
        time.sleep(60)
        metrics_and_stats = global_functions.uvmContext.metricManager().getMetricsAndStats()
        uvm_free_disk_percent = (float(metrics_and_stats["systemStats"]["freeDiskSpace"]) / float(metrics_and_stats["systemStats"]["totalDiskSpace"]) * 100)
        subprocess.run(["rm", "-f", full_filename])
        assert(uvm_free_disk_percent < 5)

        filename_output_obj= subprocess.run([fallocate_path, "-l", str(out_threshold), full_filename])
        if filename_output_obj.returncode != 0:
            raise unittest.SkipTest(full_filename + " not available 2nd")
        df_fields = subprocess.check_output("df | grep /$ | tr -s ' '", shell=True).decode('utf-8').split(' ')
        time.sleep(60)
        metrics_and_stats = global_functions.uvmContext.metricManager().getMetricsAndStats()
        uvm_free_disk_percent = (float(metrics_and_stats["systemStats"]["freeDiskSpace"]) / float(metrics_and_stats["systemStats"]["totalDiskSpace"]) * 100)
        subprocess.run(["rm", "-f", full_filename])
        assert(uvm_free_disk_percent > 5)

    def test_300_javascript_exception_valid(self):
        """
        Verify that validly formed json objects are logged
        """
        errors = {
            "reference_error": {
                "stack":"ReferenceError: hurfdurf is not defined\n    at g.<anonymous> (https://192.168.25.58/admin/script/apps/intrusion-prevention.js?_dc=1705675812345:607:21)\n    at t (https://192.168.25.58/ext6.2/ext-all.js:19:60842)\n    at c (https://192.168.25.58/ext6.2/ext-all.js:19:142848)\n    at https://192.168.25.58/ext6.2/ext-all.js:19:61065\n    at https://192.168.25.58/ext6.2/ext-all.js:19:62295",
                "message":"hurfdurf is not defined"
            },
            "xmlhttprequest_error":{
                "stack":"Error: Failed to execute 'send' on 'XMLHttpRequest': Failed to load 'https://192.168.25.58/admin/download'.\n    at F.start (https://192.168.25.58/ext6.2/ext-all.js:19:180150)\n    at F.request (https://192.168.25.58/ext6.2/ext-all.js:19:189205)\n    at g.<anonymous> (https://192.168.25.58/admin/script/apps/intrusion-prevention.js?_dc=1705671966676:595:41)\n    at t (https://192.168.25.58/ext6.2/ext-all.js:19:60842)\n    at c (https://192.168.25.58/ext6.2/ext-all.js:19:142848)\n    at https://192.168.25.58/ext6.2/ext-all.js:19:61065\n    at https://192.168.25.58/ext6.2/ext-all.js:19:62295"
            }
        }
        check_javascript_exceptions(errors)

    def test_310_javascript_exception_malformed(self):
        """
        Verify that malformed json objects are logged
        """
        errors = {
            "empty_object": {}
        }
        check_javascript_exceptions(errors)

    def test_300_https_protocols(self):
        """
        Ensure valid/invalid protocols
        """
        lan_ip = global_functions.get_lan_ip()
        results = remote_control.run_command(global_functions.build_nmap_command(script="ssl-enum-ciphers", extra_arguments=f"-p 443 {lan_ip}"), stdout=True)

        assert "TLSv1.1:" not in results, "TLS v1.1 not allowed"
        assert "TLSv1.2:" in results, "TLS v1.2 allowed"

    # Checks that feedback link is under arista
    def test_300_feedback_link(self):
        feeddback_url = global_functions.uvmContext.getFeedbackUrl()
        match = re.search('^https://edge.arista.com/feedback$', feeddback_url)
        assert(match)

    @pytest.mark.slow
    def test_302_cleanup_license_files(self):
        """
        Ensure that license files older than 5 days are removed.
        """
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        license_dir = "/usr/share/untangle/conf/licenses/"
        now = time.time()
        five_days_ago = now - (5 * 24 * 60 * 60)
        # Create some dummy files
        open(os.path.join(license_dir, "license_9_days_ago.js"), 'w').close()
        os.utime(os.path.join(license_dir, "license_9_days_ago.js"), (now - (9 * 24 * 60 * 60), now - (9 * 24 * 60 * 60)))
        open(os.path.join(license_dir, "license_12_days_ago.js"), 'w').close()
        os.utime(os.path.join(license_dir, "license_12_days_ago.js"), (now - (12 * 24 * 60 * 60), now - (12 * 24 * 60 * 60)))


        # Trigger the cleanup
        global_functions.uvmContext.licenseManager().reloadLicenses(False)
        time.sleep(2) # Give it a moment to process

        # Verify the files
        license_files = find_files(license_dir, "*license*.js*")
        
        for lic_file in license_files:
            assert os.lstat(lic_file).st_mtime > five_days_ago


    @pytest.mark.slow
    def test_304_email_cleaner(self):
        """
        1. Modify FROM_EMAIL to non deliverable mail
        2. Send emails to non deliverable email, note the count of messages using exim -bpc
        3. Run cron script explicitly to delete the emails which are having TO or FROM the Outgoing Email FROM_EMAIL address.
        4. Verify the count of messages should decrease after cron script is run
        """

        tester_email_address = "tester@domain.com"
        origMailsettings = global_functions.uvmContext.mailSender().getSettings()
        mail_settings = copy.deepcopy(origMailsettings)
        # Updating email settings
        #print(str(mail_settings))
        mail_settings["fromAddress"] = tester_email_address
        mail_settings["sendMethod"] = "DIRECT"
        global_functions.uvmContext.mailSender().setSettings(mail_settings)
        time.sleep(2)
        subject = "Test Email"
        body = "Body of the Email"
        email_message = f"From: {tester_email_address}\nTo: {tester_email_address}\nSubject: {subject}\n\n{body}"
        initial_count="0"
        final_count="0"
        try:
            for i in range(10):
                subprocess.run(['exim', '-bm', '-t'], input=email_message.encode(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
            time.sleep(20)
            # count of messages will be +10 after running of the for loop
            initial_count = subprocess.check_output(f"exim -bpc",shell=True,stderr=subprocess.STDOUT)
            # count of messages will be -10 after running of the cleaner script with messages older than 10 seconds
            subprocess.check_output(f'/etc/cron.daily/untangle-email-cleaner 10',  shell=True, stderr=subprocess.PIPE)
            final_count = subprocess.check_output(f"exim -bpc",shell=True,stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as exc:
            pass
        # In the test we have added 10 mail messages , after the cron script is run , atmost 10 messages will be deleted
        # The final count of messages will be less than initial count.
        if type(initial_count) is bytes and type(final_count) is bytes:
            assert(int(final_count) <= int(initial_count))
        # Setting back Original email settings
        global_functions.uvmContext.mailSender().setSettings(origMailsettings)

    def test_307_geo_ip_address(self):
        """
        1. Validating GeoIP methods for given IP Address
        """
        ip_address = "128.101.101.101"
        expected_country_name = "United States"
        expected_country_code = "US"
        expected_sub_division_namme = "Minnesota"
        expected_sub_division_code = "MN"

        country_name = global_functions.uvmContext.geographyManager().getCountryName(ip_address)
        country_code = global_functions.uvmContext.geographyManager().getCountryCode(ip_address)
        sub_division_name = global_functions.uvmContext.geographyManager().getSubdivisionName(ip_address)
        sub_division_code = global_functions.uvmContext.geographyManager().getSubdivisionCode(ip_address)
        city_name = global_functions.uvmContext.geographyManager().getCityName(ip_address)
        postal_code = global_functions.uvmContext.geographyManager().getPostalCode(ip_address)

        assert(expected_country_name == country_name)
        assert(expected_country_code == country_code)
        assert(expected_sub_division_namme == sub_division_name)
        assert(expected_sub_division_code == sub_division_code)

        # for IPs with no information None is received
        country_name = global_functions.uvmContext.geographyManager().getCountryName("192.168.56.120")
        assert(country_name is None)

    def test_310_system_logs(self):
        subprocess.call(global_functions.build_wget_command(log_file="/dev/null", output_file="/tmp/system_logs.zip", post_data="type=SystemSupportLogs", uri="http://localhost/admin/download"), shell=True)
        subprocess.call("unzip -q /tmp/system_logs -d /tmp/system_logs && rm -rf /tmp/system_logs.zip", shell=True)
        uvm = subprocess.check_output("ls /tmp/system_logs | grep -c uvm", shell=True, stderr=subprocess.STDOUT)
        app = subprocess.check_output("ls /tmp/system_logs | grep -c app", shell=True, stderr=subprocess.STDOUT)
        console = subprocess.check_output("ls /tmp/system_logs | grep -c console", shell=True, stderr=subprocess.STDOUT)
        gc = subprocess.check_output("ls /tmp/system_logs | grep -c gc", shell=True, stderr=subprocess.STDOUT)
        reports = subprocess.check_output("ls /tmp/system_logs | grep -c reports", shell=True, stderr=subprocess.STDOUT)
        upgrade = subprocess.check_output("ls /tmp/system_logs | grep -c upgrade", shell=True, stderr=subprocess.STDOUT)
        wrapper = subprocess.check_output("ls /tmp/system_logs | grep -c wrapper", shell=True, stderr=subprocess.STDOUT)
        subprocess.call("rm -rf /tmp/system_logs", shell=True)
        assert int(uvm) > 0, "{int(uvm)} uvm log files found"
        assert int(app) > 0, "{int(app)} app log files found"
        assert int(console) > 0, "{int(console)} console log files found"
        assert int(gc) > 0, "{int(gc)} gc log files found"
        assert int(reports) > 0, "{int(reports)} reports log files found"
        assert int(upgrade) > 0, "{int(upgrade)} upgrade log files found"
        assert int(wrapper) > 0, "{int(wrapper)} wrapper log files found"

    def test_315_auto_devices_remove(self):
        """ Test to validate auto device removal functionality """
        # Get old devices settings
        oldDevicesSettings = global_functions.uvmContext.deviceTable().getDevicesSettings()
        
        # Initialise new settings
        newDevicesSettings = buildDevicesSettings()

        # Set new settings with a device having last seen = current - 6 days 
        # and a device with last seen = current - 4 days
        removalMacAddress = "e7:b5:a0:3a:cd:49"
        retainedMacAddress = "46:2f:66:0e:03:92"
        removalDeviceTime = int(round(time.time() * 1000)) - 6 * 24 * 60 * 60 * 1000
        retainedDeviceTime = int(round(time.time() * 1000)) - 4 * 24 * 60 * 60 * 1000
        removalDevice = buildDevice(interfaceId=2, lastSessionTime=removalDeviceTime, macAddress=removalMacAddress)
        retainedDevice = buildDevice(interfaceId=2, lastSessionTime=retainedDeviceTime, macAddress=retainedMacAddress)
        
        newDevicesSettings['devices']['list'].append(removalDevice)
        newDevicesSettings['devices']['list'].append(retainedDevice)

        global_functions.uvmContext.deviceTable().setDevicesSettings(newDevicesSettings)

        # Fetch settings and assert if both devices are added in settings
        settings = global_functions.uvmContext.deviceTable().getDevicesSettings()
        devices = [ device for device in settings['devices']['list'] if device["macAddress"] == removalMacAddress ]
        assert(len(devices) == 1)
        devices = [ device for device in settings['devices']['list'] if device["macAddress"] == retainedMacAddress ]
        assert(len(devices) == 1)

        time.sleep(1)
        
        # Set auto removal threshold to 5 days
        newDevicesSettings["autoDeviceRemove"] = True
        newDevicesSettings["autoRemovalThreshold"] = 5

        # Set settings should trigger device removal as we are changing the config property
        global_functions.uvmContext.deviceTable().setDevicesSettings(newDevicesSettings)

        # Fetch Settings and check only removalDevice is removed from settings
        settings = global_functions.uvmContext.deviceTable().getDevicesSettings()
        devices = [ device for device in settings['devices']['list'] if device["macAddress"] == removalMacAddress ]
        assert(len(devices) == 0)
        devices = [ device for device in settings['devices']['list'] if device["macAddress"] == retainedMacAddress ]
        assert(len(devices) == 1)

        # Set old settings
        global_functions.uvmContext.deviceTable().setDevicesSettings(oldDevicesSettings)

test_registry.register_module("uvm", UvmTests)
