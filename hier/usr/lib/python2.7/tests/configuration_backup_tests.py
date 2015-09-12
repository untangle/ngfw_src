import unittest2
import os
import re
import sys
import random
import string
reload(sys)
sys.setdefaultencoding("utf-8")
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import test_registry
import remote_control
import pdb
import time
import urllib2

node = None
uvmContext = Uvm().getUvmContext()
defaultRackId = 1

class BoxBackupTests(unittest2.TestCase):
    
    @staticmethod
    def nodeName():
        return "untangle-node-configuration-backup"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_020_backupNow(self):
        global node
        timeout = 3000  # countdown timer
        boxUID = uvmContext.getServerUID()
        node.sendBackup()
        lastUpdate = node.getLatestEvent()
        currentTime = (int(time.time()) - (5*60)) * 1000
        print "Current time <%s> backup time <%s>" % (currentTime,lastUpdate['timeStamp']['time'])
        while timeout and (currentTime > lastUpdate['timeStamp']['time']):
            # wait for the backup to occur
            timeout -= 1
            lastUpdate = node.getLatestEvent()
        # we have a successful backup if the loop did not timeout
        assert(timeout)
        
    def test_030_verifyBackupCronjob(self):
        assert( os.path.isfile("/etc/cron.d/untangle-configuration-backup-nightly")  )
        
    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None


test_registry.registerNode("configuration-backup", BoxBackupTests)
