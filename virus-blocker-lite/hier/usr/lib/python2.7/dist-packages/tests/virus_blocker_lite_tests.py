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

        # give it up to 20 minutes to download signatures for the first time
        print("Waiting for server to start...")
        for i in xrange(1200):
            time.sleep(1)
            result = subprocess.call("netcat -n -z 127.0.0.1 3310 >/dev/null 2>&1", shell=True)
            if result == 0:
                break
        print("Number of sleep cycles waiting: %d" % i)

test_registry.registerApp("virus-blocker-lite", VirusBlockerLiteTests)
