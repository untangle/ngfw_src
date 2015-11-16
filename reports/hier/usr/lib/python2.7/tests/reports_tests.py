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
from datetime import datetime
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
canRelay = None
# special box with testshell in the sudoer group  - used to connect to as client
# DNS MX record on 10.111.56.57 for domains untangletestvm.com and untangletest.com
listFakeSmtpServerHosts = [('10.112.56.30','16','untangletestvm.com'),('10.111.56.84','16','untangletest.com')]
specialDnsServer = "10.111.56.96"
fakeSmtpServerHost = ""
fakeSmtpServerHostResult = -1
testdomain = ""
testEmailAddress = ""
orig_settings = None
orig_netsettings = None
orig_mailsettings = None

# pdb.set_trace()

def sendTestmessage(smtpHost=listFakeSmtpServerHosts[0]):
    sender = 'test@example.com'
    receivers = ['qa@example.com']
    relaySuccess = False
    
    message = """From: Test <test@example.com>
    To: Test Group <qa@example.com>
    Subject: SMTP e-mail test

    This is a test e-mail message.
    """
    remote_control.runCommand("sudo python fakemail.py --host=" + smtpHost +" --log=/tmp/report_test.log --port 25 --background --path=/tmp/", host=smtpHost, stdout=False, nowait=True)
    time.sleep(2) # its run in the background so wait for it to start
    
    try:
       smtpObj = smtplib.SMTP(smtpHost)
       smtpObj.sendmail(sender, receivers, message)
       print "Successfully sent email"
       relaySuccess = True
    except smtplib.SMTPException, e:
       print "Error: unable to send email" + str(e)
       relaySuccess =  False
       
    remote_control.runCommand("sudo pkill -INT python",host=smtpHost)
    return relaySuccess

def createFakeEmailEnvironment(emailLogFile="report_test.log"):
    global orig_mailsettings
    orig_mailsettings = uvmContext.mailSender().getSettings()
    new_mailsettings = copy.deepcopy(orig_mailsettings)
    new_mailsettings['sendMethod'] = 'DIRECT'
    uvmContext.mailSender().setSettings(new_mailsettings)

    # set untangletest email to get to fakeSmtpServerHost where fake SMTP sink is running using special DNS server
    netsettings = uvmContext.networkManager().getNetworkSettings()
    # Change DNS to point at special DNS server with entry for fake domain untangletest.com
    # Only run test if WAN IP is static or DHCP
    for i in range(len(netsettings['interfaces']['list'])):
        if not netsettings['interfaces']['list'][i]['disabled'] and  netsettings['interfaces']['list'][i]['isWan']:
        # if netsettings['interfaces']['list'][i]['v4StaticAddress'] == wan_IP:
            if netsettings['interfaces']['list'][i]['configType'] == "ADDRESSED" and netsettings['interfaces']['list'][i]['v4ConfigType'] == "STATIC":
                netsettings['interfaces']['list'][i]['v4StaticDns1'] = specialDnsServer
            elif netsettings['interfaces']['list'][i]['configType'] == "ADDRESSED" and netsettings['interfaces']['list'][i]['v4ConfigType'] == "AUTO":
                netsettings['interfaces']['list'][i]['v4AutoDns1Override'] = specialDnsServer
            else:
                # only use if interface is addressed
                raise unittest2.SkipTest('Unable to use Interface ' + netsettings['interfaces']['list'][i]['name'])

    uvmContext.networkManager().setNetworkSettings(netsettings)

    # Remove old email and log files.
    remote_control.runCommand("sudo rm /tmp/" + emailLogFile, host=fakeSmtpServerHost)
    remote_control.runCommand("sudo rm /tmp/" + testEmailAddress + "*", host=fakeSmtpServerHost)
    remote_control.runCommand("sudo python fakemail.py --host=" + fakeSmtpServerHost +" --log=/tmp/" + emailLogFile + " --port 25 --background --path=/tmp/", host=fakeSmtpServerHost, stdout=False, nowait=True)

def findEmailContent(searchTerm1,searchTerm2):
    ifFound = False
    timeout = 120
    print "Looking at email " + testEmailAddress
    while not ifFound and timeout > 0:
        timeout -= 1
        time.sleep(1)
        emailfile = remote_control.runCommand("ls -l /tmp/" + testEmailAddress + "*",host=fakeSmtpServerHost)
        if (emailfile == 0):
            ifFound = True
    grepContext=remote_control.runCommand("grep -i '" + searchTerm1 + "' /tmp/" + testEmailAddress + "*",host=fakeSmtpServerHost, stdout=True)
    grepContext2=remote_control.runCommand("grep -i '" + searchTerm2 + "' /tmp/" + testEmailAddress + "*",host=fakeSmtpServerHost, stdout=True)
    return(ifFound, grepContext, grepContext2)
    
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

def createReportProfile(profile_email=testEmailAddress):
    print "Email in createReportProfile " + profile_email
    return  {
            "emailAddress": profile_email,
            "emailSummaries": True,
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

    def setUp(self):
        global node, orig_settings, orig_netsettings, fakeSmtpServerHost, fakeSmtpServerHostResult, testdomain, testEmailAddress, canRelay
        if orig_netsettings == None:
            orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "Node %s already installed" % self.nodeName()
                # report node is normally installed.
                # raise Exception('node %s already instantiated' % self.nodeName())
                node = uvmContext.nodeManager().node(self.nodeName())
            else:
                node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        if orig_settings == None:
            reportSettings = node.getSettings()
            orig_settings = copy.deepcopy(reportSettings)

        # Skip checking relaying is possible if we have determined it as true on previous test.
        if canRelay == None:
            wan_IP = uvmContext.networkManager().getFirstWanAddress()
            for smtpServerHostIP in listFakeSmtpServerHosts:
                interfaceNet = smtpServerHostIP[0] + "/" + str(smtpServerHostIP[1])
                if ipaddr.IPAddress(wan_IP) in ipaddr.IPv4Network(interfaceNet):
                    fakeSmtpServerHost = smtpServerHostIP[0]
                    testdomain = smtpServerHostIP[2]
                    testEmailAddress = "qa@" + testdomain                
            print "fakeSmtpServerHost " + fakeSmtpServerHost
            if (fakeSmtpServerHost == ""):
                canRelay = None
            else: 
                fakeSmtpServerHostResult = subprocess.call(["ping","-c","1",fakeSmtpServerHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
                print "fakeSmtpServerHostResult " + str(fakeSmtpServerHostResult)
                if (fakeSmtpServerHostResult == 0):
                    try:
                        canRelay = sendTestmessage(smtpHost=fakeSmtpServerHost)
                    except Exception,e:
                        canRelay = False
                
    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)
    
    def test_040_remoteSyslog(self):
        if (not canRelay):
            raise unittest2.SkipTest('Unable to relay through ' + fakeSmtpServerHost)
        if (fakeSmtpServerHostResult != 0):
            raise unittest2.SkipTest("Syslog server unreachable")        

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
        result = remote_control.isOnline()
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
            rsyslogResult = remote_control.runCommand("sudo tail -n 50 /var/log/localhost/localhost.log | grep 'FirewallEvent'", host=fakeSmtpServerHost, stdout=True)
            for line in rsyslogResult.splitlines():
                print "\nchecking line: %s " % line
                timeout -= 1
                for string in strings_to_find:
                    if not string in line:
                        print "missing: %s" % string
                        continue
                    else:
                        found_count += 1
                        print "found: %s" % string
                break

        assert(found_count == len(strings_to_find))

    def test_070_basic_alert(self):
        fname = sys._getframe().f_code.co_name
        settings = node.getSettings()
        settings["alertRules"]["list"] = []
        settings["alertRules"]["list"].append(createAlertRule(fname,"class","=","*SessionEvent*","SServerPort","=","80"))
        print settings["alertRules"]["list"]
        node.setSettings(settings)

        result = remote_control.isOnline()
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

        result = remote_control.isOnline()
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

        for x in range(0,20): remote_control.isOnline()
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
            nodeWanFailover = uvmContext.nodeManager().instantiate(self.nodeWanFailoverName(), defaultRackId)

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
        fname = sys._getframe().f_code.co_name
        settings = node.getSettings()
        # set email address and alert for downloads
        settings["reportsUsers"]["list"].append(createReportProfile(profile_email=testEmailAddress))
        settings["alertRules"]["list"] = []
        settings["alertRules"]["list"].append(createAlertRule(fname,"class","=","*SessionEvent*","SServerPort","=","80"))
        node.setSettings(settings)

        # Create settings to receive testEmailAddress 
        createFakeEmailEnvironment("test_080.log")
        
        # set admin email to get alerts
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser(useremail=testEmailAddress))
        uvmContext.adminManager().setSettings(adminsettings)

        # trigger alert
        result = remote_control.isOnline()

        # look for alert email
        emailFound, emailContext, emailContext2 = findEmailContent('alert',fname)

        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=fakeSmtpServerHost)
        
        # restore admin settings and network settings
        uvmContext.adminManager().setSettings(orig_adminsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert(emailFound)
        assert(("Server Alert" in emailContext) and (fname in emailContext2))

        node.flushEvents() # flush events so the rules are evaluated
        events = global_functions.get_events('Reports','Alert Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 'description', fname)
        assert(found)
        
    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            node.setSettings(orig_settings)
        if orig_mailsettings != None:
            uvmContext.mailSender().setSettings(orig_mailsettings)
        node = None

test_registry.registerNode("reports", ReportsTests)
