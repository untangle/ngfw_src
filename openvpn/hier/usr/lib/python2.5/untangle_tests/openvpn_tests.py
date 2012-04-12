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
nodeDesc = None
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
        global nodeDesc, nodeData, node
        if nodeDesc == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            nodeDesc = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            node = uvmContext.nodeManager().nodeContext(nodeDesc['nodeSettings']).node()
            nodeData = node.getSettings()

    def test_999_finalTearDown(self):
        global nodeDesc
        global node
        uvmContext.nodeManager().destroy(nodeDesc['nodeSettings']['id']);
        node = None
        nodeDesc = None
