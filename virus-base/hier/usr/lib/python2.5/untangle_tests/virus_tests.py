import unittest
import time
import sys
import os
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl

uvmContext = Uvm().getUvmContext()
defaultRackId = uvmContext.policyManager().getDefaultPolicy()['id']
clientControl = ClientControl()
nodeDesc = None
node = None

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

class VirusTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-base-virus"

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

    # test that client can download zip
    def test_011_httpNonVirusNotBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.zip 2>&1 | grep -q text123")
        assert (result == 0)

    # test that client can download zip
    def test_012_httpVirusBlocked(self):
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/eicar.zip 2>&1 | grep -q blocked")
        assert (result == 0)

    def test_100_eventlog_httpVirus(self):
        fname = sys._getframe().f_code.co_name
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/eicar.zip?arg=%s 2>&1 | grep -q blocked" % fname)
        assert (result == 0)
        flushEvents()
        query = None;
        for q in node.getWebEventQueries():
            if q['name'] == 'Infected Web Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        assert(events['list'][0]['host'] == "metaloft.com")
        assert(events['list'][0]['uri'] == ("/test/eicar.zip?arg=%s" % fname))
        assert(events['list'][0]['virus' + self.vendorName() + 'Name'] != None)
        assert(events['list'][0]['virus' + self.vendorName() + 'Clean'] == False)

    def test_101_eventlog_httpNonVirus(self):
        fname = sys._getframe().f_code.co_name
        result = clientControl.runCommand("wget -q -O - http://metaloft.com/test/test.zip?arg=%s 2>&1 | grep -q text123" % fname)
        assert (result == 0)
        flushEvents()
        query = None;
        for q in node.getWebEventQueries():
            if q['name'] == 'Clean Web Events': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        assert(events['list'][0]['host'] == "metaloft.com")
        assert(events['list'][0]['uri'] == ("/test/test.zip?arg=%s" % fname))
        assert(events['list'][0]['virus' + self.vendorName() + 'Clean'] == True)

    def test_999_finalTearDown(self):
        global nodeDesc
        global node
        uvmContext.nodeManager().destroy(nodeDesc['nodeId']);
        node = None
        nodeDesc = None
        








