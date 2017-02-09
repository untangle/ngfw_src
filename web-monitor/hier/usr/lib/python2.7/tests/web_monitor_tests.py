import unittest2
import time
import sys
import datetime
import string
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
from tests.web_filter_base_tests import WebFilterBaseTests
import remote_control
import test_registry
import global_functions
import pprint

defaultRackId = 1
node = None

def addFlaggedUrl(node, url, blocked=False, flagged=True, description="description"):
    newRule = { "blocked": False, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getBlockedUrls()
    rules["list"].append(newRule)
    node.setBlockedUrls(rules)

def nukeFlaggedUrls(node):
    rules = node.getBlockedUrls()
    rules["list"] = []
    node.setBlockedUrls(rules)

class WebMonitorTests(WebFilterBaseTests):

    @staticmethod
    def nodeName():
        return "untangle-node-web-monitor"

    @staticmethod
    def shortNodeName():
        return "web-monitor"

    @staticmethod
    def eventNodeName():
        return "web_monitor"

    @staticmethod
    def displayName():
        return "Web Monitor"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        nodemetrics = uvmContext.metricManager().getMetrics(node.getNodeSettings()["id"])
        self.node = node

    # verify flagged site url list works
    def test_026_flaggedUrl(self):
        # raise unittest2.SkipTest("Flag stats not yet enabled")
        # pre_events_scan = global_functions.getStatusValue(node, "scan")
        # pre_events_flagged = global_functions.getStatusValue(node, "flagged")
        addFlaggedUrl(self.node, url="test.untangle.com/test/testPage1.html")
        # this test URL should now be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1", stdout=True)
        nukeFlaggedUrls(self.node)
        assert ( "blockpage" not in result )

        # Check to see if the faceplate counters have incremented.
        # post_events_scan = global_functions.getStatusValue(node, "scan")
        # post_events_flagged = global_functions.getStatusValue(node, "flagged")
        # print "Pre flagged: " + str(pre_events_flagged) + " Post flagged: " + str(post_events_flagged)
        # assert(pre_events_scan < post_events_scan)
        # assert(pre_events_flagged < post_events_flagged)
        events = global_functions.get_events("Web Monitor", "All Web Events", None, 1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", "/test/testPage1.html",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', True )
        assert( found )


    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("web-monitor", WebMonitorTests)
