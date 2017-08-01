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
import global_functions

defaultRackId = 1
appData = None
app = None
appWeb = None
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
            "javaClass": "com.untangle.app.openvpn.OpenVpnRemoteClient", 
            "name": vpn_name
    }

def create_export(network, name="export", enabled=True):
    return {
        "javaClass": "com.untangle.app.openvpn.OpenVpnExport", 
        "name": name,
        "enabled": enabled,
        "network": network
    }

def waitForServerVPNtoConnect():
    timeout = 60  # wait for up to one minute for the VPN to connect
    while timeout > 0:
        time.sleep(1)
        timeout -= 1
        listOfServers = app.getRemoteServersStatus()
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
        listOfServers = app.getActiveClients()
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
    def appName():
        return "openvpn"

    @staticmethod
    def appWebName():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"
        
    @staticmethod
    def initialSetUp(self):
        global app, appWeb, appData, vpnHostResult, vpnClientResult, vpnServerResult
        if (uvmContext.appManager().isInstantiated(self.appName())):
            raise Exception('app %s already instantiated' % self.appName())
        app = uvmContext.appManager().instantiate(self.appName(), defaultRackId)
        app.start()
        appWeb = None
        if (uvmContext.appManager().isInstantiated(self.appWebName())):
            raise Exception('app %s already instantiated' % self.appWebName())
        appWeb = uvmContext.appManager().instantiate(self.appWebName(), defaultRackId)
        vpnHostResult = subprocess.call(["ping","-W","5","-c","1",global_functions.vpnServerVpnIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        vpnClientResult = subprocess.call(["ping","-W","5","-c","1",global_functions.vpnClientVpnIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        wanIP = uvmContext.networkManager().getFirstWanAddress()
        if vpnClientResult == 0:
            vpnServerResult = remote_control.run_command("ping -W 5 -c 1 " + wanIP, host=global_functions.vpnClientVpnIP)
        else:
            vpnServerResult = 1

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_020_createVPNTunnel(self):
        global tunnelUp
        tunnelUp = False
        if (vpnHostResult != 0):
            raise unittest2.SkipTest("No paried VPN server available")
        # Download remote system VPN config
        result = os.system("wget -o /dev/null -t 1 --timeout=3 " + vpnSite2SiteFile + " -O /tmp/config.zip")
        assert (result == 0) # verify the download was successful
        app.importClientConfig("/tmp/config.zip")
        # wait for vpn tunnel to form
        timeout = waitForServerVPNtoConnect()
        # If VPN tunnel has failed to connect, fail the test,
        assert(timeout > 0)

        remoteHostResult = waitForPing(global_functions.vpnServerVpnLanIP,0)
        assert (remoteHostResult)
        listOfServers = app.getRemoteServersStatus()
        # print listOfServers
        assert(listOfServers['list'][0]['name'] == vpnSite2SiteHostname)
        tunnelUp = True
   
    def test_030_disableRemoteClientVPNTunnel(self):
        global tunnelUp 
        if (not tunnelUp):
            raise unittest2.SkipTest("previous test test_020_createVPNTunnel failed")
        appData = app.getSettings()
        # print appData
        i=0
        found = False
        for remoteGuest in appData['remoteServers']['list']:
            if (remoteGuest['name'] == vpnSite2SiteHostname):
                found = True 
            if (not found):
                i+=1
        assert (found) # test profile not found in remoteServers list
        appData['remoteServers']['list'][i]['enabled'] = False
        app.setSettings(appData)
        remoteHostResult = waitForPing(global_functions.vpnServerVpnLanIP,1)
        assert (remoteHostResult)
        tunnelUp = False
        
    def test_040_createClientVPNTunnel(self):
        global appData, vpnServerResult, vpnClientResult
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest2.SkipTest("No paried VPN client available")

        pre_events_connect = global_functions.get_app_metric_value(app,"connect")
        
        running = remote_control.run_command("pidof openvpn", host=global_functions.vpnClientVpnIP,)
        loopLimit = 5
        while ((running == 0) and (loopLimit > 0)):
            # OpenVPN is running, wait 5 sec to see if openvpm is done
            loopLimit -= 1
            time.sleep(5)
            running = remote_control.run_command("pidof openvpn", host=global_functions.vpnClientVpnIP)
        if loopLimit == 0:
            # try killing the openvpn session as it is probably stuck
            remote_control.run_command("sudo pkill openvpn", host=global_functions.vpnClientVpnIP)
            time.sleep(2)
            running = remote_control.run_command("pidof openvpn", host=global_functions.vpnClientVpnIP)
        if running == 0:
            raise unittest2.SkipTest("OpenVPN test machine already in use")
            
        appData = app.getSettings()
        appData["serverEnabled"]=True
        siteName = appData['siteName']
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData['remoteClients']['list'][:] = []  
        appData['remoteClients']['list'].append(setUpClient())
        app.setSettings(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnClientName,"zip")
        # print clientLink

        # download client config file
        result = os.system("wget -o /dev/null -t 1 --timeout=3 http://localhost"+clientLink+" -O /tmp/clientconfig.zip")
        assert (result == 0)
        # copy the config file to the remote PC, unzip the files and move to the openvpn directory on the remote device
        result = os.system("scp -o 'StrictHostKeyChecking=no' -i " + global_functions.get_prefix() + "/usr/lib/python2.7/tests/test_shell.key /tmp/clientconfig.zip testshell@" + global_functions.vpnClientVpnIP + ":/tmp/>/dev/null 2>&1")
        assert (result == 0)

        remote_control.run_command("sudo unzip -o /tmp/clientconfig.zip -d /tmp/", host=global_functions.vpnClientVpnIP)
        remote_control.run_command("sudo rm -f /etc/openvpn/*.conf; sudo rm -f /etc/openvpn/*.ovpn; sudo rm -rf /etc/openvpn/keys", host=global_functions.vpnClientVpnIP)
        remote_control.run_command("sudo mv -f /tmp/untangle-vpn/* /etc/openvpn/", host=global_functions.vpnClientVpnIP)
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.vpnClientVpnIP)

        timeout = waitForClientVPNtoConnect()
        # If VPN tunnel has failed to connect so fail the test,
        assert(timeout > 0)
        # ping the test host behind the Untangle from the remote testbox
        result = remote_control.run_command("ping -c 2 " + remote_control.clientIP, host=global_functions.vpnClientVpnIP)
        
        listOfClients = app.getActiveClients()
        print "address " + listOfClients['list'][0]['address']
        print "vpn address 1 " + listOfClients['list'][0]['poolAddress']

        host_result = remote_control.run_command("host test.untangle.com", stdout=True)
        # print "host_result <%s>" % host_result
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')

        # stop the vpn tunnel on remote box
        remote_control.run_command("sudo pkill openvpn", host=global_functions.vpnClientVpnIP)
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
        post_events_connect = global_functions.get_app_metric_value(app, "connect")
        assert(pre_events_connect < post_events_connect)
        
    def test_050_createClientVPNFullTunnel(self):
        global appData, vpnServerResult, vpnClientResult
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest2.SkipTest("No paried VPN client available")
        running = remote_control.run_command("pidof openvpn", host=global_functions.vpnClientVpnIP)
        if running == 0:
            raise unittest2.SkipTest("OpenVPN test machine already in use")
        appData = app.getSettings()
        appData["serverEnabled"]=True
        siteName = appData['siteName']  
        appData['remoteClients']['list'][:] = []  
        appData['remoteClients']['list'].append(setUpClient(vpn_name=vpnFullClientName))
        appData['groups']['list'][0]['fullTunnel'] = True
        appData['groups']['list'][0]['fullTunnel'] = True
        app.setSettings(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnFullClientName,"zip")
        # print clientLink

        # download client config file
        result = os.system("wget -o /dev/null -t 1 --timeout=3 http://localhost"+clientLink+" -O /tmp/clientconfig.zip")
        assert (result == 0)
        # Copy the config file to the remote PC, unzip the files and move to the openvpn directory on the remote device
        os.system("scp -o 'StrictHostKeyChecking=no' -i " + global_functions.get_prefix() + "/usr/lib/python2.7/tests/test_shell.key /tmp/clientconfig.zip testshell@" + global_functions.vpnClientVpnIP + ":/tmp/>/dev/null 2>&1")
        remote_control.run_command("sudo unzip -o /tmp/clientconfig.zip -d /tmp/", host=global_functions.vpnClientVpnIP)
        remote_control.run_command("sudo rm -f /etc/openvpn/*.conf; sudo rm -f /etc/openvpn/*.ovpn; sudo rm -rf /etc/openvpn/keys", host=global_functions.vpnClientVpnIP)
        remote_control.run_command("sudo mv -f /tmp/untangle-vpn/* /etc/openvpn/", host=global_functions.vpnClientVpnIP)
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.vpnClientVpnIP)

        result1 = 1
        tries = 40
        while result1 != 0 and tries > 0:
            time.sleep(1)
            tries -= 1

            listOfClients = app.getActiveClients()
            if len(listOfClients['list']):
                vpnPoolAddressIP = listOfClients['list'][0]['poolAddress']

                # ping the test host behind the Untangle from the remote testbox
                print "vpn pool address: " + vpnPoolAddressIP
                result1 = os.system("ping -c1 " + vpnPoolAddressIP + " >/dev/null 2>&1")
        if result1 == 0:        
            result2 = remote_control.run_command("ping -c 2 " + remote_control.clientIP, host=vpnPoolAddressIP)

            # run a web request to internet and make sure it goes through web filter
            # webresult = remote_control.run_command("wget -q -O - http://www.playboy.com | grep -q blockpage", host=vpnPoolAddressIP)
            webresult = remote_control.run_command("wget --timeout=4 -q -O - http://www.playboy.com | grep -q blockpage", host=vpnPoolAddressIP)

            print "result1 <%d> result2 <%d> webresult <%d>" % (result1,result2,webresult)
        else:
            print "No VPN IP address found"
        # Shutdown VPN on both sides.
        # Delete profile on server
        appData['remoteClients']['list'][:] = []  
        app.setSettings(appData)
        time.sleep(5) # wait for vpn tunnel to go down 
        # kill the client side
        remote_control.run_command("sudo pkill openvpn", host=global_functions.vpnClientVpnIP)
        time.sleep(3) # openvpn takes time to shut down
        # print ("result " + str(result) + " webresult " + str(webresult))
        assert(result1==0)
        assert(result2==0)
        assert(listOfClients['list'][0]['address'] == global_functions.vpnClientVpnIP)
        assert(webresult==0)

    @staticmethod
    def finalTearDown(self):
        global app, appWeb
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        if appWeb != None:
            uvmContext.appManager().destroy( appWeb.getAppSettings()["id"] )
            appWeb = None

test_registry.registerApp("openvpn", OpenVpnTests)
