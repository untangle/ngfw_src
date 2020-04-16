"""wireguard-vpn tests"""
import time
import re
import subprocess
import base64
import runtests
import unittest
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions


@pytest.mark.wireguard-vpn
class WireguardVpnTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        return "wireguard-vpn"

    @staticmethod
    def appWebName():
        return "wireguard-vpn"

    @staticmethod
    def vendorName():
        return "Untangle"
        
    @classmethod
    def initial_extra_setup(cls):
        return True

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))


test_registry.register_module("wireguard-vpn", WireguardVpnTests)
