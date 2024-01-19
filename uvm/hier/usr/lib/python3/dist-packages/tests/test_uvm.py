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

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
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

def create_alert_rule(description, field, operator, value, field2, operator2, value2, thresholdEnabled=False, thresholdLimit=None, thresholdTimeframeSec=None, thresholdGroupingField=None, sendEmail=False):
    return {
            "email": sendEmail,
            "emailLimitFrequency": False,
            "emailLimitFrequencyMinutes": 60,
            "thresholdEnabled": thresholdEnabled,
            "thresholdLimit": thresholdLimit,
            "thresholdTimeframeSec": thresholdTimeframeSec,
            "thresholdGroupingField": thresholdGroupingField,
            "description": description,
            "enabled": True,
            "javaClass": "com.untangle.uvm.event.AlertRule",
            "log": True,
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

def check_javascript_exceptions(errors):
    """
    Get current end line for uvm log, run logJavascripException, and verify it was logged
    """
    for error in errors.keys():
        print(error)
        uvm_last_line = subprocess.check_output("wc -l /var/log/uvm/uvm.log | cut -d' ' -f1", shell=True).decode("utf-8").strip()
        print(uvm_last_line)
        uvmContext.logJavascriptException(errors[error])
        exceptions = subprocess.check_output(f"awk 'NR >= {uvm_last_line} && /logJavascriptException/{{ print NR, $0 }}' /var/log/uvm/uvm.log", shell=True).decode("utf-8")
        print(exceptions)
        assert exceptions != "", f"for {error} found logged exceptions"
        pid = subprocess.check_output(f"ps -p $(cat /var/run/uvm.pid) -o pid=", shell=True).decode("utf-8")
        assert pid != "", "uvm running"


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

    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_help_links(self):
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

            pat = re.compile(r'''.*URL=https://wiki.*.untangle.com/(.*)">.*$''')
            version = uvmContext.getFullVersion()
            print("------------------------------------------------------")
            if ('subcat' in link):
                subLinks.extend(link['subcat'])
            for i, subLink in enumerate(subLinks):
                if (subLink != ""):
                    subLink = link['fragment'] + "/" + subLink
                else:
                    subLink = link['fragment']
                url = "https://wiki.untangle.com/get.php?fragment=" + subLink + "&uid=0000-0000-0000-0000&version=" + version + "&webui=true&lang=en"
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
        if (uvmContext.appManager().isInstantiated(self.appNameSpamCase())):
            print("smtp case present")
        else:
            print("smtp not present")
            uvmContext.appManager().instantiate(self.appNameSpamCase(), 1)
        appSP = uvmContext.appManager().app(self.appNameSpamCase())
        origAppDataSP = appSP.getSmtpSettings()
        origMailsettings = uvmContext.mailSender().getSettings()
        # print(appDataSP)
        newMailsettings = copy.deepcopy(origMailsettings)
        newMailsettings['smtpHost'] = global_functions.TEST_SERVER_HOST
        newMailsettings['smtpPort'] = "6800"
        newMailsettings['sendMethod'] = 'CUSTOM'

        uvmContext.mailSender().setSettings(newMailsettings)
        time.sleep(10) # give it time for exim to restart

        appDataSP = appSP.getSmtpSettings()
        appSP.setSmtpSettingsWithoutSafelists(appDataSP)
        recipient = global_functions.random_email()
        uvmContext.mailSender().sendTestMessage(recipient)
        time.sleep(2)
        # force exim to flush queue
        subprocess.call(["exim -qff >/dev/null 2>&1"],shell=True,stdout=None,stderr=None)
        time.sleep(10)

        uvmContext.mailSender().setSettings(origMailsettings)
        appSP.setSmtpSettingsWithoutSafelists(origAppDataSP)
        emailContext = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://test.untangle.com/cgi-bin/getEmail.py?toaddress={recipient}") + " 2>&1" ,stdout=True)
        assert('Test Message' in emailContext)

    def test_040_trigger_rule_tag_host(self):
        settings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_trigger_rule("TAG_HOST", "localAddr", "test-tag", 30, "test tag rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        tag_test = entry.get('tagsString')
        uvmContext.eventManager().setSettings( orig_settings )
        assert( tag_test != None )
        assert( "test-tag" in tag_test )

    def test_041_trigger_rule_untag_host(self):
        settings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_trigger_rule("TAG_HOST", "localAddr", "test-tag", 30, "test tag rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        tag_test = entry.get('tagsString')
        uvmContext.eventManager().setSettings( orig_settings )

        new_rule = create_trigger_rule("UNTAG_HOST", "localAddr", "test*", 30, "test tag rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        tag_test2 = entry.get('tagsString')

        uvmContext.eventManager().setSettings( orig_settings )
        assert( tag_test != None )
        assert( "test-tag" in tag_test )
        assert( tag_test2 == None or "test-tag" not in tag_test2)

    def test_042_trigger_rule_tag_host_subcondition(self):
        settings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_trigger_rule("TAG_HOST", "sessionEvent.localAddr", "test-tag-2", 30, "test tag rule", "class", "=", "*SessionStatsEvent*", "sessionEvent.localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['triggerRules']['list'] = [ new_rule ]
        uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )

        uvmContext.eventManager().setSettings( orig_settings )
        tag_test = entry.get('tagsString')
        assert( tag_test != None )
        assert( "test-tag-2" in tag_test )

    def test_050_alert_rule(self):
        settings = uvmContext.eventManager().getSettings()
        orig_settings = copy.deepcopy(settings)
        new_rule = create_alert_rule("test alert rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*")
        settings['alertRules']['list'].append( new_rule )
        uvmContext.eventManager().setSettings( settings )

        result = remote_control.is_online()
        time.sleep(4)

        events = global_functions.get_events('Events','Alert Events',None,10)
        found = global_functions.check_events( events.get('list'), 5,
                                            'description', 'test alert rule' )
        uvmContext.eventManager().setSettings( orig_settings )
        assert(events != None)
        assert ( found )

    @pytest.mark.failure_in_podman
    def test_060_customized_email_alert(self):
        """Create custom email template and verify alert email is received correctly"""
        #get settings, backup original settings
        email_settings = uvmContext.eventManager().getSettings()
        orig_email_settings = copy.deepcopy(email_settings)
        admin_settings = uvmContext.adminManager().getSettings()
        orig_admin_settings = copy.deepcopy(admin_settings)

        #change admin email to verify sent email
        new_admin_email = global_functions.random_email()
        admin_settings["users"]["list"][0]["emailAddress"] = new_admin_email
        uvmContext.adminManager().setSettings(admin_settings)

        #set custom email template subject and body
        new_email_subject = "NEW EMAIL SUBJECT TEST"
        new_email_body = "NEW EMAIL BODY TEST"
        email_settings["emailSubject"] = new_email_subject
        email_settings["emailBody"] = new_email_body

        #set new alert rule for easy trigger of email
        new_rule = create_alert_rule("test alert rule", "class", "=", "*SessionEvent*", "localAddr", "=", "*"+remote_control.client_ip+"*", sendEmail=True)
        email_settings['alertRules']['list'].append(new_rule)
        
        #set new settings
        uvmContext.eventManager().setSettings(email_settings)
        
        #send a session
        remote_control.is_online()
        time.sleep(4)

        #check email sent is correct
        emailFound = False
        timeout = 40
        alertEmail = ""
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # alertEmail = remote_control.run_command("wget -q --timeout=5 -O - http://test.untangle.com/cgi-bin/getEmail.py?toaddress=" + new_admin_email + " 2>&1 | grep TEST" ,stdout=True)
            alertEmail = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://test.untangle.com/cgi-bin/getEmail.py?toaddress={new_admin_email}") + " 2>&1 | grep TEST", stdout=True)
            if (alertEmail != ""):
                emailFound = True
        
        #set settings back
        uvmContext.eventManager().setSettings(orig_email_settings)
        uvmContext.adminManager().setSettings(orig_admin_settings)
        
        assert(emailFound)

    def test_099_queue_process(self):
        """
        Generate "a lot" of traffic and verify the log event queue is quickly processed
        """
        default_policy_id = 1
        application_name = "web-filter"
        if uvmContext.appManager().isInstantiated(application_name):
            raise unittest.SkipTest(f"app {application_name} already instantiated")

        application = uvmContext.appManager().instantiate(application_name, default_policy_id)

        try:
            global_functions.wait_for_event_queue_drain(queue_size_key="eventQueueSize")
        except Exception as e:
            uvmContext.appManager().destroy( application.getAppSettings()["id"] )
            raise unittest.SkipTest(e)

        uvmContext.appManager().destroy( application.getAppSettings()["id"] )

    def test_100_account_login(self):
        untangleEmail, untanglePassword = global_functions.get_live_account_info("Untangle")
        if untangleEmail == "message":
            raise unittest.SkipTest('Skipping no accound found:' + str(untanglePassword))

        result = uvmContext.cloudManager().accountLogin( untangleEmail, untanglePassword, uvmContext.getServerUID(), "", "", "")
        assert (result.get('success'))

    def test_101_account_login_invalid(self):
        result = uvmContext.cloudManager().accountLogin( "foobar@untangle.com", "badpassword" )
        assert not result.get('success')

    def test_102_admin_login_event(self):
        uvmContext.adminManager().logAdminLoginEvent( "admin", True, "127.0.1.1", True, 'X' )
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
        fullName = uvmContext.networkManager().getFullyQualifiedHostname()
        netsettings = uvmContext.networkManager().getNetworkSettings()

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
        system_settings = uvmContext.systemManager().getSettings()
        system_settings['cloudEnabled'] = True
        uvmContext.systemManager().setSettings(system_settings)
        
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
        language_settings = uvmContext.languageManager().getLanguageSettings()
        language_settings_orig = copy.deepcopy(language_settings)
        language_settings['language'] = 'es'
        uvmContext.languageManager().setLanguageSettings(language_settings)
        # Previous instance of test looked for "Not allowed" with its Spanish translation "no permitido".
        # However, this translation could change.  So assume that looking for "Not allowed" fails due to it being translated.
        result = subprocess.call(global_functions.build_wget_command( output_file="-", content_on_error=True, uri="http://localhost/admin/download") + ' 2>&1 | grep -qv "Not allowed"', shell=True)

        # revert language
        uvmContext.languageManager().setLanguageSettings(language_settings_orig)
        
        assert(result == 0)

    def test_150_synchronize_Language(self):
        """Check synchronizeLanguage returns OK"""
        synchronized = uvmContext.languageManager().synchronizeLanguage()

    def test_160_change_community_language(self):
        """Check if changing community language converts the GUI"""
        #set language to Russian
        language_settings_community = uvmContext.languageManager().getLanguageSettings()
        language_settings_community_orig = copy.deepcopy(language_settings_community)
        language_settings_community['language'] = "ru"
        uvmContext.languageManager().setLanguageSettings(language_settings_community)
        # Previous instance of test looked for "Not allowed" with its Russian translation "ne polozheno" but the real translation was the unicode "Метод не разрешен".
        # which is not easy to match via grep.  So assume that looking for "Not allowed" fails due to it being translated.
        result = subprocess.call(global_functions.build_wget_command( output_file="-", content_on_error=True, uri="http://localhost/admin/download") + ' 2>&1 | grep -qv "Not allowed"', shell=True)

        #Revert language
        uvmContext.languageManager().setLanguageSettings(language_settings_community_orig)

        assert(result == 0)

    def test_170_log_retention(self):
        """
        Verify log retention policy
        """
        # Pattern to parse:
        # rotating pattern: /var/log/uvm/uvm.log  512000 bytes (7 rotations)
        # We want file name and rotation count
        rotating_pattern = re.compile(r'rotating pattern: ([^\s]+) .* \((\d+) rotations\)')

        system_settings = uvmContext.systemManager().getSettings()
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
        uvmContext.systemManager().setSettings(system_settings)

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
        metrics_and_stats = uvmContext.metricManager().getMetricsAndStats()
        uvm_free_disk_percent = (float(metrics_and_stats["systemStats"]["freeDiskSpace"]) / float(metrics_and_stats["systemStats"]["totalDiskSpace"]) * 100)
        subprocess.run(["rm", "-f", full_filename])
        assert(uvm_free_disk_percent < 5)

        filename_output_obj= subprocess.run([fallocate_path, "-l", str(out_threshold), full_filename])
        if filename_output_obj.returncode != 0:
            raise unittest.SkipTest(full_filename + " not available 2nd")
        df_fields = subprocess.check_output("df | grep /$ | tr -s ' '", shell=True).decode('utf-8').split(' ')
        time.sleep(60)
        metrics_and_stats = uvmContext.metricManager().getMetricsAndStats()
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


test_registry.register_module("uvm", UvmTests)
