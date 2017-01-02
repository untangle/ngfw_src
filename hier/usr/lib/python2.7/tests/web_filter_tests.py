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

def addBlockedUrl(node, url, blocked=True, flagged=True, description="description"):
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
    
#
# Just extends the web filter tests
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

    # verify client is online
    def test_510_clientIsOnlineHttps(self):
        global remote_control
        result = remote_control.runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 --no-check-certificate -o /dev/null https://test.untangle.com/")
        assert (result == 0)

    # check for block page with HTTPS request
    def test_530_httpsPornIsBlocked(self):
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://penthouse.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)

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

    # Check SNI block list handling
    def test_550_httpsBlockListWithSNI(self):
        addBlockedUrl(self.node, "playboy.com")
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://www.playboy.com/ 2>&1 | grep -q blockpage")
        nukeBlockedUrls(self.node)
        assert (result == 0)

    # verify that a block list glob * doesnt overmatch
    def test_560_blockedUrlGlobStarWithSNI(self):
        addBlockedUrl(self.node, "*st.untangle.com")
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls(self.node)
        assert (result == 0)

    # verify that a block list glob ? matches a single character
    def test_561_blockedUrlGlobQuestionMarkWithSNI(self):
        addBlockedUrl(self.node, "t?st.untangle.com")
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls(self.node)
        assert (result == 0)

    # verify that untangle.com block rule also blocks test.untangle.com
    def test_562_blockedUrlSubdomainWithSNI(self):
        addBlockedUrl(self.node,"untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://test.untangle.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls(self.node)
        assert (result == 0)

     # verify that t.untangle.com block rule DOES NOT block test.untangle.com ( it should block foo.t.untangle.com though )
    def test_563_blockedUrlSubdomain2WithSNI(self):
        addBlockedUrl(self.node,"t.untangle.com")
        # this test URL should NOT be blocked
        result = remote_control.runCommand("wget -q -4 -t 2 --timeout=8 --no-check-certificate -O - https://test.untangle.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls(self.node)
        assert (result == 0)

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

test_registry.registerNode("web-filter", WebFilterTests)
