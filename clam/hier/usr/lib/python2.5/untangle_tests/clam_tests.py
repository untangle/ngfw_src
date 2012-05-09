import unittest
import time
import sys
import os
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests.virus_tests import VirusTests

#
# Just extends the virus base tests
#
class ClamTests(VirusTests):

    @staticmethod
    def nodeName():
        return "untangle-node-clam"

    @staticmethod
    def vendorName():
        return "clam"

    # verify daemon is running
    def test_009_clamdIsRunning(self):
        result = os.system("ps aux | grep clamd | grep -v grep >/dev/null 2>&1")
        assert (result == 0)
