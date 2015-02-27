import unittest2
import time
import sys
import os
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
from tests.virus_tests import VirusTests
import test_registry

#
# Just extends the virus base tests
#
class ClamTests(VirusTests):

    @staticmethod
    def nodeName():
        return "untangle-node-clam"

    @staticmethod
    def shortName():
        return "clam"

    # verify daemon is running
    def test_009_clamdIsRunning(self):
        # wait for freshclam to finish updating sigs
        os.system("freshclam >/dev/null 2>&1")
        result = os.system("pidof clamd >/dev/null 2>&1")
        assert (result == 0)

test_registry.registerNode("clam", ClamTests)
