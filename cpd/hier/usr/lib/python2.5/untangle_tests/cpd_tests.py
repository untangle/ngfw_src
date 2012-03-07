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
clientControl = ClientControl()
nodeData = None
nodeDesc = None
node = None

#pdb.set_trace()

def createCaptureRule():
    return {
        "alert": False, 
        "capture": True, 
        "category": "[no category]", 
        "clientAddress": "any", 
        "clientInterface": "any", 
        "days": "mon,tue,wed,thu,fri,sat,sun", 
        "description": "Any Interface", 
        "endTime": "23:59", 
        "javaClass": "com.untangle.node.cpd.CaptureRule", 
        "live": True, 
        "log": False, 
        "name": "[no name]", 
        "serverAddress": "any", 
        };

class CpdTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-cpd"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global nodeDesc, nodeData, node
        if nodeDesc == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            nodeDesc = uvmContext.nodeManager().instantiate(self.nodeName(), uvmContext.policyManager().getDefaultPolicy())
            node = uvmContext.nodeManager().nodeContext(nodeDesc['nodeId']).node()
            nodeData = node.getSettings()

    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -a /tmp/cpd_test_010.log -O /tmp/cpd_test_010.out -t 2 --timeout=5 http://www.untangle.com/")
        assert (result == 0)

    def test_011_daemonIsRunning(self):
        result = os.system("ps aux | grep cpd | grep -v grep >/dev/null 2>&1")
        assert (result == 0)

    def test_020_nodeSettings_Default_Google(self):
        result = clientControl.runCommand("wget -4 -a /tmp/cpd_test_020.log -O /tmp/cpd_test_020.out -t 2 --timeout=5 http://www.google.com/")
        assert (result == 0)

    def test_021_nodeSettings_Capture_Google(self):
        global node, nodeData
        nodeData['captureRules']['list'].append(createCaptureRule())
        node.setSettings(nodeData)
        node.start()
        result = clientControl.runCommand("wget -4 -a /tmp/cpd_test_021.log -O /tmp/cpd_test_021.out -t 2 --timeout=5 http://www.google.com/")        
        assert (result != 0)

    def test_999_finalTearDown(self):
        global nodeDesc
        global node
        uvmContext.nodeManager().destroy(nodeDesc['nodeId'])
        node = None
        nodeDesc = None
