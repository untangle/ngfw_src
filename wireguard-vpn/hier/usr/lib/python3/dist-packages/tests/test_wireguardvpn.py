"""wireguard-vpn tests"""
import time
import copy
import re
import subprocess
import base64
import runtests
import unittest
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
appData = None
app = None
appWeb = None
tunnelUp = False

#remote WireGuard Untangle from global_functions
remotePublicKey = global_functions.WG_VPN_SERVICE_INFO["publicKey"]
remoteEndpointAddress = global_functions.WG_VPN_SERVICE_INFO["endpointAddress"]
remotePeerAddress = global_functions.WG_VPN_SERVICE_INFO["peerAddress"]
remoteNetworks = global_functions.WG_VPN_SERVICE_INFO["networks"]
remoteDescription = global_functions.WG_VPN_SERVICE_INFO["hostname"]

#set these locally to match what's on remote/static WireGuard untangle
localPublicKey = "1YbeQWcyHrPnnUJhBKbxKt2ZUbr2I8EiuinG9cYqQmE="
localPrivateKey = "sICMfPW0s1m74egk3VS4BXe7mah3m5XF+gCN25B0Y2w="
localAddressPool = "10.133.205.1/24"

def setupWireguardTunnel(tunnel_enabled=True, remotePK=remotePublicKey, remotePeer=remotePeerAddress, description=remoteDescription, endpointAddress=remoteEndpointAddress, networks=remoteNetworks):
    return {
        "description": description,
        "enabled": tunnel_enabled,
        "endpointAddress": endpointAddress,
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
        "publicKey": remotePublicKey
    }

def waitForPing(target_IP="127.0.0.1",ping_result_expected=0):
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
        global appData, vpnHostResult, vpnClientResult, vpnServerResult

        vpnHostResult = subprocess.call(["ping","-W","5","-c","1",global_functions.WG_VPN_SERVER_IP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        vpnClientResult = subprocess.call(["ping","-W","5","-c","1",global_functions.VPN_CLIENT_IP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        wanIP = uvmContext.networkManager().getFirstWanAddress()
        if vpnClientResult == 0:
            vpnServerResult = remote_control.run_command("ping -W 5 -c 1 " + wanIP, host=global_functions.VPN_CLIENT_IP)
        else:
            vpnServerResult = 1

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_createWireGuardTunnel(self):
        """Set up WireGuard Tunnel and ping between hosts behind each end"""
        if (vpnHostResult != 0):
            raise unittest.SkipTest("No paried VPN server available")
        
        #get and overwrite local service settings to match tunnel settings on the remote/static test box
        appData = self._app.getSettings()
        appData["publicKey"] = localPublicKey
        appData["privateKey"] = localPrivateKey
        appData["addressPool"] = localAddressPool

        #set new tunnel settings
        appData["tunnels"]["list"].append(setupWireguardTunnel())
        self._app.setSettings(appData)

        result = waitForPing(global_functions.WG_VPN_SERVER_LAN_IP,0)
        assert(result)
        tunnelUp = True

    def test_031_netSettingsAndDefaultWGNetworks(self):
        """Test if changing the Network Settings LAN address properly updates the wireguard local networks"""
        
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
                if intf['dhcpEnabled']:
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

        # Assert that old settings were set back properly proper now
        assert(testingAddressNet not in wgSettings['networks']['list'][0]['address'])

    def test_032_netSettingsAndCustomWGNetworks(self):
        """Test if changing the Network Settings LAN address DOES NOT update custom local networks in the WG app"""
        
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
        newWGSettings['networks'] = testingCustomWGAddr
        self._app.setSettings(newWGSettings)

        # Verify custom configuration is returned with get settings
        assert(testingCustomWGAddr in self._app.getSettings()['networks']['list'][0]['address'])

        # Find a DHCP LAN device and set it's address to the testing address
        for intf in newNetSettings['interfaces']['list']:
            if not intf['isWan']:
                intf['v4StaticAddress'] = testingAddress
                if intf['dhcpEnabled']:
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

        # Set network settings back to normal
        uvmContext.networkManager().setNetworkSettings( origNetSettings )

        # Get the WG settings again to verify
        wgSettings = self._app.getSettings()

        # Assert that old settings were set back properly
        assert(testingAddressNet not in wgSettings['networks']['list'][0]['address'])
        assert(testingCustomWGAddr not in wgSettings['networks']['list'][0]['address'])


test_registry.register_module("wireguard-vpn", WireGuardVpnTests)
