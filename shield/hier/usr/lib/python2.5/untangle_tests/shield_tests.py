import unittest
import time
import sys
import pdb
import os
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
defaultRackId = uvmContext.policyManager().getDefaultPolicy()['id']
clientControl = ClientControl()
nodeData = None
nodeDesc = None
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
        global nodeDesc, nodeData, node
        if nodeDesc == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName();
                raise Exception('node %s already instantiated' % self.nodeName())
            nodeDesc = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), uvmContext.policyManager().getDefaultPolicy())
            node = uvmContext.nodeManager().nodeContext(nodeDesc['nodeId']).node()
            nodeData = node.getSettings()

    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -a /tmp/shield_test_010.log -t 2 --timeout=5 http://www.untangle.com/")
        assert (result == 0)

    def test_011_shieldIsRunning(self):
        result = os.system("ps aux | grep /usr/bin/shield | grep -v grep >/dev/null 2>&1")
        assert (result == 0)

    def test_012_shieldDetectsNmap(self):
        result = clientControl.runCommand("nmap -sT -TInsane -p10000-11000 metaloft.com 2>&1 >/dev/null")
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

    def test_999_finalTearDown(self):
        global nodeDesc
        global node
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        node = None
        nodeDesc = None
        
