"""threat_prevention tests"""
import re
import subprocess

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
app = None

class WebrootTests(NGFWTestCase):

    systemd_service_filename = "/usr/lib/systemd/system/untangle-bctid.service"

    not_an_app = True

    @staticmethod
    def module_name():
        return "webroot"

    @staticmethod
    def displayName():
        return "Webroot"

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_020_check_max_connections(self):
        """
        Verify that bctid is running with the max proccesses
        """
        # Start web-filter to trigger bctid start
        app_webfilter = WebrootTests.get_app("web-filter")

        bctid_exec_start = None
        with open(WebrootTests.systemd_service_filename, "r") as service:
            for line in service:
                if line.startswith("ExecStart="):
                    print(line)
                    bctid_exec_start = line.split("=", 2)[1]
        assert bctid_exec_start is not None, "found bctid execstart"

        assert re.search(" -m \d+ ", bctid_exec_start), "bctid exectstart has max clients argument"

        process_result = subprocess.call(f"ps aux | grep '{bctid_exec_start}' >/dev/null 2>&1", shell=True)
        assert process_result == 0, "bctid running with exec start"

    @classmethod
    def final_extra_tear_down(cls):
        pass
        
test_registry.register_module("webroot", WebrootTests)
