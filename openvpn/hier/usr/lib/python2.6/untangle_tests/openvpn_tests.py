import unittest2
import time
import sys
import pdb
import os
import re
import subprocess
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
from untangle_tests import ClientControl
from untangle_tests import TestDict

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
clientControl = ClientControl()
nodeData = None
node = None
vpnClientName = "atsclient"
vpnFullClientName = "atsfullclient"
vpnHostResult = 0
vpnClientResult = 0 
vpnServerResult = 0
qaHostVPN = "10.111.56.57"
qaHostVPNLanIP = "192.168.234.57"
# special box with testshell in the sudoer group
# using no password and openvpn installed.
qaClientVPN = "10.111.56.32"  
tunnelUp = False

#pdb.set_trace()

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

def nukeDNSRules():
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'][:] = []
    uvmContext.networkManager().setNetworkSettings(netsettings)    

def createDNSRule( networkAddr, name):
    return {
        "address": networkAddr, 
        "javaClass": "com.untangle.uvm.network.DnsStaticEntry", 
        "name": name
         }

def appendDNSRule(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['dnsSettings']['staticEntries']['list'].append(newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)

def setUpClient(vpn_enabled=True,vpn_export=False,vpn_exportNetwork="127.0.0.1",vpn_groupId=1,vpn_name=vpnClientName):
    return {
            "enabled": vpn_enabled, 
            "export": vpn_export, 
            "exportNetwork": vpn_exportNetwork, 
            "groupId": vpn_groupId, 
            "javaClass": "com.untangle.node.openvpn.OpenVpnRemoteClient", 
            "name": vpn_name
    }

class OpenVpnTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-openvpn"

    @staticmethod
    def nodeWebName():
        return "untangle-node-sitefilter"

    @staticmethod
    def vendorName():
        return "Untangle"
        
    def setUp(self):
        global node, nodeWeb, nodeData, vpnHostResult, vpnClientResult, vpnServerResult
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            node.start()
            nodeWeb = uvmContext.nodeManager().instantiate(self.nodeWebName(), defaultRackId)
            vpnHostResult = subprocess.call(["ping","-c","1",qaHostVPN],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            vpnClientResult = subprocess.call(["ping","-c","1",qaClientVPN],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            serverVPNIP = uvmContext.networkManager().getFirstWanAddress()
            vpnServerResult = os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"ping -c 1 " + serverVPNIP + " >/dev/null 2>&1\"")

    # verify client is online
    def test_010_clientIsOnline(self):
        ClientControl.verbosity = 1
        result = clientControl.runCommand("wget -4 -t 2 --timeout=5 -o /dev/null http://test.untangle.com/")
        assert (result == 0)

    def test_020_createVPNTunnel(self):
        global tunnelUp
        if (vpnHostResult != 0):
            raise unittest2.SkipTest("No paried VPN server available")
        # Download remote system VPN config
        result = os.system("wget -o /dev/null -t 1 --timeout=3 http://test.untangle.com/test/config-9.4-ats-test-site2-site-client.zip -O /tmp/config.zip")
        assert (result == 0) # verify the download was successful
        node.importClientConfig("/tmp/config.zip")
        # nodeData = node.getSettings()
        # print nodeData
        time.sleep(10) # wait for vpn tunnel to form
        remoteHostResult = subprocess.call(["ping","-c","1",qaHostVPNLanIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        listOfServers = node.getRemoteServersStatus()
        assert (remoteHostResult == 0)
        assert(listOfServers['list'][0]['connected'])
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
            if (remoteGuest['name'] == 'test'):
                found = True 
            if (not found):
                i+=1
        assert (found) # test profile not found in remoteServers list
        nodeData['remoteServers']['list'][i]['enabled'] = False
        node.setSettings(nodeData)
        time.sleep(10) # wait for vpn tunnel to fall
        remoteHostResult = subprocess.call(["ping","-c","1",qaHostVPNLanIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        assert (remoteHostResult != 0)
        tunnelUp = False
        
    def test_040_createClientVPNTunnel(self):
        global nodeData, vpnServerResult, vpnClientResult
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest2.SkipTest("No paried VPN client available")
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
        os.system("scp -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key /tmp/clientconfig.zip testshell@" + qaClientVPN + ":/tmp/>/dev/null 2>&1")
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"sudo unzip -o /tmp/clientconfig.zip -d /tmp/ >/dev/null 2>&1\"")
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"sudo rm -f /etc/openvpn/*.conf; sudo rm -f /etc/openvpn/*.ovpn; sudo rm -rf /etc/openvpn/keys\"")
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"sudo mv -f /tmp/untangle-vpn/* /etc/openvpn/\"")
        # connect openvpn from the PC to the Untangle server.
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &\"")
        time.sleep(10) # wait for vpn tunnel to form 
        # ping the test host behind the Untangle from the remote testbox
        result = os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"ping -c 2 "+ clientControl.hostIP +" >/dev/null 2>&1\"")
        
        listOfClients = node.getActiveClients()
        print "address " + listOfClients['list'][0]['address']
        print "vpn address 1 " + listOfClients['list'][0]['poolAddress']

        host_result = clientControl.runCommand("host test.untangle.com", True)
        # print "host_result <%s>" % host_result
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')
        # print "IP address of test.untangle.com <%s>" % ip_address_testuntangle
        nukeDNSRules()
        test_dns_name = "testname.ats.com"
        appendDNSRule(createDNSRule(ip_address_testuntangle,test_dns_name))
        # time.sleep(5) # wait for DNS to refresh
        dns_name_result = os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"ping -c 2 "+ test_dns_name +" >/dev/null 2>&1\"")

        # stop the vpn tunnel on remote box
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"sudo pkill openvpn\"")
        nukeDNSRules()
        assert(result==0)
        assert(dns_name_result==0)
        assert(listOfClients['list'][0]['address'] == qaClientVPN)

        # check event log
        flushEvents()
        query = None;
        for q in node.getStatusEventsQueries():
            if q['name'] == 'Connections': query = q;
        assert(query != None)
        events = uvmContext.getEvents(query['query'],defaultRackId,1)
        # print events['list']
        assert(events != None)
        assert(events['list'] != None)
        assert(len(events['list']) > 0)
        assert(events['list'][0]['remote_address'] == qaClientVPN)
        assert(events['list'][0]['client_name'] == vpnClientName)
        
    def test_050_createClientVPNFullTunnel(self):
        global nodeData, vpnServerResult, vpnClientResult
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest2.SkipTest("No paried VPN client available")
        nodeData = node.getSettings()
        nodeData["serverEnabled"]=True
        siteName = nodeData['siteName']  
        nodeData['remoteClients']['list'][:] = []  
        nodeData['remoteClients']['list'].append(setUpClient(vpn_name=vpnFullClientName))
        nodeData['groups']['list'][0]['fullTunnel'] = True
        node.setSettings(nodeData)
        clientLink = node.getClientDistributionDownloadLink(vpnFullClientName,"zip")
        # print clientLink

        # download client config file
        result = os.system("wget -o /dev/null -t 1 --timeout=3 http://localhost"+clientLink+" -O /tmp/clientconfig.zip")
        assert (result == 0)
        # Copy the config file to the remote PC, unzip the files and move to the openvpn directory on the remote device
        os.system("scp -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key /tmp/clientconfig.zip testshell@" + qaClientVPN + ":/tmp/>/dev/null 2>&1")
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"sudo unzip -o /tmp/clientconfig.zip -d /tmp/ >/dev/null 2>&1\"")
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"sudo rm -f /etc/openvpn/*.conf; sudo rm -f /etc/openvpn/*.ovpn; sudo rm -rf /etc/openvpn/keys\"")
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"sudo mv -f /tmp/untangle-vpn/* /etc/openvpn/\"")
        # Connect openvpn from the PC to the Untangle server.
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + qaClientVPN + " \"cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &\"")
        timeout = 50
        ping_result = 0
        while not ping_result and timeout > 0:
            time.sleep(1)
            timeout -= 1
            ping_result = os.system("ping -c 1 " + qaClientVPN + " >/dev/null 2>&1")
        time.sleep(5) # wait for vpn tunnel to form 
        flushEvents()
        listOfClients = node.getActiveClients()
        vpn_address = listOfClients['list'][0]['poolAddress']

        # ping the test host behind the Untangle from the remote testbox
        print "vpn address " + vpn_address
        timeout = 50
        result1 = 1
        while result1 and timeout > 0:
            time.sleep(1)
            timeout -= 1
            result1 = os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + vpn_address + " \"ls -l >/dev/null 2>&1\"")
        result2 = os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + vpn_address + " \"ping -c 2 " +  ClientControl.hostIP + ">/dev/null 2>&1\"")
        # print "look for block page"
        webresult = os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + vpn_address + " \"wget -q -O - http://www.playboy.com | grep -q blockpage\"")
        # print "result1 <%d> result2 <%d> webresult <%d>" % (result1,result2,webresult)

        # stop the vpn tunnel on remote box
        nodeData['remoteClients']['list'][:] = []  
        node.setSettings(nodeData)
        os.system("ssh -o 'StrictHostKeyChecking=no' -i @PREFIX@/usr/lib/python2.6/untangle_tests/testShell.key testshell@" + vpn_address + " \"nohup sudo pkill openvpn >/dev/null 2>&1 \"")
        time.sleep(5) # wait for vpn tunnel to go down 
        # print ("result " + str(result) + " webresult " + str(webresult))
        assert(listOfClients['list'][0]['address'] == qaClientVPN)
        assert(result1==0)
        assert(result2==0)
        assert(webresult==0)


    def test_999_finalTearDown(self):
        global node
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        uvmContext.nodeManager().destroy( nodeWeb.getNodeSettings()["id"] )
        node = None

TestDict.registerNode("openvpn", OpenVpnTests)

