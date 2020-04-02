"""web_monitor tests"""
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

from .test_web_filter_base import WebFilterBaseTests

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

    def test_016_flag_url(self):
        """verify basic URL blocking the the url block list"""
        pre_events_scan = global_functions.get_app_metric_value(self._app, "scan")
        pre_events_pass = global_functions.get_app_metric_value(self._app, "pass")
        pre_events_block = global_functions.get_app_metric_value(self._app, "block")
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert ( result == 0 )
        # verify the faceplate counters have incremented.
        post_events_scan = global_functions.get_app_metric_value(self._app, "scan")
        post_events_pass = global_functions.get_app_metric_value(self._app, "pass")
        post_events_block = global_functions.get_app_metric_value(self._app, "block")
        assert(pre_events_scan < post_events_scan)
        assert(pre_events_pass < post_events_pass)
        assert(pre_events_block == post_events_block)


test_registry.register_module("web-monitor", WebMonitorTests)
