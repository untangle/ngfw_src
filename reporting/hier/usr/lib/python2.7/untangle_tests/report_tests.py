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

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
nodeFirewall = None
# special box with testshell in the sudoer group  - used to connect to as client
syslogHostIP = "10.111.56.32"
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
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node, nodeFirewall, syslogSettings, syslogHostResult
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
        
        # Remote old email and log files.
        remote_control.runCommand("rm /tmp/test_020_sendReportOut*", host=syslogHostIP, stdout=False, nowait=True)
        remote_control.runCommand("rm /tmp/qa@untangletest.com*", host=syslogHostIP, stdout=False, nowait=True)
        remote_control.runCommand("sudo python fakemail.py --host=" + syslogHostIP +" --log=/tmp/test_020_sendReportOut.log --port 25 --background --path=/tmp/", host=syslogHostIP, stdout=False, nowait=True)
        report_date = time.strftime("%Y-%m-%d")
        # print "report_date %s" % report_date
        report_results = subprocess.call(["/usr/share/untangle/bin/reporting-generate-reports.py", "-r", "1", "-d",report_date],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        # print "report_results %s" % report_results
        time.sleep(10) # wait for email to arrive
        # Kill the mail sink
        remote_control.runCommand("sudo pkill -INT python",host=syslogHostIP)
        emailContext=remote_control.runCommand("grep 'Untangle PDF Summary Reports' /tmp/qa@untangletest.com.1",host=syslogHostIP, stdout=True)
        emailContext2=remote_control.runCommand("grep 'Content-Disposition' /tmp/qa@untangletest.com.1",host=syslogHostIP, stdout=True)
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

    @staticmethod
    def finalTearDown(self):
        global node, nodeFirewall
        # no need to uninstall reports
        # if node != None:
        # uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node.setSettings(syslogSettings)
        node = None
        if nodeFirewall != None:
            uvmContext.nodeManager().destroy( nodeFirewall.getNodeSettings()["id"] )
        nodeFirewall = None

test_registry.registerNode("reporting", ReportTests)
