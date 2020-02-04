"""virus_blocker_lite tests"""
import time
import subprocess

import pytest
import runtests.test_registry as test_registry

from .test_virus_blocker_base import VirusBlockerBaseTests
import tests.global_functions as global_functions

#
# Just extends the virus base tests
#
@pytest.mark.virus_blocker_lite
class VirusBlockerLiteTests(VirusBlockerBaseTests):

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = VirusBlockerBaseTests._app
        return "virus-blocker-lite"

    @staticmethod
    def shortName():
        return "virus_blocker_lite"

    @staticmethod
    def displayName():
        return "Virus Blocker Lite"

    # verify daemon is running
    def test_009_clamdIsRunning(self):
        """
        test_009_clamdIsRunning runs the check_clamd_ready function to 
        verify clamd is running and also that signatures are done downloading
        """
        result = global_functions.check_clamd_ready()
        assert (result)


test_registry.register_module("virus-blocker-lite", VirusBlockerLiteTests)
