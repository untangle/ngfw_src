import unittest
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

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
nodeData = None
node = None

def flushEvents():
    global uvmContext
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

class ShieldTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-shield"

    def setUp(self):
        global nodeData, node
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName();
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), 1)
            nodeData = node.getSettings()

    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -a /tmp/shield_test_010.log -t 2 --timeout=5 http://www.untangle.com/")
        assert (result == 0)

    def test_011_shieldIsRunning(self):
        result = os.system("ps aux | grep /usr/bin/shield | grep -v grep >/dev/null 2>&1")
        assert (result == 0)

    def test_012_shieldDetectsNmap(self):
        startTime = datetime.now()
        result = clientControl.runCommand("nmap -PN -sT -TInsane -p10000-12000 1.2.3.4 2>&1 >/dev/null")
        assert (result == 0)
        time.sleep(20) # sleep 20 seconds, the daemon logs its events directly so flush events won't work
        flushEvents()
        query = None;
        for q in node.getEventQueries():
            if q['name'] == 'Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        assert(events['list'][0]['clientAddr'] == ClientControl.hostIP)
        assert(datetime.fromtimestamp((events['list'][0]['timeStamp']['time'])/1000) > startTime)


    def test_999_finalTearDown(self):
        global node
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None
        nodeSettings = None
        
