import unittest2
import time
import sys
import datetime
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

class WebCacheTests(unittest2.TestCase):

    @staticmethod
    def appName():
        return "web-cache"

    @staticmethod
    def initial_setup(self):
        global app
        if (uvmContext.appManager().isInstantiated(self.appName())):
            raise Exception('app %s already instantiated' % self.appName())
        app = uvmContext.appManager().instantiate(self.appName(), default_policy_id)
        app.start() # must be called since web cache doesn't auto-start

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.appName()))

    def test_020_testBasicWebCache(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        pre_events_hit = global_functions.get_app_metric_value(app,"hit")

        app.clearSquidCache()
        for x in range(0, 10):
            result = remote_control.run_command("wget -q -O /dev/null -4 -t 2 --timeout=5 http://test.untangle.com/")
            time.sleep(1)
        assert (result == 0)
        time.sleep(65) # summary-events only written every 60 seconds

        events = global_functions.get_events('Web Cache','Web Cache Events',None,1)
        assert(events != None)
        # verify at least one hit
        assert(events['list'][0])
        assert(events['list'][0]['hits'])

        # Check to see if the faceplate counters have incremented. 
        post_events_hit = global_functions.get_app_metric_value(app,"hit")

        assert(pre_events_hit < post_events_hit)

    @staticmethod
    def final_tear_down(self):
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        
test_registry.registerApp("web-cache", WebCacheTests)
