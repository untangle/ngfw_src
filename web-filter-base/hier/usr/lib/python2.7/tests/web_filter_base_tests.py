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
import global_functions

def addBlockedUrl(node, url, blocked=True, flagged=True, description="description"):
    node_name = node.getAppName()
    if ("monitor" in node_name):
        newRule = { "blocked": False, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    else:
        newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getBlockedUrls()
    rules["list"].append(newRule)
    node.setBlockedUrls(rules)

def nukeBlockedUrls(node):
    rules = node.getBlockedUrls()
    rules["list"] = []
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

def addWebFilterRule(node, conditionType, conditionData, blocked=True, flagged=True, description="description"):
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

def nukeWebFilterRules(node):
    rules = node.getFilterRules()
    rules["list"] = []
    node.setFilterRules(rules)

def getWebSiteResults(node, url="http://test.untangle.com", expected=None):
    extra_opts = ""
    node_name = node.getAppName()
    if ("https" in url):
        extra_opts = "--no-check-certificate "
    if ((expected == None) or (("monitor" in node_name) and (expected == "blockpage"))):
        result = remote_control.runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 " + extra_opts +  url)
    else:
        result = remote_control.runCommand("wget -q -O - " + extra_opts + url + " 2>&1 | grep -q " + expected)
    return result
    
def checkEvents(node, host="", url="", isBlocked=True):
    node_display_name = node.getNodeTitle()
    isFlagged = isBlocked
    if (("Monitor" in node_display_name) and isBlocked):
        isBlocked = False
    if (isBlocked):
        event_list = "Blocked Web Events"
    elif (isFlagged):
        event_list = "Flagged Web Events"
    else:
        event_list = "All Web Events"
#    event_list = "All Web Events"
    time.sleep(1);
    events = global_functions.get_events(node_display_name, event_list, None, 5)
    assert(events != None)
    found = global_functions.check_events( events.get('list'), 5,
                                        "host", host,
                                        "uri", url,
                                        'web_filter_blocked', isBlocked,
                                        'web_filter_flagged', isFlagged )
    return found

class WebFilterBaseTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-base-web-filter"

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

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_011_defaultPornIsBlocked(self):
        result = getWebSiteResults(self.node, url="http://playboy.com/", expected="blockpage")
        assert (result == 0)
        found = checkEvents(self.node, "playboy.com", "/", True)
        assert( found )

    # verify porn site is blocked in default config
    def test_012_defaultPornIsBlockedWithSubdomain(self):
        result = getWebSiteResults(self.node, url="http://www.playboy.com/", expected="blockpage")
        assert (result == 0)
        found = checkEvents(self.node, "www.playboy.com", "/", True)
        assert( found )

    # verify porn site is blocked in default config
    def test_013_defaultPornIsBlockedWithUrl(self):
        result = getWebSiteResults(self.node, url="http://www.playboy.com/", expected="blockpage")
        assert (result == 0)
        found = checkEvents(self.node, "www.playboy.com", "/", True)
        assert( found )

    # verify porn site is blocked in default config
    def test_014_defaultPornIsBlockedWithUrlAndSubdomain(self):
        result = getWebSiteResults(self.node, url="http://www.playboy.com/bunnies.html", expected="blockpage")
        assert (result == 0)
        found = checkEvents(self.node, "www.playboy.com", "/bunnies.html", True)
        assert( found )

    # verify test site is not blocked in default config
    def test_015_defaultTestSiteIsNotBlocked(self):
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="text123")
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    # verify that a block list entry does not match when the URI doesnt match exactly
    def test_017_blockedUrl2(self):
        addBlockedUrl(self.node, "test.untangle.com/test/testPage1.html")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    # verify that a block list entry correctly appends "(/.*)?" to the rigth side anchor
    def test_018_blockedUrlRightSideAnchor(self):
        addBlockedUrl(self.node, "test.untangle.com/test([\\\?/]\.\*)\?$")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result0 = getWebSiteResults(self.node, url="http://test.untangle.com/testPage1.html", expected="text123")
        result1 = getWebSiteResults(self.node, url="http://test.untangle.com/test/", expected="blockpage")
        nukeBlockedUrls(self.node)
        assert (result0 == 0)
        assert (result1 == 0)
        found0 = checkEvents(self.node, "test.untangle.com", "/testPage1.html", False)
        found1 = checkEvents(self.node, "test.untangle.com", "/test/", True)
        assert( found0 )
        assert( found1 )

    # verify that a block list entry does not match when the URI capitalization is different
    def test_019_blockedUrlCapitalization(self):
        addBlockedUrl(self.node, "test.untangle.com/test/testPage1.html")
        # this test URL should NOT be blocked (capitalization is different)
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testpage1.html", expected="text123")
        assert (result == 0)
        nukeBlockedUrls(self.node)

    # verify that a block list glob functions with * at the end
    def test_030_blockedUrlGlobStar(self):
        addBlockedUrl(self.node, "test.untangle.com/test/test*")
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    # verify that a block list glob functions with * at the end and at the beginning
    def test_031_blockedUrlGlobStar(self):
        addBlockedUrl(self.node, "tes*tangle.com/test/test*")
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    # verify that a block list glob functions with * at the end and at the beginning and in the middle
    def test_032_blockedUrlGlobStar(self):
        addBlockedUrl(self.node, "*est*angle.com/test/test*")
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    # verify that a block list glob matches the whole URL
    def test_033_blockedUrlGlobStar(self):
        addBlockedUrl(self.node, "test.untangle.com*")
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    # verify that a block list glob * matches zero characters
    def test_034_blockedUrlGlobStar(self):
        addBlockedUrl(self.node, "te*st.untangle.com*")
        # this test URL should NOT be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    # verify that a block list glob * doesnt overmatch
    def test_035_blockedUrlGlobStar(self):
        addBlockedUrl(self.node, "test.untangle.com/test/testP*.html")
        # this test URL should NOT be blocked (uri is different)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/test.html", False)
        assert( found )

    # verify that a block list glob ? matches a single character
    def test_036_blockedUrlGlobQuestionMark(self):
        addBlockedUrl(self.node, "te?t.untangle.com/test/testP?ge1.html")
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    # verify that a block list glob ? matches ONLY single character (but not two or more)
    def test_037_blockedUrlGlobQuestionMark(self):
        addBlockedUrl(self.node, "metalo?t.com/test/testP?.html")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    # verify that the full URI is included in the match (even things after argument) bug #10067
    def test_038_blockedUrlGlobArgument(self):
        addBlockedUrl(self.node, "test.untangle.com/*foo*")
        # this test URL should NOT be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html?arg=foobar", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html?arg=foobar", True)
        assert( found )

    # verify that untangle.com block rule also blocks test.untangle.com
    def test_039_blockedUrlSubdomain(self):
        addBlockedUrl(self.node, "untangle.com")
        # this test URL should NOT be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", True)
        assert( found )

    # verify that t.untangle.com block rule DOES NOT block test.untangle.com ( it should block foo.t.untangle.com though )
    def test_040_blockedUrlSubdomain2(self):
        addBlockedUrl(self.node, "t.untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    # verify that a the action in taken from the first rule
    def test_045_blockedUrlRuleOrder(self):
        addBlockedUrl(self.node, "test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedUrl(self.node, "test.untangle.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    # verify that a the action in taken from the second rule (first rule doesn't match)
    def test_046_blockedUrlRuleOrder(self):
        addBlockedUrl(self.node, "test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedUrl(self.node, "test.untangle.com", blocked=True, flagged=True)
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/testPage2.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage2.html", True)
        assert( found )

    # verify that an entry in the pass list overrides a blocked category
    def test_050_passedUrlOverridesBlockedCategory(self):
        addPassedUrl(self.node, "playboy.com")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result1 = getWebSiteResults(self.node, url="http://playboy.com/")
        result2 = getWebSiteResults(self.node, url="http://www.playboy.com/")
        nukeBlockedUrls(self.node)
        assert (result1 == 0)
        assert (result2 == 0)
        found1 = checkEvents(self.node, "playboy.com", "/", False)
        found2 = checkEvents(self.node, "www.playboy.com", "/", False)
        assert( found1 )
        assert( found2 )

    # verify that an entry in the pass list overrides a blocked category
    def test_051_passedUrlOverridesBlockedUrl(self):
        addBlockedUrl(self.node, "test.untangle.com")
        addPassedUrl(self.node, "test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        nukePassedUrls(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/testPage1.html", False)
        assert( found )

    # verify that an entry in the pass list overrides a blocked category
    def test_052_passedUrlOverridesBlockedMimeType(self):
        nukeWebFilterRules(self.node)
        addWebFilterRule(self.node, "WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        addPassedUrl(self.node, "test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        nukePassedUrls(self.node)
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_053_passedUrlOverridesBlockedExtension(self):
        nukeWebFilterRules(self.node)
        addWebFilterRule(self.node, "WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        addPassedUrl(self.node, "test.untangle.com/test/")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.txt 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        nukePassedUrls(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/test.txt", False)
        assert( found )

    # verify that an entry in the mime type block list functions
    def test_060_blockedMimeType(self):
        nukeWebFilterRules(self.node)
        addWebFilterRule(self.node, "WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/test.txt", expected="blockpage")
        nukeWebFilterRules(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/test.txt", True)
        assert( found )

    # verify that an entry in the mime type block list doesn't overmatch
    def test_061_blockedMimeType(self):
        nukeWebFilterRules(self.node)
        addWebFilterRule(self.node, "WEB_FILTER_RESPONSE_CONTENT_TYPE","text/plain")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        nukeWebFilterRules(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/test.html", False)
        assert( found )

    # verify that an entry in the file extension block list functions
    def test_070_blockedExtension(self):
        nukeWebFilterRules(self.node)
        addWebFilterRule(self.node, "WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/test.txt", expected="blockpage")
        nukeWebFilterRules(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/test.txt", True)
        assert( found )

    # verify that an entry in the file extension block list doesn't overmatch
    def test_071_blockedExtension(self):
        nukeWebFilterRules(self.node)
        addWebFilterRule(self.node, "WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        nukeWebFilterRules(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/test.html", False)
        assert( found )

    # verify that an entry in the file extension block list doesn't overmatch
    def test_072_blockedExtension(self):
        nukeWebFilterRules(self.node)
        addWebFilterRule(self.node, "WEB_FILTER_REQUEST_FILE_EXTENSION","tml") # not this should only block ".tml" not ".html"
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/test.html 2>&1 | grep -q text123")
        nukeWebFilterRules(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/test.html", False)
        assert( found )

    # verify that an entry in the file extension block list functions
    def test_073_blockedExtensionWithArgument(self):
        nukeWebFilterRules(self.node)
        addWebFilterRule(self.node, "WEB_FILTER_REQUEST_FILE_EXTENSION","txt")
        # this test URL should be blocked
        result = getWebSiteResults(self.node, url="http://test.untangle.com/test/test.txt?argument", expected="blockpage")
        nukeWebFilterRules(self.node)
        assert (result == 0)
        found = checkEvents(self.node, "test.untangle.com", "/test/test.txt?argument", True)
        assert( found )

    def test_101_eventlog_flaggedUrl(self):
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls(self.node);
        addBlockedUrl(self.node, "test.untangle.com/test/testPage1.html", blocked=False, flagged=True)
        # specify an argument so it isn't confused with other events
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        time.sleep(1);

        events = global_functions.get_events(self.displayName(),'Flagged Web Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", ("/test/testPage1.html?arg=%s" % fname),
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', True )
        assert( found )

    def test_102_eventlog_allUrls(self):
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls(self.node);
        # specify an argument so it isn't confused with other events
        result1 = remote_control.runCommand("wget -q -O - http://test.untangle.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        time.sleep(1);

        events = global_functions.get_events(self.displayName(),'All Web Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","test.untangle.com",
                                            "uri", ("/test/testPage1.html?arg=%s" % fname),
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', False )
        assert( found )

    # verify client is online
    def test_510_clientIsOnlineHttps(self):
        global remote_control
        result = remote_control.runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 --no-check-certificate -o /dev/null https://test.untangle.com/")
        assert (result == 0)

    # check for block page with HTTPS request
    def test_530_httpsPornIsBlocked(self):
        result = getWebSiteResults(self.node, url="https://www.playboy.com/", expected="blockpage")
        assert (result == 0)
        found = checkEvents(self.node, "www.playboy.com", "/", True)
        assert( found )

    # Check SNI block list handling
    def test_550_httpsBlockListWithSNI(self):
        addBlockedUrl(self.node, "test.untangle.com")
        result = getWebSiteResults(self.node, url="https://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/", True)
        assert( found )

    # verify that a block list glob * doesnt overmatch
    def test_560_blockedUrlGlobStarWithSNI(self):
        addBlockedUrl(self.node, "*st.untangle.com")
        result = getWebSiteResults(self.node, url="https://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/", True)
        assert( found )

    # verify that a block list glob ? matches a single character
    def test_561_blockedUrlGlobQuestionMarkWithSNI(self):
        addBlockedUrl(self.node, "t?st.untangle.com")
        result = getWebSiteResults(self.node, url="https://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/", True)
        assert( found )

    # verify that untangle.com block rule also blocks test.untangle.com
    def test_562_blockedUrlSubdomainWithSNI(self):
        addBlockedUrl(self.node,"untangle.com")
        # this test URL should NOT be blocked
        result = getWebSiteResults(self.node, url="https://test.untangle.com/test/testPage1.html", expected="blockpage")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/", True)
        assert( found )

     # verify that t.untangle.com block rule DOES NOT block test.untangle.com ( it should block foo.t.untangle.com though )
    def test_563_blockedUrlSubdomain2WithSNI(self):
        addBlockedUrl(self.node,"t.untangle.com")
        # this test URL should NOT be blocked
        result = getWebSiteResults(self.node, url="https://test.untangle.com/test/testPage1.html", expected="text123")
        assert (result == 0)
        nukeBlockedUrls(self.node)
        found = checkEvents(self.node, "test.untangle.com", "/", False)
        assert( found )

     # verify that an entry in the pass list overrides a blocked category
    def test_564_passedUrlOverridesBlockedCategoryWithSNI(self):
        addPassedUrl(self.node,"playboy.com")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result != 0)
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://www.playboy.com/ 2>&1 | grep -q blockpage")
        assert (result != 0)
        nukePassedUrls(self.node)

    def test_565_passedUrlOverridesBlockedUrlWithSNI(self):
        addBlockedUrl(self.node,"untangle.com")
        addPassedUrl(self.node,"test.untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        nukePassedUrls(self.node)
        assert (result == 0)

    # Query eventlog
    def test_600_queryEventLog(self):
        termTests = [{
            "host": "www.bing.com",
            "uri":  "/search?q=oneterm&qs=n&form=QBRE",
            "term": "oneterm"
        },{
            "host": "www.bing.com",
            "uri":  "/search?q=two+terms&qs=n&form=QBRE",
            "term": "two terms"
        },{
            "host": "www.bing.com",
            "uri":  "/search?q=%22quoted+terms%22&qs=n&form=QBRE",
            "term": '"quoted terms"'
        }]
        host = "www.bing.com"
        uri = "/search?q=oneterm&qs=n&form=QBRE"
        for t in termTests:
            fname = sys._getframe().f_code.co_name
            eventTime = datetime.datetime.now()
            result1 = remote_control.runCommand("wget -q -O - 'http://%s%s' 2>&1 >/dev/null" % ( t["host"], t["uri"] ) )
            time.sleep(1);

            events = global_functions.get_events(self.displayName(),'All Query Events',None,1)
            assert(events != None)
            found = global_functions.check_events( events.get('list'), 5,
                                                "host", t["host"],
                                                "term", t["term"])
            assert( found )

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
