"""spam_blocker ssl tests"""
import subprocess

import unittest2
from tests.global_functions import uvmContext
import tests.remote_control as remote_control
import tests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

from tests.spam_blocker_base_tests import SpamBlockerBaseTests

#
# Just extends the spam base tests to include SSL Inspector with default settings
#
class SpamBlockerTests(SpamBlockerBaseTests):

    @staticmethod
    def module_name():
        return "spam-blocker"

    @staticmethod
    def shortName():
        return "spam-blocker"

    @staticmethod
    def displayName():
        return "Spam Blocker"

    # verify daemon is running
    def test_009_IsRunningAndSSL(self):
        appSSL = appSP = uvmContext.appManager().app(self.appNameSSLInspector())
        appSSL.start()
        result = subprocess.call("ps aux | grep spamd | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result == 0)
        result = subprocess.call("ps aux | grep spamcatd | grep -v grep >/dev/null 2>&1", shell=True)
        assert ( result == 0 )

test_registry.register_module("spam-blocker-w-ssl", SpamBlockerTests)
