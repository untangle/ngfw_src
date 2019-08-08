import datetime
import pytest

import runtests

# these need to be transformed into command-line switches as well
runtests.remote_control.external_interface = 1
runtests.remote_control.interface = 2

def pytest_addoption(parser):
    parser.addoption("--runtests-host", type=foo, default="192.168.122.122", help="client IP")
    parser.addoption("--skip-instantiated", type=bar, default=True, help="skip tests if app is already instantiated")

def foo(host):
    runtests.remote_control.client_ip = host
    return host

def bar(skip_instantiated):
    runtests.skip_instantiated = skip_instantiated in ['true', 'True', '1', 't', 'y', 'yes']
    return runtests.skip_instantiated

runtests.test_start_time = datetime.datetime.now()
