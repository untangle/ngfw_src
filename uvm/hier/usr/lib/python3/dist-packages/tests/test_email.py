import pytest
import re
import copy
import time
from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests
import unittest
import runtests.test_registry as test_registry

@pytest.mark.email_tests
class EmailTests(NGFWTestCase):

    not_an_app= True

    @staticmethod
    def module_name():
        return "email-tests"

    @pytest.mark.slow
    def test_010_mail_send_method_modes(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        origMailsettings = global_functions.uvmContext.mailSender().getSettings()
        #Default sendMethod Mode is DIRECT
        assert (origMailsettings['sendMethod'] == 'DIRECT')

        #Upgrade scenario simulating RELAY method 
        newMailsettings = copy.deepcopy(origMailsettings)
        newMailsettings['sendMethod'] = 'RELAY'

        #Updating existing sendMethod mode to RELAY
        global_functions.uvmContext.mailSender().setSettings(newMailsettings)
        time.sleep(10) # give it time for exim to restart

        #Upgrade method scenario is simulated, sendMethod mode set to RELAY
        updatedMailSettings = global_functions.uvmContext.mailSender().getSettings()
        assert (updatedMailSettings['sendMethod'] == 'RELAY')

        #Restart UVM, this should set the sendMethod mode to DIRECT
        uvmContext = global_functions.restart_uvm()
        restartedMailSettings = uvmContext.mailSender().getSettings()
        assert (restartedMailSettings['sendMethod'] == 'DIRECT')

test_registry.register_module("email-tests", EmailTests)