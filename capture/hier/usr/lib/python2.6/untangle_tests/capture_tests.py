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
from untangle_tests import TestDict

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
nodeData = None
node = None

#pdb.set_trace()

def createCaptureRule():
    return {
        "capture": True,
        "description": "Test Rule - Capture all internal traffic",
        "enabled": True,
        "id": 1,
        "javaClass": "com.untangle.node.capture.CaptureRule",
        "matchers": {
            "javaClass": "java.util.LinkedList",
            "list": [{
                "invert": False,
                "javaClass": "com.untangle.node.capture.CaptureRuleMatcher",
                "matcherType": "SRC_INTF",
                "value": "2"
                }]
            },
        "ruleId": 1
    };

class CaptureTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-capture"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global nodeData, node
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), defaultRackId)
            nodeData = node.getCaptureSettings()

    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_010.log -O /tmp/capture_test_010.out http://www.untangle.com/")
        assert (result == 0)

    def test_020_defaultTrafficCheck(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_020.log -O /tmp/capture_test_020.out http://www.google.com/")
        assert (result == 0)

    def test_021_captureTrafficCheck(self):
        global node, nodeData
        nodeData['captureRules']['list'].append(createCaptureRule())
        node.setSettings(nodeData)
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/capture_test_021.log -O /tmp/capture_test_021.out http://www.google.com/")
        assert (result == 0)
        search = clientControl.runCommand("grep -q 'Captive Portal' /tmp/capture_test_021.out")
        assert (search == 0)

    def test_999_finalTearDown(self):
        global node
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None

TestDict.registerNode("capture", CaptureTests)

