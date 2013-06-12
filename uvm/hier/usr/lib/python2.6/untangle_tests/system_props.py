# -*- coding: utf-8 -*-
import os
import sys
import subprocess
import simplejson as json
import socket
import ipaddr
import fcntl
import struct

class SystemProperties():

    global network_settings_file
    network_settings_file = '/usr/share/untangle/settings/untangle-vm/network.js'

    def getInterface(self, name):
        NETCONFIG_JSON_OBJ = json.loads(open(network_settings_file , 'r').read())
        for intf in NETCONFIG_JSON_OBJ['interfaces']['list']:
            if intf['name'] == name:
                return intf
        return None


    def findInterfaceIPbyIP(self, remoteIP):
        # finds the first IP address of any interface which is on the same network as the IP passed in
        NETCONFIG_JSON_OBJ = json.loads(open(network_settings_file , 'r').read())
        for intf in NETCONFIG_JSON_OBJ['interfaces']['list']:
            # is interface configured
            if (intf['configType'] == 'ADDRESSED'):
                if (intf['v4ConfigType'] == 'STATIC'):
                    # is this a static IP
                    testIP = intf['v4StaticAddress']
                    testMask = intf['v4StaticNetmask']
                    testRange = testIP + '/' + self.get_net_size(testMask)
                    testNet = ipaddr.IPNetwork(testRange)
                    testAddr = ipaddr.IPAddress(remoteIP)
                    if testAddr in testNet:
                        return testIP
                    else:
                        pass
                elif (intf['v4ConfigType'] == 'AUTO'):
                    # is this a dynamic IP
                    nicDevice = str(intf['physicalDev'])
                    testIP = self.get_ip_address(nicDevice)
                    testMask =  self.get_netmask(nicDevice)
                    testRange = testIP + '/' + self.get_net_size(testMask)
                    testNet = ipaddr.IPNetwork(testRange)
                    testAddr = ipaddr.IPAddress(remoteIP)
                    if testAddr in testNet:
                        return testIP
                    else:
                        pass
                else:
                    pass
            else:
                pass
        return None

    def get_net_size(self, netmask):
        netmaskpart = netmask.split('.')
        binary_str = ''
        for octet in netmaskpart:
            binary_str += bin(int(octet))[2:].zfill(8)
        return str(len(binary_str.rstrip('0')))

    def get_ip_address(self, ifname):
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        return socket.inet_ntoa(fcntl.ioctl(
            s.fileno(),
            0x8915,  # SIOCGIFADDR
            struct.pack('256s', ifname[:15])
        )[20:24])

    def get_netmask(self, ifname):
        s= socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        return socket.inet_ntoa(fcntl.ioctl(
            s, 
            35099,
            struct.pack('256s', ifname)
        )[20:24])    
        
# debug
# systemProperties = SystemProperties()
# print "return value <%s>" % systemProperties.findInterfaceIPbyIP('10.5.6.60')
# print "return value <%s>" % systemProperties.get_ip_address('eth0')
# print "return value <%s>" % systemProperties.get_ip_address('eth4')
