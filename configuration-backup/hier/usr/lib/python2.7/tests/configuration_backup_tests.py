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
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import test_registry
import remote_control
import pdb
import time
import urllib2
import global_functions

node = None
defaultRackId = 1

class ConfigurationBackupTests(unittest2.TestCase):
    
    @staticmethod
    def nodeName():
        return "configuration-backup"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_020_backupNow(self):
        global node
        boxUID = uvmContext.getServerUID()
        node.sendBackup()

        events = global_functions.get_events('Configuration Backup','Backup Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                               'success', True ) 
        assert( found )

    def test_030_verifyBackupCronjob(self):
        assert( os.path.isfile("/etc/cron.d/untangle-configuration-backup-nightly")  )
        
    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None


test_registry.registerNode("configuration-backup", ConfigurationBackupTests)
