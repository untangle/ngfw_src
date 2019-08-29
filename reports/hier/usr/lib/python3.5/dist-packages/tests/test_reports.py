"""report tests"""
import time
import subprocess
import copy
import re
import base64
import calendar
import email
import runtests
import unittest
import pytest

from io import BytesIO as BytesIO
from datetime import datetime
from datetime import timedelta
from html.parser import HTMLParser

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
from PIL import Image as Image

default_policy_id = 1
apps_list_short = ["firewall", "web-filter", "spam-blocker", "ad-blocker", "web-cache", "bandwidth-control", "application-control", "ssl-inspector", "captive-portal", "web-monitor", "application-control-lite", "policy-manager", "directory-connector", "wan-failover", "wan-balancer", "configuration-backup", "intrusion-prevention", "ipsec-vpn", "openvpn"]
apps_name_list_short = ['Daily','Firewall','Web Filter','Spam Blocker','Ad Blocker','Web Cache','Bandwidth Control','Application Control','SSL Inspector','Web Monitor','Captive Portal','Application Control Lite','Policy Manager','Directory Connector','WAN Failover','WAN Balancer','Configuration Backup','Intrusion Prevention','IPsec VPN','OpenVPN']
apps_list = ["firewall", "web-filter", "virus-blocker", "spam-blocker", "phish-blocker", "ad-blocker", "web-cache", "bandwidth-control", "application-control", "ssl-inspector", "captive-portal", "web-monitor", "virus-blocker-lite", "spam-blocker-lite", "application-control-lite", "policy-manager", "directory-connector", "wan-failover", "wan-balancer", "configuration-backup", "intrusion-prevention", "ipsec-vpn", "openvpn"]
apps_name_list = ['Daily','Firewall','Web Filter','Virus Blocker','Spam Blocker','Phish Blocker','Ad Blocker','Web Cache','Bandwidth Control','Application Control','SSL Inspector','Web Monitor','Captive Portal','Virus Blocker Lite','Spam Blocker Lite','Application Control Lite','Policy Manager','Directory Connector','WAN Failover','WAN Balancer','Configuration Backup','Intrusion Prevention','IPsec VPN','OpenVPN']
app = None
web_app = None
can_relay = None
can_syslog = None
orig_settings = None
orig_mailsettings = None
syslog_server_host = ""
test_email_address = ""
# pdb.set_trace()


class ContentIdParser(HTMLParser):
    content_ids = []
    cid_src_regex = re.compile(r'^cid:(.*)')
    def handle_startendtag(self, tag, attrs):
        if tag == "img":
            for attr in attrs:
                if attr[0] == "src":
                    matches = self.cid_src_regex.match(attr[1])
                    if matches is not None and len(matches.groups()) > 0:
                        self.content_ids.append(matches.group(1))                    


def configure_mail_relay():
    global orig_mailsettings, test_email_address
    test_email_address = global_functions.random_email()
    orig_mailsettings = uvmContext.mailSender().getSettings()
    new_mailsettings = copy.deepcopy(orig_mailsettings)
    new_mailsettings['sendMethod'] = 'DIRECT'
    new_mailsettings['fromAddress'] = test_email_address
    uvmContext.mailSender().setSettings(new_mailsettings)


def create_firewall_rule( conditionType, value, blocked=True ):
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


def create_reports_user(profile_email=test_email_address, email_template_id=1, access=False):
    passwd_encoded = base64.b64encode("passwd".encode("utf-8"))
    return  {
            "emailAddress": profile_email,
            "emailSummaries": True,
            "emailAlerts": True,
            "emailTemplateIds": {
                "javaClass": "java.util.LinkedList",
                "list": [
                    email_template_id
                ]
            },
            "javaClass": "com.untangle.app.reports.ReportsUser",
            "onlineAccess": access,
            "passwordHashBase64": passwd_encoded.decode("utf-8")
    }


def create_admin_user(useremail=test_email_address):
    username,domainname = useremail.split("@")
    return {
            "description": "System Administrator",
            "emailSummaries": True,
            "emailAlerts": True,
            "emailAddress": useremail,
            "javaClass": "com.untangle.uvm.AdminUserSettings",
            "passwordHashBase64": "YWdlQWnp64i/3IZ6O34JLF0h+BJQ0J3W",
            "username": username
        }


def create_email_template(mobile=False):
    return {
        "description": "Custom description",
        "enabledAppIds": {
            "javaClass": "java.util.LinkedList",
            "list": []
        },
        "enabledConfigIds": {
            "javaClass": "java.util.LinkedList",
            "list": [
                "Administration-VWuRol5uWw"
            ]
        },
        "interval": 86400,
        "intervalWeekStart": 1,
        "javaClass": "com.untangle.app.reports.EmailTemplate",
        "mobile": mobile,
        "readOnly": False,
        "templateId": 2,
        "title": "Custom Report"
    }


def fetch_email( filename, email_address, tries=40 ):
    remote_control.run_command("rm -f %s" % filename)
    while tries > 0:
        tries -= 1
        # Check to see if the delivered email file is present
        result = remote_control.run_command("wget -q --timeout=20 --tries=1 -O %s http://test.untangle.com/cgi-bin/getEmail.py?toaddress=%s 2>&1" % (filename, email_address))
        time.sleep(30)
        if (result == 0):
            if (remote_control.run_command("grep -q -i 'Subject: .*Report.*' %s 2>&1" % filename) == 0):
                return True
    return False


@pytest.mark.reports
class ReportsTests(NGFWTestCase):

    @staticmethod
    def module_name():
        global app
        app = ReportsTests._app
        return "reports"

    @staticmethod
    def webAppName():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"

    @classmethod
    def initial_extra_setup(cls):
        global orig_settings, test_email_address, can_relay, can_syslog, syslog_server_host, web_app

        reportSettings = cls._app.getSettings()
        orig_settings = copy.deepcopy(reportSettings)

        if (uvmContext.appManager().isInstantiated(cls.webAppName())):
            raise Exception('app %s already instantiated' % cls.webAppName())
        web_app = uvmContext.appManager().instantiate(cls.webAppName(), default_policy_id)
        # Skip checking relaying is possible if we have determined it as true on previous test.
        try:
            can_relay = global_functions.send_test_email()
        except Exception as e:
            can_relay = False

        if can_syslog == None:
            can_syslog = False
            wan_IP = uvmContext.networkManager().getFirstWanAddress()
            syslog_server_host = global_functions.find_syslog_server(wan_IP)
            if syslog_server_host:
                portResult = remote_control.run_command("sudo lsof -i :514", host=syslog_server_host)
                if portResult == 0:
                    can_syslog = True
               
    # verify client is online
    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)
    
    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_040_remote_syslog(self):
        if (not can_syslog):
            raise unittest.SkipTest('Unable to syslog through ' + syslog_server_host)

        firewall_app = None
        if (uvmContext.appManager().isInstantiated("firewall")):
            print("App %s already installed" % "firewall")
            firewall_app = uvmContext.appManager().app("firewall")
        else:
            firewall_app = uvmContext.appManager().instantiate("firewall", default_policy_id)

        # Install firewall rule to generate syslog events
        rules = firewall_app.getRules()
        rules["list"].append(create_firewall_rule("SRC_ADDR",remote_control.client_ip));
        firewall_app.setRules(rules);
        rules = firewall_app.getRules()
        # Get rule ID
        for rule in rules['list']:
            if rule['enabled'] and rule['block']:
                targetRuleId = rule['ruleId']
                break
        # Setup syslog to send events to syslog host in /config/events/syslog
        syslogSettings = uvmContext.eventManager().getSettings()
        syslogSettings["syslogEnabled"] = True
        syslogSettings["syslogPort"] = 514
        syslogSettings["syslogProtocol"] = "UDP"
        syslogSettings["syslogHost"] = syslog_server_host
        uvmContext.eventManager().setSettings( syslogSettings )

        # create some traffic (blocked by firewall and thus create a syslog event)
        exactly_now = datetime.now()
        exactly_now_minus1 = datetime.now() - timedelta(minutes=1)
        exactly_now_plus1 = datetime.now() + timedelta(minutes=1)
        timestamp = exactly_now.strftime('%Y-%m-%d %H:%M')
        timestamp_minus1 = exactly_now_minus1.strftime('%Y-%m-%d %H:%M')
        timestamp_now_plus1 = exactly_now_plus1.strftime('%Y-%m-%d %H:%M')
        result = remote_control.is_online(tries=1)
        # flush out events
        app.flushEvents()

        # remove the firewall rule aet syslog back to original settings
        app.setSettings(orig_settings)
        rules["list"]=[];
        firewall_app.setRules(rules);

        # remove firewall
        if firewall_app != None:
            uvmContext.appManager().destroy( firewall_app.getAppSettings()["id"] )
        firewall_app = None
        
        # parse the output and look for a rule that matches the expected values
        tries = 5
        found_count = 0
        timestamp_variations  = [str('\"timeStamp\":\"%s' % timestamp_minus1),str('\"timeStamp\":\"%s' % timestamp_now_plus1)]
        strings_to_find = ['\"blocked\":true',str('\"ruleId\":%i' % targetRuleId),str('\"timeStamp\":\"%s' % timestamp)]
        num_string_find = len(strings_to_find)
        while (tries > 0 and found_count < num_string_find):
            # get syslog results on server
            rsyslogResult = remote_control.run_command("sudo tail -n 200 /var/log/syslog | grep 'FirewallEvent'", host=syslog_server_host, stdout=True)
            tries -= 1
            for line in rsyslogResult.splitlines():
                print("\nchecking line: %s " % line)
                found_count = 0
                for string in strings_to_find:
                    if not string in line:
                        print("missing: %s" % string)
                        if ('timeStamp' in string):
                            # Allow +/- one minute in timestamp
                            if (timestamp_variations [0] in line) or (timestamp_variations [1] in line):
                                print("found: time with varation %s or %s" % (timestamp_variations [0],timestamp_variations [1]))
                                found_count += 1
                            else:
                                break
                        else:
                            # continue
                            break
                    else:
                        found_count += 1
                        print("found: %s" % string)
                # break if all the strings have been found.
                if found_count == num_string_find:
                    break
            time.sleep(2)

        # Disable syslog
        syslogSettings = uvmContext.eventManager().getSettings()
        syslogSettings["syslogEnabled"] = False
        uvmContext.eventManager().setSettings( syslogSettings )
            
        assert(found_count == num_string_find)

    def test_050_export_report_events(self):
        """
        Test export of events to CSV file
        """
        # Delete any old csv file if it exists
        csv_tmp = "/tmp/test_50_export_report_events.csv"
        subprocess.call(('/bin/rm -f %s' % csv_tmp), shell=True)

        remote_control.run_command("wget -q -O /dev/null http://test.untangle.com")
        remote_control.run_command("wget -q -O /dev/null http://www.untangle.com")
        remote_control.run_command("wget -q -O /dev/null http://news.google.com")
        remote_control.run_command("wget -q -O /dev/null http://www.yahoo.com")
        remote_control.run_command("wget -q -O /dev/null http://www.reddit.com")
        app.flushEvents()
        time.sleep(5)
        
        # Get CSV export of events
        current_epoch = calendar.timegm(time.gmtime())
        current_epoch += 1200  # add twenty minutes to get all events
        current_epoch *= 1000
        an_hour_ago_epoch = current_epoch - 360000 # one day milliseconds
        post_data = "type=eventLogExport"  # CSV file title/name
        post_data += "&arg1=Web_Filter-download"
        post_data += '&arg2={"javaClass":"com.untangle.app.reports.ReportEntry","displayOrder":1010,"description":"Shows+all+scanned+web+requests.","units":null,"orderByColumn":null,"title":"All+Web+Events","colors":null,"enabled":true,"defaultColumns":["time_stamp","c_client_addr","s_server_addr","s_server_port","username","hostname","host","uri","web_filter_blocked","web_filter_flagged","web_filter_reason","web_filter_category"],"pieNumSlices":null,"seriesRenderer":null,"timeDataDynamicAggregationFunction":null,"pieStyle":null,"pieSumColumn":null,"timeDataDynamicAllowNull":null,"orderDesc":null,"table":"http_events","approximation":null,"timeDataInterval":null,"timeStyle":null,"timeDataDynamicValue":null,"readOnly":true,"timeDataDynamicLimit":null,"timeDataDynamicColumn":null,"pieGroupColumn":null,"timeDataColumns":null,"textColumns":null,"category":"Web+Filter","conditions":[],"uniqueId":"web-filter-SRSZBBKXLN","textString":null,"type":"EVENT_LIST","localizedTitle":"All+Web+Events","localizedDescription":"Shows+all+scanned+web+requests.","slug":"all-web-events","url":"web-filter/all-web-events","icon":"fa-list-ul","_id":"Ung.model.Report-390"}'
        post_data += "&arg3="
        post_data += "&arg4=time_stamp,c_client_addr,s_server_addr,s_server_port,username,hostname,host,uri,web_filter_blocked,web_filter_flagged,web_filter_reason,web_filter_category"
        post_data += "&arg5=" + str(an_hour_ago_epoch)  # epoch start time
        post_data += "&arg6=" + str(current_epoch)  # epach end time
        # print(post_data)
        
        subprocess.call(("wget -q -O %s --post-data='%s' http://localhost/admin/download" % (csv_tmp,post_data)), shell=True)
        result = subprocess.check_output('wc -l /tmp/test_50_export_report_events.csv', shell=True)
        print("Result of wc on %s : %s" % (csv_tmp,str(result)))
        assert(int.from_bytes(result,byteorder='little') > 3)

    def test_100_email_report_admin(self):
        """
        The "default" configuration test:
        - Administrator email account gets
        """
        if (not can_relay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # create settings to receive test_email_address 
        configure_mail_relay()
        subprocess.call("rm /tmp/test_100_email_report_admin_file > /dev/null 2>&1", shell=True)

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(create_admin_user(useremail=test_email_address))
        uvmContext.adminManager().setSettings(adminsettings)

        # clear all report users
        settings = app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        app.setSettings(settings)

        # send emails
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_100_email_report_admin_file", test_email_address )
        email_context_found1 = ""
        email_context_found2 = ""
        if email_found:
            email_context_found1 = remote_control.run_command("grep -i -e 'Reports:.*Daily.*' /tmp/test_100_email_report_admin_file 2>&1", stdout=True)
            email_context_found2 = remote_control.run_command("grep -i -e 'Content-Type: image/png; name=' /tmp/test_100_email_report_admin_file 2>&1", stdout=True)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)

        assert(email_found)
        assert((email_context_found1) and (email_context_found2))

        ## Verify that all images are intact.
        # copy mail from remote client
        subprocess.call("scp -q -i %s testshell@%s:/tmp/test_100_email_report_admin_file /tmp/" % (remote_control.host_key_file, remote_control.client_ip), shell=True)
        fp = open("/tmp/test_100_email_report_admin_file")
        email_string = fp.read()
        fp.close()
        # Delete the first line as it is blank and throws off the parser
        email_string = '\n'.join(email_string.split('\n')[1:])
        msg = email.message_from_string(email_string)

        mime_content_ids = []
        parser = ContentIdParser();
        for part in msg.walk():
            if part.get_content_maintype() == "image":
                for index, key in enumerate(part.keys()):
                    if key == "Content-ID":
                        mime_content_ids.append(part.values()[index])
            elif part.get_content_maintype() == "text":
                parser.feed((part.get_payload(decode=True)).decode("utf-8"))

        assert(len(parser.content_ids) == len(mime_content_ids))

    def test_101_email_admin_override_custom_report(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom report with test not in default.
        """
        if (not can_relay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # Create settings to receive test_email_address 
        configure_mail_relay()

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(create_admin_user(useremail=test_email_address))
        uvmContext.adminManager().setSettings(adminsettings)

        settings = app.getSettings()
        # add custom template with a test not in daily reports
        settings["emailTemplates"]["list"] = settings["emailTemplates"]["list"][:1]
        settings["emailTemplates"]["list"].append(create_email_template())

        # add report user with test_email_address
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, email_template_id=2))
        app.setSettings(settings)

        # send email
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_101_email_admin_override_custom_report_file", test_email_address )
        if email_found:
            email_context_found1 = remote_control.run_command("grep -i 'Custom Report' /tmp/test_101_email_admin_override_custom_report_file 2>&1", stdout=True)
            email_context_found2 = remote_control.run_command("grep -i 'Administration-VWuRol5uWw' /tmp/test_101_email_admin_override_custom_report_file 2>&1", stdout=True)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)

        assert(email_found)
        assert((email_context_found1) and (email_context_found2))

    def test_102_email_admin_override_custom_report_mobile(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom report with test not in default.
        """
        if (not can_relay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # Create settings to receive test_email_address 
        configure_mail_relay()
        subprocess.call("rm /tmp/test_102_email_admin_override_custom_report_mobile_file > /dev/null 2>&1", shell=True)

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(create_admin_user(useremail=test_email_address))
        uvmContext.adminManager().setSettings(adminsettings)

        settings = app.getSettings()
        # add custom template with a test not in daily reports
        settings["emailTemplates"]["list"] = settings["emailTemplates"]["list"][:1]
        settings["emailTemplates"]["list"].append(create_email_template(mobile=True))

        # add report user with test_email_address
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email=test_email_address, email_template_id=2))
        app.setSettings(settings)

        # send email
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_102_email_admin_override_custom_report_mobile_file", test_email_address )
        if email_found:
            email_context_found1 = remote_control.run_command("grep -i 'Custom Report' /tmp/test_102_email_admin_override_custom_report_mobile_file 2>&1", stdout=True)
            email_context_found2 = remote_control.run_command("grep -i 'Administration-VWuRol5uWw' /tmp/test_102_email_admin_override_custom_report_mobile_file 2>&1", stdout=True)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)

        assert(email_found)
        assert((email_context_found1) and (email_context_found2))

        # Verify that all images are less than 3502350.
        # copy mail from remote client
        subprocess.call("scp -q -i %s testshell@%s:/tmp/test_102_email_admin_override_custom_report_mobile_file /tmp/" % (remote_control.host_key_file, remote_control.client_ip), shell=True)
        fp = open("/tmp/test_102_email_admin_override_custom_report_mobile_file")
        email_string = fp.read()
        fp.close()
        # Delete the first line as it is blank and throws off the parser
        email_string = '\n'.join(email_string.split('\n')[1:])
        msg = email.message_from_string(email_string)

        mime_content_ids = []
        for part in msg.walk():
            if part.get_content_maintype() == "image":
                # print("Image found")
                for index, key in enumerate(part.keys()):
                    if key == "Content-ID" and "untangle.int" in part.values()[index]:
                        email_image = part.get_payload(decode=True)
                        im = Image.open(BytesIO(email_image))
                        (image_width,image_height) = im.size
                        print("Image %s width: %d height: %d" % (part.values()[index], image_width, image_height))
                        assert(image_width <= 350 and image_height <= 350)

    def test_103_email_report_verify_apps(self):
        """
        1) Install all apps
        2) Generate a report
        3) Verify that the emailed report contains a section for each app
        """
        global app,apps_list,apps_name_list
        if (not can_relay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        # create settings to receive test_email_address 
        configure_mail_relay()

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(create_admin_user(useremail=test_email_address))
        uvmContext.adminManager().setSettings(adminsettings)

        # clear all report users
        settings = app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        app.setSettings(settings)
        
        # install all the apps that aren't already installed
        system_stats = uvmContext.metricManager().getStats()
        # print system_stats
        system_memory = system_stats['systemStats']['MemTotal']
        if (int(system_memory) < 2200000000):   # don't use high memory apps in devices with 2G or less.
            apps_list = apps_list_short
            apps_name_list = apps_name_list_short
        apps = []
        for name in apps_list:
            if (uvmContext.appManager().isInstantiated(name)):
                print("App %s already installed" % name)
            else:
                apps.append( uvmContext.appManager().instantiate(name, default_policy_id) )
            
        # create some traffic 
        result = remote_control.is_online(tries=1)

        # flush out events
        app.flushEvents()

        # send emails
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_103_email_report_admin_file", test_email_address )

        # look for all the appropriate sections in the report email
        results = []
        if email_found:
            for str in apps_name_list:
                results.append(remote_control.run_command("grep -q -i '%s' /tmp/test_103_email_report_admin_file 2>&1"%str))

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)

        # remove apps that were installed above
        for a in apps: uvmContext.appManager().destroy( a.getAppSettings()["id"] )
        
        assert(email_found)
        for result in results:
            assert(result == 0)

    def test_110_verify_report_users(self):
        # Test report only user can login and report serlvet displays 
        # add report user with test_email_address
        settings = app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(create_reports_user(profile_email='test', access=True))  # password = passwd
        app.setSettings(settings)
        adminURL = global_functions.get_http_url()
        print("URL %s" % adminURL)
        resultLoginPage = subprocess.call("wget -q -O - " + adminURL + "reports 2>&1 | grep -q Login", shell=True)
        assert (resultLoginPage == 0)
        
        resultLoginPage = subprocess.call("wget -q -O - " + adminURL + '"auth/login?url=/reports&realm=Reports&username=test&password=passwd" 2>&1 | grep -q Report', shell=True)
        assert (resultLoginPage == 0)
        
    @classmethod
    def final_extra_tear_down(cls):
        global web_app

        # remove all the apps in case test 103 does not remove them.
        for name in apps_list:
            if (uvmContext.appManager().isInstantiated(name)):
                remove_app = uvmContext.appManager().app(name)
                uvmContext.appManager().destroy(remove_app.getAppSettings()["id"])
        if orig_mailsettings != None:
            uvmContext.mailSender().setSettings(orig_mailsettings)

        web_app = None

        
test_registry.register_module("reports", ReportsTests)
