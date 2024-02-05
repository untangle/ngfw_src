import pytest
import re

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control

@pytest.mark.setup_wizard
class SetupWizard(NGFWTestCase):

    not_an_app = True

    @staticmethod
    def module_name():
        return "setup-wizard"

    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    # Checks the oem url and license agreement url
    def test_020_about_license_agreement(self):
        oem_url = uvmContext.oemManager().getOemUrl()
        match = re.search('^.*edge.arista.com$', oem_url)
        assert(match)
        
        license_url = uvmContext.oemManager().getLicenseAgreementUrl();
        match = re.search('^.*edge.arista.com/legal$', license_url)
        assert(match)

test_registry.register_module("setup-wizard", SetupWizard)