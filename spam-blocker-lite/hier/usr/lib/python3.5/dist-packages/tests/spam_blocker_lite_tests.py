"""spam_blocker_lite tests"""
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
@pytest.mark.spam_blocker_lite
class SpamBlockerLiteTests(SpamBlockerBaseTests):

    @staticmethod
    def module_name():
        return "spam-blocker-lite"

    @staticmethod
    def shortName():
        return "spam-blocker-lite"

    @staticmethod
    def displayName():
        return "Spam Blocker Lite"

    # verify daemon is running
    def test_009_IsRunning(self):
        result = subprocess.call("ps aux | grep spamd | grep -v grep >/dev/null 2>&1", shell=True)
        assert (result == 0)

test_registry.register_module("spam-blocker-lite", SpamBlockerLiteTests)
