import unittest2
import time
import sys
import datetime
import re
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
from tests.web_filter_base_tests import WebFilterBaseTests
import test_registry
import global_functions

defaultRackId = 1
node = None

def addBlockedUrl(url, blocked=True, flagged=True, description="description"):
    newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getBlockedUrls()
    rules["list"].append(newRule)
    node.setBlockedUrls(rules)

def nukeBlockedUrls():
    rules = node.getBlockedUrls()
    rules["list"] = []
    node.setBlockedUrls(rules)

def addPassedUrl(url, enabled=True, description="description"):
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getPassedUrls()
    rules["list"].append(newRule)
    node.setPassedUrls(rules)

def nukePassedUrls():
    rules = node.getPassedUrls()
    rules["list"] = []
    node.setPassedUrls(rules)

def addWebFilterRule(conditionType, conditionData, blocked=True, flagged=True, description="description"):
    newRule =  {
        "blocked": blocked,
        "flagged": flagged,
        "enabled": True,
        "description": description,
        "javaClass": "com.untangle.node.web_filter.WebFilterRule",
            "conditions": {
                "javaClass": "java.util.LinkedList",
                "list": [
                    {
                        "conditionType": conditionType,
                        "invert": False,
                        "javaClass": "com.untangle.node.web_filter.WebFilterRuleCondition",
                        "value": conditionData
                    }
                ]
            }
        }
    rules = node.getFilterRules()
    rules["list"].append(newRule)
    node.setFilterRules(rules)

def nukeWebFilterRules():
    rules = node.getFilterRules()
    rules["list"] = []
    node.setFilterRules(rules)

class WebFilterLiteTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-web-filter-lite"

    @staticmethod
    def shortNodeName():
        return "web-filter-lite"

    @staticmethod
    def eventNodeName():
        return "web_filter_lite"

    @staticmethod
    def displayName():
        return "Web Filter Lite"

    @staticmethod
    def vendorName():
        return "untangle"

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
    def test_011_defaultPornIsBlocked(self):
        result = remote_control.runCommand("wget -q -O - http://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_012_defaultPornIsBlockedWithSubdomain(self):
        result = remote_control.runCommand("wget -q -O - http://www.playboy.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_013_defaultPornIsBlockedWithUrl(self):
        result = remote_control.runCommand("wget -q -O - http://playboy.com/index.html 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_014_defaultPornIsBlockedWithUrlAndSubdomain(self):
        result = remote_control.runCommand("wget -q -O - http://www.playboy.com/index.html 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify test site is not blocked in default config
    def test_015_defaultTestSiteIsNotBlocked(self):
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html", stdout=True)
        assert ( "text123" in result )

    # verify blocked site url list works
    def test_016_blockedUrl(self):
        pre_events_scan = global_functions.getStatusValue(node, "scan")
        pre_events_block = global_functions.getStatusValue(node, "block")

        addBlockedUrl("test.untangle.com/test/testPage1.html")
        # this test URL should now be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1", stdout=True)
        nukeBlockedUrls()
        assert ( "blockpage" in result )

        # Check to see if the faceplate counters have incremented.
        post_events_scan = global_functions.getStatusValue(node, "scan")
        post_events_block = global_functions.getStatusValue(node, "block")
        assert(pre_events_scan < post_events_scan)
        assert(pre_events_block < post_events_block)

    # verify that a block list entry does not match when the URI doesnt match exactly
    def test_017_blockedUrl2(self):
        addBlockedUrl("test.untangle.com/test/testPage1.html")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage2.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list entry correctly appends "(/.*)?" to the rigth side anchor
    def test_018_blockedUrlRightSideAnchor(self):
        addBlockedUrl("test.untangle.com/test([\\\?/]\.\*)\?$")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result0 = remote_control.runCommand("wget -q -O - http://test.untangle.com/testPage1.html 2>&1 | grep -q text123")
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/ 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result0 == 0)
        assert (result1 == 0)

    # verify that a block list entry does not match when the URI capitalization is different
    def test_019_blockedUrlCapitalization(self):
        addBlockedUrl("test.untangle.com/test/testPage1.html")
        # this test URL should NOT be blocked (capitalization is different)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testpage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob functions with * at the end
    def test_030_blockedUrlGlobStar(self):
        addBlockedUrl("test.untangle.com/test/test*")
        # this test URL should be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob functions with * at the end and at the beginning
    def test_031_blockedUrlGlobStar(self):
        addBlockedUrl("tes*tangle.com/test/test*")
        # this test URL should be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob functions with * at the end and at the beginning and in the middle
    def test_032_blockedUrlGlobStar(self):
        addBlockedUrl("*est*angle.com/test/test*")
        # this test URL should be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob matches the whole URL
    def test_033_blockedUrlGlobStar(self):
        addBlockedUrl("test.untangle.com*")
        # this test URL should be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob * matches zero characters
    def test_034_blockedUrlGlobStar(self):
        addBlockedUrl("te*st.untangle.com*")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob * doesnt overmatch
    def test_035_blockedUrlGlobStar(self):
        addBlockedUrl("test.untangle.com/test/testP*.html")
        # this test URL should NOT be blocked (uri is different)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob ? matches a single character
    def test_036_blockedUrlGlobQuestionMark(self):
        addBlockedUrl("te?t.untangle.com/test/testP?ge1.html")
        # this test URL should be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob ? matches ONLY single character (but not two or more)
    def test_037_blockedUrlGlobQuestionMark(self):
        addBlockedUrl("metalo?t.com/test/testP?.html")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that the full URI is included in the match (even things after argument) bug #10067
    def test_038_blockedUrlGlobArgument(self):
        addBlockedUrl("test.untangle.com/*foo*")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=foobar 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that untangle.com block rule also blocks test.untangle.com
    def test_039_blockedUrlSubdomain(self):
        addBlockedUrl("untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that t.untangle.com block rule DOES NOT block test.untangle.com ( it should block foo.t.untangle.com though )
    def test_040_blockedUrlSubdomain2(self):
        addBlockedUrl("t.untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a the action in taken from the first rule
    def test_045_blockedUrlRuleOrder(self):
        addBlockedUrl("test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedUrl("test.untangle.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a the action in taken from the second rule (first rule doesn't match)
    def test_046_blockedUrlRuleOrder(self):
        addBlockedUrl("test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedUrl("test.untangle.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage2.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_050_passedUrlOverridesBlockedCategory(self):
        addPassedUrl("playboy.com")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result = remote_control.runCommand("wget -q -O - http://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result != 0)
        result = remote_control.runCommand("wget -q -O - http://www.playboy.com/ 2>&1 | grep -q blockpage")
        assert (result != 0)
        nukePassedUrls()

    # verify that an entry in the pass list overrides a blocked category
    def test_051_passedUrlOverridesBlockedUrl(self):
        addBlockedUrl("test.untangle.com")
        addPassedUrl("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        nukePassedUrls()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_052_passedUrlOverridesBlockedMimeType(self):
        nukeWebFilterRules()
        addWebFilterRule("WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        addPassedUrl("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q text123")
        nukeBlockedUrls()
        nukePassedUrls()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_053_passedUrlOverridesBlockedExtension(self):
        nukeWebFilterRules()
        addWebFilterRule("WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        addPassedUrl("test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q text123")
        nukeBlockedUrls()
        nukePassedUrls()
        assert (result == 0)

    # verify that an entry in the mime type block list functions
    def test_060_blockedMimeType(self):
        nukeWebFilterRules()
        addWebFilterRule("WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        # this test URL should be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q blockpage")
        nukeWebFilterRules()
        assert (result == 0)

    # verify that an entry in the mime type block list doesn't overmatch
    def test_061_blockedMimeType(self):
        nukeWebFilterRules()
        addWebFilterRule("WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        nukeWebFilterRules()
        assert (result == 0)

    # verify that an entry in the file extension block list functions
    def test_070_blockedExtension(self):
        nukeWebFilterRules()
        addWebFilterRule("WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q blockpage")
        nukeWebFilterRules()
        assert (result == 0)

    # verify that an entry in the file extension block list doesn't overmatch
    def test_071_blockedExtension(self):
        nukeWebFilterRules()
        addWebFilterRule("WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        nukeWebFilterRules()
        assert (result == 0)

    # verify that an entry in the file extension block list doesn't overmatch
    def test_072_blockedExtension(self):
        nukeWebFilterRules()
        addWebFilterRule("WEB_FILTER_REQUEST_FILE_EXTENSION","tml") # not this should only block ".tml" not ".html"
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        nukeWebFilterRules()
        assert (result == 0)

    # verify that an entry in the file extension block list functions
    def test_073_blockedExtensionWithArgument(self):
        nukeWebFilterRules()
        addWebFilterRule("WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt?argument 2>&1 | grep -q blockpage")
        nukeWebFilterRules()
        assert (result == 0)

    def test_100_eventlog_blockedUrl(self):
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls();
        addBlockedUrl("test.untangle.com/test/testPage1.html", blocked=True, flagged=True)
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

    def test_101_eventlog_flaggedUrl(self):
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls();
        addBlockedUrl("test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        # specify an argument so it isn't confused with other events
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        time.sleep(1);

        events = global_functions.get_events(self.displayName(),'Flagged Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", ("/test/testPage1.html?arg=%s" % fname),
                                            self.eventNodeName() + '_blocked', False,
                                            self.eventNodeName() + '_flagged', True )
        assert( found )

    def test_102_eventlog_allUrls(self):
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls();
        # specify an argument so it isn't confused with other events
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        time.sleep(1);

        events = global_functions.get_events(self.displayName(),'All Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", ("/test/testPage1.html?arg=%s" % fname),
                                            self.eventNodeName() + '_blocked', False,
                                            self.eventNodeName() + '_flagged', False )
        assert( found )

    # verify that a block page is shown but unblock button option is available.
    def test_120_unblockOption(self):
        global node
        addBlockedUrl("test.untangle.com/test/testPage1.html")
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

        nukeBlockedUrls()
        node.flushAllUnblockedSites()

        print "block %s button %s unblock %s" % (resultBlock,resultButton,resultUnBlock)
        assert (resultBlock == 0)
        assert (resultButton == 0)
        assert (resultUnBlock == 0)

    # disable pass referer and verify that a page with content that would be blocked is blocked.
    def test_130_passrefererDisabled(self):
        global node
        addBlockedUrl("test.untangle.com/test/refererPage.html")
        addPassedUrl("test.untangle.com/test/testPage1.html")
        settings = node.getSettings()
        settings["passReferers"] = False
        node.setSettings(settings)
        resultReferer = remote_control.runCommand("wget -q --header 'Referer: http://test.untangle.com/test/testPage1.html' -O - http://test.untangle.com/test/refererPage.html 2>&1 | grep -q 'Welcome to the referer page.'");
        print "result %s passReferers %s" % (resultReferer,settings["passReferers"])

        nukeBlockedUrls()
        nukePassedUrls()
        assert( resultReferer == 1 )

    # disable pass referer and verify that a page with content that would be blocked is allowed.
    def test_131_passrefererEnabled(self):
        global node

        addBlockedUrl("test.untangle.com/test/refererPage.html")
        addPassedUrl("test.untangle.com/test/testPage1.html")
        settings = node.getSettings()
        settings["passReferers"] = True
        node.setSettings(settings)
        resultReferer = remote_control.runCommand("wget -q --header 'Referer: http://test.untangle.com/test/testPage1.html' -O - http://test.untangle.com/test/refererPage.html 2>&1 | grep -q 'Welcome to the referer page.'");
        print "result %s passReferers %s" % (resultReferer,settings["passReferers"])

        nukeBlockedUrls()
        nukePassedUrls()
        assert( resultReferer == 0 )

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("web-filter-lite", WebFilterLiteTests)
