import pytest
from unittest import TestCase

import runtests
from tests.global_functions import uvmContext


class NGFWTestCase(TestCase):

    # overload the next 2 in subclasses if needed
    force_start = False

    _app = None
    default_policy_id = 1

    # once we switch to pytest this should be converted to a class variable
    @staticmethod
    def module_name():
        """Return module name; to be implemented in subclasses."""
        return "ad-blocker"

    @staticmethod
    def skip_instantiated():
        return getattr(runtests, 'skip_instantiated', True)

    @classmethod
    def get_app(cls):
        return uvmContext.appManager().appInstances(cls.module_name(),
                                                    cls.default_policy_id)["list"][0]

    @classmethod
    def get_app_id(cls):
        return cls.get_app().getAppSettings()["id"]

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
