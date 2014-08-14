import unittest2
import time
import sys
import pdb
import os
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from datetime import datetime
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests import TestDict

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
node = None

def flushEvents():
    global uvmContext
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

class ShieldTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-shield"

    def setUp(self):
        global node
        if node == None:
            if (not uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s is not installed" % self.nodeName();
                raise Exception('node %s is not installed' % self.nodeName())
            node = uvmContext.nodeManager().node(self.nodeName())

    def test_010_clientIsOnline(self):
        result = clientControl.isOnline()
        assert (result == 0)

    def test_011_shieldDetectsNmap(self):
        startTime = datetime.now()
        result = clientControl.runCommand("nmap -PN -sT -T5 --min-parallelism 15 -p10000-12000 1.2.3.4 2>&1 >/dev/null")
        assert (result == 0)
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'Blocked Sessions': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        found = clientControl.check_events( events.get('list'), 5,
                                            'c_client_addr', ClientControl.clientIP,
                                            min_date=startTime)
        assert( found )

    @staticmethod
    def finalTearDown(self):
        # sleep so the reputation goes down so it will not interfere with any future tests
        time.sleep(3)
        # shield is always installed, do not remove it
        

TestDict.registerNode("shield", ShieldTests)
