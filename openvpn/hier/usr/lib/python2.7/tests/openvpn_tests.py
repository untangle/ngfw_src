import unittest2
import time
import sys
import pdb
import os
import re
import subprocess

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import system_properties
import global_functions

defaultRackId = 1
nodeData = None
node = None
nodeWeb = None
vpnClientName = "atsclient"
vpnFullClientName = "atsfullclient"
vpnHostResult = 0
vpnClientResult = 0 
vpnServerResult = 0
vpnSite2SiteFile = "http://test.untangle.com/test/openvpn-site2site10-config.zip"
vpnSite2SiteHostname = "untangle-268"
tunnelUp = False

def setUpClient(vpn_enabled=True,vpn_export=False,vpn_exportNetwork="127.0.0.1",vpn_groupId=1,vpn_name=vpnClientName):
    return {
            "enabled": vpn_enabled, 
            "export": vpn_export, 
            "exportNetwork": vpn_exportNetwork, 
            "groupId": vpn_groupId, 
            "javaClass": "com.untangle.node.openvpn.OpenVpnRemoteClient", 
            "name": vpn_name
    }

def waitForServerVPNtoConnect():
    timeout = 60  # wait for up to one minute for the VPN to connect
    while timeout > 0:
        time.sleep(1)
        timeout -= 1
        listOfServers = node.getRemoteServersStatus()
        if (len(listOfServers['list']) > 0):
            if (listOfServers['list'][0]['connected']):
                # VPN has connected
                break;
    return timeout

def waitForClientVPNtoConnect():
    timeout = 120  # wait for up to two minute for the VPN to connect
    while timeout > 0:
        time.sleep(1)
        timeout -= 1
        listOfServers = node.getActiveClients()
        if (len(listOfServers['list']) > 0):
            if (listOfServers['list'][0]['clientName']):
                # VPN has connected
                time.sleep(5) # wait for client to get connectivity
                break;
    return timeout

def waitForPing(target_IP="127.0.0.1",ping_result_expected=0):
    timeout = 60  # wait for up to one minute for the target ping result
    ping_result = False
    while timeout > 0:
        time.sleep(1)
        timeout -= 1
        result = subprocess.call(["ping","-W","5","-c","1",global_functions.vpnServerVpnLanIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if (result == ping_result_expected):
            # target is reachable if succ
            ping_result = True
            break;
    return ping_result

class OpenVpnTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-openvpn"

    @staticmethod
    def nodeWebName():
        return "untangle-node-web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"
        
    @staticmethod
    def initialSetUp(self):
        global node, nodeWeb, nodeData, vpnHostResult, vpnClientResult, vpnServerResult
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        node.start()
        nodeWeb = None
        if (uvmContext.nodeManager().isInstantiated(self.nodeWebName())):
            raise Exception('node %s already instantiated' % self.nodeWebName())
        nodeWeb = uvmContext.nodeManager().instantiate(self.nodeWebName(), defaultRackId)
        vpnHostResult = subprocess.call(["ping","-W","5","-c","1",global_functions.vpnServerVpnIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        vpnClientResult = subprocess.call(["ping","-W","5","-c","1",global_functions.vpnClientVpnIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        wanIP = uvmContext.networkManager().getFirstWanAddress()
        if vpnClientResult == 0:
            vpnServerResult = remote_control.runCommand("ping -W 5 -c 1 " + wanIP, host=global_functions.vpnClientVpnIP)
        else:
            vpnServerResult = 1

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_020_createVPNTunnel(self):
        global tunnelUp
        tunnelUp = False
        if (vpnHostResult != 0):
            raise unittest2.SkipTest("No paried VPN server available")
        # Download remote system VPN config
        result = os.system("wget -o /dev/null -t 1 --timeout=3 " + vpnSite2SiteFile + " -O /tmp/config.zip")
        assert (result == 0) # verify the download was successful
        node.importClientConfig("/tmp/config.zip")
        # wait for vpn tunnel to form
        timeout = waitForServerVPNtoConnect()
        # If VPN tunnel has failed to connect, fail the test,
        assert(timeout > 0)

        remoteHostResult = waitForPing(global_functions.vpnServerVpnLanIP,0)
        assert (remoteHostResult)
        listOfServers = node.getRemoteServersStatus()
        # print listOfServers
        assert(listOfServers['list'][0]['name'] == vpnSite2SiteHostname)
        tunnelUp = True
   
    def test_030_disableRemoteClientVPNTunnel(self):
        global tunnelUp 
        if (not tunnelUp):
            raise unittest2.SkipTest("previous test test_020_createVPNTunnel failed")
        nodeData = node.getSettings()
        # print nodeData
        i=0
        found = False
        for remoteGuest in nodeData['remoteServers']['list']:
            if (remoteGuest['name'] == vpnSite2SiteHostname):
                found = True 
            if (not found):
                i+=1
        assert (found) # test profile not found in remoteServers list
        nodeData['remoteServers']['list'][i]['enabled'] = False
        node.setSettings(nodeData)
        remoteHostResult = waitForPing(global_functions.vpnServerVpnLanIP,1)
        assert (remoteHostResult)
        tunnelUp = False
        
    def test_040_createClientVPNTunnel(self):
        global nodeData, vpnServerResult, vpnClientResult
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest2.SkipTest("No paried VPN client available")

        pre_events_connect = global_functions.getStatusValue(node,"connect")
        
        running = remote_control.runCommand("pidof openvpn", host=global_functions.vpnClientVpnIP,)
        loopLimit = 5
        while ((running == 0) and (loopLimit > 0)):
            # OpenVPN is running, wait 5 sec to see if openvpm is done
            loopLimit -= 1
            time.sleep(5)
            running = remote_control.runCommand("pidof openvpn", host=global_functions.vpnClientVpnIP)
        if loopLimit == 0:
            # try killing the openvpn session as it is probably stuck
            remote_control.runCommand("sudo pkill openvpn", host=global_functions.vpnClientVpnIP)
            time.sleep(2)
            running = remote_control.runCommand("pidof openvpn", host=global_functions.vpnClientVpnIP)
        if running == 0:
            raise unittest2.SkipTest("OpenVPN test machine already in use")
            
        nodeData = node.getSettings()
        nodeData["serverEnabled"]=True
        siteName = nodeData['siteName']
        nodeData['remoteClients']['list'][:] = []  
        nodeData['remoteClients']['list'].append(setUpClient())
        node.setSettings(nodeData)
        clientLink = node.getClientDistributionDownloadLink(vpnClientName,"zip")
        # print clientLink

        # download client config file
        result = os.system("wget -o /dev/null -t 1 --timeout=3 http://localhost"+clientLink+" -O /tmp/clientconfig.zip")
        assert (result == 0)
        # copy the config file to the remote PC, unzip the files and move to the openvpn directory on the remote device
        result = os.system("scp -o 'StrictHostKeyChecking=no' -i " + system_properties.get_prefix() + "/usr/lib/python2.7/tests/testShell.key /tmp/clientconfig.zip testshell@" + global_functions.vpnClientVpnIP + ":/tmp/>/dev/null 2>&1")
        assert (result == 0)

        remote_control.runCommand("sudo unzip -o /tmp/clientconfig.zip -d /tmp/", host=global_functions.vpnClientVpnIP)
        remote_control.runCommand("sudo rm -f /etc/openvpn/*.conf; sudo rm -f /etc/openvpn/*.ovpn; sudo rm -rf /etc/openvpn/keys", host=global_functions.vpnClientVpnIP)
        remote_control.runCommand("sudo mv -f /tmp/untangle-vpn/* /etc/openvpn/", host=global_functions.vpnClientVpnIP)
        remote_control.runCommand("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.vpnClientVpnIP)

        timeout = waitForClientVPNtoConnect()
        # If VPN tunnel has failed to connect so fail the test,
        assert(timeout > 0)
        # ping the test host behind the Untangle from the remote testbox
        result = remote_control.runCommand("ping -c 2 " + remote_control.clientIP, host=global_functions.vpnClientVpnIP)
        
        listOfClients = node.getActiveClients()
        print "address " + listOfClients['list'][0]['address']
        print "vpn address 1 " + listOfClients['list'][0]['poolAddress']

        host_result = remote_control.runCommand("host test.untangle.com", stdout=True)
        # print "host_result <%s>" % host_result
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')

        # stop the vpn tunnel on remote box
        remote_control.runCommand("sudo pkill openvpn", host=global_functions.vpnClientVpnIP)
        time.sleep(3) # openvpn takes time to shut down

        assert(result==0)
        assert(listOfClients['list'][0]['address'] == global_functions.vpnClientVpnIP)

        events = global_functions.get_events('OpenVPN','Connection Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'remote_address', global_functions.vpnClientVpnIP,
                                            'client_name', vpnClientName )
        assert( found )

        # Check to see if the faceplate counters have incremented. 
        post_events_connect = global_functions.getStatusValue(node, "connect")
        assert(pre_events_connect < post_events_connect)
        
    def test_050_createClientVPNFullTunnel(self):
        global nodeData, vpnServerResult, vpnClientResult
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest2.SkipTest("No paried VPN client available")
        running = remote_control.runCommand("pidof openvpn", host=global_functions.vpnClientVpnIP)
        if running == 0:
            raise unittest2.SkipTest("OpenVPN test machine already in use")
        nodeData = node.getSettings()
        nodeData["serverEnabled"]=True
        siteName = nodeData['siteName']  
        nodeData['remoteClients']['list'][:] = []  
        nodeData['remoteClients']['list'].append(setUpClient(vpn_name=vpnFullClientName))
        nodeData['groups']['list'][0]['fullTunnel'] = True
        nodeData['groups']['list'][0]['fullTunnel'] = True
        node.setSettings(nodeData)
        clientLink = node.getClientDistributionDownloadLink(vpnFullClientName,"zip")
        # print clientLink

        # download client config file
        result = os.system("wget -o /dev/null -t 1 --timeout=3 http://localhost"+clientLink+" -O /tmp/clientconfig.zip")
        assert (result == 0)
        # Copy the config file to the remote PC, unzip the files and move to the openvpn directory on the remote device
        os.system("scp -o 'StrictHostKeyChecking=no' -i " + system_properties.get_prefix() + "/usr/lib/python2.7/tests/testShell.key /tmp/clientconfig.zip testshell@" + global_functions.vpnClientVpnIP + ":/tmp/>/dev/null 2>&1")
        remote_control.runCommand("sudo unzip -o /tmp/clientconfig.zip -d /tmp/", host=global_functions.vpnClientVpnIP)
        remote_control.runCommand("sudo rm -f /etc/openvpn/*.conf; sudo rm -f /etc/openvpn/*.ovpn; sudo rm -rf /etc/openvpn/keys", host=global_functions.vpnClientVpnIP)
        remote_control.runCommand("sudo mv -f /tmp/untangle-vpn/* /etc/openvpn/", host=global_functions.vpnClientVpnIP)
        remote_control.runCommand("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.vpnClientVpnIP)

        time.sleep(10) # wait for vpn tunnel to form 

        listOfClients = node.getActiveClients()
        vpnPoolAddressIP = listOfClients['list'][0]['poolAddress']

        # ping the test host behind the Untangle from the remote testbox
        print "vpn pool address: " + vpnPoolAddressIP

        result1 = 1
        tries = 10
        while result1 and tries > 0:
            time.sleep(1)
            tries -= 1
            result1 = os.system("ping -c1 " + vpnPoolAddressIP + " >/dev/null 2>&1")
        result2 = remote_control.runCommand("ping -c 2 " + remote_control.clientIP, host=global_functions.vpnClientVpnIP)

        # run a web request to internet and make sure it goes through web filter
        # webresult = remote_control.runCommand("wget -q -O - http://www.playboy.com | grep -q blockpage", host=vpnPoolAddressIP)
        webresult = remote_control.runCommand("wget -q -O - http://www.playboy.com | grep -q blockpage", host=global_functions.vpnClientVpnIP)

        print "result1 <%d> result2 <%d> webresult <%d>" % (result1,result2,webresult)

        # Shutdown VPN on both sides.
        # remote_control.runCommand("sudo pkill openvpn < /dev/null > /dev/null 2>&1 &", host=vpnPoolAddressIP)
        remote_control.runCommand("sudo pkill openvpn", host=global_functions.vpnClientVpnIP)
        time.sleep(3) # openvpn takes time to shut down

        nodeData['remoteClients']['list'][:] = []  
        node.setSettings(nodeData)
        time.sleep(5) # wait for vpn tunnel to go down 
        # print ("result " + str(result) + " webresult " + str(webresult))
        assert(listOfClients['list'][0]['address'] == global_functions.vpnClientVpnIP)
        assert(result1==0)
        assert(result2==0)
        assert(webresult==0)

    @staticmethod
    def finalTearDown(self):
        global node, nodeWeb
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        if nodeWeb != None:
            uvmContext.nodeManager().destroy( nodeWeb.getNodeSettings()["id"] )
            nodeWeb = None

test_registry.registerNode("openvpn", OpenVpnTests)
