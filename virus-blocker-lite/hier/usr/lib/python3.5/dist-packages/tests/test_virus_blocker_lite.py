"""virus_blocker_lite tests"""
import time
import subprocess

import unittest
import pytest
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

from .test_virus_blocker_base import VirusBlockerBaseTests

#
# Just extends the virus base tests
#
@pytest.mark.virus_blocker_lite
class VirusBlockerLiteTests(VirusBlockerBaseTests):

    @staticmethod
    def module_name():
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
        for i in range(1200):
            time.sleep(1)
            result = subprocess.call("netcat -n -z 127.0.0.1 3310 >/dev/null 2>&1", shell=True)
            if result == 0:
                break
        print("Number of sleep cycles waiting: %d" % i)

test_registry.register_module("virus-blocker-lite", VirusBlockerLiteTests)
