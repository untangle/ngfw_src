
"""dynamic block list tests"""
import unittest
import pytest
import time
import sys
import traceback
import socket
import subprocess
import copy

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm



@pytest.mark.dynamicList
class DynamicListTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = DynamicListTests._app
        return "dynamic-lists"

    @staticmethod
    def appWebName():
        return  "dynamic-lists"

    @staticmethod
    def vendorName():
        return "Untangle"

    @classmethod
    def initial_extra_setup(cls):
        global orig_dbl_settings
        dblSettings = cls._app.getSettings()
        orig_dbl_settings = copy.deepcopy(dblSettings)


    def check_ipset_exists(self, ipset_name):
        """Check if the given ipset exists."""
        try:
            result = subprocess.run(
                f"ipset list {ipset_name}",
                shell=True,
                check=False,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE
            )
            return result.returncode == 0
        except subprocess.CalledProcessError:
            return False


    def check_iptables_rule_exists(self, rule):
        """Check if a specific iptables rule exists."""
        try:
            result = subprocess.run(
                f"iptables -t filter -S | grep  'dynamic-block-list'",
                shell=True,
                check=False,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True
            )
            output_lines = result.stdout.strip().splitlines()
            if len(output_lines) == 4:
                return True
            else:
                return False
        except subprocess.CalledProcessError:
            return False

        # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_012_dbl_setup(self):
        """
        Verify dbl-setup script run at app startup
        This script runs when app starts and create parent ipset and iptables rule 
        """
        # Check if the ipset "dblsets" exists
        self.assertTrue(self.check_ipset_exists("dblsets"), "The ipset 'dblsets' does not exist.")
        
        # Check if the "dbl" chain exists in iptables
        self.assertTrue(self.check_iptables_rule_exists("dynamic-block-list"), "The iptables rule for dynamic-block-list does not exist.")

    
    def test_100_dbl_cleanup(self):
        """
        Test dbl-claenup script
        This script remove all the ipset and iptables rules applied if app is stopped.
        """
        #Disable the app and check dblsets, blocklists folder and iptable rules delete successfully 
        self._app.stop()
        
        # Check if the ipset "dblsets" does NOT exist
        self.assertFalse(self.check_ipset_exists("dblsets"), "The ipset 'dblsets' exists unexpectedly.")
        
        # Check if the "dbl" chain does NOT exist in iptables
        self.assertFalse(self.check_iptables_rule_exists("dynamic-block-list"), "The 'dbl' iptables chain exists unexpectedly.")
        


test_registry.register_module("dynamic-lists", DynamicListTests)
