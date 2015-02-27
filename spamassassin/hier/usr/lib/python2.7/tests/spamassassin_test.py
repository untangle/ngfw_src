import unittest2
import time
import sys
import os
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
from tests.spam_tests import SpamTests
import test_registry

#
# Just extends the spam base tests
#
class SpamassassinTests(SpamTests):

    @staticmethod
    def nodeName():
        return "untangle-node-spamassassin"

    @staticmethod
    def shortName():
        return "spamassassin"

    # verify daemon is running
    def test_009_IsRunning(self):
        result = os.system("ps aux | grep spamd | grep -v grep >/dev/null 2>&1")
        assert (result == 0)

test_registry.registerNode("spamassassin", SpamassassinTests)
