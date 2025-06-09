"""ipsec_vpn tests"""
import time
import copy
import subprocess
import base64
import unittest
import pytest
import sys

from tests.common import NGFWTestCase
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import runtests.overrides as overrides

import tests.test_wan_failover as test_wan_failover

from uvm import Uvm
from datetime import datetime

# IPSec Configuration
L2TP_SERVER_HOSTS = overrides.get("L2TP_SERVER_HOSTS", default=
    ["10.112.56.61","10.112.56.49","10.112.56.89","10.112.11.53","10.112.0.134",
    "10.112.56.91","10.112.56.94","10.112.56.57","10.112.56.58","10.112.56.59"]
)
L2TP_CLIENT_HOST = overrides.get("L2TP_CLIENT_HOST", default="10.112.56.84")  # Windows 10 using builtin OpenSSH
L2TP_ALIAS_IP = overrides.get("L2TP_ALIAS_IP", default="10.112.56.200")
L2TP_LOCAL_USER = overrides.get("L2TP_LOCAL_USER", default="test")
L2TP_LOCAL_PASSWORD = overrides.get("L2TP_LOCAL_PASSWORD", default="passwd")

IPSEC_HOST = overrides.get("IPSEC_HOST", default="10.112.56.96")
IPSEC_HOST_LAN = overrides.get("IPSEC_HOST_LAN", default="192.168.235.0/24")
IPSEC_HOST_LAN_IP = overrides.get("IPSEC_HOST_LAN_IP", default="192.168.235.96")
IPSEC_PC_LAN_IP = overrides.get("IPSEC_PC_LAN_IP", default="192.168.235.83")
IPSEC_HOST_NAME = overrides.get("IPSEC_HOST_NAME", default="ipsecsite.untangle.int")
IPSEC_CONFIGURED_HOST_IPS = overrides.get("IPSEC_CONFIGURED_HOST_IPS", default=
                        [('10.112.13.36','192.168.10.1','192.168.10.1/24'), # ATS
                        ('10.112.56.89','10.112.56.89','10.112.56.0/24'),  # QA 3 Bridged
                        ('10.112.56.57','192.168.10.1','192.168.10.0/24'),  # QA box .57
                        ('10.112.56.58','192.168.10.1','192.168.10.0/24'),  # QA box .58
                        ('10.112.56.59','192.168.10.1','192.168.10.0/24')] # QA box Dual .59
)

default_policy_id = 1
appAD = None
appDataRD = None
appFW = None
tunnelUp = False
ipsecTestLAN = ""
orig_netsettings = None

local_host_ip = None
local_host_lan_ip = None

Remote_ngfw = overrides.get("Remote_ngfw", default={
        "serverAddress": IPSEC_HOST,
        "adminPassword": "passwd"
})

def build_ipsec_tunnel(remote_ip=IPSEC_HOST, remote_lan=IPSEC_HOST_LAN, local_ip=None, local_lan_ip=None, local_lan_range=None):
    """
    Create an ipsec tunnel settings entry.
    If the Local values are not defined, use the WAN address to search for them from IPSEC_CONFIGURED_HOST_IPS.
    """
    global local_host_ip, local_host_lan_ip
    if ( local_ip is None or
         local_lan_ip is None or
         local_lan_range is None ):
        # Lookup local config from associated WAN
        wan_ip = global_functions.uvmContext.networkManager().getFirstWanAddress()
        for host_config in IPSEC_CONFIGURED_HOST_IPS:
            if (wan_ip == host_config[0]):
                if local_ip is None:
                    local_ip = host_config[0]
                if local_lan_ip is None:
                    local_lan_ip = host_config[1]
                if local_lan_range is None:
                    local_lan_range = host_config[2]
                local_host_ip = local_ip
                local_host_lan_ip = local_lan_ip
                break

        if ( local_ip is None or
            local_lan_ip is None or
            local_lan_range is None ):
            # Unable to find local configuration for this WAN
            raise unittest.SkipTest(f"cannot find local configuration for wan {wan_ip}")

    return {
        "active": True, 
        "adapter": "- Custom -", 
        "conntype": "tunnel", 
        "description": "ipsec test profile", 
        "id": 0, 
        "javaClass": "com.untangle.app.ipsec_vpn.IpsecVpnTunnel", 
        "left": local_ip,  # local WAN
        "leftSourceIp": local_lan_ip, # local LAN IP
        "leftSubnet": local_lan_range,  # local LAN range
        "pfs": True, 
        "right": remote_ip,  # remote WAN
        "rightSubnet": remote_lan, # remote LAN range
        "rightId": "%any",
        "runmode": "start", 
        "secret": "supersecret",
        "phase1Manual": True,
        "phase1Lifetime": "28800",
        "phase1Group": "modp2048",
        "phase1Hash": "sha1",
        "phase1Cipher": "aes128",
        "dpdtimeout": "120",
        "phase2Manual": True,
        "phase2Lifetime": "3600",
        "phase2Group": "modp2048",
        "phase2Hash": "sha1",
        "phase2Cipher": "aes256gcm128",
        "ikeVersion": 2
    }    
    
def nukeIPSecTunnels(app):
    ipsecSettings = app.getSettings()
    ipsecSettings["tunnels"]["list"] = []
    app.setSettings(ipsecSettings)


def createL2TPconfig(ipsecSettings,authType="LOCAL_DIRECTORY"):
    ipsecSettings["authenticationType"] = authType
    ipsecSettings["virtualAddressPool"] = "198.18.0.0/16"
    ipsecSettings["virtualSecret"] = "testthis"
    ipsecSettings["vpnflag"] = True
    return ipsecSettings


def createLocalDirectoryUser(userpassword=L2TP_LOCAL_PASSWORD):
    passwd_encoded = base64.b64encode(userpassword.encode("utf-8"))
    return {'javaClass': 'java.util.LinkedList', 
        'list': [{
            'username': L2TP_LOCAL_USER,
            'firstName': '[firstName]', 
            'lastName': '[lastName]', 
            'javaClass': 'com.untangle.uvm.LocalDirectoryUser', 
            'expirationTime': 0, 
            'passwordBase64Hash': passwd_encoded.decode("utf-8"),
            'email': 'test@example.com'
            },]
    }

def removeLocalDirectoryUser():
    return {'javaClass': 'java.util.LinkedList', 
        'list': []
    }


def createRadiusSettings():
    return {
        "activeDirectorySettings": {
            "LDAPHost": "ad_server.mydomain.int",
            "LDAPPort": 636,
            "LDAPSecure": True,
            "OUFilter": "",
            "OUFilters": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "domain": "mydomain.int",
            "enabled": False,
            "javaClass": "com.untangle.app.directory_connector.ActiveDirectorySettings",
            "superuser": "Administrator",
            "superuserPass": "mypassword"
        },
        "apiEnabled": True,
        "facebookSettings": {
            "authenticationEnabled": False,
            "javaClass": "com.untangle.app.directory_connector.FacebookSettings"
        },
        "googleSettings": {
            "authenticationEnabled": False,
            "javaClass": "com.untangle.app.directory_connector.GoogleSettings"
        },
        "javaClass": "com.untangle.app.directory_connector.DirectoryConnectorSettings",
        "radiusSettings": {
            "acctPort": 1813,
            "authPort": 1812,
            "authenticationMethod": "MSCHAPV2",
            "enabled": True,
            "javaClass": "com.untangle.app.directory_connector.RadiusSettings",
            "server": global_functions.RADIUS_SERVER,
            "sharedSecret": global_functions.RADIUS_SERVER_PASSWORD
        },
        "version": 1
    }

    
def createDNSRule( networkAddr, name):
    return {
        "address": networkAddr,
        "javaClass": "com.untangle.uvm.network.DnsStaticEntry",
        "name": name
         }


def addDNSRule(newRule):
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'].insert(0,newRule)
    global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)  

def create_alias(ipAddress,ipNetmask,ipPrefix):
    return {
            "javaClass": "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
            "staticAddress": ipAddress,
            "staticNetmask": ipNetmask,
            "staticPrefix": ipPrefix
        }

def create_firewall_rule( conditionType, value, blocked=True ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "javaClass": "com.untangle.app.firewall.FirewallRule", 
        "id": 1, 
        "enabled": True, 
        "description": "Single Matcher: " + conditionTypeStr + " = " + valueStr, 
        "log": True, 
        "block": blocked, 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.app.firewall.FirewallRuleCondition", 
                    "conditionType": conditionTypeStr, 
                    "value": valueStr
                    }
                ]
            }
        }

@pytest.mark.ipsec_vpn
class IPsecTests(NGFWTestCase):
    force_start = True

    @staticmethod
    def module_name():
        global app
        app = IPsecTests._app
        return "ipsec-vpn"

    @staticmethod
    def appNameAD():
        return "directory-connector"

    @staticmethod
    def appNameFW():
        return "firewall"

    @staticmethod
    def vendorName():
        return "Untangle"

    @classmethod
    def initial_extra_setup(cls):
        global orig_netsettings, ipsecHostResult, l2tpClientHostResult, appAD, appDataRD, appFW, radiusResult

        tunnelUp = False

        if orig_netsettings == None:
            orig_netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        if (global_functions.uvmContext.appManager().isInstantiated(cls.appNameAD())):
            if cls.skip_instantiated():
                pytest.skip('app %s already instantiated' % cls.appNameAD())
            else:
                appAD = global_functions.uvmContext.appManager().app(cls.appNameAD())
        else:
            appAD = global_functions.uvmContext.appManager().instantiate(cls.appNameAD(), default_policy_id)

        appDataRD = appAD.getSettings().get('radiusSettings')
        appFW = global_functions.uvmContext.appManager().instantiate(cls.appNameFW(), default_policy_id)
        ipsecHostResult = subprocess.call(["ping","-c","1",IPSEC_HOST],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        l2tpClientHostResult = subprocess.call(["ping","-c","1",L2TP_CLIENT_HOST],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        radiusResult = subprocess.call(["ping","-c","1",global_functions.RADIUS_SERVER],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(global_functions.uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_createIpsecTunnel(self):
        global tunnelUp, ipsecTestLAN
        if (ipsecHostResult != 0):
            raise unittest.SkipTest("No paired IPSec server available")
        pre_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")

        appData = self._app.getSettings()
        appData["tunnels"]["list"].append(build_ipsec_tunnel())
        self._app.setSettings(appData)
        timeout = 10
        ipsecHostLANResult = 1
        while (ipsecHostLANResult != 0 and timeout > 0):
            timeout -= 1
            time.sleep(1)
            # Access remote LAN to see if the IPsec tunnel is connected.
            ipsecHostLANResult = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{IPSEC_HOST_LAN_IP}/"))
        assert (ipsecHostLANResult == 0)
        ipsecPcLanResult = remote_control.run_command("ping -c 1 %s" % IPSEC_PC_LAN_IP)
        assert (ipsecPcLanResult == 0)
        tunnelUp = True

        # Check to see if the faceplate counters have incremented. 
        post_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")
        assert(pre_events_enabled < post_events_enabled)
    
    def test_021_no_vlan_conflict(self):
        """
        Ensure that traffic is not marked for the last LAN interface
        """
        if (not tunnelUp):
            raise unittest.SkipTest("Test test_020_createIpsecTunnel success required ")

        ##
        ## Add a dummy vlan inteface
        ##
        vlan_interface_name = "ats_vlan"
        network_settings = copy.deepcopy(orig_netsettings)
        vlan_netspace = global_functions.uvmContext.netspaceManager().getAvailableAddressSpace("IPv4", 1);
        vlan_host, vlan_netmask = global_functions.cidr_to_netmask(vlan_netspace)
        network_settings["interfaces"]["list"].append({
            "configType": "ADDRESSED",
            "dhcpType": "DISABLED",
            "dhcpLeaseDuration": 0,
            "dhcpOptions": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "downloadBandwidthKbps": 0,
            "hidden": False,
            "interfaceId": -1,
            "isVirtualInterface": False,
            "isVlanInterface": True,
            "isWan": False,
            "isWirelessInterface": False,
            "javaClass": "com.untangle.uvm.network.InterfaceSettings",
            "name": vlan_interface_name,
            "physicalDev": "eth1",
            "symbolicDev": "eth1.999",
            "systemDev": "eth1.999",
            "uploadBandwidthKbps": 0,
            "v4Aliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "v4ConfigType": "STATIC",
            "v4NatEgressTraffic": True,
            "v4NatIngressTraffic": False,
            "v4PPPoEUsePeerDns": False,
            "v4StaticAddress": vlan_host,
            "v4StaticNetmask": vlan_netmask,
            "v4StaticPrefix": vlan_netspace.split("/")[1],
            "v6Aliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "v6ConfigType": "STATIC",
            "vlanParent": 2,
            "vlanTag": 999,
            "vrrpAliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "vrrpEnabled": False,
            "wirelessCountryCode": "",
            "wirelessMode": "AP",
            "wirelessVisibility": 0
        })
        global_functions.uvmContext.networkManager().setNetworkSettings(network_settings)
        network_settings = global_functions.uvmContext.networkManager().getNetworkSettings()
        interface_id = None
        for interface in network_settings["interfaces"]["list"]:
            if interface["name"] == vlan_interface_name:
                interface_id = interface["interfaceId"]

        ##
        ## Filter block rule for the vlan interface
        ##
        network_settings["filterRules"]["list"].append({
                "blocked": True,
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "conditionType": "SRC_INTF",
                            "invert": False,
                            "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                            "value": interface_id
                        }
                    ]
                },
                "description": "ats block vlan",
                "enabled": True,
                "ipv6Enabled": False,
                "javaClass": "com.untangle.uvm.network.FilterRule",
                "ruleId": -1
        })
        global_functions.uvmContext.networkManager().setNetworkSettings(network_settings)

        # Attempt to ping from the remote network back to us
        # If we are marked for the vlan, this will fail
        ipsecPcLanResult = remote_control.run_command("ping -c 1 %s" % remote_control.client_ip, host=IPSEC_PC_LAN_IP)
        # clear firewall rule in case test fails so it does not affect other tests
        network_settings["filterRules"]["list"] =[]
        global_functions.uvmContext.networkManager().setNetworkSettings(network_settings)
        assert(ipsecPcLanResult == 0)

    def test_025_verifyIPsecBypass(self):           
        if (not tunnelUp):
            raise unittest.SkipTest("Test test_020_createIpsecTunnel success required ")
        ipsecHostLANResultNoFW = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{IPSEC_PC_LAN_IP}/"))
        ipsecHostLANResultnoFWRW = remote_control.run_command("nc -w 2 %s 22 > /dev/null" % remote_control.client_ip, host=IPSEC_PC_LAN_IP)
        assert (ipsecHostLANResultNoFW == 0)
        assert (ipsecHostLANResultnoFWRW == 0)

        # Install firewall rule to generate syslog events
        rules = appFW.getRules()
        rules["list"].append(create_firewall_rule("SRC_ADDR",remote_control.client_ip))
        rules["list"].append(create_firewall_rule("DST_ADDR",remote_control.client_ip))
        appFW.setRules(rules)
        # To and from the client IP should be blocked by the firewall rule
        ipsecHostLANResultFW = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{IPSEC_PC_LAN_IP}/"))
        ipsecHostLANResultFWRW = remote_control.run_command("nc -w 2 %s 22 > /dev/null" % remote_control.client_ip, host=IPSEC_PC_LAN_IP)
        originalAppData = self._app.getSettings()
        appData = copy.deepcopy(originalAppData)
        appData["bypassflag"] = True
        self._app.setSettings(appData)
        # Bypass true on IPsec should bypass firewall rules.
        ipsecHostLANResultFWBypassed = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{IPSEC_PC_LAN_IP}/"))
        ipsecHostLANResultFWBypassedRW = remote_control.run_command("nc -w 2 %s 22 > /dev/null" % remote_control.client_ip, host=IPSEC_PC_LAN_IP)
        # clear out firwall rules before checking results so other tests are not affected.
        rules["list"]=[]
        appFW.setRules(rules)
        # if firewall blocked tunnel request
        assert (ipsecHostLANResultFW != 0)
        # Below line should be un-commented when NGFW-15003 is Fixed
        # assert (ipsecHostLANResultFWRW != 0)  # NGFW-13477
        # if firewall was bypassed.
        assert (ipsecHostLANResultFWBypassed == 0)
        assert (ipsecHostLANResultFWBypassedRW == 0)
        self._app.setSettings(originalAppData)

    def test_030_restartNetworkVerifyIpsecTunnel(self):
        # save a setting in networking and test ipsec tunnel is set connected.
        global tunnelUp
        if (not tunnelUp):
            raise unittest.SkipTest("Test test_020_createIpsecTunnel success required ")
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        # wait for networking to restart
        timeout = 60
        ipsecHostLANResult = 1
        while (ipsecHostLANResult != 0 and timeout > 0):
            timeout -= 1
            time.sleep(1)
            ipsecHostLANResult = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{IPSEC_HOST_LAN_IP}/"))
        ipsecPcLanResult = remote_control.run_command("ping -c 1 %s" % IPSEC_PC_LAN_IP)
        # delete tunnel
        nukeIPSecTunnels(self._app)
        tunnelUp = False
        assert (ipsecHostLANResult == 0)
        assert (ipsecPcLanResult == 0)
        
    def test_040_windowsL2TPlocalDirectory(self):
        wan_IP = global_functions.uvmContext.networkManager().getFirstWanAddress()
        if (l2tpClientHostResult != 0):
            raise unittest.SkipTest("l2tpClientHostResult not available")
        if (not wan_IP in L2TP_SERVER_HOSTS):
            raise unittest.SkipTest("No paired L2TP client available")
        global_functions.uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        appData = self._app.getSettings()
        appData = createL2TPconfig(appData,"LOCAL_DIRECTORY")
        self._app.setSettings(appData)
        timeout = 480
        found = False
        # Send command for Windows VPN connect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_IP,L2TP_LOCAL_USER,L2TP_LOCAL_PASSWORD), host=L2TP_CLIENT_HOST)
        if vpnServerResult == 0:
            while not found and timeout > 0:
                timeout -= 1
                time.sleep(1)
                virtUsers = self._app.getVirtualUsers()
                for user in virtUsers['list']:
                    if user['clientUsername'] == L2TP_LOCAL_USER:
                        found = True
            # Send command for Windows VPN disconnect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_IP), host=L2TP_CLIENT_HOST)
        global_functions.uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        assert(found)
        # Use same user with different password
        new_user_password = "testtest"
        global_functions.uvmContext.localDirectory().setUsers(createLocalDirectoryUser(userpassword=new_user_password))
        appData = createL2TPconfig(appData,"LOCAL_DIRECTORY")
        self._app.setSettings(appData)
        timeout = 480
        found = False
        # Send command for Windows VPN connect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_IP,L2TP_LOCAL_USER,new_user_password), host=L2TP_CLIENT_HOST)
        if vpnServerResult == 0:
            while not found and timeout > 0:
                timeout -= 1
                time.sleep(1)
                virtUsers = self._app.getVirtualUsers()
                for user in virtUsers['list']:
                    if user['clientUsername'] == L2TP_LOCAL_USER:
                        found = True
        # Send command for Windows VPN disconnect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_IP), host=L2TP_CLIENT_HOST)
        global_functions.uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        assert(found)

    def test_042_windowsL2TPAlias(self):
        wan_IP = global_functions.uvmContext.networkManager().getFirstWanAddress()
        device_in_office = global_functions.is_in_office_network(wan_IP)
        # L2TP Alias only works at the office network.
        if not device_in_office:
            raise unittest.SkipTest("Not on office network, skipping")
        if (l2tpClientHostResult != 0):
            raise unittest.SkipTest("l2tpClientHostResult not available")
            
        # Add reserve IP address to WAN only if WAN is static.
        netsettings = copy.deepcopy(orig_netsettings)
        ip_alias_set = False
        for i in range(len(netsettings['interfaces']['list'])):
            if netsettings['interfaces']['list'][i]['configType'] == "ADDRESSED":
                if netsettings['interfaces']['list'][i]['v4ConfigType'] == "STATIC":
                    if netsettings['interfaces']['list'][i]['v4StaticAddress'] == wan_IP:
                        netsettings['interfaces']['list'][i]['v4Aliases']['list'].append(create_alias(L2TP_ALIAS_IP,
                                                                                         netsettings['interfaces']['list'][i]['v4StaticNetmask'],
                                                                                         netsettings['interfaces']['list'][i]['v4StaticPrefix']))
                        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
                        ip_alias_set = True
                        break;

        if not ip_alias_set:
            raise unittest.SkipTest("Unable to set alias IP")
        wan_addresses = [wan_IP,L2TP_ALIAS_IP]
        # Set Local Directory users
        global_functions.uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        orig_app_settings = self._app.getSettings()
        newAppSettings = copy.deepcopy(orig_app_settings)
        newAppSettings = createL2TPconfig(newAppSettings,"LOCAL_DIRECTORY")

        # Set aliases for L2TP
        wan_alias = []
        for idx, val in enumerate(wan_addresses):
            wan_alias.append({'address': val, 'javaClass': 'com.untangle.app.ipsec_vpn.VirtualListen', 'id': idx})
        newAppSettings['virtualListenList']['list'] = wan_alias

        # Set the settings
        self._app.setSettings(newAppSettings)

        # Test both aliases for connectivity
        for wan_addr in wan_addresses:
            timeout = 480
            found = False
            # Send command for Windows VPN connect.
            vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_addr,L2TP_LOCAL_USER,L2TP_LOCAL_PASSWORD), host=L2TP_CLIENT_HOST)
            if vpnServerResult == 0:
                while not found and timeout > 0:
                    timeout -= 1
                    time.sleep(1)
                    virtUsers = self._app.getVirtualUsers()
                    for user in virtUsers['list']:
                        if user['clientUsername'] == L2TP_LOCAL_USER:
                            found = True
                # Send command for Windows VPN disconnect.
            vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_addr), host=L2TP_CLIENT_HOST)
            global_functions.uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
            assert(found)
            # Use same user with different password
            new_user_password = "testtest"
            global_functions.uvmContext.localDirectory().setUsers(createLocalDirectoryUser(userpassword=new_user_password))
            timeout = 480
            found = False
            # Send command for Windows VPN connect.
            vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_addr,L2TP_LOCAL_USER,new_user_password), host=L2TP_CLIENT_HOST)
            if vpnServerResult == 0:
                while not found and timeout > 0:
                    timeout -= 1
                    time.sleep(1)
                    virtUsers = self._app.getVirtualUsers()
                    for user in virtUsers['list']:
                        if user['clientUsername'] == L2TP_LOCAL_USER:
                            found = True
            # Send command for Windows VPN disconnect.
            vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_addr), host=L2TP_CLIENT_HOST)
            # set original user and password
            global_functions.uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
            assert(found)

        # Clean up settings
        global_functions.uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        netsettings['interfaces']['list'][i]['v4Aliases']['list'][:] = []
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        self._app.setSettings(orig_app_settings)

    def test_050_windowsL2TPRadiusDirectory(self):
        global appAD
        wan_IP = global_functions.uvmContext.networkManager().getFirstWanAddress()
        if (radiusResult != 0):
            raise unittest.SkipTest("No RADIUS server available")
        if (l2tpClientHostResult != 0):
            raise unittest.SkipTest("l2tpClientHostResult not available")
        if (not wan_IP in L2TP_SERVER_HOSTS):
            raise unittest.SkipTest("No paired L2TP client available")
        # Configure RADIUS settings
        appAD.setSettings(createRadiusSettings())
        appData = self._app.getSettings()
        appData = createL2TPconfig(appData,"RADIUS_SERVER")
        self._app.setSettings(appData)
        timeout = 480
        found = False
        vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_IP,global_functions.RADIUS_USER,global_functions.RADIUS_PASSWORD), host=L2TP_CLIENT_HOST)
        while not found and timeout > 0:
            timeout -= 1
            time.sleep(1)
            virtUsers = self._app.getVirtualUsers()
            for user in virtUsers['list']:
                if user['clientUsername'] == global_functions.RADIUS_USER:
                    found = True
        # Send command for Windows VPN disconnect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_IP), host=L2TP_CLIENT_HOST)
        assert(found)

    def test_060_createIpsecTunnelHostname(self):
        if (ipsecHostResult != 0):
            raise unittest.SkipTest("No paired IPSec server available")
        pre_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")

        addDNSRule(createDNSRule(IPSEC_HOST,IPSEC_HOST_NAME))
        appData = self._app.getSettings()
        appData["vpnflag"] = False
        appData["tunnels"]["list"].append(build_ipsec_tunnel())
        self._app.setSettings(appData)
        timeout = 10
        ipsecHostLANResult = 1
        while (ipsecHostLANResult != 0 and timeout > 0):
            timeout -= 1
            time.sleep(1)
            # ping the remote LAN to see if the IPsec tunnel is connected.
            ipsecHostLANResult = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{IPSEC_HOST_LAN_IP}/"))
        post_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")
        nukeIPSecTunnels(self._app)
        assert (ipsecHostLANResult == 0)
        # Check to see if the faceplate counters have incremented. 
        assert(pre_events_enabled < post_events_enabled)

    def test_060_xauth_mschap_valid(self):
        """
        Verify xauth + mschap

        There are"better" functional ways to test this:
        1. Setup a Windows client with Shrewsoft VPN client.
        2. Add xauth + mschap to our IPSec implementation and setup a remote tunnel that can be triggered to connect here.

        Both are very difficult.  So for now, we'll verify by making sure appropriate files exist.
        """
        try:
            result = subprocess.check_output("ls -1 /usr/lib/ipsec/plugins/libstrongswan-*xauth* | wc -l", shell=True, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as exc:
            result = exc.output
            pass
        if type(result) is bytes:
            result = result.decode("utf-8")

        assert int(result) > 0, "{int(result)} xauth plugin modules found"

        try:
            result = subprocess.check_output("ls -1 /usr/lib/ipsec/plugins/libstrongswan-*mschap* | wc -l", shell=True, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as exc:
            result = exc.output
            pass
        if type(result) is bytes:
            result = result.decode("utf-8")

        assert int(result) > 0, "{int(result)} mschap plugin modules found"

    def test_080_active_wan_address(self):
        """
        Verify ipsec tunnel can connect to remote that accepts "any" via this systems's active wan address
        """
        ipsec_settings = self._app.getSettings()
        ipsec_settings["tunnels"]["list"] = [build_ipsec_tunnel(local_ip="active_wan_address")]
        self._app.setSettings(ipsec_settings)

        # Ping remote LAN host
        lan_host_ping_result = global_functions.get_wait_for_command_result(command=global_functions.build_ping_command(target=IPSEC_HOST_LAN_IP), success_result=0)
        assert lan_host_ping_result is True, "reached remote lan host"

        # Ping remote LAN client
        lan_client_ping_result = global_functions.get_wait_for_command_result(command=global_functions.build_ping_command(target=IPSEC_PC_LAN_IP), success_result=0)
        assert lan_client_ping_result is True, "reached remote lan client"

        ipsec_settings["tunnels"]["list"] = []
        self._app.setSettings(ipsec_settings)

    def test_081_active_wan_address_failover(self):
        """
        Verify ipsec tunnel failover and failback with WAN failover
        """
        wans = global_functions.get_wan_tuples()

        if len(wans) < 2:
            raise unittest.SkipTest("not enough wan devices")

        app_wan_failover = IPsecTests.get_app("wan-failover")
        for wan in wans:
            interface_id = wan[0]
            test_wan_failover.build_wan_test(interface_id, wf_app=app_wan_failover)

        ipsec_settings = self._app.getSettings()
        ipsec_settings["tunnels"]["list"] = [build_ipsec_tunnel(local_ip="active_wan_address")]
        self._app.setSettings(ipsec_settings)

        # Verify connection comes up
        # Ping remote LAN host
        lan_host_ping_result = global_functions.get_wait_for_command_result(command=global_functions.build_ping_command(target=IPSEC_HOST_LAN_IP), success_result=0)
        assert lan_host_ping_result is True, "reached remote lan host"

        # Ping remote LAN client
        lan_client_ping_result = global_functions.get_wait_for_command_result(command=global_functions.build_ping_command(target=IPSEC_PC_LAN_IP), success_result=0)
        assert lan_client_ping_result is True, "reached remote lan client"

        # Get our primary WAN IP address
        primary_default_address = global_functions.get_wait_for_command_output(command="ip route get 8.8.8.8 | head -1 | cut -d' ' -f3", local=True)
        print(f"primary_default_address={primary_default_address}")

        # "Disable" primary WAN interface with a test that will always fail
        wf_settings = app_wan_failover.getSettings()
        wf_settings["tests"]["list"][0]["type"] = "http"
        wf_settings["tests"]["list"][0]["httpUrl"] = "http://1.2.3.4"
        print(wf_settings)
        app_wan_failover.setSettings(wf_settings)

        # Wait for failure
        assert True is global_functions.get_wait_for_events(
            prefix="wan failover",
            report_category="WAN Failover",
            report_title='Test Events',
            matches={
                "interface_id": wans[0][0],
                "success": False
            },
            tries=30), "found failover event"

        # Get the new default route address which should NOT be primary        
        failover_default_address = global_functions.get_wait_for_command_output(
            command="ip route get 8.8.8.8 | head -1 | cut -d' ' -f3", 
            local=True,
            success_test=lambda address: address != primary_default_address,
            tries=30)
        print(f"failover_default_address={failover_default_address}")

        assert primary_default_address != failover_default_address, "primary_default_address != failover_default_address"

        # Verify connection comes up
        # Ping remote LAN host
        lan_host_ping_result = global_functions.get_wait_for_command_result(command=global_functions.build_ping_command(target=IPSEC_HOST_LAN_IP), success_result=0)
        assert lan_host_ping_result is True, "reached remote lan host"

        # Ping remote LAN client
        lan_client_ping_result = global_functions.get_wait_for_command_result(command=global_functions.build_ping_command(target=IPSEC_PC_LAN_IP), success_result=0)
        assert lan_client_ping_result is True, "reached remote lan client"

        # "Fix" primary's test so it becomes active again
        wf_settings = app_wan_failover.getSettings()
        wf_settings["tests"]["list"][0]["type"] = "ping"
        wf_settings["tests"]["list"][0]["pingHostname"] = "8.8.8.8"
        print(wf_settings)
        app_wan_failover.setSettings(wf_settings)

        # Wait for primary test success
        assert True is global_functions.get_wait_for_events(
            prefix="wan failover",
            report_category="WAN Failover",
            report_title='Test Events',
            matches={
                "interface_id": wans[0][0],
                "success": True
            },
            tries=30), "found failover event"

        # Get the new default route address which should NOT be failback
        failback_default_address = global_functions.get_wait_for_command_output(
            command="ip route get 8.8.8.8 | head -1 | cut -d' ' -f3", 
            local=True,
            success_test=lambda address: address != failover_default_address,
            tries=30)
        print(f"failback_default_address={failback_default_address}")
        assert primary_default_address == failback_default_address, "primary_default_address == failover_default_address"

        # Verify connection comes up
        # Ping remote LAN host
        lan_host_ping_result = global_functions.get_wait_for_command_result(command=global_functions.build_ping_command(target=IPSEC_HOST_LAN_IP), success_result=0)
        assert lan_host_ping_result is True, "reached remote lan host"

        # Ping remote LAN client
        lan_client_ping_result = global_functions.get_wait_for_command_result(command=global_functions.build_ping_command(target=IPSEC_PC_LAN_IP), success_result=0)
        assert lan_client_ping_result is True, "reached remote lan client"

    def test_082_any_remote_tunnel_ping(self):
        """
        Verify ipsec tunnel with any remote does't ping pingAddress and generate Tunnel Connection Events
        """

        # Configure local tunnel with remote any
        org_ipsec_settings = self._app.getSettings()
        ipsec_settings = copy.deepcopy(org_ipsec_settings)
        ipsec_settings["tunnels"]["list"] = [build_ipsec_tunnel(remote_ip="%any", remote_lan=IPSEC_HOST_LAN)]
        self._app.setSettings(ipsec_settings)

        # Add pingAddress in local NGFW tunnel
        ipsec_settings["tunnels"]["list"][0]["pingAddress"] = IPSEC_HOST_LAN_IP
        self._app.setSettings(ipsec_settings)
        time.sleep(40)

        # Check for events logged
        events = global_functions.get_events("IPsec VPN",'Tunnel Connection Events',None,5)
        found = global_functions.check_events( events.get('list'), 5, 
                                              "event_type", "UNREACHABLE" )
        # set to original settings
        self._app.setSettings(org_ipsec_settings)
        assert(found == False)

    @classmethod
    def final_extra_tear_down(cls):
        global appAD, appFW

        # Restore original settings to return to initial settings
        # print("orig_netsettings <%s>" % orig_netsettings)
        if orig_netsettings:
            global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        # Remove Directory Connector
        if appAD != None:
            global_functions.uvmContext.appManager().destroy( appAD.getAppSettings()["id"] )
            appAD = None
        # Remove Firewall
        if appFW != None:
            global_functions.uvmContext.appManager().destroy( appFW.getAppSettings()["id"] )
            appFW = None


test_registry.register_module("ipsec-vpn", IPsecTests)
