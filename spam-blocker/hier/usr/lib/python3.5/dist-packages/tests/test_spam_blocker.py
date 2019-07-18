"""spam_blocker tests"""

import subprocess
import unittest
import pytest

import runtests.test_registry as test_registry
import tests.global_functions as global_functions
from .test_spam_blocker_base import SpamBlockerBaseTests


@pytest.mark.spam_blocker
class SpamBlockerTests(SpamBlockerBaseTests):

    @staticmethod
    def module_name():
        global app
        app = SpamBlockerTests._app
        return "spam-blocker"

    @staticmethod
    def shortName():
        return "spam-blocker"

    @staticmethod
    def displayName():
        return "Spam Blocker"

    # verify daemon is running
    def test_009_IsRunning(self):
        self.module_name() # do not remove
        result1 = subprocess.call("ps aux | grep spamd | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result1 == 0)
        result2 = subprocess.call("ps aux | grep spamcatd | grep -v grep >/dev/null 2>&1", shell=True)
        assert ( result2 == 0 )

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
