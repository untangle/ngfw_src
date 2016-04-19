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
import remote_control
import test_registry
import global_functions

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None

class ShieldTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-shield"

    @staticmethod
    def initialSetUp(self):
        global node
        if (not uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().node(self.nodeName())

    def setUp(self):
        pass

    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_011_shieldDetectsNmap(self):
        startTime = datetime.now()
        result = remote_control.runCommand("nmap -PN -sT -T5 --min-parallelism 15 -p10000-12000 1.2.3.4 2>&1 >/dev/null")
        assert (result == 0)

        events = global_functions.get_events('Shield','Blocked Session Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.clientIP,
                                            min_date=startTime)
        assert( found )

    @staticmethod
    def finalTearDown(self):
        # sleep so the reputation goes down so it will not interfere with any future tests
        time.sleep(3)
        # shield is always installed, do not remove it
        

test_registry.registerNode("shield", ShieldTests)
