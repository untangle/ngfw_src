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

class OpenVPNTests(unittest.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-openvpn"

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
            node = uvmContext.nodeManager().node(nodeSettings["id"])
            nodeData = node.getSettings()

    def test_999_finalTearDown(self):
        global nodeSettings
        global node
        uvmContext.nodeManager().destroy(nodeSettings['id']);
        node = None
        nodeSettings = None
