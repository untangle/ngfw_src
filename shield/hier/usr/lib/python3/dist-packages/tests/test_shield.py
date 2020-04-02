"""shield tests"""
import time
import datetime

import unittest
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm


@pytest.mark.shield
class ShieldTests(NGFWTestCase):

    do_not_install_app = True
    do_not_remove_app = True

    @staticmethod
    def module_name():
        # cheap trick to force class variable _app into global namespace as app
        global app
        app = ShieldTests._app
        return "shield"

    @classmethod
    def initial_extra_setup(cls):
        cls._default_enabled = cls._app.getSettings()['shieldEnabled']

    @classmethod
    def final_extra_tear_down(cls):
        if getattr(cls, '_default_enabled'):
            settings = cls._app.getSettings()
            settings['shieldEnabled'] = cls._default_enabled
            cls._app.setSettings(settings)

        # sleep so the reputation goes down so it will not interfere with any future tests
        time.sleep(3)

    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_shieldDetectsNmap(self):
        # enable logging of blocked settings
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['logBlockedSessions'] = True
        netsettings['logBypassedSessions'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)

        settings = app.getSettings()
        settings['shieldEnabled'] = True
        app.setSettings(settings)

        start_time = datetime.datetime.now()
        result = remote_control.run_command("nmap -PN -sT -T5 --min-parallelism 15 -p10000-11000 1.2.3.4 2>&1 >/dev/null")
        assert (result == 0)

        events = global_functions.get_events('Shield','Blocked Session Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                               'c_client_addr', remote_control.client_ip,
                                               min_date=start_time)
        assert( found )

    def test_021_shieldOffNmap(self):
        # enable logging of blocked settings
        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['logBlockedSessions'] = True
        netsettings['logBypassedSessions'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)

        settings = app.getSettings()
        settings['shieldEnabled'] = False
        app.setSettings(settings)

        start_time = datetime.datetime.now()
        result = remote_control.run_command("nmap -PN -sT -T5 --min-parallelism 15 -p10000-10100 1.2.3.5 2>&1 >/dev/null")
        assert (result == 0)

        events = global_functions.get_events('Shield','Blocked Session Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                               'c_client_addr', remote_control.client_ip,
                                               's_server_addr', '1.2.3.5',
                                               min_date=start_time)
        assert( not found )


test_registry.register_module("shield", ShieldTests)
