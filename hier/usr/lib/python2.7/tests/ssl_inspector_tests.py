import unittest2
import time
import sys
import datetime
import random
import string

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
nodeWeb = None
testedServerName="news.ycombinator.com"
dropboxIssuer="/C=US/ST=California/L=San Francisco/O=Dropbox"

def addBlockedUrl(url, blocked=True, flagged=True, description="description"):
    newRule = { "blocked": blocked, "description": description, "flagged": flagged, "javaClass": "com.untangle.uvm.node.GenericRule", "string": url }
    rules = nodeWeb.getBlockedUrls()
    rules["list"].append(newRule)
    nodeWeb.setBlockedUrls(rules)

def nukeBlockedUrls():
    rules = nodeWeb.getBlockedUrls()
    rules["list"] = []
    nodeWeb.setBlockedUrls(rules)

class SslInspectorTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-casing-ssl-inspector"

    @staticmethod
    def nodeWeb():
        return "untangle-node-web-filter"

    def setUp(self):
        global node, nodeData, nodeWeb, nodeWebData
        if node == None:
            if uvmContext.nodeManager().isInstantiated(self.nodeName()):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            node.start() # must be called since the node doesn't auto-start
            nodeData = node.getSettings()
        if nodeWeb == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeWeb())):
                print "ERROR: Node %s already installed" % self.nodeWeb()
                raise Exception('node %s already instantiated' % self.nodeWeb())
            nodeWeb = uvmContext.nodeManager().instantiate(self.nodeWeb(), defaultRackId)
            nodeWebData = nodeWeb.getSettings()

    def test_005_setInspectAllTraffic(self):
        # print nodeData['ignoreRules']['list']
        if nodeData['ignoreRules']['list'][4]['description'] != 'Inspect All Traffic':
            raise unittest2.SkipTest('Rule 4 of SSL Inspector is not Inspect All traffic')
        else:
            nodeData['ignoreRules']['list'][4]['live'] = True
            node.setSettings(nodeData)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)
            
    def test_011_checkServerCertificate(self):
        result = remote_control.runCommand('echo -n | openssl s_client -connect %s:443 -servername %s 2>/dev/null | grep -qi "untangle"' % (testedServerName, testedServerName))
        assert (result == 0)

    def test_020_checkIgnoreCertificate(self):
        if nodeData['ignoreRules']['list'][3]['description'] != 'Ignore Dropbox':
            raise unittest2.SkipTest('Rule 3 of SSL Inspector is not Ignore Dropbox')
        else:
            result = remote_control.runCommand('echo -n | openssl s_client -connect www.dropbox.com:443 -servername www.dropbox.com 2>/dev/null | grep -q \'%s\'' % (dropboxIssuer))
            assert (result == 0)

    def test_030_checkSslInspectorInspectorEventLog(self):
        events = global_functions.get_events('SSL Inspector','All Sessions',None,5)
        assert(events != None)
        print "List of events"
        found = global_functions.check_events( events.get('list'), 5,
                                            "ssl_inspector_status","INSPECTED",
                                            "ssl_inspector_detail",testedServerName)
        assert( found )

    def test_040_checkWebFilterEventLog(self):
        addBlockedUrl(testedServerName)
        remote_control.runCommand('curl -s -4 --connect-timeout 2 --trace /tmp/ssl_test_040.trace --output /tmp/ssl_test_040.output --insecure https://%s' % (testedServerName))
        nukeBlockedUrls()

        events = global_functions.get_events('Web Filter','Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host", testedServerName,
                                            "web_filter_blocked", True)
        assert( found )

    # check Youtube for school feature
    def test_050_youtubeForSchools(self):
        raise unittest2.SkipTest('Youtube For Schools API has changed, waiting on bug 12566 fix')
        settings = nodeWeb.getSettings()
        settings["youtubeForSchoolsIdentifier"] = "Wc1bnoBwWO8Di_TKQQ3IqA"
        settings["enforceYoutubeForSchools"] = True
        nodeWeb.setSettings(settings)
        results = []

        # try several videos.
        # youtube doesn't always seem to enforce youtube-for-schools so try several to see if ngfw
        # is properly modifying the url
        results.append( remote_control.runCommand("curl -s -4 --insecure 'https://www.youtube.com/watch?v=-kkMrZYNrt0' | grep -q 'Education Filter'") )
        if not 0 in results:
            results.append( remote_control.runCommand("curl -s -4 --insecure 'https://www.youtube.com/watch?v=4r7wHMg5Yjg' | grep -q 'Education Filter'") )
        if not 0 in results:
            results.append( remote_control.runCommand("curl -s -4 --insecure 'https://www.youtube.com/watch?v=_OBlgSz8sSM' | grep -q 'Education Filter'") )
        if not 0 in results:
            results.append( remote_control.runCommand("curl -s -4 --insecure 'https://www.youtube.com/watch?v=kHmvkRoEowc' | grep -q 'Education Filter'") )

        settings["youtubeForSchoolsIdentifier"] = None
        settings["enforceYoutubeForSchools"] = False
        nodeWeb.setSettings(settings)
        assert ( 0 in results )

    # Query eventlog
    def test_060_queryEventLog(self):
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
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=oneterm",
            "term": "oneterm"
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=%22quoted+terms%22",
            "term": '"quoted terms"'
        },{
            "host": "search.yahoo.com",
            "uri":  "/search?p=two+terms",
            "term": "two terms"
        }]
        host = "www.bing.com"
        uri = "/search?q=oneterm&qs=n&form=QBRE"
        for t in termTests:
            eventTime = datetime.datetime.now()
            result = remote_control.runCommand("curl -s -4 -o /dev/null -A 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1' --connect-timeout 10 --insecure 'https://%s%s'" % ( t["host"], t["uri"] ) )
            assert( result == 0 )

            events = global_functions.get_events('Web Filter','All Query Events',None,1)
            assert(events != None)
            found = global_functions.check_events( events.get('list'), 5,
                                                "host", t["host"],
                                                "term", t["term"])
            assert( found )

    @staticmethod
    def finalTearDown(self):
        global node, nodeWeb
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        if nodeWeb != None:
            uvmContext.nodeManager().destroy( nodeWeb.getNodeSettings()["id"])
            nodeWeb = None

test_registry.registerNode("ssl-inspector", SslInspectorTests)
