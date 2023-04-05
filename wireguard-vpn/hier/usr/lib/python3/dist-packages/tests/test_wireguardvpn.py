"""wireguard-vpn tests"""
import time
import copy
import subprocess
import runtests
import unittest
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import runtests.overrides as overrides

import tests.test_network as test_network

default_policy_id = 1
appData = None
app = None
appWeb = None
wanIP = None

# Local configuration
WG_LOCAL = overrides.get("WG_LOCAL", default={
    "publicKey": "1YbeQWcyHrPnnUJhBKbxKt2ZUbr2I8EiuinG9cYqQmE=",
    "privateKey": "sICMfPW0s1m74egk3VS4BXe7mah3m5XF+gCN25B0Y2w=",
    "addressPool": "10.133.205.1/24"
})

# Remote server
WG_REMOTE = overrides.get("WG_REMOTE", default={
        "serverAddress": "10.113.150.117",
        "hostname": "untangle-ats-wireguard",
        "publicKey": "fupwK1yQLvtBOFpW8nHxjIYjSDAzkpCwYGYL2rS5xUU=",
        "endpointPort": 51820,
        "peerAddress": "172.31.53.1",
        "networks": "192.168.20.0/24",
        "lanAddress": "192.168.20.170"
})

def build_wireguard_tunnel(tunnel_enabled=True, remotePK=WG_REMOTE["publicKey"], remotePeer=WG_REMOTE["peerAddress"], description=WG_REMOTE["hostname"], endpointHostname=WG_REMOTE["serverAddress"], networks=WG_REMOTE["networks"]):
    return {
        "description": description,
        "enabled": tunnel_enabled,
        "endpointHostname": endpointHostname,
        "endpointDynamic": False,
        "endpointPort": 51820,
        "id": 1,
        "javaClass": "com.untangle.app.wireguard_vpn.WireGuardVpnTunnel",
        "networks": networks,
        "peerAddress": remotePeer,
        "pingConnectionEvents": True,
        "pingInterval": 60,
        "pingUnreachableEvents": False,
        "privateKey": "",
        "publicKey": WG_REMOTE["publicKey"]
    }

def wait_for_ping(target_IP="127.0.0.1",ping_result_expected=0):
    timeout = 60  # wait for up to one minute for the target ping result
    ping_result = False
    while timeout > 0:
        time.sleep(1)
        timeout -= 1
        result = subprocess.call(["ping","-W","5","-c","1",target_IP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (result == ping_result_expected):
            # target is reachable if succ
            ping_result = True
            break
    return ping_result

@pytest.mark.wireguardvpn
class WireGuardVpnTests(NGFWTestCase):

    force_start = True

    @staticmethod
    def module_name():
        return "wireguard-vpn"

    @staticmethod
    def appWebName():
        return "wireguard-vpn"

    @staticmethod
    def vendorName():
        return "Untangle"
        
    @classmethod
    def initial_extra_setup(cls):
        global appData, vpnHostResult, wanIP

        vpnHostResult = subprocess.call(["ping","-W","5","-c","1",WG_REMOTE["serverAddress"]],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        wanIP = uvmContext.networkManager().getFirstWanAddress()

    def test_010_client_is_online(self):
        """
        Verify LAN client online
        """
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        """
        Verify valid license
        """
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    @pytest.mark.failure_in_podman
    def test_020_create_wireguard_tunnel(self):
        """
        Set up WireGuard Tunnel and ping between hosts behind each end
        """
        if (vpnHostResult != 0):
            raise unittest.SkipTest("No paired VPN server available")

        #get and overwrite local service settings to match tunnel settings on the remote/static test box
        appData = self._app.getSettings()
        appData["publicKey"] = WG_LOCAL["publicKey"]
        appData["privateKey"] = WG_LOCAL["privateKey"]
        appData["addressPool"] = WG_LOCAL["addressPool"]

        #set new tunnel settings
        appData["tunnels"]["list"].append(build_wireguard_tunnel())
        self._app.setSettings(appData)

        assert True is global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard"), "wireguard interface and rule is running"

        result = wait_for_ping(WG_REMOTE["lanAddress"],0)
        assert result, "received ping from remote lan"

        assert True is global_functions.is_vpn_routing(route_table=f"wireguard", expected_route=WG_REMOTE["networks"].split(",")[0]), "wireguard routing on wireguard table"

    def test_021_no_static_route_conflict(self):
        """
        A static route matching one of the alllowed interfaces in the tunnel won't prevent wg from starting
        """
        if (vpnHostResult != 0):
            raise unittest.SkipTest("No paired VPN server available")
        
        #get and overwrite local service settings to match tunnel settings on the remote/static test box
        appData = self._app.getSettings()
        appData["publicKey"] = WG_LOCAL["publicKey"]
        appData["privateKey"] = WG_LOCAL["privateKey"]
        appData["addressPool"] = WG_LOCAL["addressPool"]

        #set new tunnel settings
        appData["tunnels"]["list"].append(build_wireguard_tunnel())

        self._app.setSettings(appData)

        original_network_settings = uvmContext.networkManager().getNetworkSettings()

        # deepcopy WG network settings for manipulation
        new_network_settings = copy.deepcopy( original_network_settings )

        # Set network settings back to normal
        uvmContext.networkManager().setNetworkSettings( new_network_settings )

        # Get first network from remote networks list
        network = WG_REMOTE["networks"].split(",")[0].split('/')
        new_network_settings["staticRoutes"]['list'].insert(0,test_network.create_route_rule(network[0], network[1], "127.0.0.1"))

        self._app.stop()
        self._app.start()

        assert True is global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard"), "wireguard interface and rule is running"

        # result = wait_for_ping(WG_REMOTE["lanAddress"],0)
        # assert result, "received ping from remote lan"

        assert True is global_functions.is_vpn_routing(route_table=f"wireguard", expected_route=WG_REMOTE["networks"].split(",")[0]), "wireguard routing on wireguard table"

        # Set network settings back to normal
        uvmContext.networkManager().setNetworkSettings( original_network_settings )


    def test_031_network_settings_and_default_wireguard_networks(self):
        """
        Test if changing the Network Settings LAN address properly updates the wireguard local networks
        """

        #if the configuration is Bridged, Local Networks are not imported into WireGuard's settings, so this test would fail. Skip it.
        if global_functions.is_bridged(wanIP):
            raise unittest.SkipTest("skipping on Bridged configuration, no Local Networks")
        
        # Address we want to test with
        testingAddressNet = "192.168.90"
        testingAddress = testingAddressNet + ".1"
        testingDhcpStart = testingAddressNet + ".100"
        testingDhcpEnd = testingAddressNet + ".200"

        # Pull out the current WG settings and the current Network Settings
        wgSettings = self._app.getSettings()
        origNetSettings = uvmContext.networkManager().getNetworkSettings()

        # deepcopy WG network settings for manipulation
        newNetSettings = copy.deepcopy( origNetSettings )

        # Verify the WG settings don't already have this address stored
        assert(testingAddressNet not in wgSettings['networks']['list'][0]['address'])

        # Find a DHCP LAN device and set it's address to the testing address
        for intf in newNetSettings['interfaces']['list']:
            if not intf['isWan']:
                intf['v4StaticAddress'] = testingAddress
                if intf['dhcpType'] == 'SERVER':
                    intf['dhcpRangeStart'] = testingDhcpStart
                    intf['dhcpRangeEnd'] = testingDhcpEnd
                break

        # Set the settings to new config
        uvmContext.networkManager().setNetworkSettings( newNetSettings )

        # Query the WG settings to assert if that address made it in
        newWGSettings = self._app.getSettings()

        # Test that new WG LAN matches network LAN
        assert(testingAddressNet in newWGSettings['networks']['list'][0]['address'])

        # Set network settings back to normal
        uvmContext.networkManager().setNetworkSettings( origNetSettings )

        # Get the WG settings again to verify
        wgSettings = self._app.getSettings()

        assert True is global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard"), "wireguard interface and rule is running"

        # Assert that old settings were set back properly proper now
        assert(testingAddressNet not in wgSettings['networks']['list'][0]['address'])

    def test_032_network_settings_and_custom_wireguard_networks(self):
        """
        Test if changing the Network Settings LAN address DOES NOT update custom local networks in the WG app
        """

        #if the configuration is Bridged, Local Networks are not imported into WireGuard's settings, so this test would fail. Skip it.
        if global_functions.is_bridged(wanIP):
            raise unittest.SkipTest("skipping on Bridged configuration, no Local Networks")
        
        # Address we want to test with
        testingAddressNet = "192.168.90"
        testingAddress = testingAddressNet + ".1"
        testingDhcpStart = testingAddressNet + ".100"
        testingDhcpEnd = testingAddressNet + ".200"
        testingCustomWGAddr = "192.168.92.0/24"

        # Pull out the current WG settings and the current Network Settings
        origWGSettings = self._app.getSettings()
        origNetSettings = uvmContext.networkManager().getNetworkSettings()

        # Verify the WG settings don't already have this address stored
        assert(testingAddressNet not in origWGSettings['networks']['list'][0]['address'])

        # deepcopy network settings for manipulation
        newNetSettings = copy.deepcopy( origNetSettings )

        # deepcopy WG settings for manipulation
        newWGSettings = copy.deepcopy( origWGSettings )

        # update WG settings with a custom configuration
        newWGSettings['networks'] = {
                                        "javaClass": "java.util.LinkedList", 
                                        "list": [
                                            {
                                                "address": testingCustomWGAddr, 
                                                "maskedAddress": testingCustomWGAddr, 
                                                "javaClass": "com.untangle.app.wireguard_vpn.WireGuardVpnNetwork", 
                                                "id": 1
                                            }
                                        ]
                                    }
        self._app.setSettings(newWGSettings)

        # Verify custom configuration is returned with get settings
        assert(testingCustomWGAddr in self._app.getSettings()['networks']['list'][0]['address'])

        # Find a DHCP LAN device and set it's address to the testing address
        for intf in newNetSettings['interfaces']['list']:
            if not intf['isWan']:
                intf['v4StaticAddress'] = testingAddress
                if intf['dhcpType'] == 'SERVER':
                    intf['dhcpRangeStart'] = testingDhcpStart
                    intf['dhcpRangeEnd'] = testingDhcpEnd
                break

        # Set the settings to new config
        uvmContext.networkManager().setNetworkSettings( newNetSettings )

        # Test that the custom WG network exists in the latest WG networks list, and that the local networks are not
        wgSettings = self._app.getSettings()
        assert(testingAddressNet not in wgSettings['networks']['list'][0]['address'])
        assert(testingCustomWGAddr in wgSettings['networks']['list'][0]['address'])

        # Set app back to normal
        self._app.setSettings(origWGSettings)

        assert True is global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard"), "wireguard interface and rule is running"

        # Set network settings back to normal
        uvmContext.networkManager().setNetworkSettings( origNetSettings )

        # Get the WG settings again to verify
        wgSettings = self._app.getSettings()

        # Assert that old settings were set back properly
        assert(testingAddressNet not in wgSettings['networks']['list'][0]['address'])
        assert(testingCustomWGAddr not in wgSettings['networks']['list'][0]['address'])

    @pytest.mark.failure_in_podman
    def test_050_shutdown_app(self):
        """
        Shut down app and verify wg0 interface no longer appears
        """
        self._app.stop()

        assert global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard") is False, "wireguard interface and rule is not running"

        assert global_functions.is_vpn_routing(route_table=f"wireguard", expected_route=WG_REMOTE["networks"].split(",")[0]) is False, "wireguard routing on wireguard table"

        self._app.start()


    @pytest.mark.failure_in_podman
    def test_051_delete_wireguard_tunnel(self):
        """
        Remove wireguard tunnel and verify routes no longer appear
        """
        #get and overwrite local service settings to match tunnel settings on the remote/static test box
        appData = self._app.getSettings()

        #set new tunnel settings
        appData["tunnels"]["list"] = []
        self._app.setSettings(appData)

        assert True is global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard"), "wireguard interface and rule is running"

        assert global_functions.is_vpn_routing(route_table=f"wireguard", expected_route=WG_REMOTE["networks"].split(",")[0]) is False, "wireguard routing on wireguard table"


test_registry.register_module("wireguard-vpn", WireGuardVpnTests)
