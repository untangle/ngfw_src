import time
import pytest
from unittest import TestCase

import runtests
import tests.global_functions
from tests.global_functions import uvmContext


class NGFWTestCase(TestCase):

    # overload the next 4 in subclasses if needed
    force_start = False
    do_not_install_app = False
    do_not_remove_app = False
    wait_for_daemon_ready = False
    no_settings = False
    not_an_app = False

    _app = None
    _appSettings = None
    default_policy_id = 1

    apps = {}

    # Run positions =
    # 1 first
    # 2 Normal
    # 3 after all others (like peformance)
    run_order=2

    # once we switch to pytest this should be converted to a class variable
    @staticmethod
    def module_name():
        """Return module name; to be implemented in subclasses."""

    @staticmethod
    def skip_instantiated():
        return getattr(runtests, 'skip_instantiated', True)

    @classmethod
    def get_app(cls, name=None):
        """
        Return app by name
        """
        if name is None:
            # Return this tests primary app
            return cls._app
        elif name in cls.apps:
            # We've instantiated a secondary app and it exists
            return cls.apps[name]
        else:
            # Instantiated a secondary app
            if (uvmContext.appManager().isInstantiated(name)):
                if cls.skip_instantiated():
                    pytest.skip( f'app {name} already instantiated')
                else:
                    app = uvmContext.appManager().app(name)
            else:
                app = uvmContext.appManager().instantiate(name, cls.default_policy_id)
            app.start()
            cls.apps[name] = app
            return app

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
    def initial_extra_setup(cls):
        """To be implemented in subclasses."""

    @classmethod
    def initial_setup(cls, unused=None):
        global uvmContext

        # Re-obtain uvm context reference in case another test restarted it
        uvmContext = tests.global_functions.uvmContext

        cls._orig_netsettings = uvmContext.networkManager().getNetworkSettings()

        if not cls.not_an_app:
            name = cls.module_name()
            print("initial_setup for app %s" % name)
            if cls._app or uvmContext.appManager().isInstantiated(name):
                if cls.skip_instantiated() and name not in ["reports","shield"]:
                    pytest.skip('app %s already instantiated' % cls.module_name())
                else:
                    cls._app = uvmContext.appManager().app(name)
                    if not cls.do_not_install_app:
                        # delete and install
                        cls.final_tear_down()
                        cls._app = uvmContext.appManager().instantiate(name, cls.default_policy_id)
            else:
                print("starting %s" % (name,))
                cls._app = uvmContext.appManager().instantiate(name, cls.default_policy_id)

            if cls.force_start:
                cls._app.start()
                if cls.wait_for_daemon_ready:
                    cls.do_wait_for_daemon_ready()

            if not cls.no_settings:
                cls._appSettings = cls._app.getSettings()

        cls.initial_extra_setup()

    @classmethod
    def final_extra_tear_down(cls):
        """To be implemented in subclasses."""

    @classmethod
    def final_tear_down(cls, unused=None):
        global uvmContext
        # Re-obtain uvm context reference in case another test restarted it
        uvmContext = tests.global_functions.uvmContext

        uvmContext.networkManager().setNetworkSettings(cls._orig_netsettings)

        # Shut down secondary apps
        for app in cls.apps.values():
            app.stop()
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
        cls.apps.clear()

        if not cls.do_not_remove_app:
            name = cls.module_name()
            if cls._app or uvmContext.appManager().isInstantiated(name):
                uvmContext.appManager().destroy(cls.get_app_id())
            cls._app = None
        
        cls.final_extra_tear_down()

    @classmethod
    def setup_class(cls):
        cls.initial_setup()

    @classmethod
    def teardown_class(cls):
        cls.final_tear_down()
