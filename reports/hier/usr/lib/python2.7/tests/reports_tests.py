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
    def appWebFilterName():
        return "web-filter"
    
    @staticmethod
    def appWanFailoverName():
        return "wan-failover"
    
    @staticmethod
    def appVirusBlockerName():
        return "virus-blocker"
    
    @staticmethod
    def appSpamBlockerName():
        return "spam-blocker"
    
    @staticmethod
    def appPhishBlockerName():
        return "phish-blocker"
    
    @staticmethod
    def appAdBlockerName():
        return "ad-blocker"
        
    @staticmethod
    def appWebCacheName():
        return "web-cache"
        
    @staticmethod
    def appBandwidthControlName():
        return "bandwidth-control"
        
    @staticmethod
    def appApplicationControlName():
        return "application-control"
        
    @staticmethod
    def appSSLInspectorName():
        return "ssl-inspector"
        
    @staticmethod
    def appCaptivePortalName():
        return "captive-portal"
        
    @staticmethod
    def appWebMonitorName():
        return "web-monitor"        
        
    @staticmethod
    def appVirusBlockerLiteName():
        return "virus-blocker-lite"
            
    @staticmethod
    def appSpamBlockerLiteName():
        return "spam-blocker-lite"
        
    @staticmethod
    def appApplicationControlLiteName():
        return "application-control-lite"    
    
    @staticmethod
    def appPolicyManagerName():
        return "policy-manager"

    @staticmethod
    def appDirectoryConnectorName():
        return "directory-connector"
    
    @staticmethod
    def appWANFailoverName():
        return "wan-failover"
    
    @staticmethod
    def appWANBalancerName():
        return "wan-balancer"
    
    @staticmethod
    def appConfigurationBackupName():
        return "configuration-backup"
    
    @staticmethod
    def appIntrusionPreventionName():
        return "intrusion-prevention"
    
    @staticmethod
    def appIPSecVPNName():
        return "ipsec-vpn"
    
    @staticmethod
    def appOpenVPNName():
        return "openvpn"
                                
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

        # send emails
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
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
        # assert(('Daily Reports' in emailContext) and ('Content-Type: image/png; name=' in emailContext2))
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

        # send email
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
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

        # send email
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
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
        
    def test_103_email_report_verify_apps(self):
        """
        Generate traffic then verify apps are found in the mailed report        
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
        
        # Install apps found in mailed reports
        appFirewall = None
        if (uvmContext.appManager().isInstantiated(self.appFirewallName())):
            print "App %s already installed" % self.appFirewallName()
            appFirewall = uvmContext.appManager().app(self.appFirewallName())
        else:
            appFirewall = uvmContext.appManager().instantiate(self.appFirewallName(), defaultRackId)
            
        appWebFilter = None
        if (uvmContext.appManager().isInstantiated(self.appWebFilterName())):
            print "App %s already installed" % self.appWebFilterName()
            appWebFilter = uvmContext.appManager().app(self.appWebFilterName())
        else:
            appWebFilter = uvmContext.appManager().instantiate(self.appWebFilterName(), defaultRackId)
                      
        appVirusBlocker = None
        if (uvmContext.appManager().isInstantiated(self.appVirusBlockerName())):
            print "App %s already installed" % self.appVirusBlockerName()
            appVirusBlocker = uvmContext.appManager().app(self.appVirusBlockerName())
        else:
            appVirusBlocker = uvmContext.appManager().instantiate(self.appVirusBlockerName(), defaultRackId)
            
        appSpamBlocker = None
        if (uvmContext.appManager().isInstantiated(self.appSpamBlockerName())):
            print "App %s already installed" % self.appSpamBlockerName()
            appSpamBlocker = uvmContext.appManager().app(self.appSpamBlockerName())
        else:
            appSpamBlocker = uvmContext.appManager().instantiate(self.appSpamBlockerName(), defaultRackId)
        
        appPhishBlocker = None
        if (uvmContext.appManager().isInstantiated(self.appPhishBlockerName())):
            print "App %s already installed" % self.appPhishBlockerName()
            appPhishBlocker = uvmContext.appManager().app(self.appPhishBlockerName())
        else:
            appPhishBlocker = uvmContext.appManager().instantiate(self.appPhishBlockerName(), defaultRackId)
        
        appAdBlocker = None
        if (uvmContext.appManager().isInstantiated(self.appAdBlockerName())):
            print "App %s already installed" % self.appAdBlockerName()
            appAdBlocker = uvmContext.appManager().app(self.appAdBlockerName())
        else:
            appAdBlocker = uvmContext.appManager().instantiate(self.appAdBlockerName(), defaultRackId)
        
        appWebCache = None
        if (uvmContext.appManager().isInstantiated(self.appWebCacheName())):
            print "App %s already installed" % self.appWebCacheName()
            appWebCache = uvmContext.appManager().app(self.appWebCacheName())
        else:
            appWebCache = uvmContext.appManager().instantiate(self.appWebCacheName(), defaultRackId)
        
        appBandwidthControl = None
        if (uvmContext.appManager().isInstantiated(self.appBandwidthControlName())):
            print "App %s already installed" % self.appBandwidthControlName()
            appBandwidthControl = uvmContext.appManager().app(self.appBandwidthControlName())
        else:
            appBandwidthControl = uvmContext.appManager().instantiate(self.appBandwidthControlName(), defaultRackId)
            
        appApplicationControl = None
        if (uvmContext.appManager().isInstantiated(self.appApplicationControlName())):
            print "App %s already installed" % self.appApplicationControlName()
            appApplicationControl = uvmContext.appManager().app(self.appApplicationControlName())
        else:
            appApplicationControl = uvmContext.appManager().instantiate(self.appApplicationControlName(), defaultRackId)
            
        appSSLInspector = None
        if (uvmContext.appManager().isInstantiated(self.appSSLInspectorName())):
            print "App %s already installed" % self.appSSLInspectorName()
            appSSLInspector = uvmContext.appManager().app(self.appSSLInspectorName())
        else:
            appSSLInspector = uvmContext.appManager().instantiate(self.appSSLInspectorName(), defaultRackId)     
       
        appWebMonitor = None
        if (uvmContext.appManager().isInstantiated(self.appWebMonitorName())):
            print "App %s already installed" % self.appWebMonitorName()
            appWebMonitor = uvmContext.appManager().app(self.appWebMonitorName())
        else:
            appWebMonitor = uvmContext.appManager().instantiate(self.appWebMonitorName(), defaultRackId)    
            
        appCaptivePortal = None
        if (uvmContext.appManager().isInstantiated(self.appCaptivePortalName())):
            print "App %s already installed" % self.appCaptivePortalName()
            appCaptivePortal = uvmContext.appManager().app(self.appCaptivePortalName())
        else:
            appCaptivePortal = uvmContext.appManager().instantiate(self.appCaptivePortalName(), defaultRackId)    
        
        appVirusBlockerLite = None
        if (uvmContext.appManager().isInstantiated(self.appVirusBlockerLiteName())):
            print "App %s already installed" % self.appVirusBlockerLiteName()
            appVirusBlockerLite = uvmContext.appManager().app(self.appVirusBlockerLiteName())
        else:
            appVirusBlockerLite = uvmContext.appManager().instantiate(self.appVirusBlockerLiteName(), defaultRackId)
            
        appSpamBlockerLite = None
        if (uvmContext.appManager().isInstantiated(self.appSpamBlockerLiteName())):
            print "App %s already installed" % self.appSpamBlockerLiteName()
            appSpamBlockerLite = uvmContext.appManager().app(self.appSpamBlockerLiteName())
        else:
            appSpamBlockerLite = uvmContext.appManager().instantiate(self.appSpamBlockerLiteName(), defaultRackId)    
            
        appApplicationControlLite = None
        if (uvmContext.appManager().isInstantiated(self.appApplicationControlLiteName())):
            print "App %s already installed" % self.appApplicationControlLiteName()
            appApplicationControlLite = uvmContext.appManager().app(self.appApplicationControlLiteName())
        else:
            appApplicationControlLite = uvmContext.appManager().instantiate(self.appApplicationControlLiteName(), defaultRackId)    
        
        appPolicyManager = None
        if (uvmContext.appManager().isInstantiated(self.appPolicyManagerName())):
            print "App %s already installed" % self.appPolicyManagerName()
            appPolicyManager = uvmContext.appManager().app(self.appPolicyManagerName())
        else:
            appPolicyManager = uvmContext.appManager().instantiate(self.appPolicyManagerName(), defaultRackId)    
        
        appDirectoryConnector = None
        if (uvmContext.appManager().isInstantiated(self.appDirectoryConnectorName())):
            print "App %s already installed" % self.appDirectoryConnectorName()
            appDirectoryConnector = uvmContext.appManager().app(self.appDirectoryConnectorName())
        else:
            appDirectoryConnector = uvmContext.appManager().instantiate(self.appDirectoryConnectorName(), defaultRackId)    
        
        appWANFailover = None
        if (uvmContext.appManager().isInstantiated(self.appWANFailoverName())):
            print "App %s already installed" % self.appWANFailoverName()
            appWANFailover = uvmContext.appManager().app(self.appWANFailoverName())
        else:
            appWANFailover = uvmContext.appManager().instantiate(self.appWANFailoverName(), defaultRackId)    
        
        appWANBalancer = None
        if (uvmContext.appManager().isInstantiated(self.appWANBalancerName())):
            print "App %s already installed" % self.appWANBalancerName()
            appWANBalancer = uvmContext.appManager().app(self.appWANBalancerName())
        else:
            appWANBalancer = uvmContext.appManager().instantiate(self.appWANBalancerName(), defaultRackId)    
        
        appConfigurationBackup = None
        if (uvmContext.appManager().isInstantiated(self.appConfigurationBackupName())):
            print "App %s already installed" % self.appConfigurationBackupName()
            appConfigurationBackup = uvmContext.appManager().app(self.appConfigurationBackupName())
        else:
            appConfigurationBackup = uvmContext.appManager().instantiate(self.appConfigurationBackupName(), defaultRackId)    
        
        appIntrusionPrevention = None
        if (uvmContext.appManager().isInstantiated(self.appIntrusionPreventionName())):
            print "App %s already installed" % self.appIntrusionPreventionName()
            appIntrusionPrevention = uvmContext.appManager().app(self.appIntrusionPreventionName())
        else:
            appIntrusionPrevention = uvmContext.appManager().instantiate(self.appIntrusionPreventionName(), defaultRackId)    
        
        appIPSecVPN = None
        if (uvmContext.appManager().isInstantiated(self.appIPSecVPNName())):
            print "App %s already installed" % self.appIPSecVPNName()
            appIPSecVPN = uvmContext.appManager().app(self.appIPSecVPNName())
        else:
            appIPSecVPN = uvmContext.appManager().instantiate(self.appIPSecVPNName(), defaultRackId)    
        
        appOpenVPN = None
        if (uvmContext.appManager().isInstantiated(self.appOpenVPNName())):
            print "App %s already installed" % self.appOpenVPNName()
            appOpenVPN = uvmContext.appManager().app(self.appOpenVPNName())
        else:
            appOpenVPN = uvmContext.appManager().instantiate(self.appOpenVPNName(), defaultRackId) 
        
        # create some traffic 
        result = remote_control.is_online(tries=1)

        # flush out events
        app.flushEvents()

        # send emails
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for email
        remote_control.run_command("rm -f /tmp/test_103_email_report_admin_file")
        emailFound = False
        result1 = ""
        result2 = ""
        result3 = ""
        result4 = ""
        result5 = ""
        result6 = ""
        result7 = ""
        result8 = ""
        result9 = ""
        result10 = ""
        result11 = ""
        result12 = ""
        result13 = ""
        result14 = ""
        result15 = ""
        result16 = ""
        result17 = ""
        result18 = ""
        result19 = ""
        result20 = ""
        result21 = ""
        result22 = ""
        result23 = ""
        result24 = ""
        timeout = 120
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            # Check to see if the delivered email file is present
            result = remote_control.run_command("wget -q --timeout=5 -O /tmp/test_103_email_report_admin_file http://test.untangle.com/cgi-bin/getEmail.py?toaddress=" + testEmailAddress + " 2>&1")
            if (result == 0):
                emailFound = True
                result1 = remote_control.run_command("grep -i 'Daily Reports' /tmp/test_103_email_report_admin_file 2>&1")
                result2 = remote_control.run_command("grep -i 'Firewall' /tmp/test_103_email_report_admin_file 2>&1")
                result3 = remote_control.run_command("grep -i 'Web Filter' /tmp/test_103_email_report_admin_file 2>&1")
                result4 = remote_control.run_command("grep -i 'Virus Blocker' /tmp/test_103_email_report_admin_file 2>&1")
                result5 = remote_control.run_command("grep -i 'Spam Blocker' /tmp/test_103_email_report_admin_file 2>&1")
                result6 = remote_control.run_command("grep -i 'Phish Blocker' /tmp/test_103_email_report_admin_file 2>&1")
                result7 = remote_control.run_command("grep -i 'Ad Blocker' /tmp/test_103_email_report_admin_file 2>&1")
                result8 = remote_control.run_command("grep -i 'Web Cache' /tmp/test_103_email_report_admin_file 2>&1")
                result9 = remote_control.run_command("grep -i 'Bandwidth Control' /tmp/test_103_email_report_admin_file 2>&1")
                result10 = remote_control.run_command("grep -i 'Application Control' /tmp/test_103_email_report_admin_file 2>&1")
                result11 = remote_control.run_command("grep -i 'SSL Inspector' /tmp/test_103_email_report_admin_file 2>&1")
                result12 = remote_control.run_command("grep -i 'Web Monitor' /tmp/test_103_email_report_admin_file 2>&1")
                result13 = remote_control.run_command("grep -i 'Captive Portal' /tmp/test_103_email_report_admin_file 2>&1")
                result14 = remote_control.run_command("grep -i 'Virus Blocker Lite' /tmp/test_103_email_report_admin_file 2>&1")
                result15 = remote_control.run_command("grep -i 'Spam Blocker Lite' /tmp/test_103_email_report_admin_file 2>&1")
                result16 = remote_control.run_command("grep -i 'Application Control Lite' /tmp/test_103_email_report_admin_file 2>&1")
                result17 = remote_control.run_command("grep -i 'Policy Manager' /tmp/test_103_email_report_admin_file 2>&1")
                result18 = remote_control.run_command("grep -i 'Directory Connector' /tmp/test_103_email_report_admin_file 2>&1")
                result19 = remote_control.run_command("grep -i 'WAN Failover' /tmp/test_103_email_report_admin_file 2>&1")
                result20 = remote_control.run_command("grep -i 'WAN Balancer' /tmp/test_103_email_report_admin_file 2>&1")
                result21 = remote_control.run_command("grep -i 'Configuration Backup' /tmp/test_103_email_report_admin_file 2>&1")
                result22 = remote_control.run_command("grep -i 'Intrusion Prevention' /tmp/test_103_email_report_admin_file 2>&1")
                result23 = remote_control.run_command("grep -i 'IPsec VPN' /tmp/test_103_email_report_admin_file 2>&1")
                result24 = remote_control.run_command("grep -i 'OpenVPN' /tmp/test_103_email_report_admin_file 2>&1")

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        
        # remove firewall
        if appFirewall != None:
            uvmContext.appManager().destroy( appFirewall.getAppSettings()["id"] )
        appFirewall = None
        
        # remove web filter
        if appWebFilter != None:
            uvmContext.appManager().destroy( appWebFilter.getAppSettings()["id"] )
        appWebFilter = None
        
        # remove virus blocker
        if appVirusBlocker != None:
            uvmContext.appManager().destroy( appVirusBlocker.getAppSettings()["id"] )
        appVirusBlocker = None
        
        # remove spam blocker
        if appSpamBlocker != None:
            uvmContext.appManager().destroy( appSpamBlocker.getAppSettings()["id"] )
        appSpamBlocker = None
        
        # remove phish blocker
        if appPhishBlocker != None:
            uvmContext.appManager().destroy( appPhishBlocker.getAppSettings()["id"] )
        appPhishBlocker = None
        
        # remove ad blocker
        if appAdBlocker != None:
            uvmContext.appManager().destroy( appAdBlocker.getAppSettings()["id"] )
        appAdBlocker = None
        
        # remove web cache
        if appWebCache != None:
            uvmContext.appManager().destroy( appWebCache.getAppSettings()["id"] )
        appWebCache = None
        
        # remove bandwidth control
        if appBandwidthControl != None:
            uvmContext.appManager().destroy( appBandwidthControl.getAppSettings()["id"] )
        appBandwidthControl = None
        
        # remove application control
        if appApplicationControl != None:
            uvmContext.appManager().destroy( appApplicationControl.getAppSettings()["id"] )
        appApplicationControl = None
        
        # remove ssl inspector
        if appSSLInspector != None:
            uvmContext.appManager().destroy( appSSLInspector.getAppSettings()["id"] )
        appSSLInspector = None
        
        # remove web monitor
        if appWebMonitor != None:
            uvmContext.appManager().destroy( appWebMonitor.getAppSettings()["id"] )
        appWebMonitor = None
        
        # remove captive portal
        if appCaptivePortal != None:
            uvmContext.appManager().destroy( appCaptivePortal.getAppSettings()["id"] )
        appCaptivePortal = None
        
        # remove virus blocker lite
        if appVirusBlockerLite != None:
            uvmContext.appManager().destroy( appVirusBlockerLite.getAppSettings()["id"] )
        appVirusBlockerLite = None
        
         # remove spam blocker lite
        if appSpamBlockerLite != None:
            uvmContext.appManager().destroy( appSpamBlockerLite.getAppSettings()["id"] )
        appSpamBlockerLite = None
        
        # remove application control lite
        if appApplicationControlLite != None:
            uvmContext.appManager().destroy( appApplicationControlLite.getAppSettings()["id"] )
        appApplicationControlLite = None
        
        # remove policy manager
        if appPolicyManager != None:
            uvmContext.appManager().destroy( appPolicyManager.getAppSettings()["id"] )
        appPolicyManager = None
        
        # remove directory connector
        if appDirectoryConnector != None:
            uvmContext.appManager().destroy( appDirectoryConnector.getAppSettings()["id"] )
        appDirectoryConnector = None
        
        # remove wan failover
        if appWANFailover != None:
            uvmContext.appManager().destroy( appWANFailover.getAppSettings()["id"] )
        appWANFailover = None
        
        # remove wan balancer
        if appWANBalancer != None:
            uvmContext.appManager().destroy( appWANBalancer.getAppSettings()["id"] )
        appWANBalancer = None
        
        # remove config backup
        if appConfigurationBackup != None:
            uvmContext.appManager().destroy( appConfigurationBackup.getAppSettings()["id"] )
        appConfigurationBackup = None
        
        # remove intrusion prevention
        if appIntrusionPrevention != None:
            uvmContext.appManager().destroy( appIntrusionPrevention.getAppSettings()["id"] )
        appIntrusionPrevention = None
        
        # remove ipsec vpn
        if appIPSecVPN != None:
            uvmContext.appManager().destroy( appIPSecVPN.getAppSettings()["id"] )
        appIPSecVPN = None
        
        # remove open vpn
        if appOpenVPN != None:
            uvmContext.appManager().destroy( appOpenVPN.getAppSettings()["id"] )
        appOpenVPN = None
        
        assert(emailFound)
        # assert()
        assert(result1 == 0)
        assert(result2 == 0)
        assert(result3 == 0)
        assert(result4 == 0)
        assert(result5 == 0)
        assert(result6 == 0)
        assert(result7 == 0)
        assert(result8 == 0)
        assert(result9 == 0)
        assert(result10 == 0)
        assert(result11 == 0)
        assert(result12 == 0)
        assert(result13 == 0)
        assert(result14 == 0)
        assert(result15 == 0)
        assert(result16 == 0)
        assert(result17 == 0)
        assert(result18 == 0)
        assert(result19 == 0)
        assert(result20 == 0)
        assert(result21 == 0)
        assert(result22 == 0)
        assert(result23 == 0)
        assert(result24 == 0)
             
    @staticmethod
    def finalTearDown(self):
        global app
        if app != None:
            app.setSettings(orig_settings)
        if orig_mailsettings != None:
            uvmContext.mailSender().setSettings(orig_mailsettings)
        app = None

test_registry.registerApp("reports", ReportsTests)
