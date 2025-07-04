"""wireguard-vpn tests"""
import time
import copy
import subprocess
import runtests
import unittest
import pytest
import ipaddress
import re

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import runtests.overrides as overrides
from uvm import Uvm
import tests.test_network as test_network

default_policy_id = 1
appData = None
app = None
appWeb = None
wanIP = None

WG_LOCAL_CONFIG = overrides.get("WG_LOCAL_CONFIG", default=
                        [
                        ('10.112.13.36','192.168.10.0/24',"qP9f5uaOS/0tLJ2SW5AeAJoueaAIJOod8v14x/WD4mY=","10.133.201.1/24"), # ATS Dynamics
                        ('10.112.56.89','172.16.54.0/24',"sGy3LyIUAKMxjJYQNyppBuJw9ibqCcvdOoOrUT0BQGE=","10.133.202.1/24"),  # QA 3 Bridged
                        ('10.112.56.57','192.168.10.0/24',"sBgaDBcvqmxAdJJrVJB1FoK8VyxpAF5KyRrBdox0yGo=","10.133.203.1/24"),  # QA box .57
                        ('10.112.56.58','192.168.10.0/24',"OFuDMenzEmR87trbLa+nF0akxPBfeXBbEohKX94dlHg=","10.133.204.1/24"),  # QA box .58
                        ('10.112.56.59','192.168.10.0/24',"AB7CXs0VSiu4FhJZuW8f18oIKQ5T583/W66aHruwyWY=","10.133.205.1/24"), # QA box .59
                        ('172.17.18.3','192.168.10.0/24 ',"kEEvdr5X3oePByJuuDSWFpKtu/sXzAdHUl5oDVAaxW4=","10.133.206.1/24") #PPPOE server
                    ])


def set_local_wg_config(self, local_ip=None, local_public_key=None, local_private_key=None, local_addr_pool=None):
    """
    Set the wiregaurd config for local host same as configured in remote server 
    """
    if ( local_ip is None or
         local_public_key is None or
         local_private_key is None ):
        # Lookup local config from associated WAN
        wan_ip = global_functions.uvmContext.networkManager().getFirstWanAddress()
        for host_config in WG_LOCAL_CONFIG:
            if (wan_ip == host_config[0]):
                if local_ip is None:
                    local_ip = host_config[0]
                if local_private_key is None:
                    local_private_key = host_config[2]
                if local_public_key is None :
                    command = f'echo "{local_private_key}" | wg pubkey'
                    result = subprocess.run(command, shell=True, capture_output=True, text=True)
                    local_public_key = result.stdout.strip()
                if local_addr_pool is None:
                    local_addr_pool = host_config[3]
                break

        if ( local_public_key is None or
            local_private_key is None or
            local_addr_pool is None ):
            # Unable to find local configuration for this WAN
            raise unittest.SkipTest(f"cannot find local configuration for wan {wan_ip}")
        appData = self._app.getSettings()
        appData["publicKey"] = local_public_key
        appData["privateKey"] = local_private_key
        appData["addressPool"] = local_addr_pool
        self._app.setSettings(appData)

# Commenting out the previous remote server as it is currently unreachable.
# Temporarily replacing it with 10.112.56.96.
# WG_REMOTE = overrides.get("WG_REMOTE", default={
#         "serverAddress": "10.113.150.117",
#         "hostname": "untangle-ats-wireguard",
#         "publicKey": "fupwK1yQLvtBOFpW8nHxjIYjSDAzkpCwYGYL2rS5xUU=",
#         "endpointPort": 51820,
#         "peerAddress": "172.31.53.1",
#         "networks": "192.168.20.0/24",
#         "lanAddress": "192.168.20.170"
# })

WG_REMOTE = overrides.get("WG_REMOTE", default={
        "serverAddress": "10.112.56.96",
        "hostname": "untangle-ats-vpn",
        "publicKey": "dgVORmxoCXTeh1McCkGQ5EikBv8v0gUx/pmX8TTLeDs=",
        "endpointPort": 51820,
        "peerAddress": "172.24.91.1",
        "networks": "192.168.235.0/24",
        "lanAddress": "192.168.235.96"
})


# Roaming configuration
WG_ROAMING = overrides.get("WG_ROAMING", default={
    "pingInterval": 5,
    "endpointHostname": "192.168.1.100",
    "description": "RoamingTunnel",
    "networks": "192.168.2.0/24",
    "enabled": True,
    "peerAddress": "172.16.1.1",
    "endpointDynamic": True,
    "endpointAddress": "",
    "pingConnectionEvents": False,
    "endpointPort": 51820,
    "pingAddress": "",
    "pingUnreachableEvents": False,
    "serverAddress": "192.168.1.101",
    "adminPassword": "passwd",
    "privateKey": "yDvO4Rhb972rtRDdG7QVfTKjr8+Q8hhvrTuaxbW9UmA=",
    "publicKey": "vuVHr0Y5zPeWiYdUklBvryYXqH6NnuHwvl40GRzFVBw="
  })

# NON Roaming Configuration
WG_NON_ROAMING = overrides.get("WG_NON_ROAMING", default={
    "enabled": True,
    "description": "NonRoamingclient",
    "endpointDynamic": False,
    "endpointHostname": "192.168.1.101",
    "endpointPort": 51820,
    "peerAddress": "172.16.3.1",
    "networks": "192.168.56.0/24",
    "pingInterval": 5,
    "pingConnectionEvents": False,
    "pingUnreachableEvents": False,
    "pingAddress": "",
    "serverAddress": "192.168.1.100",
    "adminPassword": "passwd",
    "privateKey": ""
                })

def build_wireguard_tunnel_roaming(tunnel_enabled, description, endpointHostname, endpointDynamic, endpointPort, publicKey,privateKey, networks, peerAddress, pingUnreachableEvents, pingInterval,pingConnectionEvents, pingAddress, assignDnsServer=False):
    return {
        "description": description,
        "enabled": tunnel_enabled,
        "endpointHostname": endpointHostname,
        "endpointDynamic": endpointDynamic,
        "endpointPort": endpointPort,
        "id": 1,
        "javaClass": "com.untangle.app.wireguard_vpn.WireGuardVpnTunnel",
        "networks": networks,
        "peerAddress": peerAddress,
        "pingConnectionEvents": pingConnectionEvents,
        "pingInterval": pingInterval,
        "pingUnreachableEvents": pingUnreachableEvents,
        "pingAddress": pingAddress,
        "privateKey": privateKey,
        "publicKey": publicKey,
        "assignDnsServer": assignDnsServer,
        "routedNetworkProfiles": {
            "javaClass": "java.util.LinkedList",
            "list": [
                "Default"
            ]
        }
    }

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
        "publicKey": WG_REMOTE["publicKey"],
        "assignDnsServer": False,
        "routedNetworkProfiles": {
            "javaClass": "java.util.LinkedList",
            "list": [
                "Default"
            ]
        }
    }

def is_value_in_config_line(config, target_value, key_name):
    """
    Checks if the target_value is present in the comma-separated values of a specific config line.

    :param config: The full config string.
    :param target_value: The value to search for.
    :param key_name: The name of the line key (e.g., 'AllowedIPs').
    :return: True if found, False otherwise.
    """
    match = re.search(rf"{re.escape(key_name)}\s*=\s*(.+)", config)
    if match:
        values = match.group(1)
        return target_value in values
    else:
        return False
    
def build_network_profile(profile_name, network_addresses):
    return {
        "javaClass": "com.untangle.app.wireguard_vpn.WireGuardVpnNetworkProfile",
        "profileName": profile_name,
        "subnetsAsString": network_addresses
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
        wanIP = global_functions.uvmContext.networkManager().getFirstWanAddress()

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
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))

    @pytest.mark.failure_in_podman
    def test_020_create_wireguard_tunnel(self):
        """
        Set up WireGuard Tunnel and ping between hosts behind each end
        """
        if (vpnHostResult != 0):
            raise unittest.SkipTest("No paired VPN server available")
        appData = self._app.getSettings()
        org_wg_settings = copy.deepcopy( appData )
        #get and overwrite local service settings to match tunnel settings on the remote/static test box
        set_local_wg_config(self)

        #set new tunnel settings
        appData = self._app.getSettings()
        appData["tunnels"]["list"].append(build_wireguard_tunnel())
        self._app.setSettings(appData)
        time.sleep(60)
        assert True is global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard"), "wireguard interface and rule is running"

        result = wait_for_ping(WG_REMOTE["lanAddress"],0)
        assert result, "received ping from remote lan"

        assert True is global_functions.is_vpn_routing(route_table=f"wireguard", expected_route=WG_REMOTE["networks"].split(",")[0]), "wireguard routing on wireguard table"
        self._app.setSettings(org_wg_settings)

    def test_021_no_static_route_conflict(self):
        """
        A static route matching one of the alllowed interfaces in the tunnel won't prevent wg from starting
        """
        if (vpnHostResult != 0):
            raise unittest.SkipTest("No paired VPN server available")
    
        appData = self._app.getSettings()
        org_wg_settings = copy.deepcopy( appData )
        
        #get and overwrite local service settings to match tunnel settings on the remote/static test box
        set_local_wg_config(self)

        #set new tunnel settings
        appData = self._app.getSettings()
        appData["tunnels"]["list"].append(build_wireguard_tunnel())

        self._app.setSettings(appData)

        original_network_settings = global_functions.uvmContext.networkManager().getNetworkSettings()

        # deepcopy WG network settings for manipulation
        new_network_settings = copy.deepcopy( original_network_settings )

        # Set network settings back to normal
        global_functions.uvmContext.networkManager().setNetworkSettings( new_network_settings )

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
        global_functions.uvmContext.networkManager().setNetworkSettings( original_network_settings )

        self._app.setSettings(org_wg_settings)

    def test_026_tun_conn_with_default_and_full_tunnel_profile(self):
        """
        Verify static tunnel working with dafault and full tunnel profiles.
        Also verify remote config change when different profiles are selected.
        """
        
        # Pull out the current WG settings and the current Network Settings
        origWGSettings = self._app.getSettings()
        origNetSettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        # deepcopy WG settings for manipulation
        newWGSettings = copy.deepcopy( origWGSettings )

        # Set new tunnel settings
        newWGSettings["tunnels"]["list"] = []
        newWGSettings["tunnels"]["list"].append(build_wireguard_tunnel())
        self._app.setSettings(newWGSettings)

        # ping test with default Profile
        result = wait_for_ping(WG_REMOTE["lanAddress"],0)
        assert result, "received ping from remote lan"

        # Find a DHCP LAN device
        lanAddress = None
        for intf in origNetSettings['interfaces']['list']:
            if not intf['isWan']:
                lanAddress = intf['v4StaticAddress']
                break

        # AllowedIPs should have lan network in wireguard config generated.
        remoteConfig = self._app.getRemoteConfig(WG_REMOTE['publicKey'])
        lanNetwork = ipaddress.ip_network(f"{lanAddress}/24", strict=False)
        assert(is_value_in_config_line(remoteConfig, str(lanNetwork), "AllowedIPs"))

        # Change the Routed Network Profile for tunnel
        newWGSettings["tunnels"]["list"][0]["routedNetworkProfiles"]["list"] = [ "Full Tunnel" ]
        self._app.setSettings(newWGSettings)

        # ping test with Full Tunnel Profile
        result = wait_for_ping(WG_REMOTE["lanAddress"],0)
        assert result, "received ping from remote lan"

        # AllowedIPs should have 0.0.0.0/0 network in wireguard config generated.
        remoteConfig = self._app.getRemoteConfig(WG_REMOTE['publicKey'])
        assert(is_value_in_config_line(remoteConfig, "0.0.0.0/0", "AllowedIPs"))

        # Set app back to normal
        self._app.setSettings(origWGSettings)

    def test_027_tunnel_conf_change_on_profile_edit_delete(self):
        """
        Verify if profile is used as routed network profile by a tunnel then
        1. Editing the network addresses should reflect in tunnel routed profiles and routed networks.
        2. Deleting the profile should remove it from tunnel configuration
        """

        # Declare constants
        profileName = "MyProfile"
        net1 = "10.12.35.0/18"
        net2 = "10.20.36.0/18"
        net3 = "10.32.37.0/18"

        # Pull out the current WG settings and copy it
        origWGSettings = self._app.getSettings()
        newAppSettings = copy.deepcopy( origWGSettings )

        # Add new profiles in settings
        newAppSettings["networkProfiles"]["list"].append(build_network_profile(profileName, net1 + "," + net2))

        # Empty tunnels and Create a new tunnel settings
        newAppSettings["tunnels"]["list"] = []
        tunnel = build_wireguard_tunnel()

        # Set MyProfile as routedNetworkProfiles in tunnel and append it to tunnel list
        tunnel["routedNetworkProfiles"]["list"].append(profileName)
        newAppSettings["tunnels"]["list"].append(tunnel)

        # Set settings and get updated settings from backend
        self._app.setSettings(newAppSettings)
        newAppSettings = self._app.getSettings()

        # Verify if MyProfile addresses are present in tunnel routedNetworks
        routedNetworks = newAppSettings["tunnels"]["list"][0]["routedNetworks"]
        assert (net1 in routedNetworks) and (net2 in routedNetworks)

        # Edit MyProfile addresses
        newAppSettings["networkProfiles"]["list"][-1]["subnetsAsString"] = net1 + "," + net3

        # Set settings and get updated settings from backend
        self._app.setSettings(newAppSettings)
        newAppSettings = self._app.getSettings()

        # Verify that net2 is not in tunnel routedNetworks but net1 and net3 are
        routedNetworks = newAppSettings["tunnels"]["list"][0]["routedNetworks"]
        assert (net2 not in routedNetworks) and (net1 in routedNetworks) and (net3 in routedNetworks)
        assert(profileName in newAppSettings["tunnels"]["list"][0]["routedNetworkProfiles"]["list"])

        # Delete MyProfile from local network profiles
        newAppSettings["networkProfiles"]["list"].pop()

        # Set settings and get updated settings from backend
        self._app.setSettings(newAppSettings)
        newAppSettings = self._app.getSettings()

        # Verify that net1, net2, net3 are not in tunnel routedNetworks
        # and MyProfile is removed from routed network profiles
        routedNetworks = newAppSettings["tunnels"]["list"][0]["routedNetworks"]
        assert (net1 not in routedNetworks) and (net2 not in routedNetworks) and (net3 not in routedNetworks)
        assert(profileName not in newAppSettings["tunnels"]["list"][0]["routedNetworkProfiles"]["list"])

        # Set app back to normal
        self._app.setSettings(origWGSettings)

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
        origNetSettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        # deepcopy WG network settings for manipulation
        newNetSettings = copy.deepcopy( origNetSettings )

        # Verify the WG settings Default profile don't already have this address stored
        assert(testingAddressNet not in wgSettings['networkProfiles']['list'][1]['subnetsAsString'])

        # Find a DHCP LAN device and set it's address to the testing address
        for intf in newNetSettings['interfaces']['list']:
            if not intf['isWan']:
                intf['v4StaticAddress'] = testingAddress
                if intf['dhcpType'] == 'SERVER':
                    intf['dhcpRangeStart'] = testingDhcpStart
                    intf['dhcpRangeEnd'] = testingDhcpEnd
                break

        # Set the settings to new config
        global_functions.uvmContext.networkManager().setNetworkSettings( newNetSettings )

        # Query the WG settings to assert if that address made it in
        newWGSettings = self._app.getSettings()

        # Test that new WG LAN matches network LAN
        assert(testingAddressNet in newWGSettings['networkProfiles']['list'][1]['subnetsAsString'])

        # Set network settings back to normal
        global_functions.uvmContext.networkManager().setNetworkSettings( origNetSettings )

        # Get the WG settings again to verify
        wgSettings = self._app.getSettings()

        assert True is global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard"), "wireguard interface and rule is running"

        # Assert that old settings were set back properly proper now
        assert(testingAddressNet not in wgSettings['networkProfiles']['list'][1]['subnetsAsString'])

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
        origNetSettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        # Verify the WG settings Default profile don't already have this address stored
        assert(testingAddressNet not in origWGSettings['networkProfiles']['list'][1]['subnetsAsString'])

        # deepcopy network settings for manipulation
        newNetSettings = copy.deepcopy( origNetSettings )

        # deepcopy WG settings for manipulation
        newWGSettings = copy.deepcopy( origWGSettings )

        # update WG settings Default profile with a custom configuration
        newWGSettings['networkProfiles']['list'][1] = {
                                        "javaClass": "com.untangle.app.wireguard_vpn.WireGuardVpnNetworkProfile",
                                        "subnetsAsString": testingCustomWGAddr
                                    }
        self._app.setSettings(newWGSettings)

        # Verify custom configuration is returned with get settings
        assert(testingCustomWGAddr in self._app.getSettings()['networkProfiles']['list'][1]['subnetsAsString'])

        # Find a DHCP LAN device and set it's address to the testing address
        for intf in newNetSettings['interfaces']['list']:
            if not intf['isWan']:
                intf['v4StaticAddress'] = testingAddress
                if intf['dhcpType'] == 'SERVER':
                    intf['dhcpRangeStart'] = testingDhcpStart
                    intf['dhcpRangeEnd'] = testingDhcpEnd
                break

        # Set the settings to new config
        global_functions.uvmContext.networkManager().setNetworkSettings( newNetSettings )

        # Test that the custom WG network exists in the latest WG Default profile networks list, and that the local networks are not
        wgSettings = self._app.getSettings()
        assert(testingAddressNet not in wgSettings['networkProfiles']['list'][1]['subnetsAsString'])
        assert(testingCustomWGAddr in wgSettings['networkProfiles']['list'][1]['subnetsAsString'])

        # Set app back to normal
        self._app.setSettings(origWGSettings)

        assert True is global_functions.is_vpn_running(interface=f"wg0",route_table=f"wireguard"), "wireguard interface and rule is running"

        # Set network settings back to normal
        global_functions.uvmContext.networkManager().setNetworkSettings( origNetSettings )

        # Get the WG settings again to verify
        wgSettings = self._app.getSettings()

        # Assert that old settings were set back properly
        assert(testingAddressNet not in wgSettings['networkProfiles']['list'][1]['subnetsAsString'])
        assert(testingCustomWGAddr not in wgSettings['networkProfiles']['list'][1]['subnetsAsString'])

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

    @pytest.mark.failure_in_podman
    @pytest.mark.slow
    def test_041_verify_wg_non_roaming_on_disconnect(self):
        '''
        Verify Following
        1. Roaming Tunnels in NGFW, after disconnection don't generate events
        2. If connection is successful then Tunnel Description  is stored as username and hostname in HostTableEntry

        Test Prerequisites (Here Roaming NGFW is Wireguard VPN server, Non Roaming NGFW is Wireguard client)
        1. Roaming NGFW with Wireguard uninstalled (Server, Roaming tunnel creation) and a client to run tests
        2. Non Roaming NGFW (WireguardClient Connects to Roaming Tunnel)


        WG_ROAMING contains information of Roaming NGFW with parameters from Non Roaming NGFW
        Override Variables [endpointHostname, networks, peerAddress, endpointPort,endpointDynamic] from Non Roaming NGFW
        Override Variables [serverAddress,adminPassword] from Roaming NGFW

        WG_NON_ROAMING contains information of Remote NGFW which acts as client and connects to Roaming Tunnel with parameters from Roaming NGFW
        Override Variables [endpointHostname, networks, peerAddress, endpointPort,endpointDynamic] from Roaming NGFW
        Override Variables [serverAddress,adminPassword] from Non Roaming NGFW
        '''

        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        WGNonRoamingHostResult = subprocess.call(["ping","-W","5","-c","1",WG_NON_ROAMING["serverAddress"]],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        WGRoamingHostResult = subprocess.call(["ping","-W","5","-c","1",WG_ROAMING["serverAddress"]],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (WGRoamingHostResult !=0):
            raise unittest.SkipTest("Roaming Wireguard NGFW Unreachable")
        if (WGNonRoamingHostResult !=0):
            raise unittest.SkipTest("NonRoaming Wireguard NGFW Unreachable")
        remote_uvm_context = Uvm().getUvmContext(timeout=240, scheme="https", hostname=WG_NON_ROAMING["serverAddress"], username="admin", password=WG_NON_ROAMING["adminPassword"])
        remote_uid = remote_uvm_context.getServerUID()
        local_uid = global_functions.uvmContext.getServerUID()
        assert(remote_uid != local_uid)
        roaming_wg_app = global_functions.uvmContext.appManager().app("wireguard-vpn")
        roaming_wg_app_data = roaming_wg_app.getSettings()
        #set Non Roaming NGFW Tunnel public key in Roaming NGFW WG Tunnel publickey
        non_roaming_wg_app = remote_uvm_context.appManager().app("wireguard-vpn")
        non_roaming_wg_app_data =  non_roaming_wg_app.getSettings()
        WG_ROAMING['publicKey'] = non_roaming_wg_app_data["publicKey"]
        #Roaming tunnel setup on Local NGFW running tests
        roaming_wg_app_data["tunnels"]["list"].append(build_wireguard_tunnel_roaming(WG_ROAMING['enabled'],  WG_ROAMING['description'], WG_ROAMING['endpointHostname'], WG_ROAMING['endpointDynamic'],WG_ROAMING['endpointPort'],WG_ROAMING['publicKey'],WG_ROAMING['privateKey'],WG_ROAMING['networks'],WG_ROAMING['peerAddress'],WG_ROAMING['pingUnreachableEvents'],WG_ROAMING['pingInterval'],WG_ROAMING['pingConnectionEvents'],WG_ROAMING['pingAddress']))
        roaming_wg_app.setSettings(roaming_wg_app_data)

        #Non Roaming Wireguard client setup on Remote NGFW
        non_roaming_wg_app = remote_uvm_context.appManager().app("wireguard-vpn")
        non_roaming_wg_app_data =  non_roaming_wg_app.getSettings()
        #set Roaming NGFW Tunnel public key in Non Roaming Tunnel NGFW publickey
        WG_NON_ROAMING['publicKey'] = roaming_wg_app_data["publicKey"]
        non_roaming_wg_app_data["tunnels"]["list"].append(build_wireguard_tunnel_roaming(WG_NON_ROAMING['enabled'],  WG_NON_ROAMING['description'], WG_NON_ROAMING['endpointHostname'], WG_NON_ROAMING['endpointDynamic'],WG_NON_ROAMING['endpointPort'],WG_NON_ROAMING['publicKey'],WG_NON_ROAMING['privateKey'],WG_NON_ROAMING['networks'],WG_NON_ROAMING['peerAddress'],WG_NON_ROAMING['pingUnreachableEvents'],WG_NON_ROAMING['pingInterval'],WG_NON_ROAMING['pingConnectionEvents'],WG_NON_ROAMING['pingAddress']))
        non_roaming_wg_app.setSettings(non_roaming_wg_app_data)

        time.sleep(30)
        #Verify host entry made in  HostTableEntry for WG_ROAMING['description'] Tunnel Description 
        entry = global_functions.uvmContext.hostTable().getHostTableEntry( WG_ROAMING['peerAddress'] )
        assert (WG_ROAMING['description'] == entry["hostnameWireGuardVpn"])
        assert (WG_ROAMING['description'].lower() == entry["usernameWireGuardVpn"])
        print(entry)

        #Deleting Non Roaming Wireguard client connection on Remote NGFW
        non_roaming_wg_app_data["tunnels"]["list"] = []
        non_roaming_wg_app.setSettings(non_roaming_wg_app_data)
        #Events should not pe present for roaming clients, waiting
        time.sleep(5)

        events = global_functions.get_events("WireGuard VPN",'Monitor Events',None,5)
        found = global_functions.check_events( events.get('list'), 5,"event_type", "UNREACHABLE" )
        assert (found == False)

        #Deleting Roaming Wireguard Tunnel connection on Remote NGFW
        roaming_wg_app_data["tunnels"]["list"] = []
        roaming_wg_app.setSettings(roaming_wg_app_data)

    def test_040_verify_search_domain_updated_in_remote_config(self):
        """
        Check Search Domains updated in remote client config file
        """

        # get app settings and deepcopy it
        appSettings = self._app.getSettings()
        newAppSettings = copy.deepcopy( appSettings )

        # Create roaming client tunnel and set new tunnel in settings.
        newAppSettings["tunnels"]["list"].append(build_wireguard_tunnel_roaming(WG_ROAMING['enabled'],  WG_ROAMING['description'], "", WG_ROAMING['endpointDynamic'],WG_ROAMING['endpointPort'],WG_ROAMING['publicKey'],WG_ROAMING['privateKey'],"","172.16.1.3",WG_ROAMING['pingUnreachableEvents'],WG_ROAMING['pingInterval'],WG_ROAMING['pingConnectionEvents'],WG_ROAMING['pingAddress'],True))
        self._app.setSettings(newAppSettings)

        # Check if default search domain (i.e. NGFW domain name) is included in remote client config
        domainName = global_functions.uvmContext.networkManager().getNetworkSettings()['domainName']
        remoteConfig = self._app.getRemoteConfig(WG_ROAMING['publicKey']).split("\n")

        count = 0
        for line in remoteConfig:
            if(line.startswith("DNS") and domainName in line):
                count += 1
                break
        assert(count == 1)

        # Set search domain value to blank and check if its reflected in remote client config
        newAppSettings["dnsSearchDomain"] = ""
        self._app.setSettings(newAppSettings)
        remoteConfig = self._app.getRemoteConfig(WG_ROAMING['publicKey']).split("\n")

        count = 0
        for line in remoteConfig:
            if(line.startswith("DNS") and "," in line):
                count += 1
                break
        assert(count == 0)

        self._app.setSettings(appSettings)

    def test_045_verify_assign_dns_config(self):
        """
        Check assign dns server checkbox works as expected.
        """
        # get app settings and deepcopy it
        appSettings = self._app.getSettings()
        newAppSettings = copy.deepcopy( appSettings )

        # Create roaming client tunnel and set new tunnel in settings with default assignDnsServer value False
        newAppSettings["tunnels"]["list"] = []
        newAppSettings["tunnels"]["list"].append(build_wireguard_tunnel_roaming(WG_ROAMING['enabled'],  WG_ROAMING['description'], "", WG_ROAMING['endpointDynamic'],WG_ROAMING['endpointPort'],WG_ROAMING['publicKey'],WG_ROAMING['privateKey'],"","172.16.1.3",WG_ROAMING['pingUnreachableEvents'],WG_ROAMING['pingInterval'],WG_ROAMING['pingConnectionEvents'],WG_ROAMING['pingAddress']))
        self._app.setSettings(newAppSettings)

        # DNS config should not be present in wireguard config generated.
        remoteConfig = self._app.getRemoteConfig(WG_ROAMING['publicKey'])
        assert("DNS=" not in remoteConfig)

        # Change assignDnsServer value to True
        newAppSettings["tunnels"]["list"][0]['assignDnsServer'] = True
        self._app.setSettings(newAppSettings)

        # DNS config should be present in wireguard config generated.
        remoteConfig = self._app.getRemoteConfig(WG_ROAMING['publicKey'])
        assert("DNS=" in remoteConfig)

        # Create static client tunnel with default assignDnsServer value False
        newAppSettings["tunnels"]["list"] = []
        newAppSettings["tunnels"]["list"].append(build_wireguard_tunnel())
        self._app.setSettings(newAppSettings)

        # DNS should be present regardless of assignDnsServer flag
        remoteConfig = self._app.getRemoteConfig(WG_ROAMING['publicKey'])
        assert("DNS=" in remoteConfig)

        # Change assignDnsServer value to True
        newAppSettings["tunnels"]["list"][0]['assignDnsServer'] = True
        self._app.setSettings(newAppSettings)

        remoteConfig = self._app.getRemoteConfig(WG_ROAMING['publicKey'])
        assert("DNS=" in remoteConfig)

        self._app.setSettings(appSettings)

    def test_046_roaming_tun_conf_with_default_and_full_tunnel_profile(self):
        """
        Verify roaming tunnel remote config with dafault and full tunnel profiles.
        """
        
        # Pull out the current WG settings and the current Network Settings
        origWGSettings = self._app.getSettings()
        origNetSettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        newAppSettings = copy.deepcopy( origWGSettings )

        # Create roaming client tunnel and set new tunnel in settings with default assignDnsServer value False
        newAppSettings["tunnels"]["list"] = []
        newAppSettings["tunnels"]["list"].append(build_wireguard_tunnel_roaming(WG_ROAMING['enabled'],  WG_ROAMING['description'], "", WG_ROAMING['endpointDynamic'],WG_ROAMING['endpointPort'],WG_ROAMING['publicKey'],WG_ROAMING['privateKey'],"","172.16.1.3",WG_ROAMING['pingUnreachableEvents'],WG_ROAMING['pingInterval'],WG_ROAMING['pingConnectionEvents'],WG_ROAMING['pingAddress']))
        self._app.setSettings(newAppSettings)

        # Find a DHCP LAN device and set it's address to the testing address
        lanAddress = None
        for intf in origNetSettings['interfaces']['list']:
            if not intf['isWan']:
                lanAddress = intf['v4StaticAddress']
                break

        # AllowedIPs should have lan network in wireguard config generated.
        remoteConfig = self._app.getRemoteConfig(WG_ROAMING['publicKey'])
        lanNetwork = ipaddress.ip_network(f"{lanAddress}/24", strict=False)
        assert(is_value_in_config_line(remoteConfig, str(lanNetwork), "AllowedIPs"))

        # Change the Routed Network Profile for tunnel
        newAppSettings["tunnels"]["list"][0]["routedNetworkProfiles"]["list"] = [ "Full Tunnel" ]
        self._app.setSettings(newAppSettings)

        # AllowedIPs should have 0.0.0.0/0 network in wireguard config generated.
        remoteConfig = self._app.getRemoteConfig(WG_ROAMING['publicKey'])
        assert(is_value_in_config_line(remoteConfig, "0.0.0.0/0", "AllowedIPs"))

        # Set app back to normal
        self._app.setSettings(origWGSettings)

test_registry.register_module("wireguard-vpn", WireGuardVpnTests)
