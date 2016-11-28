import unittest2
import time
import sys
import datetime
import string
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions
import pprint

defaultRackId = 1
node = None

class WebMonitorTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-web-monitor"

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

    def test_020_eventlog_flaggedPorn(self):
        result = remote_control.runCommand("wget -q -O - http://www.playboy.com/bunnies 2>&1 >/dev/null")
        time.sleep(1);
        events = global_functions.get_events("Web Monitor", "Flagged Web Events", None, 1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","www.playboy.com",
                                            "uri", "/bunnies",
                                            'web_filter_blocked', False,
                                            'web_filter_flagged', True )
        assert( found )

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("web-monitor", WebMonitorTests)
