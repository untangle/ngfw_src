import datetime
import pytest

import runtests


def pytest_addoption(parser):
    parser.addoption("--runtests-host", type=foo, default="192.168.122.122", help="client IP")
def foo(host):
    runtests.remote_control.client_ip = host
    return host

runtests.test_start_time = datetime.datetime.now()
