import unittest2
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from . import remote_control
from .global_functions import uvmContext

class TestEnvironmentTests(unittest2.TestCase):

    # verify connectivity to untangle-vm
    def test_01_uvm_connectivity(self):
        global uvmContext
        try:
            version = uvmContext.version()
        except JSONRPCException as e:
            raise AssertionError("Failed to connect to untangle-vm")

    # verify reports is installed (needed for event log tests)
    def test_02_reports_is_installed(self):
        global uvmContext
        if not uvmContext.appManager().isInstantiated('reports'):
            uvmContext.appManager().instantiate('reports', None)
        assert (uvmContext.appManager().isInstantiated('reports'))

    # verify reports flush events works
    def test_03_reports_flush_events(self):
        reports = uvmContext.appManager().app("reports")
        assert (reports != None)
        reports.flushEvents()

    # verify connectivity to client
    def test_10_client_connectivity(self):
        assert ( remote_control.run_command("/bin/true") == 0 )

    # verify client can exec commands and return code
    def test_11_client_shell_return_code(self):
        assert ( remote_control.run_command("/bin/false") == 1 )

    # verify client can exec commands and return code
    def test_12_client_shell_output(self):
        result = remote_control.run_command("echo yay", stdout=True)
        assert (result == "yay")

    # verify client has necessary tools
    def test_13_client_has_necessary_tools(self):
        # to configure client:
        # https://test.untangle.com/test/setup_testshell.sh
        assert ( remote_control.run_command("which wget") == 0 )
        assert ( remote_control.run_command("which curl") == 0 )
        assert ( remote_control.run_command("which netcat") == 0 )
        assert ( remote_control.run_command("which nmap") == 0 )
        assert ( remote_control.run_command("which python") == 0 )
        assert ( remote_control.run_command("which mime-construct") == 0 )
        assert ( remote_control.run_command("which pidof") == 0 )
        assert ( remote_control.run_command("which host") == 0 )
        assert ( remote_control.run_command("which upnpc") == 0 )
        assert ( remote_control.run_command("which traceroute") == 0 )
        # check for netcat options
        assert ( remote_control.run_command("netcat -h 2>&1 | grep -q '\-d\s'") == 0 )
        assert ( remote_control.run_command("netcat -h 2>&1 | grep -q '\-z\s'") == 0 )
        assert ( remote_control.run_command("netcat -h 2>&1 | grep -q '\-w\s'") == 0 )
        assert ( remote_control.run_command("netcat -h 2>&1 | grep -q '\-l\s'") == 0 )
        assert ( remote_control.run_command("netcat -h 2>&1 | grep -q '\-4\s'") == 0 )
        assert ( remote_control.run_command("netcat -h 2>&1 | grep -q '\-p\s'") == 0 )

    # verify client is online
    def test_14_client_is_online(self):
        assert ( remote_control.is_online() == 0 )

    # verify client can pass UDP
    def test_15_client_is_online_udp(self):
        timeout = 5
        result_cnn = 1
        result_google = 1
        while timeout > 0 and (result_cnn != 0 or result_google != 0):
            timeout -= 1
            result_cnn =  remote_control.run_command("host cnn.com 8.8.8.8")
            result_google = remote_control.run_command("host google.com 8.8.8.8")
        
        assert ( result_cnn == 0 )
        assert ( result_google == 0 )

    # verify client is online
    def test_16_client_not_running_openvpn(self):
        assert ( remote_control.run_command("pidof openvpn") != 0 )
