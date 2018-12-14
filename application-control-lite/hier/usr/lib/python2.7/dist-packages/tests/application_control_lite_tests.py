import unittest2
import time
import sys
import datetime
import random
import string

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions

default_policy_id = 1
app = None

def nukepatterns():
    settings = app.getSettings()
    patterns = settings["patterns"]
    patterns["list"] = [];
    settings["patterns"] = patterns
    app.setSettings(settings)

def addPatterns(definition, blocked=False, log=True, protocol="protocol", description="description", category="category"):
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

class ApplicationControlLiteTests(unittest2.TestCase):

    @staticmethod
    def appName():
        return "application-control-lite"

    @staticmethod
    def initial_setup(self):
        global app
        if (uvmContext.appManager().isInstantiated(self.appName())):
            raise Exception('app %s already instantiated' % self.appName())
        app = uvmContext.appManager().instantiate(self.appName(), default_policy_id)
        app.start() # must be called since ad blocker doesn't auto-start

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.appName()))
  
    def test_020_testHttpPatternLog(self):
        nukepatterns()
        
        addPatterns(definition="http/(0\\.9|1\\.0|1\\.1) [1-5][0-9][0-9] [\\x09-\\x0d -~]*(connection:|content-type:|content-length:|date:)|post [\\x09-\\x0d -~]* http/[01]\\.[019]",
                    protocol="HTTP", 
                    category="Web", 
                    description="HyperText Transfer Protocol")
        result = remote_control.run_command("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        nukepatterns()
        assert (result == 0)
        time.sleep(3);

        events = global_functions.get_events('Application Control Lite','All Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'c_client_addr', remote_control.clientIP,
                                            'application_control_lite_protocol', 'HTTP',
                                            'application_control_lite_blocked', False )
        assert( found )

    def test_030_testHttpPatternBlocked(self):
        nukepatterns()
        
        addPatterns(definition="http/(0\\.9|1\\.0|1\\.1) [1-5][0-9][0-9] [\\x09-\\x0d -~]*(connection:|content-type:|content-length:|date:)|post [\\x09-\\x0d -~]* http/[01]\\.[019]",
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
                                            'c_client_addr', remote_control.clientIP,
                                            'application_control_lite_protocol', 'HTTP',
                                            'application_control_lite_blocked', True )
        assert( found )

    def test_040_testFtpPatternBlock(self):
        nukepatterns()
        
        addPatterns(definition="^220[\x09-\x0d -~]*ftp",
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
                                            'c_client_addr', remote_control.clientIP,
                                            'application_control_lite_protocol', 'FTP',
                                            'application_control_lite_blocked', True )
        assert( found )

    def test_050_testDnsUdpPatternLog(self):
        nukepatterns()
        
        addPatterns(definition="^.?.?.?.?[\x01\x02].?.?.?.?.?.?[\x01-?][a-z0-9][\x01-?a-z]*[\x02-\x06][a-z][a-z][fglmoprstuvz]?[aeop]?(um)?[\x01-\x10\x1c]",
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
                                            'c_client_addr', remote_control.clientIP,
                                            'application_control_lite_protocol', 'DNS',
                                            'application_control_lite_blocked', False )
        assert( found )

    def test_060_testDnsUdpPatternBlock(self):
        nukepatterns()
        
        addPatterns(definition="^.?.?.?.?[\x01\x02].?.?.?.?.?.?[\x01-?][a-z0-9][\x01-?a-z]*[\x02-\x06][a-z][a-z][fglmoprstuvz]?[aeop]?(um)?[\x01-\x10\x1c]",
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
                                            'c_client_addr', remote_control.clientIP,
                                            'application_control_lite_protocol', 'DNS',
                                            'application_control_lite_blocked', True )
        assert( found )

    @staticmethod
    def final_tear_down(self):
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        

test_registry.registerApp("application-control-lite", ApplicationControlLiteTests)
