import pytest
import re

from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control

@pytest.mark.about_tests
class AboutTests(NGFWTestCase):

    not_an_app= True

    @staticmethod
    def module_name():
        return "about-tests"

    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    # Tests some information in Config > About > Server
    def test_020_about_server_info(self):
        # Some of the info in the Server about page is constructed from Store URL
        store_url = global_functions.uvmContext.getStoreUrl()
        match = re.search(r'\w*arista\w*', store_url)
        assert(match)
        
        uid =  global_functions.uvmContext.getServerUID()
        match = re.search(r'\w{4}-\w{4}-\w{4}.\w{4}', uid)
        assert(match)

        kernel = global_functions.uvmContext.adminManager().getKernelVersion()
        match = re.search(r'\d.*', kernel)
        assert(match)

        history = global_functions.uvmContext.adminManager().getModificationState()
        match = re.search(r'(yes|no) \(\d+\)', history)
        assert(match)

        reboot_count = global_functions.uvmContext.adminManager().getRebootCount()
        match = re.search(r'\d{1,2}', reboot_count)
        assert(match)

        num_hosts = str(global_functions.uvmContext.hostTable().getCurrentActiveSize())
        match = re.search(r'\d{1,2}', num_hosts)
        assert(match)

        max_num_hosts = str(global_functions.uvmContext.hostTable().getMaxActiveSize())
        match = re.search(r'\d{1,2}', max_num_hosts)
        assert(match)

test_registry.register_module("about-tests", AboutTests)