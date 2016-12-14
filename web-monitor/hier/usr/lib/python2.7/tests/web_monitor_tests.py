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
import remote_control
import test_registry
import global_functions
import pprint

defaultRackId = 1
node = None

def addPassedUrl(url, enabled=True, description="description"):
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getPassedUrls()
    rules["list"].append(newRule)
    node.setPassedUrls(rules)

def nukePassedUrls():
    rules = node.getPassedUrls()
    rules["list"] = []
    node.setPassedUrls(rules)

def addBlockedUrl(url, blocked=False, flagged=True, description="description"):
    newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getBlockedUrls()
    rules["list"].append(newRule)
    node.setBlockedUrls(rules)

def nukeBlockedUrls():
    rules = node.getBlockedUrls()
    rules["list"] = []
    node.setBlockedUrls(rules)

class WebMonitorTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-web-monitor"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        nodemetrics = uvmContext.metricManager().getMetrics(node.getNodeSettings()["id"])
        self.node = node

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_020_eventlog_flaggedPorn(self):
        result = remote_control.runCommand("wget -q -O - http://www.playboy.com/bunnies 2>&1 >/dev/null")
        time.sleep(1);
        events = global_functions.get_events("Web Monitor", "Flagged Web Events", None, 1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","www.playboy.com",
                                            "uri", "/bunnies",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', True )
        assert( found )

    # verify porn site is not shown block page in default config
    def test_021_defaultPornIsFlagged(self):
        result = remote_control.runCommand("wget -q -O - http://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result == 1)
        time.sleep(1);
        events = global_functions.get_events("Web Monitor", "Flagged Web Events", None, 1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","www.playboy.com",
                                            "uri", "/",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', True )
        assert( found )

    # verify test site is not flagged in default config
    def test_022_defaultTestSiteIsNotFlagged(self):
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html", stdout=True)
        assert ( "text123" in result )
        time.sleep(1);
        events = global_functions.get_events("Web Monitor", "All Web Events", None, 1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", "/test/testPage1.html",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', False )
        assert( found )

    # verify flagged site url list works
    def test_023_flaggedUrl(self):
        # raise unittest2.SkipTest("Flag stats not yet enabled")
        # pre_events_scan = global_functions.getStatusValue(node, "scan")
        # pre_events_flagged = global_functions.getStatusValue(node, "flagged")

        addBlockedUrl("test.untangle.com/test/testPage1.html")
        # this test URL should now be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1", stdout=True)
        nukeBlockedUrls()
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

    # verify that a flagged list entry does not match when the URI doesnt match exactly
    def test_024_flaggedUrl2(self):
        addBlockedUrl("test.untangle.com/test/testPage1.html")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage2.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a flagged list entry correctly appends "(/.*)?" to the rigth side anchor
    def test_025_flaggedUrlRightSideAnchor(self):
        addBlockedUrl("test.untangle.com/test([\\\?/]\.\*)\?$")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result0 = remote_control.runCommand("wget -q -O - http://test.untangle.com/testPage1.html 2>&1 | grep -q text123")
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/ 2>&1", stdout=True)
        nukeBlockedUrls()
        assert (result0 == 0)
        assert ( "blockpage" not in result1 )

    # verify that a flagged list entry does not match when the URI capitalization is different
    def test_026_flaggedUrlCapitalization(self):
        addBlockedUrl("test.untangle.com/test/testPage1.html")
        # this test URL should NOT be blocked (capitalization is different)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testpage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)
        
    # verify that an entry in the pass list overrides a flagged category
    def test_030_passedUrlOverridesFlaggedCategory(self):
        addPassedUrl("playboy.com")
        result = remote_control.runCommand("wget -q -O - http://www.playboy.com/ 2>&1 >/dev/null")
        time.sleep(1);
        events = global_functions.get_events("Web Monitor", "All Web Events", None, 1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","www.playboy.com",
                                            "uri", "/",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', False )
        assert( found )
        nukePassedUrls()
        assert (result == 0)

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("web-monitor", WebMonitorTests)
