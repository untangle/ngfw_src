"""live_support tests"""

import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry

@pytest.mark.live_support
class LiveSupportTests(NGFWTestCase):

    no_settings = True

    @staticmethod
    def module_name():
        global app
        app = LiveSupportTests._app
        return "live-support"

    @staticmethod
    def vendorName():
        return "Untangle"

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

test_registry.register_module("live-support", LiveSupportTests)

