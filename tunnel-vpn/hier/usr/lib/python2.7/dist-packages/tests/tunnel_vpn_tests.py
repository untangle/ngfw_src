import unittest2
import time
import sys
import traceback
import ipaddr
import socket
import os
import subprocess

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import global_functions

default_policy_id = 1
app = None
vpn_tunnel_file = "http://10.111.56.29/openvpn-ats-test-tunnelvpn-config.zip"

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

def create_tunnel_profile(vpn_enabled=True,provider="tunnel-Untangle",vpn_tunnel_id=200):
    return {
            "allTraffic": False,
            "enabled": vpn_enabled,
            "javaClass": "com.untangle.app.tunnel_vpn.TunnelVpnTunnelSettings",
            "name": "tunnel-Untangle",
            "provider": "Untangle",
            "tags": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "tunnelId": vpn_tunnel_id
    }


class TunnelVpnTests(unittest2.TestCase):

    @staticmethod
    def appName():
        return "tunnel-vpn"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initial_setup(self):
        global app
        if (uvmContext.appManager().isInstantiated(self.appName())):
            raise Exception('app %s already instantiated' % self.appName())
        app = uvmContext.appManager().instantiate(self.appName(), default_policy_id)
        app.start()

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.appName()))

    def test_020_createVPNTunnel(self):
        result = subprocess.call("wget -o /dev/null -t 1 --timeout=3 " + vpn_tunnel_file + " -O /tmp/config.zip", shell=True)
        if (result != 0):
            raise unittest2.SkipTest("Unable to download VPN file: " + vpn_tunnel_file)
        currentWanIP = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
        if (currentWanIP == ""):
            raise unittest2.SkipTest("Unable to get WAN IP")
        print("Original WAN IP: " + currentWanIP)
        app.importTunnelConfig("/tmp/config.zip", "Untangle", 200)

        appData = app.getSettings()
        appData['rules']['list'].append(create_tunnel_rule())
        appData['tunnels']['list'].append(create_tunnel_profile())
        app.setSettings(appData)

        # wait for vpn tunnel to form
        timeout = 60
        connected = False
        while (not connected and timeout > 0):
            newWanIP = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
            if (currentWanIP != newWanIP):
                listOfConnections = app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                connected = True
                listOfConnections = app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
            else:
                time.sleep(1)
                timeout-=1

        # remove the added tunnel
        appData['rules']['list'][:] = []
        appData['tunnels']['list'][:] = []
        app.setSettings(appData)

        # If VPN tunnel has failed to connect, fail the test,
        assert(connected)
        assert(connectStatus == "CONNECTED")

    def test_030_createVPNAnyTunnel(self):
        result = subprocess.call("wget -o /dev/null -t 1 --timeout=3 " + vpn_tunnel_file + " -O /tmp/config.zip", shell=True)
        if (result != 0):
            raise unittest2.SkipTest("Unable to download VPN file: " + vpn_tunnel_file)
        currentWanIP = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
        if (currentWanIP == ""):
            raise unittest2.SkipTest("Unable to get WAN IP")
        print("Original WAN IP: " + currentWanIP)
        app.importTunnelConfig("/tmp/config.zip", "Untangle", 200)

        appData = app.getSettings()
        appData['rules']['list'].append(create_tunnel_rule(vpn_tunnel_id=-1))
        appData['tunnels']['list'].append(create_tunnel_profile())
        app.setSettings(appData)

        # wait for vpn tunnel to form
        timeout = 60
        connected = False
        while (not connected and timeout > 0):
            newWanIP = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
            if (currentWanIP != newWanIP):
                listOfConnections = app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
                connected = True
                listOfConnections = app.getTunnelStatusList()
                connectStatus = listOfConnections['list'][0]['stateInfo']
            else:
                time.sleep(1)
                timeout-=1

        # remove the added tunnel
        appData['rules']['list'][:] = []
        appData['tunnels']['list'][:] = []
        app.setSettings(appData)

        # If VPN tunnel has failed to connect, fail the test,
        assert(connected)
        assert(connectStatus == "CONNECTED")

    @staticmethod
    def final_tear_down(self):
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
        app = None

test_registry.registerApp("tunnel-vpn", TunnelVpnTests)
