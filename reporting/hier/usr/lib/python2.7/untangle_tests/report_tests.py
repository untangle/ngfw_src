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
        };

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
        settings = node.getSettings()
        settings["attachmentSizeLimit"] = 5
        node.setSettings(settings)
        # TODO set email address, send email, check attachment size

    def test_040_remoteSyslog(self):
        if (syslogHostResult != 0):
            raise unittest2.SkipTest("No syslog server available")        
        # Install firewall rule to generate syslog events
        rules = nodeFirewall.getRules()
        rules["list"].append(createFirewallSingleMatcherRule("SRC_ADDR",remote_control.clientIP));
        nodeFirewall.setRules(rules);
        rules = nodeFirewall.getRules()
        # Get rule ID
        for rule in rules['list']:
            if rule['enabled'] and rule['block']:
                targetID = rule['ruleId']
                break
        # Setup syslog to send events to syslog host
        newSyslogSettings = copy.deepcopy(syslogSettings)
        newSyslogSettings["syslogEnabled"] = True
        newSyslogSettings["syslogEnabled"] = True
        newSyslogSettings["syslogHost"] = syslogHostIP
        node.setSettings(newSyslogSettings)
        currentTime = time.strftime("%X")
        result = remote_control.isOnline()
        nowtime = datetime.now()
        currentTime =  nowtime.strftime('%b %d %H:%M')
        print "currentTime " + currentTime
        rsyslogResult = remote_control.runCommand("sudo tail -8 /var/log/localhost/localhost.log", host=syslogHostIP, stdout=True)
        resultList = rsyslogResult.splitlines()
        # remove the firewall rule aet syslog back to original settings
        node.setSettings(syslogSettings)
        rules["list"]=[];
        nodeFirewall.setRules(rules);
        
        # Get the date and time from the last record 
        matchedClientIP = False
        matchedBlockedStatus = False
        matchedFirewallRule = False
        match = re.search(r'(\w{3}\s\d\d\s\d\d:\d\d:\d\d).*', resultList[-1])
        if (match == None):
            print "Rsyslog did not have any date match"
        else:
            # print match.group(1)
            # Check to see if the logs are in this test's timeframe. 
            if (currentTime in match.group(1)):
                for line in reversed(resultList):
                    if str(match.group(1)) in line:
                        if remote_control.clientIP in line:
                            matchedClientIP = True
                        if '\"blocked\":true' in line:
                            matchedBlockedStatus = True
                        if str(targetID) in line:
                            matchedFirewallRule = True
         
        assert(matchedClientIP)
        assert(matchedBlockedStatus)
        assert(matchedFirewallRule)

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
