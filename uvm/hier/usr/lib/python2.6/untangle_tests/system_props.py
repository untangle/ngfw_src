# -*- coding: utf-8 -*-
import os
import sys
import subprocess
import simplejson as json
import socket
import ipaddr

class SystemProperties():

    global network_settings_file
    network_settings_file = '/usr/share/untangle/settings/untangle-vm/network.js'

    def getInterface(self, name):
        NETCONFIG_JSON_OBJ = json.loads(open(network_settings_file , 'r').read())
        for intf in NETCONFIG_JSON_OBJ['interfaces']['list']:
            if intf['name'] == name:
                return intf
        return None

    def internalInterfaceIP(self):
        # FIXME
        return "UnknownIP"
        
        # intf = self.getInterface("Internal")

        # while intf['configType'] == 'bridge':
        #     intf = self.getInterface(intf['bridgedTo'])

        # if intf['primaryAddressStr'] != None:
        #     return intf['primaryAddressStr'].split("/")[0]
        # else:
        #     return "UnknownIP"

    def findInterfaceIPbyIP(self, remoteIP):
        # finds the first IP address of any interface which is on the same network as the IP passed in
        NETCONFIG_JSON_OBJ = json.loads(open(network_settings_file , 'r').read())
        for intf in NETCONFIG_JSON_OBJ['interfaces']['list']:
            if intf['configType'] == 'ADDRESSED':
                testIP = intf['v4StaticAddress']
                testMask = intf['v4StaticNetmask']
                testRange = testIP + '/' + self.get_net_size(testMask)
                testNet = ipaddr.IPNetwork(testRange)
                testAddr = ipaddr.IPAddress(remoteIP)
                if testAddr in testNet:
                    return testIP
        return None

    def get_net_size(self, netmask):
        netmaskpart = netmask.split('.')
        binary_str = ''
        for octet in netmaskpart:
            binary_str += bin(int(octet))[2:].zfill(8)
        return str(len(binary_str.rstrip('0')))


# debug
# systemProperties = SystemProperties()
# print systemProperties.findInterfaceIPbyIP('192.168.10.31')
