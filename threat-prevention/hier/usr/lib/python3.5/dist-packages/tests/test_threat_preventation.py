"""threat_prevention tests"""
import datetime
import pytest
import sys

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
app = None

@pytest.mark.threat_prevention
class ThreatpreventionTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = ThreatpreventionTests._app
        return "threat-prevention"

    @staticmethod
    def eventAppName():
        return "web_filter"

    @staticmethod
    def displayName():
        return "Web Filter"

    @classmethod
    def initial_extra_setup(cls):
        global appData

        appData = cls._app.getSettings()
       
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)
            
    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_basic_block(self):
        eventTime = datetime.datetime.now()
        result = remote_control.run_command("wget -q -4 -t 2 -O - http://marbling.pe.kr  2>&1 | grep -q blocked")
        assert (result == 0)
        events = global_functions.get_events(self.displayName(),'Blocked Web Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            "host","marbling.pe.kr",
                                            self.eventAppName() + '_blocked', True,
                                            self.eventAppName() + '_flagged', True )
        
    @classmethod
    def final_extra_tear_down(cls):
        pass
        
test_registry.register_module("threat-prevention", ThreatpreventionTests)
