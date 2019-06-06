"""spam_blocker tests"""
import subprocess

import unittest
import pytest
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

from tests.spam_blocker_base_tests import SpamBlockerBaseTests

#
# Just extends the spam base tests
#
@pytest.mark.spam_blocker
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
    def test_009_IsRunning(self):
        result = subprocess.call("ps aux | grep spamd | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result == 0)
        result = subprocess.call("ps aux | grep spamcatd | grep -v grep >/dev/null 2>&1", shell=True)
        assert ( result == 0 )

    # verify MAIL_SHELL is scoring. Relies on test_20_smtpTest
    def test_021_check_for_mailshell(self):
        if (not self.canRelay):
            raise unittest.SkipTest('Unable to relay through ' + global_functions.TEST_SERVER_HOST)
        events = global_functions.get_events(self.displayName(),'Quarantined Events',None,8)
        if events != None:
            assert( events.get('list') != None )
            found = False
            for event in events['list']:
                if 'MAILSHELL_SCORE_' in event['spam_blocker_tests_string']:
                    found = True
                    break
            assert(found)
        else:
            raise unittest.SkipTest('No events to check for MAIL_SHELL')

test_registry.register_module("spam-blocker", SpamBlockerTests)
