"""smtp tests"""

import unittest
import pytest
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

default_policy_id = 1
app = None

@pytest.mark.smtp_casing
class SmtpTests(unittest.TestCase):

    @staticmethod
    def module_name():
        return "smtp"

    @staticmethod
    def initial_setup(self):
        global app
        if (uvmContext.appManager().isInstantiated(self.module_name())):
            app = uvmContext.appManager().app(self.module_name())
        else:
            app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)

    def setUp(self):
        pass

    # verify client is online
    def test_010_runTests(self):
        l = app.getTests();
        for name in l['list']:
            print(app.runTests(name))
            
        

test_registry.register_module("smtp-casing", SmtpTests)
