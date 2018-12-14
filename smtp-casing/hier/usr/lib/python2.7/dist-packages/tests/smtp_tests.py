import unittest2
import time
import sys
import datetime
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry

default_policy_id = 1
app = None

class SmtpTests(unittest2.TestCase):

    @staticmethod
    def appName():
        return "smtp"

    @staticmethod
    def initial_setup(self):
        global app
        if (uvmContext.appManager().isInstantiated(self.appName())):
            app = uvmContext.appManager().app(self.appName())
        else:
            app = uvmContext.appManager().instantiate(self.appName(), default_policy_id)

    def setUp(self):
        pass

    # verify client is online
    def test_010_runTests(self):
        l = app.getTests();
        for name in l['list']:
            print(app.runTests(name))
            
        

test_registry.registerApp("smtp-casing", SmtpTests)
