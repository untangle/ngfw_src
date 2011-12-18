import unittest
import time
import sys
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
defaultRack = uvmContext.policyManager().getDefaultPolicy()
clientControl = ClientControl()
nodeDesc = None
node = None

def addBlockedUrl(url, blocked=True, flagged=True, description="description"):
    global node
    newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = node.getBlockedUrls()
    rules["list"].append(newRule)
    node.setBlockedUrls(rules)

def nukeBlockedUrls():
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

def nukePassedUrls():
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

def flushEvents():
    global uvmContext
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents(True)

class WebFilterTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-webfilter"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global nodeDesc, node
        if nodeDesc == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName();
                raise Exception('node %s already instantiated' % self.nodeName())
            nodeDesc = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), uvmContext.policyManager().getDefaultPolicy())
            node = uvmContext.nodeManager().nodeContext(nodeDesc['nodeId']).node()

    # verify client is online
    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -o /dev/null http://google.com/")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_011_defaultPornIsBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://playboy.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_012_defaultPornIsBlockedWithSubdomain(self):
        result = clientControl.runCommand("wget -q -O - http://www.penthouse.com/ 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_013_defaultPornIsBlockedWithUrl(self):
        result = clientControl.runCommand("wget -q -O - http://penthouse.com/index.html 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify porn site is blocked in default config
    def test_014_defaultPornIsBlockedWithUrlAndSubdomain(self):
        result = clientControl.runCommand("wget -q -O - http://www.penthouse.com/index.html 2>&1 | grep -q blockpage")
        assert (result == 0)

    # verify metaloft site is not blocked in default config
    def test_015_defaultMetaloftIsNotBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        assert (result == 0)

    # verify blocked site url list works
    def test_016_blockedUrl(self):
        addBlockedUrl("metaloft.com/test/testPage1.html")
        # this test URL should now be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list entry does not match when the URI doesnt match exactly
    def test_017_blockedUrl(self):
        addBlockedUrl("metaloft.com/test/testPage1.html")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage2.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list entry correctly appends "(/.*)?" to the rigth side anchor
    def test_018_blockedUrlRightSideAnchor(self):
        addBlockedUrl("metaloft.com/test")
        # this test URL should NOT be blocked (testPage1 vs testPage2)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list entry does not match when the URI capitalization is different
    def test_019_blockedUrlCapitalization(self):
        addBlockedUrl("metaloft.com/test/testPage1.html")
        # this test URL should NOT be blocked (capitalization is different)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testpage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob functions with * at the end
    def test_030_blockedUrlGlobStar(self):
        addBlockedUrl("metaloft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob functions with * at the end and at the beginning
    def test_031_blockedUrlGlobStar(self):
        addBlockedUrl("*loft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob functions with * at the end and at the beginning and in the middle
    def test_032_blockedUrlGlobStar(self):
        addBlockedUrl("*et*loft.com/test/test*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob matches the whole URL
    def test_033_blockedUrlGlobStar(self):
        addBlockedUrl("metaloft.com*")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob * matches zero characters
    def test_034_blockedUrlGlobStar(self):
        addBlockedUrl("meta*loft.com*")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob * doesnt overmatch
    def test_035_blockedUrlGlobStar(self):
        addBlockedUrl("metaloft.com/test/testP*.html")
        # this test URL should NOT be blocked (uri is different)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob ? matches a single character
    def test_036_blockedUrlGlobQuestionMark(self):
        addBlockedUrl("metalo?t.com/test/testP?ge1.html")
        # this test URL should be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a block list glob ? matches ONLY single character (but not two or more)
    def test_037_blockedUrlGlobQuestionMark(self):
        addBlockedUrl("metalo?t.com/test/testP?.html")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a the action in taken from the first rule
    def test_038_blockedUrlRuleOrder(self):
        addBlockedUrl("metaloft.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedUrl("metaloft.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that a the action in taken from the second rule (first rule doesn't match)
    def test_039_blockedUrlRuleOrder(self):
        addBlockedUrl("metaloft.com/test/testPage1.html", blocked=False, flagged=True)
        addBlockedUrl("metaloft.com", blocked=True, flagged=True)
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage2.html 2>&1 | grep -q blockpage")
        nukeBlockedUrls()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_050_passedUrlOverridesBlockedCategory(self):
        addPassedUrl("playboy.com")
        # this test URL should NOT be blocked (porn is blocked by default, but playboy.com now on pass list
        result = clientControl.runCommand("wget -q -O - http://playboy.com/ 2>&1 | grep -q 'Nude Girls'")
        nukePassedUrls()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_051_passedUrlOverridesBlockedUrl(self):
        addBlockedUrl("metaloft.com")
        addPassedUrl("metaloft.com/test/")
        # this test URL should NOT be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html 2>&1 | grep -q text123")
        nukeBlockedUrls()
        nukePassedUrls()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_052_passedUrlOverridesBlockedMimeType(self):
        nukeBlockedMimeTypes()
        addBlockedMimeType("text/plain")
        addPassedUrl("metaloft.com/test/")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.txt 2>&1 | grep -q text123")
        nukeBlockedUrls()
        nukePassedUrls()
        assert (result == 0)

    # verify that an entry in the pass list overrides a blocked category
    def test_053_passedUrlOverridesBlockedExtension(self):
        nukeBlockedExtensions()
        addBlockedExtension("txt")
        addPassedUrl("metaloft.com/test/")
        # this test URL should NOT be blocked 
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.txt 2>&1 | grep -q text123")
        nukeBlockedUrls()
        nukePassedUrls()
        assert (result == 0)

    # verify that an entry in the mime type block list functions
    def test_060_blockedMimeType(self):
        nukeBlockedMimeTypes()
        addBlockedMimeType("text/plain")
        # this test URL should be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.txt 2>&1 | grep -q blockpage")
        nukeBlockedMimeTypes()
        assert (result == 0)

    # verify that an entry in the mime type block list doesn't overmatch
    def test_061_blockedMimeType(self):
        nukeBlockedMimeTypes()
        addBlockedMimeType("text/plain")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedMimeTypes()
        assert (result == 0)

    # verify that an entry in the mime type block list functions
    def test_070_blockedExtension(self):
        nukeBlockedExtensions()
        addBlockedExtension("txt")
        # this test URL should be blocked
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.txt 2>&1 | grep -q blockpage")
        nukeBlockedExtensions()
        assert (result == 0)

    # verify that an entry in the mime type block list doesn't overmatch
    def test_071_blockedExtension(self):
        nukeBlockedExtensions()
        addBlockedExtension("txt")
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedExtensions()
        assert (result == 0)

    # verify that an entry in the mime type block list doesn't overmatch
    def test_072_blockedExtension(self):
        nukeBlockedExtensions()
        addBlockedExtension("tml") # not this should only block ".tml" not ".html"
        # this test URL should NOT be blocked (its text/html not text/plain)
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.html 2>&1 | grep -q text123")
        nukeBlockedExtensions()
        assert (result == 0)
        
    def test_100_eventlog_blockedUrl(self):
        global node
        global defaultRack
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls();
        addBlockedUrl("metaloft.com/test/testPage1.html", blocked=True, flagged=True)
        # specify an argument so it isn't confused with other events
        result1 = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        time.sleep(1)
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'Blocked Web Traffic': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRack,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        assert(events['list'][0]['host'] == "metaloft.com")
        assert(events['list'][0]['uri'] == ("/test/testPage1.html?arg=%s" % fname))
        assert(events['list'][0]['wf' + self.vendorName() + 'Blocked'] == True)

    def test_101_eventlog_flaggedUrl(self):
        global node
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls();
        addBlockedUrl("metaloft.com/test/testPage1.html", blocked=False, flagged=True)
        # specify an argument so it isn't confused with other events
        result1 = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        time.sleep(1)
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'Flagged Web Traffic': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRack,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        assert(events['list'][0]['host'] == "metaloft.com")
        assert(events['list'][0]['uri'] == ("/test/testPage1.html?arg=%s" % fname))
        assert(events['list'][0]['wf' + self.vendorName() + 'Blocked'] == False)
        assert(events['list'][0]['wf' + self.vendorName() + 'Flagged'] == True)

    def test_102_eventlog_allUrls(self):
        global node
        fname = sys._getframe().f_code.co_name
        nukeBlockedUrls();
        # specify an argument so it isn't confused with other events
        result1 = clientControl.runCommand("wget -q -O - http://metaloft.com/test/testPage1.html?arg=%s 2>&1 >/dev/null" % fname)
        time.sleep(1)
        flushEvents()
        for q in node.getEventQueries():
            if q['name'] == 'All Web Traffic': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRack,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        assert(events['list'][0]['host'] == "metaloft.com")
        assert(events['list'][0]['uri'] == ("/test/testPage1.html?arg=%s" % fname))
        assert(events['list'][0]['wf' + self.vendorName() + 'Blocked'] == False)
        assert(events['list'][0]['wf' + self.vendorName() + 'Flagged'] == False)

    def test_999_finalTearDown(self):
        global nodeDesc
        global node
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        node = None
        nodeDesc = None
        








