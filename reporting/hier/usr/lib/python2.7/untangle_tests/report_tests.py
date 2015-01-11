import unittest2
import time
import sys
import pdb
import socket
import subprocess
import copy
import re
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
nodeFirewall = None
nodeFaild = None
nodeWeb = None
# special box with testshell in the sudoer group  - used to connect to as client
syslogHostIP = "10.112.56.30"
testEmailAddress = "qa@untangletest.com"

# pdb.set_trace()

def createFirewallSingleMatcherRule( matcherType, value, blocked=True ):
    matcherTypeStr = str(matcherType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.node.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + matcherTypeStr + " = " + valueStr, 
        "log": True, 
        "block": blocked, 
        "matchers": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.node.firewall.FirewallRuleMatcher", 
                    "matcherType": matcherTypeStr, 
                    "value": valueStr
                    }
                ]
            }
        }

def createReportProfile(profile_email=testEmailAddress):
    return  {
            "emailAddress": profile_email,
            "emailSummaries": True,
            "javaClass": "com.untangle.node.reporting.ReportingUser",
            "onlineAccess": False,
            "passwordHashBase64": ""
    }

def createDNSRule( networkAddr, name):
    return {
        "address": networkAddr, 
        "javaClass": "com.untangle.uvm.network.DnsStaticEntry", 
        "name": name
         }

def createAdminUser( useremail=testEmailAddress):
    username,domainname = useremail.split("@")
    return {
            "description": "System Administrator",
            "emailAddress": useremail,
            "javaClass": "com.untangle.uvm.AdminUserSettings",
            "passwordHashBase64": "YWdlQWnp64i/3IZ6O34JLF0h+BJQ0J3W",
            "username": username
        }

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

class ReportTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-reporting"

    @staticmethod
    def nodeFWName():
        return "untangle-node-firewall"

    @staticmethod
    def nodeFDName():
        return "untangle-node-faild"

    @staticmethod
    def nodeWebName():
        return "untangle-node-sitefilter"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node, nodeFirewall, nodeFaild, nodeWeb, syslogSettings, syslogHostResult
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "Node %s already installed" % self.nodeName()
                # report node is normally installed.
                # raise Exception('node %s already instantiated' % self.nodeName())
                node = uvmContext.nodeManager().node(self.nodeName())
            else:
                node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)

        if nodeFirewall == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeFWName())):
                print "Node %s already installed" % self.nodeFWName()
                nodeFirewall = uvmContext.nodeManager().node(self.nodeFWName())
            else:
                nodeFirewall = uvmContext.nodeManager().instantiate(self.nodeFWName(), defaultRackId)
        if nodeFaild == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeFDName())):
                print "Node %s already installed" % self.nodeFDName()
                nodeFaild = uvmContext.nodeManager().node(self.nodeFDName())
            else:
                nodeFaild = uvmContext.nodeManager().instantiate(self.nodeFDName(), defaultRackId)

        if nodeWeb == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeWebName())):
                print "Node %s already installed" % self.nodeWebName()
                nodeWeb = uvmContext.nodeManager().node(self.nodeWebName())
            else:
                nodeWeb = uvmContext.nodeManager().instantiate(self.nodeWebName(), defaultRackId)

        syslogSettings = node.getSettings()
        syslogHostResult = subprocess.call(["ping","-c","1",syslogHostIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
           
    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)
    
    def test_020_sendReportOut(self):
        if (syslogHostResult != 0):
            raise unittest2.SkipTest("Mail sink server unreachable")        
        # test if PDF is mailed out.
        settings = node.getSettings()
        orig_settings = copy.deepcopy(settings)
        settings["attachmentSizeLimit"] = 5
        settings["reportingUsers"]["list"].append(createReportProfile())
        node.setSettings(settings)
        # set untangletest email to get to syslogHostIP where fake SMTP sink is running
        netsettings = uvmContext.networkManager().getNetworkSettings()
        orig_netsettings = copy.deepcopy(netsettings)
        netsettings['dnsSettings']['staticEntries']['list'].append(createDNSRule(syslogHostIP,"untangletest.com"))
        uvmContext.networkManager().setNetworkSettings(netsettings)
        
        # Remove old email and log files.
        remote_control.runCommand("sudo rm /tmp/test_020_sendReportOut*", host=syslogHostIP, stdout=False, nowait=True)
        remote_control.runCommand("sudo rm /tmp/qa@untangletest.com*", host=syslogHostIP, stdout=False, nowait=True)
        remote_control.runCommand("sudo python fakemail.py --host=" + syslogHostIP +" --log=/tmp/test_020_sendReportOut.log --port 25 --background --path=/tmp/", host=syslogHostIP, stdout=False, nowait=True)
        report_date = time.strftime("%Y-%m-%d")
        # print "report_date %s" % report_date
        report_results = subprocess.call(["/usr/share/untangle/bin/reporting-generate-reports.py", "-r", "1", "-d",report_date],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        # print "report_results %s" % report_results
        time.sleep(10) # wait for email to arrive
        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=syslogHostIP)
        emailContext=remote_control.runCommand("grep 'Untangle PDF Summary Reports' /tmp/qa@untangletest.com*",host=syslogHostIP, stdout=True)
        emailContext2=remote_control.runCommand("grep 'Content-Disposition' /tmp/qa@untangletest.com*",host=syslogHostIP, stdout=True)
        # reset all settings to base.
        node.setSettings(orig_settings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        # test if PDF is attached
        assert(("are attached" in emailContext) and ("pdf" in emailContext2))
        
    def test_040_remoteSyslog(self):
        if (syslogHostResult != 0):
            raise unittest2.SkipTest("Mail sink server unreachable")        
        # Install firewall rule to generate syslog events
        rules = nodeFirewall.getRules()
        rules["list"].append(createFirewallSingleMatcherRule("SRC_ADDR",remote_control.clientIP));
        nodeFirewall.setRules(rules);
        rules = nodeFirewall.getRules()
        # Get rule ID
        for rule in rules['list']:
            if rule['enabled'] and rule['block']:
                targetRuleId = rule['ruleId']
                break
        # Setup syslog to send events to syslog host
        newSyslogSettings = copy.deepcopy(syslogSettings)
        newSyslogSettings["syslogEnabled"] = True
        newSyslogSettings["syslogEnabled"] = True
        newSyslogSettings["syslogHost"] = syslogHostIP
        node.setSettings(newSyslogSettings)

        # create some traffic (blocked by firewall and thus create a syslog event)
        result = remote_control.isOnline()

        # get syslog results on server
        rsyslogResult = remote_control.runCommand("sudo tail -n 10 /var/log/localhost/localhost.log | grep 'FirewallEvent'", host=syslogHostIP, stdout=True)

        # remove the firewall rule aet syslog back to original settings
        node.setSettings(syslogSettings)
        rules["list"]=[];
        nodeFirewall.setRules(rules);
        
        # parse the output and look for a rule that matches the expected values
        found = False
        for line in rsyslogResult.splitlines():
            print "\nchecking line: %s " % line
            for string in ['\"blocked\":true',str('\"ruleId\":%i' % targetRuleId)]:
                if not string in line:
                    print "missing: %s" % string
                    continue
                else:
                    print "found: %s" % string
            found = True
            break

        assert(found)

    def test_080_WAN_alerts(self):
        raise unittest2.SkipTest("Review changes in test")        
        if (syslogHostResult != 0):
            raise unittest2.SkipTest("Mail sink server unreachable")        
        settings = node.getSettings()
        orig_settings = copy.deepcopy(settings)
        settings["attachmentSizeLimit"] = 5
        settings["reportingUsers"]["list"].append(createReportProfile())
        node.setSettings(settings)
        # set untangletest email to get to syslogHostIP where fake SMTP sink is running
        netsettings = uvmContext.networkManager().getNetworkSettings()
        orig_netsettings = copy.deepcopy(netsettings)
        netsettings['dnsSettings']['staticEntries']['list'].append(createDNSRule(syslogHostIP,"untangletest.com"))
        uvmContext.networkManager().setNetworkSettings(netsettings)
        # set admin email to get alerts
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser())
        uvmContext.adminManager().setSettings(adminsettings)
        # Remove old email and log files.
        remote_control.runCommand("sudo rm /tmp/test_080_alerts*", host=syslogHostIP)
        remote_control.runCommand("sudo rm /tmp/qa@untangletest.com*", host=syslogHostIP)
        remote_control.runCommand("sudo python fakemail.py --host=" + syslogHostIP +" --log=/tmp/test_080_alerts.log --port 25 --background --path=/tmp/", host=syslogHostIP, stdout=False, nowait=True)

        # WAN is offine test
        wanIndex = 0
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
                "javaClass": "com.untangle.node.faild.WanTestSettings",
                "pingHostname": "192.168.144.1",
                "testHistorySize": 10,
                "timeoutMilliseconds": 2000,
                "type": "ping"
            }
            nodeFaildData = nodeFaild.getSettings()
            nodeFaildData["tests"]["list"].append(rule)
            nodeFaild.setSettings(nodeFaildData)
            # Wait for all the WANs to be off line before checking for alert email.
            timeout = 50000
            wanUp = True
            while wanUp and timeout > 0:
                timeout -= 1
                wanStatus = nodeFaild.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if not statusInterface['online'] and statusInterface['interfaceId'] == wanIndex:
                        wanUp = False
            assert (timeout != 0)
            # check for email file if there is no timeout
            emailFound = False
            if (timeout != 0):
                timeout = 120
                while not emailFound and timeout > 0:
                    timeout -= 1
                    time.sleep(1)
                    emailfile = remote_control.runCommand("ls -l /tmp/qa@untangletest.com*",host=syslogHostIP)
                    if (emailfile == 0):
                        emailFound = True
                emailContext=remote_control.runCommand("grep -i 'alert' /tmp/qa@untangletest.com*",host=syslogHostIP, stdout=True)
                emailContext2=remote_control.runCommand("grep -i 'WAN is offline' /tmp/qa@untangletest.com*",host=syslogHostIP, stdout=True)

        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=syslogHostIP)
        # reset all settings to base.
        nodeFaildData["tests"]["list"] = []
        nodeFaild.setSettings(nodeFaildData)
        node.setSettings(orig_settings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        uvmContext.adminManager().setSettings(orig_adminsettings)
        assert(emailFound)
        assert(("Server Alert" in emailContext) and ("WAN is offline" in emailContext2))
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'All Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 'description', 'WAN is offline')
        assert(found)

    def test_082_download_alerts(self):
        raise unittest2.SkipTest("Review changes in test")        
        if (syslogHostResult != 0):
            raise unittest2.SkipTest("Mail sink server unreachable")        
        settings = node.getSettings()
        orig_settings = copy.deepcopy(settings)
        settings["attachmentSizeLimit"] = 5
        settings["reportingUsers"]["list"].append(createReportProfile())
        node.setSettings(settings)
        # set untangletest email to get to syslogHostIP where fake SMTP sink is running
        netsettings = uvmContext.networkManager().getNetworkSettings()
        orig_netsettings = copy.deepcopy(netsettings)
        netsettings['dnsSettings']['staticEntries']['list'].append(createDNSRule(syslogHostIP,"untangletest.com"))
        uvmContext.networkManager().setNetworkSettings(netsettings)
        # set admin email to get alerts
        adminsettings = uvmContext.adminManager().getSettings()
        orig_adminsettings = copy.deepcopy(adminsettings)
        adminsettings['users']['list'].append(createAdminUser())
        uvmContext.adminManager().setSettings(adminsettings)
        # Remove old email and log files.
        remote_control.runCommand("sudo rm /tmp/test_080_alerts*", host=syslogHostIP)
        remote_control.runCommand("sudo rm /tmp/qa@untangletest.com*", host=syslogHostIP)
        remote_control.runCommand("sudo python fakemail.py --host=" + syslogHostIP +" --log=/tmp/test_080_alerts.log --port 25 --background --path=/tmp/", host=syslogHostIP, stdout=False, nowait=True)

        # start download
        global_functions.getDownloadSpeed()

        emailFound = False
        timeout = 120
        while not emailFound and timeout > 0:
            timeout -= 1
            time.sleep(1)
            emailfile = remote_control.runCommand("ls -l /tmp/qa@untangletest.com*",host=syslogHostIP)
            if (emailfile == 0):
                emailFound = True
        emailContext=remote_control.runCommand("grep -i 'alert' /tmp/qa@untangletest.com*",host=syslogHostIP, stdout=True)
        emailContext2=remote_control.runCommand("grep -i 'Host is doing' /tmp/qa@untangletest.com*",host=syslogHostIP, stdout=True)

        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=syslogHostIP)
        # reset all settings to base.
        node.setSettings(orig_settings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        uvmContext.adminManager().setSettings(orig_adminsettings)
        assert(emailFound)
        assert(("Server Alert" in emailContext) and ("Host is doing large download" in emailContext2))
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'All Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 'description', 'Host is doing large download')
        assert(found)

    @staticmethod
    def finalTearDown(self):
        global node, nodeFirewall, nodeFaild, nodeWeb
        # no need to uninstall reports
        # if node != None:
        # uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node.setSettings(syslogSettings)
        node = None
        if nodeFirewall != None:
            uvmContext.nodeManager().destroy( nodeFirewall.getNodeSettings()["id"] )
        nodeFirewall = None
        if nodeFaild != None:
            uvmContext.nodeManager().destroy( nodeFaild.getNodeSettings()["id"] )
        nodeFaild = None
        if nodeWeb != None:
            uvmContext.nodeManager().destroy( nodeWeb.getNodeSettings()["id"] )
        nodeWeb = None

test_registry.registerNode("reporting", ReportTests)
