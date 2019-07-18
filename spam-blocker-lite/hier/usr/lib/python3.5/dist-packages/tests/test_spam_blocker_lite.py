"""spam_blocker_lite tests"""

import subprocess
import unittest
import pytest

import runtests.test_registry as test_registry
from .test_spam_blocker_base import SpamBlockerBaseTests


@pytest.mark.spam_blocker_lite
class SpamBlockerLiteTests(SpamBlockerBaseTests):

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = SpamBlockerBaseTests._app
        return "spam-blocker-lite"

    @staticmethod
    def shortName():
        return "spam-blocker-lite"

    @staticmethod
    def displayName():
        return "Spam Blocker Lite"

    # verify daemon is running
    def test_009_IsRunning(self):
        self.module_name() # do not remove
        result = subprocess.call("ps aux | grep spamd | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result == 0)

test_registry.register_module("spam-blocker-lite", SpamBlockerLiteTests)
