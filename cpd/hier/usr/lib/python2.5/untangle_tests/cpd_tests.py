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
defaultRackId = 1
clientControl = ClientControl()
nodeData = None
nodeSettings = None
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
        "startTime": "00:00"
        };

class CpdTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-cpd"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global nodeSettings, nodeData, node
        if nodeSettings == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            nodeSettings = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            node = uvmContext.nodeManager().nodeContext(nodeSettings).node()
            nodeData = node.getSettings()

    def test_010_clientIsOnline(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/cpd_test_010.log -O /tmp/cpd_test_010.out http://www.untangle.com/")
        assert (result == 0)

    def test_020_daemonNotRunning_TrafficCheck(self):
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -a /tmp/cpd_test_020.log -O /tmp/cpd_test_020.out http://www.google.com/")
        assert (result == 0)

    def test_021_daemonConfig_and_Startup(self):
        global node, nodeData
        nodeData['captureRules']['list'].append(createCaptureRule())
        node.setSettings(nodeData)
        node.start()
        # cpd start seems to return pre-maturely
        # sleep to allow extra time for CPD to initialize - this should be fixed
        time.sleep(10)
        result = os.system("ps aux | grep monitor\.cpd | grep -v grep >/dev/null 2>&1")
        assert (result == 0)

    def test_022_basicCaptureRule(self):
        result = clientControl.runCommand("wget -4 -t2 --timeout=5 -a /tmp/cpd_test_021.log -O - http://www.google.com/ | grep -q captive-portal")
        assert (result == 0)

    def test_999_finalTearDown(self):
        global nodeSettings
        global node
        uvmContext.nodeManager().destroy(nodeSettings['id'])
        node = None
        nodeSettings = None
