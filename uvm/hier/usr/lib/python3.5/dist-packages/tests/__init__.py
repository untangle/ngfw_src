"""__init__"""
import tests.global_functions
import tests.remote_control
import tests.test_registry

from tests.environment_tests import TestEnvironmentTests
from tests.network_tests import NetworkTests
from tests.uvm_tests import UvmTests

import os, pkgutil
__all__ = list(module for _, module, _ in pkgutil.iter_modules([os.path.dirname(__file__)]))
