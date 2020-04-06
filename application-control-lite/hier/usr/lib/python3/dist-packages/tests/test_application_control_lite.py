"""application_control_lite tests"""
import time
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions


default_policy_id = 1
app = None


def nukepatterns(app):
    settings = app.getSettings()
    patterns = settings["patterns"]
    patterns["list"] = [];
    settings["patterns"] = patterns
    app.setSettings(settings)


def addPatterns(app, definition, blocked=False, log=True, protocol="protocol", description="description", category="category"):
    newPatterns = { 
                "alert": False, 
                "blocked": blocked, 
                "category": category, 
                "definition": definition, 
                "description": description, 
                "javaClass": "com.untangle.app.application_control_lite.ApplicationControlLitePattern", 
                "log": log, 
                "protocol": protocol, 
                "quality": ""
    }

    settings = app.getSettings()
    patterns = settings["patterns"]
    patterns["list"].append(newPatterns)
    settings["patterns"] = patterns
    app.setSettings(settings)

@pytest.mark.application_control_lite
class ApplicationControlLiteTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        global app
        app = ApplicationControlLiteTests._app
        return "application-control-lite"

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))
  
    def test_020_testHttpPatternLog(self):
        nukepatterns(self._app)
        
        addPatterns(self._app, definition="http/(0\\.9|1\\.0|1\\.1) [1-5][0-9][0-9] [\\x09-\\x0d -~]*(connection:|content-type:|content-length:|date:)|post [\\x09-\\x0d -~]* http/[01]\\.[019]",
                    protocol="HTTP", 
                    category="Web", 
                    description="HyperText Transfer Protocol")
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        nukepatterns(self._app)
        assert (result == 0)
        time.sleep(3);

        events = global_functions.get_events('Application Control Lite','All Events',None,5)
        assert(events != None)
        print(events)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            'application_control_lite_protocol', 'HTTP',
                                            'application_control_lite_blocked', False )
        assert( found )

    def test_030_testHttpPatternBlocked(self):
        nukepatterns(self._app)
        
        addPatterns(self._app, definition="http/(0\\.9|1\\.0|1\\.1) [1-5][0-9][0-9] [\\x09-\\x0d -~]*(connection:|content-type:|content-length:|date:)|post [\\x09-\\x0d -~]* http/[01]\\.[019]",
                    protocol="HTTP",
                    blocked=True,
                    category="Web", 
                    description="HyperText Transfer Protocol")
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        assert (result != 0)
        time.sleep(3);

        events = global_functions.get_events('Application Control Lite','All Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            'application_control_lite_protocol', 'HTTP',
                                            'application_control_lite_blocked', True )
        assert( found )

    def test_040_testFtpPatternBlock(self):
        nukepatterns(self._app)
        
        addPatterns(self._app, definition="^220[\x09-\x0d -~]*ftp",
                    protocol="FTP", 
                    blocked=True,
                    category="Web", 
                    description="File Transfer Protocol")
        result = remote_control.run_command("wget -q -O /dev/null -4 -t 2 ftp://test.untangle.com")
        assert (result != 0)
        time.sleep(3);

        events = global_functions.get_events('Application Control Lite','All Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            'application_control_lite_protocol', 'FTP',
                                            'application_control_lite_blocked', True )
        assert( found )

    def test_050_testDnsUdpPatternLog(self):
        nukepatterns(self._app)
        
        addPatterns(self._app, definition="^.?.?.?.?[\x01\x02].?.?.?.?.?.?[\x01-?][a-z0-9][\x01-?a-z]*[\x02-\x06][a-z][a-z][fglmoprstuvz]?[aeop]?(um)?[\x01-\x10\x1c]",
                    protocol="DNS", 
                    blocked=False,
                    category="Web", 
                    description="Domain Name System")
        result = remote_control.run_command("host -R 1 www.google.com 8.8.8.8")
        assert (result == 0)
        time.sleep(3);

        events = global_functions.get_events('Application Control Lite','All Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            'application_control_lite_protocol', 'DNS',
                                            'application_control_lite_blocked', False )
        assert( found )

    def test_060_testDnsUdpPatternBlock(self):
        nukepatterns(self._app)
        
        addPatterns(self._app, definition="^.?.?.?.?[\x01\x02].?.?.?.?.?.?[\x01-?][a-z0-9][\x01-?a-z]*[\x02-\x06][a-z][a-z][fglmoprstuvz]?[aeop]?(um)?[\x01-\x10\x1c]",
                    protocol="DNS", 
                    blocked=True,
                    category="Web", 
                    description="Domain Name System")
        result = remote_control.run_command("host -R 1 www.google.com 8.8.8.8")
        assert (result != 0)
        time.sleep(3);

        events = global_functions.get_events('Application Control Lite','All Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.client_ip,
                                            'application_control_lite_protocol', 'DNS',
                                            'application_control_lite_blocked', True )
        assert( found )


test_registry.register_module("application-control-lite", ApplicationControlLiteTests)
