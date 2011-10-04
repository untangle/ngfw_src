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

def addBlockedSite(site, blocked=True, flagged=True, description="description"):
    global node
    newUrl =  { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": site }
    blockedUrls = node.getBlockedUrls()
    blockedUrls["list"].append(newUrl)
    node.setBlockedUrls(blockedUrls)

def nukeBlockedSiteList():
    global node
    blockedUrls = node.getBlockedUrls()
    blockedUrls["list"] = []
    node.setBlockedUrls(blockedUrls)


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
        result = clientControl.runCommand("wget http://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify metaloft site is not blocked in default config
    def test_12_defaultMetaloftIsNotBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        assert (result == 0)

    # verify blocked site url list works
    def test_13_blockedSpecificUrl(self):
        addBlockedSite("metaloft.com/test/testPage1.html")
        # this test URL should now be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list entry does not match when the URI doesnt match exactly
    def test_14_blockedSpecificUrl(self):
        addBlockedSite("metaloft.com/test/testPage1.html")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage2.html 2>&1 | grep -q text123")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list entry does not match when the URI capitalization is different
    def test_15_blockedSpecificUrlCapitalization(self):
        addBlockedSite("metaloft.com/test/testPage1.html")
        # this test URL should NOT be blocked (capitalization is different)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testpage1.html 2>&1 | grep -q text123")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list glob functions with * at the end
    def test_16_blockedSpecificUrlGlobStar(self):
        addBlockedSite("metaloft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list glob functions with * at the end and at the beginning
    def test_17_blockedSpecificUrlGlobStar(self):
        addBlockedSite("*loft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list glob functions with * at the end and at the beginning and in the middle
    def test_18_blockedSpecificUrlGlobStar(self):
        addBlockedSite("*et*loft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list glob matches the whole URL
    def test_19_blockedSpecificUrlGlobStar(self):
        addBlockedSite("metaloft.com*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list glob * matches zero characters
    def test_20_blockedSpecificUrlGlobStar(self):
        addBlockedSite("meta*loft.com*")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list glob * doesnt overmatch
    def test_21_blockedSpecificUrlGlobStar(self):
        addBlockedSite("metaloft.com/test/testP*.html")
        # this test URL should NOT be blocked (uri is different)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list glob ? matches a single character
    def test_22_blockedSpecificUrlGlobQuestionMark(self):
        addBlockedSite("metalo?t.com/test/testP?ge1.html")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a block list glob ? matches ONLY single character (but not two or more)
    def test_23_blockedSpecificUrlGlobQuestionMark(self):
        addBlockedSite("metalo?t.com/test/testP?.html")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a the action in taken from the first rule
    def test_24_blockedSpecificUrlRuleOrder(self):
        addBlockedSite("metaloft.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedSite("metaloft.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        assert (result == 0)
        nukeBlockedSiteList()

    # verify that a the action in taken from the second rule (first rule doesn't match)
    def test_25_blockedSpecificUrlRuleOrder(self):
        addBlockedSite("metaloft.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedSite("metaloft.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage2.html 2>&1 | grep -q blockpage")
        assert (result == 0)
        nukeBlockedSiteList()


    def test_999_finalTearDown(self):
        global nodeDesc
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        








