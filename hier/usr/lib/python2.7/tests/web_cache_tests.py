import unittest2
import time
import sys
import datetime
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

class WebCacheTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-web-cache"

    @staticmethod
    def initialSetUp(self):
        # FIXME
        pass

    def setUp(self):
        global node
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            node.start() # must be called since web cache doesn't auto-start

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_020_testBasicWebCache(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        node.clearSquidCache()
        for x in range(0, 10):
            result = remote_control.runCommand("wget -q -O /dev/null -4 -t 2 --timeout=5 http://test.untangle.com/")
            time.sleep(1)
        assert (result == 0)
        time.sleep(65) # summary-events only written every 60 seconds

        events = global_functions.get_events('Web Cache','Web Cache Events',None,1)
        assert(events != None)
        # verify at least one hit
        assert(events['list'][0])
        assert(events['list'][0]['hits'])

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        
test_registry.registerNode("web-cache", WebCacheTests)
