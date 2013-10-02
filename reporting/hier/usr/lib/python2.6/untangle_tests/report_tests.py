import unittest2
import time
import sys
import pdb
import socket
import subprocess
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests import TestDict

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
node = None

# pdb.set_trace()

def setReportSettings():
    return {
        "attachmentSizeLimit": 10, 
        "dbHost": "localhost", 
        "dbName": "uvm", 
        "dbPassword": "foo", 
        "dbPort": 5432, 
        "dbRetention": 7, 
        "dbUser": "postgres", 
        "emailDetail": False, 
        "fileRetention": 90, 
        "generateDailyReports": "any", 
        "generateMonthlyReports": False, 
        "generateWeeklyReports": "sunday", 
        "generationHour": 2, 
        "generationMinute": 0, 
        "hostnameMap": {
            "javaClass": "java.util.LinkedList", 
            "list": []
        }, 
        "javaClass": "com.untangle.node.reporting.ReportingSettings", 
        "reportingUsers": {
            "javaClass": "java.util.LinkedList", 
            "list": []
        }, 
        "syslogEnabled": false, 
        "syslogPort": 514, 
        "syslogProtocol": "UDP"
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
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "Node %s already installed" % self.nodeName()
                # report node is normally installed.
                # raise Exception('node %s already instantiated' % self.nodeName())
                node = uvmContext.nodeManager().node(self.nodeName())
            else:
                node = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), defaultRackId)
           
    # verify client is online
    def test_010_clientIsOnline(self):
        ClientControl.verbosity = 1
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        assert (result == 0)
    
    def test_020_sendReportOut(self):
        settings = node.getSettings()
        settings["attachmentSizeLimit"] = 5
        node.setSettings(settings)
        # TODO set email address, send email, check attachment size

    def test_999_finalTearDown(self):
        global node
        # no need to uninstall reports
        # uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None

TestDict.registerNode("reporting", ReportTests)
