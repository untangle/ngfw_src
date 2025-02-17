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


test_registry.register_module("spam-blocker", SpamBlockerTests)
