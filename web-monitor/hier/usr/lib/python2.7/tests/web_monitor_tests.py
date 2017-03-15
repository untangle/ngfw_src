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
from tests.web_filter_base_tests import WebFilterBaseTests
import remote_control
import test_registry
import global_functions
import pprint

defaultRackId = 1
node = None

class WebMonitorTests(WebFilterBaseTests):

    @staticmethod
    def nodeName():
        return "web-monitor"

    @staticmethod
    def shortNodeName():
        return "web-monitor"

    @staticmethod
    def eventNodeName():
        return "web_monitor"

    @staticmethod
    def displayName():
        return "Web Monitor"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.appManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.appManager().instantiate(self.nodeName(), defaultRackId)
        nodemetrics = uvmContext.metricManager().getMetrics(node.getAppSettings()["id"])
        self.node = node

    def test_016_flag_url(self):
        """verify basic URL blocking the the url block list"""
        pre_events_scan = global_functions.get_app_metric_value(self.node, "scan")
        pre_events_pass = global_functions.get_app_metric_value(self.node, "pass")
        pre_events_block = global_functions.get_app_metric_value(self.node, "block")
        self.block_url_list_add("test.untangle.com/test/testPage1.html")
        result = self.get_web_request_results(url="http://test.untangle.com/test/testPage1.html", expected="blockpage")
        self.block_url_list_clear()
        assert ( result == 0 )
        # verify the faceplate counters have incremented.
        post_events_scan = global_functions.get_app_metric_value(self.node, "scan")
        post_events_pass = global_functions.get_app_metric_value(self.node, "pass")
        post_events_block = global_functions.get_app_metric_value(self.node, "block")
        assert(pre_events_scan < post_events_scan)
        assert(pre_events_pass < post_events_pass)
        assert(pre_events_block == post_events_block)

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.appManager().destroy( node.getAppSettings()["id"] )
            node = None

test_registry.registerNode("web-monitor", WebMonitorTests)
