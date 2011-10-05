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
    newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getBlockedUrls()
    rules["list"].append(newRule)
    node.setBlockedUrls(rules)

def nukeBlockedUrlList():
    global node
    rules = node.getBlockedUrls()
    rules["list"] = []
    node.setBlockedUrls(rules)

def addPassedUrl(url, enabled=True, description="description"):
    global node
    newRule =  { "enabled": enabled, "description": description, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getPassedUrls()
    rules["list"].append(newRule)
    node.setPassedUrls(rules)

def nukePassedUrlList():
    global node
    rules = node.getPassedUrls()
    rules["list"] = []
    node.setPassedUrls(rules)

def addBlockedMimeType(mimetype, blocked=True, flagged=True, category="category", description="description"):
    global node
    newRule =  { "blocked": blocked, "category": category, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": mimetype, "name": mimetype }
    rules = node.getBlockedMimeTypes()
    rules["list"].append(newRule)
    node.setBlockedMimeTypes(rules)

def nukeBlockedMimeTypes():
    global node
    rules = node.getBlockedMimeTypes()
    rules["list"] = []
    node.setBlockedMimeTypes(rules)

def addBlockedExtension(mimetype, blocked=True, flagged=True, category="category", description="description"):
    global node
    newRule =  { "blocked": blocked, "category": category, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": mimetype, "name": mimetype }
    rules = node.getBlockedExtensions()
    rules["list"].append(newRule)
    node.setBlockedExtensions(rules)

def nukeBlockedExtensions():
    global node
    rules = node.getBlockedExtensions()
    rules["list"] = []
    node.setBlockedExtensions(rules)

class WebFilterLiteTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-webfilter"

    def setUp(self):
        global nodeDesc, node
        if nodeDesc == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                raise Exception('node %s already instantiated' % self.nodeName())
            nodeDesc = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), uvmContext.policyManager().getDefaultPolicy())
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
        # this test URL should NOT be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        nukePassedUrlList()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_42_passedUrlOverridesBlockedMimeType(self):
        nukeBlockedMimeTypes()
        addBlockedMimeType("text/plain")
        addPassedUrl("metaloft.com/test/")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.txt 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        nukePassedUrlList()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_43_passedUrlOverridesBlockedExtension(self):
        nukeBlockedExtensions()
        addBlockedExtension("txt")
        addPassedUrl("metaloft.com/test/")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.txt 2>&1 | grep -q text123")
        nukeBlockedUrlList()
        nukePassedUrlList()
        assert (result == 0)

    # verify that an entry in the mime type block list functions
    def test_50_blockedMimeType(self):
        nukeBlockedMimeTypes()
        addBlockedMimeType("text/plain")
        # this test URL should be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.txt 2>&1 | grep -q blockpage")
        nukeBlockedMimeTypes()
        assert (result == 0)

    # verify that an entry in the mime type block list doesn't overmatch
    def test_51_blockedMimeType(self):
        nukeBlockedMimeTypes()
        addBlockedMimeType("text/plain")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedMimeTypes()
        assert (result == 0)

    # verify that an entry in the mime type block list functions
    def test_60_blockedExtension(self):
        nukeBlockedExtensions()
        addBlockedExtension("txt")
        # this test URL should be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.txt 2>&1 | grep -q blockpage")
        nukeBlockedExtensions()
        assert (result == 0)

    # verify that an entry in the mime type block list doesn't overmatch
    def test_61_blockedExtension(self):
        nukeBlockedExtensions()
        addBlockedExtension("txt")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedExtensions()
        assert (result == 0)

    # verify that an entry in the mime type block list doesn't overmatch
    def test_62_blockedExtension(self):
        nukeBlockedExtensions()
        addBlockedExtension("tml") # not this should only block ".tml" not ".html"
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedExtensions()
        assert (result == 0)

    def test_999_finalTearDown(self):
        global nodeDesc
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        








