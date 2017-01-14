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
node = None
canRelay = None
canSyslog = None
fakeSmtpServerHost = ""
fakeSmtpServerHostResult = -1
testdomain = ""
testEmailAddress = ""
orig_settings = None
orig_netsettings = None
orig_mailsettings = None
# pdb.set_trace()

def sendTestmessage(smtpHost=global_functions.listFakeSmtpServerHosts[0]):
    sender = 'test@example.com'
    receivers = ['qa@example.com']
    relaySuccess = False
    
    message = """From: Test <test@example.com>
    To: Test Group <qa@example.com>
    Subject: SMTP e-mail test

    This is a test e-mail message.
    """
    remote_control.runCommand("sudo python fakemail.py --host=" + smtpHost +" --log=/tmp/report_test.log --port 25 --background --path=/tmp/", host=smtpHost, stdout=False, nowait=True)
    time.sleep(10) # its run in the background so wait for it to start
    
    try:
       smtpObj = smtplib.SMTP(smtpHost)
       smtpObj.sendmail(sender, receivers, message)
       relaySuccess = True
    # except smtplib.SMTPException, e:
    except Exception, e:
       relaySuccess =  False
       
    remote_control.runCommand("sudo pkill -INT python",host=smtpHost)
    return relaySuccess

def createDomainDNSentries(domain,dnsServer):
    return {
        "domain": domain,
        "javaClass": "com.untangle.uvm.network.DnsLocalServer",
        "localServer": dnsServer
    }
    
def createFakeEmailEnvironment(emailLogFile="report_test.log"):
    global orig_mailsettings
    orig_mailsettings = uvmContext.mailSender().getSettings()
    new_mailsettings = copy.deepcopy(orig_mailsettings)
    new_mailsettings['sendMethod'] = 'DIRECT'
    new_mailsettings['fromAddress'] = testEmailAddress
    uvmContext.mailSender().setSettings(new_mailsettings)

    # set untangletest email to get to fakeSmtpServerHost where fake SMTP sink is running using special DNS server
    netsettings = uvmContext.networkManager().getNetworkSettings()
    # Add Domain DNS Server for special test domains of untangletestvm.com and untangletest.com
    netsettings['dnsSettings']['localServers']['list'].append(createDomainDNSentries(testdomain,global_functions.specialDnsServer))
    uvmContext.networkManager().setNetworkSettings(netsettings)

    # Remove old email and log files.
    remote_control.runCommand("sudo rm /tmp/" + emailLogFile, host=fakeSmtpServerHost)
    remote_control.runCommand("sudo rm /tmp/" + testEmailAddress + "*", host=fakeSmtpServerHost)
    remote_control.runCommand("sudo python fakemail.py --host=" + fakeSmtpServerHost +" --log=/tmp/" + emailLogFile + " --port 25 --background --path=/tmp/", host=fakeSmtpServerHost, stdout=False, nowait=True)

def findEmailContent(searchTerm1,searchTerm2,measureBegin=False,measureEnd=False):
    ifFound = False
    timeout = 30
    while not ifFound and timeout > 0:
        timeout -= 1
        time.sleep(1)
        emailfile = remote_control.runCommand("ls -l /tmp/" + testEmailAddress + "*",host=fakeSmtpServerHost)
        if (emailfile == 0):
            ifFound = True
        else:
            # unfreeze any messages in the exim queue
            subprocess.call(["exim","-qff"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    grepContext=remote_control.runCommand("grep -i '" + searchTerm1 + "' /tmp/" + testEmailAddress + "*",host=fakeSmtpServerHost, stdout=True)
    grepContext2=remote_control.runCommand("grep -i '" + searchTerm2 + "' /tmp/" + testEmailAddress + "*",host=fakeSmtpServerHost, stdout=True)

    measureLength=0
    if measureBegin != False and measureEnd != False:
        measureLength=remote_control.runCommand("sed -n '/" + measureBegin.replace('/', '\/').replace('"', '\\"') + "/,/" + measureEnd.replace('/', '\/').replace('"', '\\"') + "/p' /tmp/" + testEmailAddress + "* | wc -l",host=fakeSmtpServerHost, stdout=True)
    return(ifFound, grepContext, grepContext2, measureLength)
    
def createFirewallSingleConditionRule( conditionType, value, blocked=True ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
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
                    "javaClass": "com.untangle.node.firewall.FirewallRuleCondition", 
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
            "javaClass": "com.untangle.node.reports.ReportsUser",
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
            "javaClass": "com.untangle.node.reports.AlertRule",
            "log": True,
            "conditions": {
                "javaClass": "java.util.LinkedList",
                "list": [
                    {
                        "javaClass": "com.untangle.node.reports.AlertRuleCondition",
                        "conditionType": "FIELD_CONDITION",
                        "value": {
                            "comparator": operator,
                            "field": matcherField,
                            "javaClass": "com.untangle.node.reports.AlertRuleConditionField",
                            "value": value
                        }
                    },
                    {
                        "javaClass": "com.untangle.node.reports.AlertRuleCondition",
                        "conditionType": "FIELD_CONDITION",
                        "value": {
                            "comparator": operator2,
                            "field": matcherField2,
                            "javaClass": "com.untangle.node.reports.AlertRuleConditionField",
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
        "javaClass": "com.untangle.node.reports.EmailTemplate",
        "mobile": mobile,
        "readOnly": False,
        "templateId": 2,
        "title": "Custom Report"
    }
    
class ReportsTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-reports"

    @staticmethod
    def nodeFirewallName():
        return "untangle-node-firewall"

    @staticmethod
    def nodeWanFailoverName():
        return "untangle-node-wan-failover"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global node, orig_settings, orig_netsettings, fakeSmtpServerHost, fakeSmtpServerHostResult, testdomain, testEmailAddress, canRelay, canSyslog
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            # report node is normally installed.
            # print "Node %s already installed" % self.nodeName()
            # raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().node(self.nodeName())
        else:
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        reportSettings = node.getSettings()
        orig_settings = copy.deepcopy(reportSettings)

        # Skip checking relaying is possible if we have determined it as true on previous test.
        if canRelay == None:
            wan_IP = uvmContext.networkManager().getFirstWanAddress()
            fakeSmtpServerHost, testdomain = global_functions.findSmtpServer(wan_IP)
            testEmailAddress = "qa@" + testdomain                
            # print "fakeSmtpServerHost " + fakeSmtpServerHost
            if (fakeSmtpServerHost == ""):
                canRelay = None
            else: 
                fakeSmtpServerHostResult = subprocess.call(["ping","-c","1",fakeSmtpServerHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
                # print "fakeSmtpServerHostResult " + str(fakeSmtpServerHostResult)
                if (fakeSmtpServerHostResult == 0):
                    try:
                        canRelay = sendTestmessage(smtpHost=fakeSmtpServerHost)
                    except Exception,e:
                        canRelay = False

        if canSyslog == None:
            portResult = remote_control.runCommand("sudo lsof -i :514", host=fakeSmtpServerHost)
            if portResult == 0:
               canSyslog = True
            else:
               canSyslog = False
               
    def setUp(self):
        pass
                
    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)
    
    def test_040_remoteSyslog(self):
        if (fakeSmtpServerHostResult != 0):
            raise unittest2.SkipTest("Syslog server unreachable")        
        if (not canSyslog):
            raise unittest2.SkipTest('Unable to syslog through ' + fakeSmtpServerHost)

        nodeFirewall = None
        if (uvmContext.nodeManager().isInstantiated(self.nodeFirewallName())):
            print "Node %s already installed" % self.nodeFirewallName()
            nodeFirewall = uvmContext.nodeManager().node(self.nodeFirewallName())
        else:
            nodeFirewall = uvmContext.nodeManager().instantiate(self.nodeFirewallName(), defaultRackId)

        # Install firewall rule to generate syslog events
        rules = nodeFirewall.getRules()
        rules["list"].append(createFirewallSingleConditionRule("SRC_ADDR",remote_control.clientIP));
        nodeFirewall.setRules(rules);
        rules = nodeFirewall.getRules()
        # Get rule ID
        for rule in rules['list']:
            if rule['enabled'] and rule['block']:
                targetRuleId = rule['ruleId']
                break
        # Setup syslog to send events to syslog host
        newSyslogSettings = node.getSettings()
        newSyslogSettings["syslogEnabled"] = True
        newSyslogSettings["syslogPort"] = 514
        newSyslogSettings["syslogProtocol"] = "UDP"
        newSyslogSettings["syslogHost"] = fakeSmtpServerHost
        node.setSettings(newSyslogSettings)

        # create some traffic (blocked by firewall and thus create a syslog event)
        result = remote_control.isOnline(tries=1)
        # flush out events
        node.flushEvents()

        # remove the firewall rule aet syslog back to original settings
        node.setSettings(orig_settings)
        rules["list"]=[];
        nodeFirewall.setRules(rules);

        # remove firewall
        if nodeFirewall != None:
            uvmContext.nodeManager().destroy( nodeFirewall.getNodeSettings()["id"] )
        nodeFirewall = None
        
        # parse the output and look for a rule that matches the expected values
        timeout = 5
        found_count = 0
        strings_to_find = ['\"blocked\":true',str('\"ruleId\":%i' % targetRuleId)]
        while (timeout > 0 and found_count < 2):
            # get syslog results on server
            rsyslogResult = remote_control.runCommand("sudo tail -n 200 /var/log/localhost/localhost.log | grep 'FirewallEvent'", host=fakeSmtpServerHost, stdout=True)
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

    def test_070_basic_alert(self):
        fname = sys._getframe().f_code.co_name
        settings = node.getSettings()
        settings["alertRules"]["list"] = []
        settings["alertRules"]["list"].append(createAlertRule(fname,"class","=","*SessionEvent*","SServerPort","=","80"))
        print settings["alertRules"]["list"]
        node.setSettings(settings)

        result = remote_control.isOnline(tries=1)
        node.flushEvents() # flush events so the rules are evaluated
        events = global_functions.get_events('Reports','Alert Events',None,5)

        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 'description', fname)
        assert(found)

    def test_071_threshold_alert_under(self):
        fname = sys._getframe().f_code.co_name
        settings = node.getSettings()
        settings["alertRules"]["list"] = []
        settings["alertRules"]["list"].append(createAlertRule(fname,"class","=","*SessionEvent*","SServerPort","=","80",True,10,60,None))
        print settings["alertRules"]["list"]
        node.setSettings(settings)

        result = remote_control.isOnline(tries=1)
        node.flushEvents() # flush events so the rules are evaluated
        events = global_functions.get_events('Reports','Alert Events',None,5)

        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 'description', fname)
        assert(not found)

    def test_072_threshold_alert_over(self):
        fname = sys._getframe().f_code.co_name
        settings = node.getSettings()
        settings["alertRules"]["list"] = []
        settings["alertRules"]["list"].append(createAlertRule(fname,"class","=","*SessionEvent*","SServerPort","=","80",True,10,60,None))
        print settings["alertRules"]["list"]
        node.setSettings(settings)

        for x in range(0,20): remote_control.isOnline(tries=1)
        node.flushEvents() # flush events so the rules are evaluated
        events = global_functions.get_events('Reports','Alert Events',None,5)

        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 'description', fname)
        assert(found)
        
    def test_080_WAN_down_alert(self):
        # Just check the event log for the alert.
        settings = node.getSettings()
        settings["alertRules"]["list"] = []
        settings["alertRules"]["list"].append(createAlertRule("WAN is offline","class","=","*WanFailoverEvent*","action","=","DISCONNECTED"))
        node.setSettings(settings)

        # Install WAN Failover
        nodeWanFailover = None
        if (uvmContext.nodeManager().isInstantiated(self.nodeWanFailoverName())):
            raise unittest2.SkipTest('WAN Failover already installed')
        else:
            try:
                nodeWanFailover = uvmContext.nodeManager().instantiate(self.nodeWanFailoverName(), defaultRackId)
            except:
                # Some deployments don't have wan failover, if so skip this test
                raise unittest2.SkipTest('WAN Failover failed to install')

        # WAN is offine test
        wanIndex = 0
        netsettings = uvmContext.networkManager().getNetworkSettings()
        timeout = 50000
        for interface in netsettings['interfaces']['list']:
            if interface['isWan']:
                wanIndex =  interface['interfaceId']
                break
        if wanIndex > 0:
            name = "test ping " + str(wanIndex)
            rule = {
                "delayMilliseconds": 5000,
                "description": "test 1",
                "enabled": True,
                "failureThreshold": 3,
                "httpUrl": "http://1.2.3.4/",
                "interfaceId": wanIndex,
                "javaClass": "com.untangle.node.wan_failover.WanTestSettings",
                "pingHostname": "1.2.3.4",
                "testHistorySize": 10,
                "timeoutMilliseconds": 2000,
                "type": "ping"
            }
            nodeWanFailoverData = nodeWanFailover.getSettings()
            nodeWanFailoverData["tests"]["list"].append(rule)
            nodeWanFailover.setSettings(nodeWanFailoverData)
            # Wait for all the WANs to be off line before checking for alert.
            wanUp = True
            while wanUp and timeout > 0:
                timeout -= 1
                wanStatus = nodeWanFailover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if not statusInterface['online'] and statusInterface['interfaceId'] == wanIndex:
                        wanUp = False

        # Check event log for admin alert for WAN down.
        time.sleep(15) # There is a delay in the alert event.
        node.flushEvents() # flush events so the rules are evaluated
        events = global_functions.get_events('Reports','Alert Events',None,5)

        if nodeWanFailover != None:
            uvmContext.nodeManager().destroy( nodeWanFailover.getNodeSettings()["id"] )
        nodeWanFailover = None

        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 'description', 'WAN is offline')
        assert(found)

    def test_090_email_alert(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + fakeSmtpServerHost)
        # Create settings to receive testEmailAddress 
        createFakeEmailEnvironment("test_090.log")

        # set admin email to get alerts
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser(useremail=testEmailAddress))
        uvmContext.adminManager().setSettings(adminsettings)

        fname = sys._getframe().f_code.co_name
        settings = node.getSettings()
        orig_nodesettings = copy.deepcopy(settings)
        # set email address and alert for downloads
        settings["reportsUsers"]["list"].append(createReportsUser(profile_email=testEmailAddress))
        settings["alertRules"]["list"] = []
        settings["alertRules"]["list"].append(createAlertRule(fname,"class","=","*SessionEvent*","SServerPort","=","80"))
        node.setSettings(settings)
        
        # trigger alert
        result = remote_control.isOnline(tries=5)

        # look for alert email
        emailFound, emailContext, emailContext2, measureLength = findEmailContent('alert',fname)

        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=fakeSmtpServerHost)

        # restore admin settings and network settings
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        node.setSettings(orig_nodesettings)
        
        assert(emailFound)
        assert(("Server Alert" in emailContext) and (fname in emailContext2))

        node.flushEvents() # flush events so the rules are evaluated
        events = global_functions.get_events('Reports','Alert Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 'description', fname)
        assert(found)
        
    def test_100_email_report_admin(self):
        """
        The "default" configuration test:
        - Administrator email account gets
        """
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + fakeSmtpServerHost)

        current_method_name = inspect.stack()[0][3]

        # Create settings to receive testEmailAddress 
        createFakeEmailEnvironment( current_method_name + ".log")

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser(useremail=testEmailAddress))
        uvmContext.adminManager().setSettings(adminsettings)

        # Clear all report users
        settings = node.getSettings()
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        node.setSettings(settings)

        # trigger alert
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for alert email
        emailFound, emailContext, emailContext2, measureLength = findEmailContent('Daily Reports','Content-Type: image/png; name=')

        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=fakeSmtpServerHost)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert(emailFound)
        print emailContext
        print emailContext2
        assert(('Daily Reports' in emailContext) and ('Content-Type: image/png; name=' in emailContext2))

    def test_101_email_admin_override_custom_report(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom repor with test not in default.
        """
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + fakeSmtpServerHost)

        current_method_name = inspect.stack()[0][3]

        # Create settings to receive testEmailAddress 
        createFakeEmailEnvironment( current_method_name + ".log")

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser(useremail=testEmailAddress))
        uvmContext.adminManager().setSettings(adminsettings)

        settings = node.getSettings()
        # Add custom template with a test not in daily reports
        settings["emailTemplates"]["list"] = settings["emailTemplates"]["list"][:1]
        settings["emailTemplates"]["list"].append(createEmailTemplate())

        # Add report user with testEmailAddress
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(createReportsUser(profile_email=testEmailAddress, email_template_id=2))
        node.setSettings(settings)

        # trigger alert
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for alert email
        ## look for new template name, custom
        emailFound, emailContext, emailContext2, measureLength = findEmailContent('Custom Report','Administration-VWuRol5uWw')

        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=fakeSmtpServerHost)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert(emailFound)
        assert(('Custom Report' in emailContext) and ('Content-Type: image/png; name=' in emailContext2))

    def test_101_mobile(self):
        """
        1. Use reportuser
        2. Reportuser overrides admin user address.
        3. Custom repor with test not in default.
        4
        """
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + fakeSmtpServerHost)

        current_method_name = inspect.stack()[0][3]

        # Create settings to receive testEmailAddress 
        createFakeEmailEnvironment( current_method_name + ".log")

        # add administrator
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser(useremail=testEmailAddress))
        uvmContext.adminManager().setSettings(adminsettings)

        settings = node.getSettings()
        # Add custom template with a test not in daily reports
        settings["emailTemplates"]["list"] = settings["emailTemplates"]["list"][:1]
        settings["emailTemplates"]["list"].append(createEmailTemplate(mobile=True))

        # Add report user with testEmailAddress
        settings["reportsUsers"]["list"] = settings["reportsUsers"]["list"][:1]
        settings["reportsUsers"]["list"].append(createReportsUser(profile_email=testEmailAddress, email_template_id=2))
        node.setSettings(settings)

        # trigger alert
        subprocess.call([prefix+"/usr/share/untangle/bin/reports-generate-fixed-reports.py"],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        # look for alert email
        ## look for new template name, custom
        emailFound, emailContext, emailContext2, measureLength = findEmailContent('Custom Report','Administration-VWuRol5uWw', 'Content-Type: image/png; name="Administration-VWuRol5uWw@untangle.com.png"', '---')

        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=fakeSmtpServerHost)

        # restore
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert(emailFound)
        assert(('Custom Report' in emailContext) and ('Content-Type: image/png; name=' in emailContext2) and (int(measureLength) < 80))

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            node.setSettings(orig_settings)
        if orig_mailsettings != None:
            uvmContext.mailSender().setSettings(orig_mailsettings)
        node = None

test_registry.registerNode("reports", ReportsTests)
