"""tunnel_vpn tests"""
import time
import subprocess

import unittest
import pytest
from tests.global_functions import uvmContext

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import runtests.overrides as overrides

default_policy_id = 1
app = None
VPN_TUNNEL_URI = overrides.get( "VPN_TUNNEL_URI", default="http://10.112.56.29/openvpn-ats-test-tunnelvpn-config.zip")
TUNNEL_VPN_ISOLATED_CLIENT = overrides.get( "TUNNEL_VPN_ISOLATED_CLIENT", default="192.168.72.22")

def create_tunnel_rule(vpn_enabled=True,vpn_ipv6=True,rule_id=50,vpn_tunnel_id=200):
    return {
            "conditions": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "description": "Route all traffic over any available Tunnel.",
            "enabled": vpn_enabled,
            "ipv6Enabled": vpn_ipv6,
            "javaClass": "com.untangle.app.tunnel_vpn.TunnelVpnRule",
            "ruleId": rule_id,
            "tunnelId": vpn_tunnel_id
    }

def create_tunnel_profile(vpn_enabled=True,provider="tunnel-Arista",vpn_tunnel_id=200):
    return {
            "allTraffic": False,
            "enabled": vpn_enabled,
            "javaClass": "com.untangle.app.tunnel_vpn.TunnelVpnTunnelSettings",
            "name": "tunnel-Arista",
            "provider": "Arista",
            "tags": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "tunnelId": vpn_tunnel_id
    }


@pytest.mark.tunnel_vpn
class TunnelVpnTests(NGFWTestCase):

    force_start = True
    
    @staticmethod
    def module_name():
        return "tunnel-vpn"

    @staticmethod
    def vendorName():
        return "Untangle"

    # verify client is online
    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_create_vpn_tunnel(self):
        currentWanIP = global_functions.get_public_ip_address()
        if (currentWanIP == ""):
            raise unittest.SkipTest("Unable to get WAN IP")
        print("Original WAN IP: " + str(currentWanIP))

        result = subprocess.call(global_functions.build_wget_command(log_file="/dev/null", output_file="/tmp/config.zip",uri=VPN_TUNNEL_URI), shell=True)
        if (result != 0):
            raise unittest.SkipTest("Unable to download VPN file: " + VPN_TUNNEL_URI)
        self._app.importTunnelConfig("/tmp/config.zip", "Untangle", 200)

        appData = self._app.getSettings()
        appData['rules']['list'].append(create_tunnel_rule())
        appData['tunnels']['list'].append(create_tunnel_profile())
        self._app.setSettings(appData)

        # wait for vpn tunnel to form
        timeout = 60
        connected = False
        while (not connected and timeout > 0):
            newWanIP = global_functions.get_public_ip_address()
            if (currentWanIP != newWanIP and newWanIP != ""):
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                connected = True
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
            else:
                time.sleep(1)
                timeout-=1

        # remove the rule to see if traffic is sent through WAN by default
        appData['rules']['list'][:] = []
        self._app.setSettings(appData)
        time.sleep(5)
        newWanIP2 = global_functions.get_public_ip_address()

        # remove the added tunnel
        appData['tunnels']['list'][:] = []
        self._app.setSettings(appData)

        # If VPN tunnel has failed to connect, fail the test,
        assert(connected)
        assert(connectStatus == "CONNECTED")
        # If the VPN tunnel is routed without rules, fail the test
        assert(currentWanIP == newWanIP2)

    def test_030_create_vpn_any_tunnel(self):
        currentWanIP = global_functions.get_public_ip_address()
        if (currentWanIP == ""):
            raise unittest.SkipTest("Unable to get WAN IP")
        print("Original WAN IP: " + str(currentWanIP))
        result = subprocess.call(global_functions.build_wget_command(log_file="/dev/null", output_file="/tmp/config.zip",uri=VPN_TUNNEL_URI), shell=True)
        if (result != 0):
            raise unittest.SkipTest("Unable to download VPN file: " + VPN_TUNNEL_URI)
        self._app.importTunnelConfig("/tmp/config.zip", "Untangle", 200)

        appData = self._app.getSettings()
        appData['rules']['list'].append(create_tunnel_rule(vpn_tunnel_id=-1))
        appData['tunnels']['list'].append(create_tunnel_profile())
        self._app.setSettings(appData)

        # wait for vpn tunnel to form
        timeout = 60
        connected = False
        pingPcLanResult = ""
        while (not connected and timeout > 0):
            newWanIP = global_functions.get_public_ip_address()
            if (currentWanIP != newWanIP):
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                connected = True
                listOfConnections = self._app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                pingPcLanResult = remote_control.run_command("ping -c 1 %s" % TUNNEL_VPN_ISOLATED_CLIENT)
            else:
                time.sleep(1)
                timeout-=1

        # remove the added tunnel
        appData['rules']['list'][:] = []
        appData['tunnels']['list'][:] = []
        self._app.setSettings(appData)

        # If VPN tunnel has failed to connect, fail the test,
        assert(connected)
        assert(connectStatus == "CONNECTED")
        assert(pingPcLanResult == 0)


test_registry.register_module("tunnel-vpn", TunnelVpnTests)
