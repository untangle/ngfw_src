import unittest2
import time
import sys
import re
import datetime

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
from tests.web_filter_base_tests import WebFilterBaseTests
import remote_control
import test_registry
import global_functions

defaultRackId = 1
node = None

def nukeBlockedUrls(node):
    rules = node.getBlockedUrls()
    rules["list"] = []
    node.setBlockedUrls(rules)

def addBlockedUrl(node, url, blocked=True, flagged=True, description="description"):
    node_name = node.getAppName()
    if ("monitor" in node_name):
        newRule = { "blocked": False, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    else:
        newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getBlockedUrls()
    rules["list"].append(newRule)
    node.setBlockedUrls(rules)

def addPassedUrl(node, url, enabled=True, description="description"):
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getPassedUrls()
    rules["list"].append(newRule)
    node.setPassedUrls(rules)

def nukePassedUrls(node):
    rules = node.getPassedUrls()
    rules["list"] = []
    node.setPassedUrls(rules)
    
#
# Just extends the web filter base tests
#
class WebFilterTests(WebFilterBaseTests):

    @staticmethod
    def nodeName():
        return "untangle-node-web-filter"

    @staticmethod
    def shortNodeName():
        return "web-filter"

    @staticmethod
    def eventNodeName():
        return "web_filter"

    @staticmethod
    def displayName():
        return "Web Filter"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        nodemetrics = uvmContext.metricManager().getMetrics(node.getNodeSettings()["id"])
        self.node = node

    # verify blocked site url list works
    def test_016_blockedUrl(self):
        pre_events_scan = global_functions.getStatusValue(node, "scan")
        pre_events_block = global_functions.getStatusValue(node, "block")

        addBlockedUrl(self.node,"test.untangle.com/test/testPage1.html")
        # this test URL should now be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1", stdout=True)
        nukeBlockedUrls(self.node)
        assert ( "blockpage" in result )

        # Check to see if the faceplate counters have incremented.
        post_events_scan = global_functions.getStatusValue(node, "scan")
        post_events_block = global_functions.getStatusValue(node, "block")
        assert(pre_events_scan < post_events_scan)
        assert(pre_events_block < post_events_block)

    def test_100_eventlog_blockedUrl(self):
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls(self.node);
        addBlockedUrl(self.node, "test.untangle.com/test/testPage1.html", blocked=True, flagged=True)
        # specify an argument so it isn't confused with other events
        eventTime = datetime.datetime.now()
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        time.sleep(1);

        events = global_functions.get_events(self.displayName(),'Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", ("/test/testPage1.html?arg=%s" % fname),
                                            self.eventNodeName() + '_blocked', True,
                                            self.eventNodeName() + '_flagged', True )
        assert( found )


    # verify that a block page is shown but unblock button option is available.
    def test_120_unblockOption(self):
        global node
        addBlockedUrl(self.node, "test.untangle.com/test/testPage1.html")
        settings = node.getSettings()
        settings["unblockMode"] = "Host"
        node.setSettings(settings)
        # this test URL should be blocked but allow
        remote_control.runCommand("rm -f /tmp/web_filter_base_test_120.log")
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/web_filter_base_test_120.log -O /tmp/web_filter_base_test_120.out http://test.untangle.com/test/testPage1.html")
        resultButton = remote_control.runCommand("grep -q 'unblock' /tmp/web_filter_base_test_120.out")
        resultBlock = remote_control.runCommand("grep -q 'blockpage' /tmp/web_filter_base_test_120.out")

        # get the IP address of the block page
        ipfind = remote_control.runCommand("grep 'Location' /tmp/web_filter_base_test_120.log", stdout=True)
        # print 'ipFind %s' % ipfind
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        blockPageIP = ip[0]
        # print 'Block page IP address is %s' % blockPageIP
        blockParamaters = re.findall( r'\?(.*)\s', ipfind )
        paramaters = blockParamaters[0]
        # Use unblock button.
        unBlockParameters = "global=false&"+ paramaters + "&password="
        # print "unBlockParameters %s" % unBlockParameters
        print "wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortNodeName() + "/unblock"
        remote_control.runCommand("wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortNodeName() + "/unblock")
        resultUnBlock = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")

        nukeBlockedUrls(self.node)
        node.flushAllUnblockedSites()

        print "block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock)
        assert (resultBlock == 0)
        assert (resultButton == 0)
        assert (resultUnBlock == 0)

    # disable pass referer and verify that a page with content that would be blocked is blocked.
    def test_130_passrefererDisabled(self):
        global node
        addBlockedUrl(self.node, "test.untangle.com/test/refererPage.html")
        addPassedUrl(self.node, "test.untangle.com/test/testPage1.html")
        settings = node.getSettings()
        settings["passReferers"] = False
        node.setSettings(settings)
        resultReferer = remote_control.runCommand("wget -q --header 'Referer: http://test.untangle.com/test/testPage1.html' -O - http://test.untangle.com/test/refererPage.html 2>&1 | grep -q 'Welcome to the referer page.'");
        print "result %s passReferers %s" % (resultReferer,settings["passReferers"])

        nukeBlockedUrls(self.node)
        nukePassedUrls(self.node)
        assert( resultReferer == 1 )

    # disable pass referer and verify that a page with content that would be blocked is allowed.
    def test_131_passrefererEnabled(self):
        global node
        addBlockedUrl(self.node, "test.untangle.com/test/refererPage.html")
        addPassedUrl(self.node, "test.untangle.com/test/testPage1.html")
        settings = node.getSettings()
        settings["passReferers"] = True
        node.setSettings(settings)
        resultReferer = remote_control.runCommand("wget -q --header 'Referer: http://test.untangle.com/test/testPage1.html' -O - http://test.untangle.com/test/refererPage.html 2>&1 | grep -q 'Welcome to the referer page.'");
        print "result %s passReferers %s" % (resultReferer,settings["passReferers"])

        nukeBlockedUrls(self.node)
        nukePassedUrls(self.node)
        assert( resultReferer == 0 )

    # Check google safe search
    def test_540_safeSearchEnabled(self):
        settings = self.node.getSettings()
        settings["enforceSafeSearch"] = False
        self.node.setSettings(settings)
        resultWithoutSafe = remote_control.runCommand("wget -q -O - '$@' -U 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)' 'http://www.google.com/search?hl=en&q=boobs&safe=off' | grep -q 'safe=off'");

        settings["enforceSafeSearch"] = True
        self.node.setSettings(settings)
        resultWithSafe = remote_control.runCommand("wget -q -O - '$@' -U 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040613 Firefox/0.8.0+)' 'http://www.google.com/search?hl=en&q=boobs&safe=off' | grep -q 'safe=active'");

        assert( resultWithoutSafe == 0 )
        assert( resultWithSafe == 0 )

    # verify that a block page is shown but unblock if correct password.
    def test_590_unblockOptionWithPassword(self):
        fname = sys._getframe().f_code.co_name
        addBlockedUrl(self.node, "test.untangle.com/test/testPage2.html")
        settings = self.node.getSettings()
        settings["unblockMode"] = "Host"
        settings["unblockPassword"] = "atstest"
        settings["unblockPasswordEnabled"] = True
        self.node.setSettings(settings)

        # this test URL should be blocked but allow
        remote_control.runCommand("rm -f /tmp/%s.log"%fname)
        result = remote_control.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/%s.log -O /tmp/%s.out http://test.untangle.com/test/testPage2.html"%(fname,fname))
        resultButton = remote_control.runCommand("grep -q 'unblock' /tmp/%s.out"%fname)
        resultBlock = remote_control.runCommand("grep -q 'blockpage' /tmp/%s.out"%fname)

        # get the IP address of the block page
        ipfind = remote_control.runCommand("grep 'Location' /tmp/%s.log"%fname,stdout=True)
        print 'ipFind %s' % ipfind
        ip = re.findall( r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}(?:[0-9:]{0,6})', ipfind )
        blockPageIP = ip[0]
        # print 'Block page IP address is %s' % blockPageIP
        blockParamaters = re.findall( r'\?(.*)\s', ipfind )
        paramaters = blockParamaters[0]
        # Use unblock button.
        unBlockParameters = "global=false&"+ paramaters + "&password=atstest"
        # print "unBlockParameters %s" % unBlockParameters
        remote_control.runCommand("wget -q -O /dev/null --post-data=\'" + unBlockParameters + "\' http://" + blockPageIP + "/" + self.shortNodeName() + "/unblock")
        resultUnBlock = remote_control.runCommand("wget -O - http://test.untangle.com/test/testPage2.html 2>&1 | grep -q text123")

        settings = self.node.getSettings()
        settings["unblockMode"] = "None"
        settings["unblockPassword"] = ""
        settings["unblockPasswordEnabled"] = False

        self.node.setSettings(settings)
        nukeBlockedUrls(self.node)
        print "block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock)
        assert (resultBlock == 0 and resultButton == 0 and resultUnBlock == 0 )

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("web-filter", WebFilterTests)
