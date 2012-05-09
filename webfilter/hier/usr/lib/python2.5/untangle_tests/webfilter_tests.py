import unittest
import time
import sys
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests.webfilter_base_tests import WebFilterBaseTests

#
# Just extends the web filter tests
#
class WebFilterTests(WebFilterBaseTests):

    @staticmethod
    def nodeName():
        return "untangle-node-webfilter"

    @staticmethod
    def vendorName():
        return "untangle"

