import unittest2
import time
import sys
import datetime
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests import TestDict

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
node = None


class SmtpTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-casing-smtp"

    def setUp(self):
        global node
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                node = uvmContext.nodeManager().node(self.nodeName())
            else:
                node = uvmContext.nodeManager().instantiateAndStart(self.nodeName(), defaultRackId)

    # verify client is online
    def test_010_runTests(self):
        l = node.getTests();
        for name in l['list']:
            print node.runTests(name);
            
        

TestDict.registerNode("smtp-casing", SmtpTests)
