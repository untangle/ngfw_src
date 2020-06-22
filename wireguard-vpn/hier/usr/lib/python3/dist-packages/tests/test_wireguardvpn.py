"""wireguard-vpn tests"""
import time
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
	
test_registry.register_module("wireguard-vpn", WireGuardVpnTests)
