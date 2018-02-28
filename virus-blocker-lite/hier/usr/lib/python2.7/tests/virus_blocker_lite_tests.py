import unittest2
import time
import sys
import os
import subprocess
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
from tests.virus_blocker_base_tests import VirusBlockerBaseTests
import test_registry

#
# Just extends the virus base tests
#
class VirusBlockerLiteTests(VirusBlockerBaseTests):

    @staticmethod
    def appName():
        return "virus-blocker-lite"

    @staticmethod
    def shortName():
        return "virus_blocker_lite"

    @staticmethod
    def displayName():
        return "Virus Blocker Lite"

    # verify daemon is running
    def test_009_clamdIsRunning(self):
        # wait for freshclam to finish updating sigs
        subprocess.call("freshclam >/dev/null 2>&1", shell=True)
        result = subprocess.call("pidof clamd >/dev/null 2>&1", shell=True)
        assert (result == 0)

test_registry.registerApp("virus-blocker-lite", VirusBlockerLiteTests)
