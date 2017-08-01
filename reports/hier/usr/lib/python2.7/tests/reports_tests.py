import unittest2
import time
import sys
import pdb
import socket
import subprocess
import copy
import smtplib
import re
import ipaddr
import inspect
from datetime import datetime
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions
from global_functions import uvmContext

default_policy_id = 1
app = None
can_relay = None
can_syslog = None
orig_settings = None
orig_mailsettings = None
syslog_server_host = ""
test_email_address = ""
# pdb.set_trace()

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

def create_reports_user(profile_email=test_email_address, email_template_id=1):
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
            "onlineAccess": False,
            "passwordHashBase64": ""
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

def fetch_email( filename, email_address, tries=120 ):
    remote_control.run_command("rm -f %s" % filename)
    while tries > 0:
        tries -= 1
        # Check to see if the delivered email file is present
        result = remote_control.run_command("wget -q --timeout=5 -O %s http://test.untangle.com/cgi-bin/getEmail.py?toaddress=%s 2>&1" % (filename, email_address))
        if (result == 0):
            return True
    return False

class ReportsTests(unittest2.TestCase):

    @staticmethod
    def appName():
        return "reports"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global app, orig_settings, test_email_address, can_relay, can_syslog, syslog_server_host
        if (uvmContext.appManager().isInstantiated(self.appName())):
            # report app is normally installed.
            # print "App %s already installed" % self.appName()
            # raise Exception('app %s already instantiated' % self.appName())
            app = uvmContext.appManager().app(self.appName())
        else:
            app = uvmContext.appManager().instantiate(self.appName(), default_policy_id)
        reportSettings = app.getSettings()
        orig_settings = copy.deepcopy(reportSettings)

        # Skip checking relaying is possible if we have determined it as true on previous test.
        try:
            can_relay = global_functions.send_test_email()
        except Exception,e:
            can_relay = False

        if can_syslog == None:
            can_syslog = False
            wan_IP = uvmContext.networkManager().getFirstWanAddress()
            syslog_server_host = global_functions.find_syslog_server(wan_IP)
            if syslog_server_host:
                portResult = remote_control.run_command("sudo lsof -i :514", host=syslog_server_host)
                if portResult == 0:
                    can_syslog = True
               
    def setUp(self):
        print
                
    # verify client is online
    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)
    
    # FIXME
    # Syslog settings now live in config > events
    # I'm not sure why this test passes, since the syslog functionality has been removed from reports
    # FIXME
    def test_040_remote_syslog(self):
        if (not can_syslog):
            raise unittest2.SkipTest('Unable to syslog through ' + syslog_server_host)

        firewall_app = None
        if (uvmContext.appManager().isInstantiated("firewall")):
            print "App %s already installed" % "firewall"
            firewall_app = uvmContext.appManager().app("firewall")
        else:
            firewall_app = uvmContext.appManager().instantiate("firewall", default_policy_id)

        # Install firewall rule to generate syslog events
        rules = firewall_app.getRules()
        rules["list"].append(create_firewall_rule("SRC_ADDR",remote_control.clientIP));
        firewall_app.setRules(rules);
        rules = firewall_app.getRules()
        # Get rule ID
        for rule in rules['list']:
            if rule['enabled'] and rule['block']:
                targetRuleId = rule['ruleId']
                break
        # Setup syslog to send events to syslog host
        newSyslogSettings = app.getSettings()
        newSyslogSettings["syslogEnabled"] = True
        newSyslogSettings["syslogPort"] = 514
        newSyslogSettings["syslogProtocol"] = "UDP"
        newSyslogSettings["syslogHost"] = syslog_server_host
        app.setSettings(newSyslogSettings)

        # create some traffic (blocked by firewall and thus create a syslog event)
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
        strings_to_find = ['\"blocked\":true',str('\"ruleId\":%i' % targetRuleId)]
        while (tries > 0 and found_count < 2):
            # get syslog results on server
            rsyslogResult = remote_control.run_command("sudo tail -n 200 /var/log/localhost/localhost.log | grep 'FirewallEvent'", host=syslog_server_host, stdout=True)
            tries -= 1
            for line in rsyslogResult.splitlines():
                print "\nchecking line: %s " % line
                for string in strings_to_find:
                    if not string in line:
                        print "missing: %s" % string
                        continue
                    else:
                        found_count += 1
                        print "found: %s" % string
                break
            time.sleep(2)
            
        assert(found_count == len(strings_to_find))

    def test_100_email_report_admin(self):
        """
        The "default" configuration test:
        - Administrator email account gets
        """
        if (not can_relay):
            raise unittest2.SkipTest('Unable to relay through ' + global_functions.testServerHost)
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

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

        # send emails
        subprocess.call([global_functions.get_prefix()+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        email_found = fetch_email( "/tmp/test_100_email_report_admin_file", test_email_address )
        email_context_found1 = ""
        email_context_found2 = ""
        if email_found:
            email_context_found1 = remote_control.run_command("grep -i -e 'Reports:.*Daily.*' /tmp/test_100_email_report_admin_file 2>&1", stdout=True)
            email_context_found2 = remote_control.run_command("grep -i -e q'Content-Type: image/png; name=' /tmp/test_100_email_report_admin_file 2>&1", stdout=True)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)

        assert(email_found)
        assert((email_context_found1) and (email_context_found2))

    def test_101_email_admin_override_custom_report(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom report with test not in default.
        """
        if (not can_relay):
            raise unittest2.SkipTest('Unable to relay through ' + global_functions.testServerHost)
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

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
            raise unittest2.SkipTest('Unable to relay through ' + global_functions.testServerHost)
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

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
            measureBegin = 'Content-Type: image/png; name="Administration-VWuRol5uWw@untangle.com.png"'
            measureEnd = '---'
            measureLength = remote_control.run_command("sed -n '/" + measureBegin.replace('/', '\/').replace('"', '\\"') + "/,/" + measureEnd.replace('/', '\/').replace('"', '\\"') + "/p' /tmp/test_102_email_admin_override_custom_report_mobile_file* | wc -l", stdout=True)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)

        assert(email_found)
        assert((email_context_found1) and (email_context_found2) and (int(measureLength) < 80))
        
    def test_103_email_report_verify_apps(self):
        """
        1) Install all apps
        2) Generate a report
        3) Verify that the emailed report contains a section for each app
        """
        global app
        if (not can_relay):
            raise unittest2.SkipTest('Unable to relay through ' + global_functions.testServerHost)
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

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
        apps = []
        for name in ["firewall", "web-filter", "wan-failover", "virus-blocker", "spam-blocker", "phish-blocker", "ad-blocker", "web-cache", "bandwidth-control", "application-control", "ssl-inspector", "captive-portal", "web-monitor", "virus-blocker-lite", "spam-blocker-lite", "application-control-lite", "policy-manager", "directory-connector", "wan-failover", "wan-balancer", "configuration-backup", "intrusion-prevention", "ipsec-vpn", "openvpn"]:
            if (uvmContext.appManager().isInstantiated(name)):
                print "App %s already installed" % name
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
            for str in ['Daily','Firewall','Web Filter','Virus Blocker','Spam Blocker','Phish Blocker','Ad Blocker','Web Cache','Bandwidth Control','Application Control','SSL Inspector','Web Monitor','Captive Portal','Virus Blocker Lite','Spam Blocker Lite','Application Control Lite','Policy Manager','Directory Connector','WAN Failover','WAN Balancer','Configuration Backup','Intrusion Prevention','IPsec VPN','OpenVPN']:
                results.append(remote_control.run_command("grep -q -i '%s' /tmp/test_103_email_report_admin_file 2>&1"%str))

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)

        # remove apps that were installed above
        for a in apps: uvmContext.appManager().destroy( a.getAppSettings()["id"] )
        
        assert(email_found)
        for result in results:
            assert(result == 0)
             
    @staticmethod
    def finalTearDown(self):
        global app
        if app != None:
            app.setSettings(orig_settings)
        if orig_mailsettings != None:
            uvmContext.mailSender().setSettings(orig_mailsettings)
        app = None

test_registry.registerApp("reports", ReportsTests)
