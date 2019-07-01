import time
import pytest
from unittest import TestCase

import runtests
from tests.global_functions import uvmContext


class NGFWTestCase(TestCase):

    # overload the next 2 in subclasses if needed
    force_start = False
    wait_for_daemon_ready = False

    _app = None
    _appSettings = None
    default_policy_id = 1

    # once we switch to pytest this should be converted to a class variable
    @staticmethod
    def module_name():
        """Return module name; to be implemented in subclasses."""

    @staticmethod
    def skip_instantiated():
        return getattr(runtests, 'skip_instantiated', True)

    @classmethod
    def get_app(cls):
        return cls._app

    @classmethod
    def get_app_settings(cls):
        return cls.get_app().getAppSettings()

    @classmethod
    def get_app_id(cls):
        return cls.get_app_settings()["id"]

    @classmethod
    def do_wait_for_daemon_ready(cls):
        time.sleep(1)
        while True:
            if cls._app.getAppStatus()["daemonReady"] is True:
                break
            time.sleep(1)

    @classmethod
    def initial_setup(cls, unused=None):
        name = cls.module_name()
        if cls._app or uvmContext.appManager().isInstantiated(name):
            if cls.skip_instantiated():
                pytest.skip('app %s already instantiated' % cls.module_name())
            else:
                cls.final_tear_down()
        cls._app = uvmContext.appManager().instantiate(name, cls.default_policy_id)
        if cls.force_start:
            cls._app.start()
            if cls.wait_for_daemon_ready:
                cls.do_wait_for_daemon_ready()
        cls._appSettings = cls._app.getSettings()

    @classmethod
    def final_tear_down(cls, unused=None):
        name = cls.module_name()
        if cls._app or uvmContext.appManager().isInstantiated(name):
            uvmContext.appManager().destroy(cls.get_app_id())
        cls._app = None

    @classmethod
    def setup_class(cls):
        cls.initial_setup()

    @classmethod
    def teardown_class(cls):
        cls.final_tear_down()
