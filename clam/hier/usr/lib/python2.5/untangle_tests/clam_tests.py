import unittest
import time
import sys
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests.virus_tests import VirusTests

#
# Just extends the virus base tests
#
class ClamTests(VirusTests):

    @staticmethod
    def nodeName():
        return "untangle-node-clam"

    @staticmethod
    def vendorName():
        return "Clam"
