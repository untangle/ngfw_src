"""smtp tests"""

import unittest
import pytest


from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm


@pytest.mark.smtp_casing
class SmtpTests(NGFWTestCase):

    do_not_install_app = True
    do_not_remove_app = True
    not_an_app = True

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = SmtpTests._app
        return "smtp"

    # verify client is online
    def test_010_runTests(self):
        self.module_name()

test_registry.register_module("smtp-casing", SmtpTests)
