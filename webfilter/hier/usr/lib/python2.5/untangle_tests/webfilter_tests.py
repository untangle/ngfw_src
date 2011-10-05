import unittest
import time
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
clientControl = ClientControl()
nodeDesc = None
node = None

def addBlockedUrl(url, blocked=True, flagged=True, description="description"):
    global node
    newUrl =  { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    blockedUrls = node.getBlockedUrls()
    blockedUrls["list"].append(newUrl)
    node.setBlockedUrls(blockedUrls)


def nukeBlockedUrlList():
    global node
    blockedUrls = node.getBlockedUrls()
    blockedUrls["list"] = []
    node.setBlockedUrls(blockedUrls)

def addPassedUrl(url, enabled=True, description="description"):
    global node
    newUrl =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    passedUrls = node.getPassedUrls()
    passedUrls["list"].append(newUrl)
    node.setPassedUrls(passedUrls)

def nukePassedUrlList():
    global node
    passedUrls = node.getPassedUrls()
    passedUrls["list"] = []
    node.setPassedUrls(passedUrls)


class WebFilterTests(unittest.TestCase):


    def setUp(self):
        global nodeDesc, node
        if nodeDesc == None:
            if (uvmContext.nodeManager().isInstantiated("untangle-node-webfilter")):
                raise Exception('node webfilter already instantiated')
            nodeDesc = uvmContext.nodeManager().instantiateAndStart("untangle-node-webfilter", uvmContext.policyManager().getDefaultPolicy())
            node = uvmContext.nodeManager().nodeContext(nodeDesc['nodeId']).node()

    # verify client is online
    def test_10_clientIsOnline(self):
        result = clientControl.runCommand("wget -o /dev/null http://google.com/")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_11_defaultPornIsBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify metaloft site is not blocked in default config
    def test_12_defaultMetaloftIsNotBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        assert (result == 0)

    # verify blocked site url list works
    def test_13_blockedUrl(self):
        addBlockedUrl("metaloft.com/test/testPage1.html")
        # this test URL should now be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list entry does not match when the URI doesnt match exactly
    def test_14_blockedUrl(self):
        addBlockedUrl("metaloft.com/test/testPage1.html")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage2.html 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list entry correctly appends "(/.*)?" to the rigth side anchor
    def test_14_blockedUrlRightSideAnchor(self):
        addBlockedUrl("metaloft.com/test")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list entry does not match when the URI capitalization is different
    def test_15_blockedUrlCapitalization(self):
        addBlockedUrl("metaloft.com/test/testPage1.html")
        # this test URL should NOT be blocked (capitalization is different)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testpage1.html 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list glob functions with * at the end
    def test_20_blockedUrlGlobStar(self):
        addBlockedUrl("metaloft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list glob functions with * at the end and at the beginning
    def test_21_blockedUrlGlobStar(self):
        addBlockedUrl("*loft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list glob functions with * at the end and at the beginning and in the middle
    def test_22_blockedUrlGlobStar(self):
        addBlockedUrl("*et*loft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list glob matches the whole URL
    def test_23_blockedUrlGlobStar(self):
        addBlockedUrl("metaloft.com*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list glob * matches zero characters
    def test_24_blockedUrlGlobStar(self):
        addBlockedUrl("meta*loft.com*")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list glob * doesnt overmatch
    def test_25_blockedUrlGlobStar(self):
        addBlockedUrl("metaloft.com/test/testP*.html")
        # this test URL should NOT be blocked (uri is different)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list glob ? matches a single character
    def test_26_blockedUrlGlobQuestionMark(self):
        addBlockedUrl("metalo?t.com/test/testP?ge1.html")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a block list glob ? matches ONLY single character (but not two or more)
    def test_27_blockedUrlGlobQuestionMark(self):
        addBlockedUrl("metalo?t.com/test/testP?.html")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a the action in taken from the first rule
    def test_28_blockedUrlRuleOrder(self):
        addBlockedUrl("metaloft.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedUrl("metaloft.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that a the action in taken from the second rule (first rule doesn't match)
    def test_29_blockedUrlRuleOrder(self):
        addBlockedUrl("metaloft.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedUrl("metaloft.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage2.html 2>&1 | grep -q blockpage")
        nukeBlockedUrlList()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_40_passedUrlOverridesBlockedCategory(self):
        addPassedUrl("playboy.com")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result = clientControl.runCommand("wget -q -O - http://playboy.com/ 2>&1 | grep -q 'Nude Girls'")
        nukePassedUrlList()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_41_passedUrlOverridesBlockedUrl(self):
        addBlockedUrl("metaloft.com")
        addPassedUrl("metaloft.com/test/")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        nukePassedUrlList()
        assert (result == 0)

    def test_999_finalTearDown(self):
        global nodeDesc
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        








