import os
import sys
import subprocess
import unittest
from unittest.mock import patch

import pytest

from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests.test_registry as test_registry


UPGRADE_SCRIPT = os.path.abspath("/usr/share/untangle/bin/ut-upgrade.py")


@pytest.mark.upgrade
class UpgradeTests(NGFWTestCase):

    not_an_app = True

    @staticmethod
    def module_name():
        return "upgrade"

    @staticmethod
    def vendorName():
        return "Untangle"
    
    def test_010_disk_health_check_on_upgrade(self):
        """ Check disk health check error, pass and fail result impact on upgrade """

        # We don't want to upgrade VM from test, so skip test if upgrade available.
        available = global_functions.uvmContext.systemManager().upgradesAvailable()
        if available:
            raise unittest.SkipTest('Upgrade available, skipping test')   

        # Error condition check. 
        # By default VMWare Boxes don't support SMART so check_smart_health will return error result
        # For disk health check any error will result in proceeding upgrade
        result = subprocess.run( [sys.executable, UPGRADE_SCRIPT], capture_output=True, text=True )

        assert (result.returncode == 0)
        assert ("done" in result.stdout)

        # pass condition check
        env = {"MOCK_TEST_PASS": "1"}
        env.update(os.environ)
        result = subprocess.run( [sys.executable, UPGRADE_SCRIPT], capture_output=True, text=True, env=env )

        assert (result.returncode == 0)
        assert ("done" in result.stdout)

        # fail condition check
        env = {"MOCK_TEST_FAIL": "1"}
        env.update(os.environ)
        result = subprocess.run( [sys.executable, UPGRADE_SCRIPT], capture_output=True, text=True, env=env )

        assert (result.returncode == 1)
        assert ("Aborting Upgrade" in result.stdout)


    def test_015_skip_disk_health_check_on_upgrade(self):
        """ Check if disk check is skipped if user want's to force upgrade on disk check failure """

        # We don't want to upgrade VM from test, so skip test if upgrade available.
        available = global_functions.uvmContext.systemManager().upgradesAvailable()
        if available:
            raise unittest.SkipTest('Upgrade available, skipping test')

        # Set skipDiskCheck flag True
        global_functions.uvmContext.systemManager().setSkipDiskCheck(True)
        result = subprocess.run( [sys.executable, UPGRADE_SCRIPT], capture_output=True, text=True )

        assert (result.returncode == 0)
        assert ("Skipping drive health checks" in result.stdout)

        # Set skipDiskCheck flag False
        global_functions.uvmContext.systemManager().setSkipDiskCheck(False)
        result = subprocess.run( [sys.executable, UPGRADE_SCRIPT], capture_output=True, text=True )

        assert ("disk health status" in result.stdout)

test_registry.register_module("upgrade", UpgradeTests)