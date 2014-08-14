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
import remote_control
import test_registry

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
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
                node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
           
    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)
    
    def test_020_sendReportOut(self):
        settings = node.getSettings()
        settings["attachmentSizeLimit"] = 5
        node.setSettings(settings)
        # TODO set email address, send email, check attachment size

    @staticmethod
    def finalTearDown(self):
        global node
        # no need to uninstall reports
        # if node != None:
        # uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None

test_registry.registerNode("reporting", ReportTests)
