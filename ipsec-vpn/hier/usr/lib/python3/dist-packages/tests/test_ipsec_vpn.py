"""ipsec_vpn tests"""
import time
import copy
import subprocess
import base64
import unittest
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import runtests.overrides as overrides

default_policy_id = 1
appAD = None
appDataRD = None
appFW = None
tunnelUp = False
ipsecTestLAN = ""
orig_netsettings = None

# Defaults for ATS
l2tpServerHosts = overrides.get("l2tpServerHosts", default=
                    ["10.112.56.61","10.112.56.49","10.112.56.89","10.112.11.53","10.112.0.134",
                    "10.112.56.91","10.112.56.94","10.112.56.57","10.112.56.58","10.112.56.59"]
)
l2tpClientHost = overrides.get("l2tpClientHost", default="10.112.56.84")  # Windows 10 using builtin OpenSSH
l2tpAliasIP = overrides.get("l2tpAliasIP", default="10.112.56.200")
l2tpLocalUser = overrides.get("l2tpLocalUser", default="test")
l2tpLocalPassword = overrides.get("l2tpLocalPassword", default="passwd")
ipsecHost = overrides.get("ipsecHost", default="10.112.56.96")
ipsecHostLANIP = overrides.get("ipsecHostLANIP", default="192.168.235.96")
ipsecPcLANIP = overrides.get("ipsecPcLANIP", default="192.168.235.83")
ipsecHostLAN = overrides.get("ipsecHostLAN", default="192.168.235.0/24")
ipsecHostLAN = overrides.get("ipsecHostLAN", default="ipsecsite.untangle.int")
configuredHostIPs = overrides.get("configuredHostIPs", default=
                        [('10.112.0.134','192.168.14.1','192.168.14.0/24'), # ATS
                        ('10.112.56.49','192.168.10.49','192.168.10.0/24'), # QA 1
                        ('10.112.56.61','192.168.10.61','192.168.10.0/24'), # QA 2
                        ('10.112.56.89','10.112.56.89','10.112.56.15/32'), # QA 3 Bridged
                        ('10.112.56.94','192.168.10.94','192.168.10.0/24'), # QA 4 Dual WAN
                        ('10.112.56.57','192.168.4.1','192.168.4.0/24'), # QA box .57
                        ('10.112.56.58','192.168.12.1','192.168.12.0/24'), # QA box .58
                        ('10.112.56.59','192.168.10.59','192.168.10.0/24')] # QA box .59
)

def addIPSecTunnel(remoteIP="", remoteLAN="", localIP="", localLANIP="", localLANRange=""):
    return {
        "active": True, 
        "adapter": "- Custom -", 
        "conntype": "tunnel", 
        "description": "ipsec test profile", 
        "id": 0, 
        "javaClass": "com.untangle.app.ipsec_vpn.IpsecVpnTunnel", 
        "left": localIP,  # local WAN
        "leftSourceIp": localLANIP, # local LAN IP
        "leftSubnet": localLANRange,  # local LAN range
        "pfs": True, 
        "right": remoteIP,  # remote WAN
        "rightSubnet": remoteLAN, # remote LAN range
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


def createLocalDirectoryUser(userpassword=l2tpLocalPassword):
    passwd_encoded = base64.b64encode(userpassword.encode("utf-8"))
    return {'javaClass': 'java.util.LinkedList', 
        'list': [{
            'username': l2tpLocalUser, 
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
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'].insert(0,newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)  

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
            orig_netsettings = uvmContext.networkManager().getNetworkSettings()

        if (uvmContext.appManager().isInstantiated(cls.appNameAD())):
            if cls.skip_instantiated():
                pytest.skip('app %s already instantiated' % cls.appNameAD())
            else:
                appAD = uvmContext.appManager().app(cls.appNameAD())
        else:
            appAD = uvmContext.appManager().instantiate(cls.appNameAD(), default_policy_id)

        appDataRD = appAD.getSettings().get('radiusSettings')
        appFW = uvmContext.appManager().instantiate(cls.appNameFW(), default_policy_id)
        ipsecHostResult = subprocess.call(["ping","-c","1",ipsecHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        l2tpClientHostResult = subprocess.call(["ping","-c","1",l2tpClientHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        radiusResult = subprocess.call(["ping","-c","1",global_functions.RADIUS_SERVER],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_createIpsecTunnel(self):
        global tunnelUp, ipsecTestLAN
        if (ipsecHostResult != 0):
            raise unittest.SkipTest("No paired IPSec server available")
        pre_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")

        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        pairMatchNotFound = True
        listOfPairs = ""
        for hostConfig in configuredHostIPs:
            print(hostConfig[0])
            listOfPairs += str(hostConfig[0]) + ", "
            if (wan_IP in hostConfig[0]):
                appData = self._app.getSettings()
                appData["tunnels"]["list"].append(addIPSecTunnel(ipsecHost,ipsecHostLAN,hostConfig[0],hostConfig[1],hostConfig[2]))
                self._app.setSettings(appData)
                ipsecTestLAN = hostConfig[1]
                pairMatchNotFound = False
        if (pairMatchNotFound):
            raise unittest.SkipTest("IPsec test only configed for IPs %s" % (listOfPairs))
        timeout = 10
        ipsecHostLANResult = 1
        while (ipsecHostLANResult != 0 and timeout > 0):
            timeout -= 1
            time.sleep(1)
            # Access remote LAN to see if the IPsec tunnel is connected.
            ipsecHostLANResult = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{ipsecHostLANIP}/"))
        assert (ipsecHostLANResult == 0)
        ipsecPcLanResult = remote_control.run_command("ping -c 1 %s" % ipsecPcLANIP)
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
        vlan_netspace = uvmContext.netspaceManager().getAvailableAddressSpace("IPv4", 1);
        vlan_host, vlan_netmask = global_functions.cidr_to_netmask(vlan_netspace)
        network_settings["interfaces"]["list"].append({
            "configType": "ADDRESSED",
            "dhcpEnabled": False,
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
        uvmContext.networkManager().setNetworkSettings(network_settings)
        network_settings = uvmContext.networkManager().getNetworkSettings()
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
        uvmContext.networkManager().setNetworkSettings(network_settings)

        # Attempt to ping from the remote network back to us
        # If we are marked for the vlan, this will fail
        ipsecPcLanResult = remote_control.run_command("ping -c 1 %s" % remote_control.client_ip, host=ipsecPcLANIP)
        assert(ipsecPcLanResult != 1)

    def test_025_verifyIPsecBypass(self):           
        if (not tunnelUp):
            raise unittest.SkipTest("Test test_020_createIpsecTunnel success required ")
        ipsecHostLANResultNoFW = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{ipsecPcLANIP}/"))
        ipsecHostLANResultnoFWRW = remote_control.run_command("nc -w 2 %s 22 > /dev/null" % remote_control.client_ip, host=ipsecPcLANIP)
        assert (ipsecHostLANResultNoFW == 0)
        assert (ipsecHostLANResultnoFWRW == 0)

        # Install firewall rule to generate syslog events
        rules = appFW.getRules()
        rules["list"].append(create_firewall_rule("SRC_ADDR",remote_control.client_ip))
        rules["list"].append(create_firewall_rule("DST_ADDR",remote_control.client_ip))
        appFW.setRules(rules)
        # To and from the client IP should be blocked by the firewall rule
        ipsecHostLANResultFW = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{ipsecPcLANIP}/"))
        ipsecHostLANResultFWRW = remote_control.run_command("nc -w 2 %s 22 > /dev/null" % remote_control.client_ip, host=ipsecPcLANIP)
        appData = self._app.getSettings()
        appData["bypassflag"] = True
        self._app.setSettings(appData)
        # Bypass true on IPsec should bypass firewall rules.
        ipsecHostLANResultFWBypassed = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{ipsecPcLANIP}/"))
        ipsecHostLANResultFWBypassedRW = remote_control.run_command("nc -w 2 %s 22 > /dev/null" % remote_control.client_ip, host=ipsecPcLANIP)
        # clear out firwall rules before checking results so other tests are not affected.
        rules["list"]=[]
        appFW.setRules(rules)
        # if firewall blocked tunnel request
        assert (ipsecHostLANResultFW != 0)
        assert (ipsecHostLANResultFWRW != 0)  # NGFW-13477
        # if firewall was bypassed.
        assert (ipsecHostLANResultFWBypassed == 0)
        assert (ipsecHostLANResultFWBypassedRW == 0)

    def test_030_restartNetworkVerifyIpsecTunnel(self):
        # save a setting in networking and test ipsec tunnel is set connected.
        global tunnelUp
        if (not tunnelUp):
            raise unittest.SkipTest("Test test_020_createIpsecTunnel success required ")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        uvmContext.networkManager().setNetworkSettings(netsettings)
        # wait for networking to restart
        timeout = 60
        ipsecHostLANResult = 1
        while (ipsecHostLANResult != 0 and timeout > 0):
            timeout -= 1
            time.sleep(1)
            ipsecHostLANResult = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{ipsecHostLANIP}/"))
        ipsecPcLanResult = remote_control.run_command("ping -c 1 %s" % ipsecPcLANIP)
        # delete tunnel
        nukeIPSecTunnels(self._app)
        tunnelUp = False
        assert (ipsecHostLANResult == 0)
        assert (ipsecPcLanResult == 0)
        
    def test_040_windowsL2TPlocalDirectory(self):
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (l2tpClientHostResult != 0):
            raise unittest.SkipTest("l2tpClientHostResult not available")
        if (not wan_IP in l2tpServerHosts):
            raise unittest.SkipTest("No paired L2TP client available")
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        appData = self._app.getSettings()
        appData = createL2TPconfig(appData,"LOCAL_DIRECTORY")
        self._app.setSettings(appData)
        timeout = 480
        found = False
        # Send command for Windows VPN connect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_IP,l2tpLocalUser,l2tpLocalPassword), host=l2tpClientHost)
        if vpnServerResult == 0:
            while not found and timeout > 0:
                timeout -= 1
                time.sleep(1)
                virtUsers = self._app.getVirtualUsers()
                for user in virtUsers['list']:
                    if user['clientUsername'] == l2tpLocalUser:
                        found = True
            # Send command for Windows VPN disconnect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_IP), host=l2tpClientHost)
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        assert(found)
        # Use same user with different password
        new_user_password = "testtest"
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser(userpassword=new_user_password))
        appData = createL2TPconfig(appData,"LOCAL_DIRECTORY")
        self._app.setSettings(appData)
        timeout = 480
        found = False
        # Send command for Windows VPN connect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_IP,l2tpLocalUser,new_user_password), host=l2tpClientHost)
        if vpnServerResult == 0:
            while not found and timeout > 0:
                timeout -= 1
                time.sleep(1)
                virtUsers = self._app.getVirtualUsers()
                for user in virtUsers['list']:
                    if user['clientUsername'] == l2tpLocalUser:
                        found = True
        # Send command for Windows VPN disconnect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_IP), host=l2tpClientHost)
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        assert(found)

    def test_042_windowsL2TPAlias(self):
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
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
                        netsettings['interfaces']['list'][i]['v4Aliases']['list'].append(create_alias(l2tpAliasIP,
                                                                                         netsettings['interfaces']['list'][i]['v4StaticNetmask'],
                                                                                         netsettings['interfaces']['list'][i]['v4StaticPrefix']))
                        uvmContext.networkManager().setNetworkSettings(netsettings)
                        ip_alias_set = True
                        break;

        if not ip_alias_set:
            raise unittest.SkipTest("Unable to set alias IP")
        wan_addresses = [wan_IP,l2tpAliasIP]
        # Set Local Directory users
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
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
            vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_addr,l2tpLocalUser,l2tpLocalPassword), host=l2tpClientHost)
            if vpnServerResult == 0:
                while not found and timeout > 0:
                    timeout -= 1
                    time.sleep(1)
                    virtUsers = self._app.getVirtualUsers()
                    for user in virtUsers['list']:
                        if user['clientUsername'] == l2tpLocalUser:
                            found = True
                # Send command for Windows VPN disconnect.
            vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_addr), host=l2tpClientHost)
            uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
            assert(found)
            # Use same user with different password
            new_user_password = "testtest"
            uvmContext.localDirectory().setUsers(createLocalDirectoryUser(userpassword=new_user_password))
            timeout = 480
            found = False
            # Send command for Windows VPN connect.
            vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_addr,l2tpLocalUser,new_user_password), host=l2tpClientHost)
            if vpnServerResult == 0:
                while not found and timeout > 0:
                    timeout -= 1
                    time.sleep(1)
                    virtUsers = self._app.getVirtualUsers()
                    for user in virtUsers['list']:
                        if user['clientUsername'] == l2tpLocalUser:
                            found = True
            # Send command for Windows VPN disconnect.
            vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_addr), host=l2tpClientHost)
            # set original user and password
            uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
            assert(found)

        # Clean up settings
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        netsettings['interfaces']['list'][i]['v4Aliases']['list'][:] = []
        uvmContext.networkManager().setNetworkSettings(netsettings)
        self._app.setSettings(orig_app_settings)

    def test_050_windowsL2TPRadiusDirectory(self):
        global appAD
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (radiusResult != 0):
            raise unittest.SkipTest("No RADIUS server available")
        if (l2tpClientHostResult != 0):
            raise unittest.SkipTest("l2tpClientHostResult not available")
        if (not wan_IP in l2tpServerHosts):
            raise unittest.SkipTest("No paired L2TP client available")
        # Configure RADIUS settings
        appAD.setSettings(createRadiusSettings())
        appData = self._app.getSettings()
        appData = createL2TPconfig(appData,"RADIUS_SERVER")
        self._app.setSettings(appData)
        timeout = 480
        found = False
        vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_IP,global_functions.RADIUS_USER,global_functions.RADIUS_PASSWORD), host=l2tpClientHost)
        while not found and timeout > 0:
            timeout -= 1
            time.sleep(1)
            virtUsers = self._app.getVirtualUsers()
            for user in virtUsers['list']:
                if user['clientUsername'] == global_functions.RADIUS_USER:
                    found = True
        # Send command for Windows VPN disconnect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_IP), host=l2tpClientHost)
        assert(found)

    def test_060_createIpsecTunnelHostname(self):
        if (ipsecHostResult != 0):
            raise unittest.SkipTest("No paired IPSec server available")
        pre_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")

        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        pairMatchNotFound = True
        listOfPairs = ""
        addDNSRule(createDNSRule(ipsecHost,ipsecHostname))
        # verify L2TP is off  NGFW-7212
        ipsecSettings = self._app.getSettings()
        ipsecSettings["vpnflag"] = False
        self._app.setSettings(ipsecSettings)
        for hostConfig in configuredHostIPs:
            print(hostConfig[0])
            listOfPairs += str(hostConfig[0]) + ", "
            if (wan_IP in hostConfig[0]):
                appData = self._app.getSettings()
                appData["tunnels"]["list"].append(addIPSecTunnel(ipsecHostname,ipsecHostLAN,hostConfig[0],hostConfig[1],hostConfig[2]))
                self._app.setSettings(appData)
                pairMatchNotFound = False
        if (pairMatchNotFound):
            raise unittest.SkipTest("IPsec test only configed for IPs %s" % (listOfPairs))
        timeout = 10
        ipsecHostLANResult = 1
        while (ipsecHostLANResult != 0 and timeout > 0):
            timeout -= 1
            time.sleep(1)
            # ping the remote LAN to see if the IPsec tunnel is connected.
            ipsecHostLANResult = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", ignore_certificate=True, tries=2, timeout=5, uri=f"http://{ipsecHostLANIP}/"))
        post_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")
        nukeIPSecTunnels(self._app)
        assert (ipsecHostLANResult == 0)
        # Check to see if the faceplate counters have incremented. 
        assert(pre_events_enabled < post_events_enabled)

    @classmethod
    def final_extra_tear_down(cls):
        global appAD
        global appFW
        # Restore original settings to return to initial settings
        # print("orig_netsettings <%s>" % orig_netsettings)
        if orig_netsettings:
            uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        # Remove Directory Connector
        if appAD != None:
            uvmContext.appManager().destroy( appAD.getAppSettings()["id"] )
            appAD = None
        # Remove Firewall
        if appFW != None:
            uvmContext.appManager().destroy( appFW.getAppSettings()["id"] )
            appFW = None


test_registry.register_module("ipsec-vpn", IPsecTests)
