"""web_monitor tests"""

import unittest
import pytest
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

from .test_web_filter_base import WebFilterBaseTests

default_policy_id = 1
app = None

@pytest.mark.web_monitor
class WebMonitorTests(WebFilterBaseTests):

    @staticmethod
    def module_name():
        return "web-monitor"

    @staticmethod
    def shortAppName():
        return "web-monitor"

    @staticmethod
    def eventAppName():
        return "web_monitor"

    @staticmethod
    def displayName():
        return "Web Monitor"

    @staticmethod
    def initial_setup(self):
        global app
        if (uvmContext.appManager().isInstantiated(self.module_name())):
            raise Exception('app %s already instantiated' % self.module_name())
        app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)
        appmetrics = uvmContext.metricManager().getMetrics(app.getAppSettings()["id"])
        self.app = app

    def test_016_flag_url(self):
        """verify basic URL blocking the the url block list"""
        pre_events_scan = global_functions.get_app_metric_value(self.app, "scan")
        pre_events_pass = global_functions.get_app_metric_value(self.app, "pass")
        pre_events_block = global_functions.get_app_metric_value(self.app, "block")
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert ( result == 0 )
        # verify the faceplate counters have incremented.
        post_events_scan = global_functions.get_app_metric_value(self.app, "scan")
        post_events_pass = global_functions.get_app_metric_value(self.app, "pass")
        post_events_block = global_functions.get_app_metric_value(self.app, "block")
        assert(pre_events_scan < post_events_scan)
        assert(pre_events_pass < post_events_pass)
        assert(pre_events_block == post_events_block)

    @staticmethod
    def final_tear_down(self):
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None

test_registry.register_module("web-monitor", WebMonitorTests)
