import unittest2
import time
import sys
import os
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
from untangle_tests.virus_tests import VirusTests
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
        result = os.system("ps aux | grep clamd | grep -v grep >/dev/null 2>&1")
        # if clamd is not running check to see freshclam is download new files
        if result != 0:
            freshClamResult = os.system("ps aux | grep freshclam | grep -v grep >/dev/null 2>&1")
            print "freshClamResult " + str(freshClamResult)
            if (freshClamResult == 0):
                VirusTests.freshdRunning = True # clamd is downloading definition files skip all tests
                raise unittest2.SkipTest("Clamd is not finished downloading definition files")
        assert (result == 0)

test_registry.registerNode("clam", ClamTests)
