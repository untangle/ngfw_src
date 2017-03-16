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

prefix = "@PREFIX@"

defaultRackId = 1
app = None
canRelay = None
canSyslog = None
orig_settings = None
orig_netsettings = None
orig_mailsettings = None
syslogServerHost = ""
testEmailAddress = ""
# pdb.set_trace()

def createDomainDNSentries(domain,dnsServer):
    return {
        "domain": domain,
        "javaClass": "com.untangle.uvm.network.DnsLocalServer",
        "localServer": dnsServer
    }
    
def createFakeEmailEnvironment(emailLogFile="report_test.log"):
    global orig_mailsettings, testEmailAddress
    testEmailAddress = global_functions.random_email()
    orig_mailsettings = uvmContext.mailSender().getSettings()
    new_mailsettings = copy.deepcopy(orig_mailsettings)
    new_mailsettings['sendMethod'] = 'DIRECT'
    new_mailsettings['fromAddress'] = testEmailAddress
    uvmContext.mailSender().setSettings(new_mailsettings)

    # set untangletest email to get to testServerHost where fake SMTP sink is running using special DNS server
    netsettings = uvmContext.networkManager().getNetworkSettings()
    # Add Domain DNS Server for special test domains of untangletestvm.com and untangletest.com
    netsettings['dnsSettings']['localServers']['list'].append(createDomainDNSentries(global_functions.testServerHost,global_functions.specialDnsServer))
    uvmContext.networkManager().setNetworkSettings(netsettings)

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

def createReportsUser(profile_email=testEmailAddress, email_template_id=1):
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

def createDNSRule( networkAddr, name):
    return {
        "address": networkAddr, 
        "javaClass": "com.untangle.uvm.network.DnsStaticEntry", 
        "name": name
         }

def createAdminUser(useremail=testEmailAddress):
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

def createAlertRule(description, matcherField, operator, value, matcherField2, operator2, value2, thresholdEnabled=False, thresholdLimit=None, thresholdTimeframeSec=None, thresholdGroupingField=None):
    return {
            "alert": True,
            "alertLimitFrequency": False,
            "alertLimitFrequencyMinutes": 60,
            "thresholdEnabled": thresholdEnabled,
            "thresholdLimit": thresholdLimit,
            "thresholdTimeframeSec": thresholdTimeframeSec,
            "thresholdGroupingField": thresholdGroupingField,
            "description": description,
            "enabled": True,
            "javaClass": "com.untangle.app.reports.AlertRule",
            "log": True,
            "conditions": {
                "javaClass": "java.util.LinkedList",
                "list": [
                    {
                        "javaClass": "com.untangle.app.reports.AlertRuleCondition",
                        "conditionType": "FIELD_CONDITION",
                        "value": {
                            "comparator": operator,
                            "field": matcherField,
                            "javaClass": "com.untangle.app.reports.AlertRuleConditionField",
                            "value": value
                        }
                    },
                    {
                        "javaClass": "com.untangle.app.reports.AlertRuleCondition",
                        "conditionType": "FIELD_CONDITION",
                        "value": {
                            "comparator": operator2,
                            "field": matcherField2,
                            "javaClass": "com.untangle.app.reports.AlertRuleConditionField",
                            "value": value2
                        }
                    }
                ]
            },
            "ruleId": 1
        }

def createEmailTemplate(mobile=False):
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
    
class ReportsTests(unittest2.TestCase):

    @staticmethod
    def appName():
        return "reports"

    @staticmethod
    def appFirewallName():
        return "firewall"

    @staticmethod
    def appWanFailoverName():
        return "wan-failover"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global app, orig_settings, orig_netsettings, testServerHost, testEmailAddress, canRelay, canSyslog, syslogServerHost
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        if (uvmContext.appManager().isInstantiated(self.appName())):
            # report app is normally installed.
            # print "App %s already installed" % self.appName()
            # raise Exception('app %s already instantiated' % self.appName())
            app = uvmContext.appManager().app(self.appName())
        else:
            app = uvmContext.appManager().instantiate(self.appName(), defaultRackId)
        reportSettings = app.getSettings()
        orig_settings = copy.deepcopy(reportSettings)

        # Skip checking relaying is possible if we have determined it as true on previous test.
        try:
            canRelay = global_functions.send_test_email()
        except Exception,e:
            canRelay = False

        if canSyslog == None:
            wan_IP = uvmContext.networkManager().getFirstWanAddress()
            syslogServerHost = global_functions.find_syslog_server(wan_IP)
            portResult = remote_control.run_command("sudo lsof -i :514", host=syslogServerHost)
            if portResult == 0:
               canSyslog = True
            else:
               canSyslog = False
               
    def setUp(self):
        pass
                
    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)
    
    def test_040_remoteSyslog(self):
        if (not canSyslog):
            raise unittest2.SkipTest('Unable to syslog through ' + syslogServerHost)

        appFirewall = None
        if (uvmContext.appManager().isInstantiated(self.appFirewallName())):
            print "App %s already installed" % self.appFirewallName()
            appFirewall = uvmContext.appManager().app(self.appFirewallName())
        else:
            appFirewall = uvmContext.appManager().instantiate(self.appFirewallName(), defaultRackId)

        # Install firewall rule to generate syslog events
        rules = appFirewall.getRules()
        rules["list"].append(createFirewallSingleConditionRule("SRC_ADDR",remote_control.clientIP));
        appFirewall.setRules(rules);
        rules = appFirewall.getRules()
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
        newSyslogSettings["syslogHost"] = syslogServerHost
        app.setSettings(newSyslogSettings)

        # create some traffic (blocked by firewall and thus create a syslog event)
        result = remote_control.is_online(tries=1)
        # flush out events
        app.flushEvents()

        # remove the firewall rule aet syslog back to original settings
        app.setSettings(orig_settings)
        rules["list"]=[];
        appFirewall.setRules(rules);

        # remove firewall
        if appFirewall != None:
            uvmContext.appManager().destroy( appFirewall.getAppSettings()["id"] )
        appFirewall = None
        
        # parse the output and look for a rule that matches the expected values
        timeout = 5
        found_count = 0
        strings_to_find = ['\"blocked\":true',str('\"ruleId\":%i' % targetRuleId)]
        while (timeout > 0 and found_count < 2):
            # get syslog results on server
            rsyslogResult = remote_control.run_command("sudo tail -n 200 /var/log/localhost/localhost.log | grep 'FirewallEvent'", host=syslogServerHost, stdout=True)
            timeout -= 1
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
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + global_functions.testServerHost)

        current_method_name = inspect.stack()[0][3]

        # Create settings to receive testEmailAddress 
        createFakeEmailEnvironment( current_method_name + ".log")

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser(useremail=testEmailAddress))
        uvmContext.adminManager().setSettings(adminsettings)

        # Clear all report users
        settings = app.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        app.setSettings(settings)

        # trigger alert
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for alert email
        remote_control.run_command("rm -f /tmp/test_100_email_report_admin_file")
        emailFound = False
        emailContextFound1 = ""
        emailContextFound2 = ""
        timeout = 60
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # Check to see if the delivered email file is present
            result = remote_control.run_command("wget -q --timeout=5 -O /tmp/test_100_email_report_admin_file http://test.untangle.com/cgi-bin/getEmail.py?toaddress=" + testEmailAddress + " 2>&1")
            if (result == 0):
                emailFound = True
                emailContextFound1 = remote_control.run_command("grep -i 'Daily Reports' /tmp/test_100_email_report_admin_file 2>&1", stdout=True)
                emailContextFound2 = remote_control.run_command("grep -i 'Content-Type: image/png; name=' /tmp/test_100_email_report_admin_file 2>&1", stdout=True)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert(emailFound)
        # assert(('Daily Reports' in emailContext) and ('Content-Type: image/png; name=' in emailContext))
        assert((emailContextFound1) and (emailContextFound2))

    def test_101_email_admin_override_custom_report(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom report with test not in default.
        """
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + global_functions.testServerHost)

        current_method_name = inspect.stack()[0][3]

        # Create settings to receive testEmailAddress 
        createFakeEmailEnvironment( current_method_name + ".log")

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser(useremail=testEmailAddress))
        uvmContext.adminManager().setSettings(adminsettings)

        settings = app.getSettings()
        # Add custom template with a test not in daily reports
        settings["emailTemplates"]["list"] = settings["emailTemplates"]["list"][:1]
        settings["emailTemplates"]["list"].append(createEmailTemplate())

        # Add report user with testEmailAddress
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(createReportsUser(profile_email=testEmailAddress, email_template_id=2))
        app.setSettings(settings)

        # trigger alert
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for alert email
        ## look for new template name, custom
        remote_control.run_command("rm -f /tmp/test_101_email_admin_override_custom_report_file")
        emailFound = False
        emailContextFound1 = ""
        emailContextFound2 = ""
        timeout = 60
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # Check to see if the delivered email file is present
            result = remote_control.run_command("wget -q --timeout=5 -O /tmp/test_100_email_report_admin_file http://test.untangle.com/cgi-bin/getEmail.py?toaddress=" + testEmailAddress + " 2>&1")
            if (result == 0):
                emailFound = True
                emailContextFound1 = remote_control.run_command("grep -i 'Custom Report' /tmp/test_101_email_admin_override_custom_report_file 2>&1", stdout=True)
                emailContextFound2 = remote_control.run_command("grep -i 'Administration-VWuRol5uWw' /tmp/test_101_email_admin_override_custom_report_file 2>&1", stdout=True)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert(emailFound)
        assert((emailContextFound1) and (emailContextFound2))

    def test_102_email_admin_override_custom_report_mobile(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom report with test not in default.
        """
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + global_functions.testServerHost)

        current_method_name = inspect.stack()[0][3]

        # Create settings to receive testEmailAddress 
        createFakeEmailEnvironment( current_method_name + ".log")

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser(useremail=testEmailAddress))
        uvmContext.adminManager().setSettings(adminsettings)

        settings = app.getSettings()
        # Add custom template with a test not in daily reports
        settings["emailTemplates"]["list"] = settings["emailTemplates"]["list"][:1]
        settings["emailTemplates"]["list"].append(createEmailTemplate(mobile=True))

        # Add report user with testEmailAddress
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(createReportsUser(profile_email=testEmailAddress, email_template_id=2))
        app.setSettings(settings)

        # trigger alert
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for alert email
        ## look for new template name, custom
        # emailFound, emailContext, emailContext2, measureLength = findEmailContent('Custom Report','Administration-VWuRol5uWw', 'Content-Type: image/png; name="Administration-VWuRol5uWw@untangle.com.png"', '---')
        remote_control.run_command("rm -f /tmp/test_102_email_admin_override_custom_report_mobile_file")
        emailFound = False
        emailContextFound1 = ""
        emailContextFound2 = ""
        timeout = 60
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # Check to see if the delivered email file is present
            result = remote_control.run_command("wget -q --timeout=5 -O /tmp/test_102_email_admin_override_custom_report_mobile_file http://test.untangle.com/cgi-bin/getEmail.py?toaddress=" + testEmailAddress + " 2>&1")
            if (result == 0):
                emailFound = True
                emailContextFound1 = remote_control.run_command("grep -i 'Custom Report' /tmp/test_102_email_admin_override_custom_report_mobile_file 2>&1", stdout=True)
                emailContextFound2 = remote_control.run_command("grep -i 'Administration-VWuRol5uWw' /tmp/test_102_email_admin_override_custom_report_mobile_file 2>&1", stdout=True)
                measureBegin = 'Content-Type: image/png; name="Administration-VWuRol5uWw@untangle.com.png"'
                measureEnd = '---'
                measureLength = remote_control.run_command("sed -n '/" + measureBegin.replace('/', '\/').replace('"', '\\"') + "/,/" + measureEnd.replace('/', '\/').replace('"', '\\"') + "/p' /tmp/test_102_email_admin_override_custom_report_mobile_file* | wc -l", stdout=True)
        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert(emailFound)
        assert((emailContextFound1) and (emailContextFound2) and (int(measureLength) < 80))

    @staticmethod
    def finalTearDown(self):
        global app
        if app != None:
            app.setSettings(orig_settings)
        if orig_mailsettings != None:
            uvmContext.mailSender().setSettings(orig_mailsettings)
        app = None

test_registry.registerApp("reports", ReportsTests)
