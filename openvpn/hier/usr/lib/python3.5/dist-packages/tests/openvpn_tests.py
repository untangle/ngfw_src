"""openvpn tests"""
import time
import sys
import pdb
import os
import re
import subprocess
import base64

import runtests
import unittest
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
from uvm import Uvm

default_policy_id = 1
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
vpnSite2SiteUserPassFile = "http://test.untangle.com/test/openvpn-site2siteUserPass-config.zip"
vpnSite2SiteUserPassHostname = "untangle93-8874"
tunnelUp = False
ovpnlocaluser = "ovpnlocaluser"
ovpnPasswd = "passwd"

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
                break
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
                break
    return timeout

def waitForPing(target_IP="127.0.0.1",ping_result_expected=0):
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

def configureVPNClientForConnection(clientLink):
    "download client config from passed link, unzip, and copy to correct location"
    result = 1
    #download config
    subprocess.call("wget -o /dev/null -t 1 --timeout=3 http://localhost" + clientLink + " -O /tmp/clientconfig.zip", shell=True)
    #copy config to remote host
    subprocess.call("scp -o 'StrictHostKeyChecking=no' -i " + global_functions.get_prefix() + remote_control.host_key_file + " /tmp/clientconfig.zip testshell@" + global_functions.VPN_CLIENT_IP + ":/tmp/>/dev/null 2>&1", shell=True)
    #unzip files
    unzipFiles = remote_control.run_command("sudo unzip -o /tmp/clientconfig.zip -d /tmp/", host=global_functions.VPN_CLIENT_IP)
    #remove any existing openvpn config files
    removeOld = remote_control.run_command("sudo rm -f /etc/openvpn/*.conf; sudo rm -f /etc/openvpn/*.ovpn; sudo rm -rf /etc/openvpn/keys", host=global_functions.VPN_CLIENT_IP)
    #move new config to directory
    moveNew = remote_control.run_command("sudo mv -f /tmp/untangle-vpn/* /etc/openvpn/", host=global_functions.VPN_CLIENT_IP)
    if(unzipFiles == 0) and (removeOld == 0) and (moveNew == 0):
        result = 0
    return result

def createLocalDirectoryUser():
    passwd_encoded = base64.b64encode(ovpnPasswd.encode("utf-8"))
    return {'javaClass': 'java.util.LinkedList', 
        'list': [{
            'username': ovpnlocaluser, 
            'firstName': 'OVPNfname', 
            'lastName': 'OVPNlname', 
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

def createDirectoryConnectorSettings(ad_enable=False, radius_enable=False, ldap_secure=False):
    # Need to send Radius setting even though it's not used in this case.
    if ldap_secure == True:
        ldap_port = 636
    else:
        ldap_port = 389
    return {
        "activeDirectorySettings": {
            "LDAPPort": -1,
            "LDAPSecure": True,
            "OUFilter": "",
            "enabled": True,
            "javaClass": "com.untangle.app.directory_connector.ActiveDirectorySettings",
            "servers": {
                "javaClass": "java.util.LinkedList",
                "list": [
                    {
                        "LDAPHost": global_functions.AD_SERVER,
                        "LDAPPort": ldap_port,
                        "LDAPSecure": ldap_secure,
                        "OUFilters": {
                            "javaClass": "java.util.LinkedList",
                            "list": []
                        },
                        "domain": global_functions.AD_DOMAIN,
                        "enabled": ad_enable,
                        "javaClass": "com.untangle.app.directory_connector.ActiveDirectoryServer",
                        "superuser": global_functions.AD_ADMIN,
                        "superuserPass": global_functions.AD_PASSWORD
                    }
                ]
            }
        },
        "apiEnabled": True,
        "apiManualAddressAllowed": False,
        "googleSettings": {
            "javaClass": "com.untangle.app.directory_connector.GoogleSettings",
             "authenticationEnabled": True
        },
        "javaClass": "com.untangle.app.directory_connector.DirectoryConnectorSettings",
        "radiusSettings": {
            "acctPort": 1813,
            "authPort": 1812,
            "authenticationMethod": "PAP",
            "enabled": radius_enable,
            "javaClass": "com.untangle.app.directory_connector.RadiusSettings",
            "server": global_functions.RADIUS_SERVER,
            "sharedSecret": global_functions.RADIUS_SERVER_PASSWORD
        }
    }

class OpenVpnTests(unittest.TestCase):

    @staticmethod
    def module_name():
        return "openvpn"

    @staticmethod
    def appWebName():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"
        
    @staticmethod
    def initial_setup(self):
        global app, appWeb, appDC, tunnelApp, appData, vpnHostResult, vpnClientResult, vpnServerResult, vpnUserPassHostResult, adResult, radiusResult
        if (uvmContext.appManager().isInstantiated(self.module_name())):
            raise Exception('app %s already instantiated' % self.module_name())
        app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)
        app.start()
        appWeb = None
        appDC = None
        tunnelApp = None
        if (uvmContext.appManager().isInstantiated(self.appWebName())):
            raise Exception('app %s already instantiated' % self.appWebName())
        appWeb = uvmContext.appManager().instantiate(self.appWebName(), default_policy_id)
        vpnHostResult = subprocess.call(["ping","-W","5","-c","1",global_functions.VPN_SERVER_IP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        vpnUserPassHostResult = subprocess.call(["ping","-W","5","-c","1",global_functions.VPN_SERVER_USER_PASS_IP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        vpnClientResult = subprocess.call(["ping","-W","5","-c","1",global_functions.VPN_CLIENT_IP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        wanIP = uvmContext.networkManager().getFirstWanAddress()
        if vpnClientResult == 0:
            vpnServerResult = remote_control.run_command("ping -W 5 -c 1 " + wanIP, host=global_functions.VPN_CLIENT_IP)
        else:
            vpnServerResult = 1
        adResult = subprocess.call(["ping","-c","1",global_functions.AD_SERVER],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        radiusResult = subprocess.call(["ping","-c","1",global_functions.RADIUS_SERVER],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_createVPNTunnel(self):
        global tunnelUp
        tunnelUp = False
        if (vpnHostResult != 0):
            raise unittest.SkipTest("No paried VPN server available")
        # Download remote system VPN config
        result = subprocess.call("wget -o /dev/null -t 1 --timeout=3 " + vpnSite2SiteFile + " -O /tmp/config.zip", shell=True)
        assert (result == 0) # verify the download was successful
        app.importClientConfig("/tmp/config.zip")
        # wait for vpn tunnel to form
        timeout = waitForServerVPNtoConnect()
        # If VPN tunnel has failed to connect, fail the test,
        assert(timeout > 0)

        remoteHostResult = waitForPing(global_functions.VPN_SERVER_LAN_IP,0)
        assert (remoteHostResult)
        listOfServers = app.getRemoteServersStatus()
        # print(listOfServers)
        assert(listOfServers['list'][0]['name'] == vpnSite2SiteHostname)
        tunnelUp = True

    def test_030_disableRemoteClientVPNTunnel(self):
        global tunnelUp 
        if (not tunnelUp):
            raise unittest.SkipTest("previous test test_020_createVPNTunnel failed")
        appData = app.getSettings()
        # print(appData)
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
        remoteHostResult = waitForPing(global_functions.VPN_SERVER_LAN_IP,1)
        assert (remoteHostResult)
        tunnelUp = False

        #remove server from remoteServers so it doesn't interfere with later tests
        appData = app.getSettings()
        appData["remoteServers"]["list"][:] = []
        app.setSettings(appData)

    def test_035_createVPNTunnel_userpass(self):
        """Create Site-to-Site connection with local username/password authentication"""
        if (vpnUserPassHostResult != 0):
            raise unittest.SkipTest("User/Pass VPN server not available")

        # Download remote system VPN config
        result = subprocess.call("wget -o /dev/null -t 1 --timeout=3 " + vpnSite2SiteUserPassFile + " -O /tmp/UserPassConfig.zip", shell=True)
        assert(result == 0) #verify download was successful
        app.importClientConfig("/tmp/UserPassConfig.zip")

        #set username/password in remoteServer settings
        appData = app.getSettings()
        appData["serverEnabled"]=True
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData["remoteServers"]["list"][0]["authUserPass"]=True
        appData["remoteServers"]["list"][0]["authUsername"]=ovpnlocaluser
        appData["remoteServers"]["list"][0]["authPassword"]=ovpnPasswd
        #enable user/password authentication, set to local directory
        appData['authUserPass']=True
        appData["authenticationType"]="LOCAL_DIRECTORY"
        app.setSettings(appData)

        #wait for vpn tunnel to form
        timeout = waitForServerVPNtoConnect()
        # If VPN tunnel has failed to connect, fail the test,
        assert(timeout > 0)

        remoteHostResultUserPass = waitForPing(global_functions.VPN_SERVER_USER_PASS_LAN_IP,0)
        assert(remoteHostResultUserPass)
        listOfServers = app.getRemoteServersStatus()
        #print(listOfServers)
        assert(listOfServers["list"][0]['name'] == vpnSite2SiteUserPassHostname)

        #remove server from remoteServers so it doesn't interfere with later tests
        appData = app.getSettings()
        appData['authUserPass']=False
        appData["remoteServers"]["list"][:] = []
        app.setSettings(appData)

    def test_040_createClientVPNTunnel(self):
        global appData, vpnServerResult, vpnClientResult
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest.SkipTest("No paried VPN client available")

        pre_events_connect = global_functions.get_app_metric_value(app,"connect")
        
        running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP,)
        loopLimit = 5
        while ((running == 0) and (loopLimit > 0)):
            # OpenVPN is running, wait 5 sec to see if openvpm is done
            loopLimit -= 1
            time.sleep(5)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if loopLimit == 0:
            # try killing the openvpn session as it is probably stuck
            remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
            time.sleep(2)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if running == 0:
            raise unittest.SkipTest("OpenVPN test machine already in use")
            
        appData = app.getSettings()
        appData["serverEnabled"]=True
        siteName = appData['siteName']
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData['remoteClients']['list'][:] = []  
        appData['remoteClients']['list'].append(setUpClient())
        app.setSettings(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnClientName,"zip")
        # print(clientLink)

        #download, unzip, move config to correct directory
        result = configureVPNClientForConnection(clientLink)
        assert(result == 0)

        #start openvpn tunnel
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.VPN_CLIENT_IP)

        timeout = waitForClientVPNtoConnect()
        # If VPN tunnel has failed to connect so fail the test,
        assert(timeout > 0)
        # ping the test host behind the Untangle from the remote testbox
        result = remote_control.run_command("ping -c 2 " + remote_control.client_ip, host=global_functions.VPN_CLIENT_IP)
        
        listOfClients = app.getActiveClients()
        print("address " + listOfClients['list'][0]['address'])
        print("vpn address 1 " + listOfClients['list'][0]['poolAddress'])

        host_result = remote_control.run_command("host test.untangle.com", stdout=True)
        # print("host_result <%s>" % host_result)
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')

        # stop the vpn tunnel on remote box
        remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
        time.sleep(3) # openvpn takes time to shut down

        assert(result==0)
        assert(listOfClients['list'][0]['address'] == global_functions.VPN_CLIENT_IP)

        events = global_functions.get_events('OpenVPN','Connection Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'remote_address', global_functions.VPN_CLIENT_IP,
                                            'client_name', vpnClientName )
        assert( found )

        # Check to see if the faceplate counters have incremented. 
        post_events_connect = global_functions.get_app_metric_value(app, "connect")
        assert(pre_events_connect < post_events_connect)
        
    def test_050_createClientVPNFullTunnel(self):
        global appData, vpnServerResult, vpnClientResult
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest.SkipTest("No paried VPN client available")
        running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if running == 0:
            raise unittest.SkipTest("OpenVPN test machine already in use")
        appData = app.getSettings()
        appData["serverEnabled"]=True
        siteName = appData['siteName']  
        appData['remoteClients']['list'][:] = []  
        appData['remoteClients']['list'].append(setUpClient(vpn_name=vpnFullClientName))
        appData['groups']['list'][0]['fullTunnel'] = True
        appData['groups']['list'][0]['fullTunnel'] = True
        app.setSettings(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnFullClientName,"zip")
        # print(clientLink)

        # download client config file
        configureVPNClientForConnection(clientLink)

        #connect openvpn tunnel
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.VPN_CLIENT_IP)

        result1 = 1
        tries = 40
        while result1 != 0 and tries > 0:
            time.sleep(1)
            tries -= 1

            listOfClients = app.getActiveClients()
            if len(listOfClients['list']):
                vpnPoolAddressIP = listOfClients['list'][0]['poolAddress']

                # ping the test host behind the Untangle from the remote testbox
                print("vpn pool address: " + vpnPoolAddressIP)
                result1 = subprocess.call("ping -c1 " + vpnPoolAddressIP + " >/dev/null 2>&1", shell=True)
        if result1 == 0:        
            result2 = remote_control.run_command("ping -c 2 " + remote_control.client_ip, host=vpnPoolAddressIP)

            # run a web request to internet and make sure it goes through web filter
            # webresult = remote_control.run_command("wget -q -O - http://www.playboy.com | grep -q blockpage", host=vpnPoolAddressIP)
            webresult = remote_control.run_command("wget --timeout=4 -q -O - http://www.playboy.com | grep -q blockpage", host=vpnPoolAddressIP)

            print("result1 <%d> result2 <%d> webresult <%d>" % (result1,result2,webresult))
        else:
            print("No VPN IP address found")
        # Shutdown VPN on both sides.
        # Delete profile on server
        appData['remoteClients']['list'][:] = []  
        app.setSettings(appData)
        time.sleep(5) # wait for vpn tunnel to go down 
        # kill the client side
        remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
        time.sleep(3) # openvpn takes time to shut down
        # print(("result " + str(result) + " webresult " + str(webresult)))
        assert(result1==0)
        assert(result2==0)
        assert(listOfClients['list'][0]['address'] == global_functions.VPN_CLIENT_IP)
        assert(webresult==0)

    def test_060_createDeleteClientVPNTunnel(self):
        global appData, vpnServerResult, vpnClientResult
        if(vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest.SkipTest("No paried VPN client available")
        
        pre_events_connect = global_functions.get_app_metric_value(app, "connect")

        running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        loopLimit = 5
        while((running == 0) and (loopLimit > 0)):
            # OpenVPN is running, wait 5 sec to see if openvpn is done
            loopLimit -= 1
            time.sleep(5)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if loopLimit == 0:
            # openvpn is probably stuck, kill it and re-run
            remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
            time.sleep(2)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if running == 0:
            raise unittest.SkipTest("OpenVPN test machine already in use")

        appData = app.getSettings()
        appData["serverEnabled"] = True
        siteName = appData['siteName']
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData['remoteClients']['list'][:] = []
        appData['remoteClients']['list'].append(setUpClient())
        app.setSettings(appData)
        #print(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnClientName, "zip")
        print(clientLink)
        
        #download, unzip, move config to correct directory
        result = configureVPNClientForConnection(clientLink)
        assert(result == 0)

        #start openvpn tunnel
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.VPN_CLIENT_IP)

        timeout = waitForClientVPNtoConnect()
        # fail test if vpn tunnel does not connect
        assert(timeout > 0) 
        result = remote_control.run_command("ping -c 2 " + remote_control.client_ip, host=global_functions.VPN_CLIENT_IP)

        listOfClients = app.getActiveClients()
        print("address " + listOfClients['list'][0]['address'])
        print("vpn address 1 " + listOfClients['list'][0]['poolAddress'])

        host_result = remote_control.run_command("host test.untangle.com", stdout=True)
        print("host_result <%s>" % host_result)
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')

        #stop the vpn tunnel
        remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
        time.sleep(3) # wait for openvpn to stop
        
        assert(result==0)
        assert(listOfClients['list'][0]['address'] == global_functions.VPN_CLIENT_IP)

        events = global_functions.get_events('OpenVPN','Connection Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'remote_address', global_functions.VPN_CLIENT_IP,
                                            'client_name', vpnClientName )
        assert( found )

        #check to see if the faceplate counters have incremented
        post_events_connect = global_functions.get_app_metric_value(app, "connect")
        assert(pre_events_connect < post_events_connect)

        #delete the user
        appData['remoteClients']['list'][:] = []
        app.setSettings(appData)

        #attempt to connect with now deleted user
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.VPN_CLIENT_IP)
        timeout = waitForClientVPNtoConnect()
        #fail the test if it does connect
        assert(timeout <= 0)

        #create the same user again
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData['remoteClients']['list'][:] = []
        appData['remoteClients']['list'].append(setUpClient())
        app.setSettings(appData)
        #print(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnClientName, "zip")
        print(clientLink)

        #download, unzip, move config to correct directory
        result = configureVPNClientForConnection(clientLink)
        assert(result == 0)

        #check the key files to make sure they aren't O length
        for x in range(0, 3):
            if x == 0:
                crtSize = remote_control.run_command("du /etc/openvpn/keys/" + siteName + "-" + vpnClientName + ".crt",host=global_functions.VPN_CLIENT_IP,stdout=True)
                fileSize = int(crtSize[0])
            elif x == 1:
                cacrtSize = remote_control.run_command("du /etc/openvpn/keys/" + siteName + "-" + vpnClientName + "-ca.crt",host=global_functions.VPN_CLIENT_IP,stdout=True)
                fileSize = int(cacrtSize[0])
            elif x == 2:
                keySize = remote_control.run_command("du /etc/openvpn/keys/" + siteName + "-" + vpnClientName + ".key",host=global_functions.VPN_CLIENT_IP,stdout=True)
                fileSize = int(keySize[0])
            assert(fileSize > 0)

        #start openvpn
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn "+siteName+".conf >/dev/null 2>&1 &", host=global_functions.VPN_CLIENT_IP)
        timeout = waitForClientVPNtoConnect()
        # fail test if vpn tunnel does not connect
        assert(timeout > 0) 
        result = remote_control.run_command("ping -c 2 " + remote_control.client_ip, host=global_functions.VPN_CLIENT_IP)

        listOfClients = app.getActiveClients()
        print("address " + listOfClients['list'][0]['address'])
        print("vpn address 1 " + listOfClients['list'][0]['poolAddress'])

        host_result = remote_control.run_command("host test.untangle.com", stdout=True)
        print("host_result <%s>" % host_result)
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')

        #stop the vpn tunnel
        remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
        time.sleep(3) # wait for openvpn to stop
        
        assert(result==0)
        assert(listOfClients['list'][0]['address'] == global_functions.VPN_CLIENT_IP)

        events = global_functions.get_events('OpenVPN','Connection Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'remote_address', global_functions.VPN_CLIENT_IP,
                                            'client_name', vpnClientName )
        assert( found )

        #check to see if the faceplate counters have incremented
        post_events_connect = global_functions.get_app_metric_value(app, "connect")
        assert(pre_events_connect < post_events_connect)

    def test_070_createClientVPNTunnelLocalUserPass(self):
        global appData, vpnServerResult, vpnClientResult
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest.SkipTest("No paried VPN client available")

        pre_events_connect = global_functions.get_app_metric_value(app,"connect")
        
        running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP,)
        loopLimit = 5
        while ((running == 0) and (loopLimit > 0)):
            # OpenVPN is running, wait 5 sec to see if openvpn is done
            loopLimit -= 1
            time.sleep(5)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if loopLimit == 0:
            # try killing the openvpn session as it is probably stuck
            remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
            time.sleep(2)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if running == 0:
            raise unittest.SkipTest("OpenVPN test machine already in use")
            
        appData = app.getSettings()
        appData["serverEnabled"]=True
        siteName = appData['siteName']
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData['remoteClients']['list'][:] = []  
        appData['remoteClients']['list'].append(setUpClient())
        #enable user/password authentication, set to local directory
        appData['authUserPass']=True
        appData["authenticationType"]="LOCAL_DIRECTORY"
        app.setSettings(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnClientName,"zip")

        #create Local Directory User for authentication
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser())

        #download, unzip, move config to correct directory
        result = configureVPNClientForConnection(clientLink)
        assert(result == 0)
        
        #create credentials file containing username/password
        remote_control.run_command("echo " + ovpnlocaluser + " > /tmp/authUserPassFile; echo " + ovpnPasswd + " >> /tmp/authUserPassFile", host=global_functions.VPN_CLIENT_IP)
        #connect to openvpn using the file
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn --config " + siteName + ".conf --auth-user-pass /tmp/authUserPassFile >/dev/null 2>&1 &", host=global_functions.VPN_CLIENT_IP)

        timeout = waitForClientVPNtoConnect()
        # fail if tunnel doesn't connect
        assert(timeout > 0)
        # ping the test host behind the Untangle from the remote testbox
        result = remote_control.run_command("ping -c 2 " + remote_control.client_ip, host=global_functions.VPN_CLIENT_IP)
        
        listOfClients = app.getActiveClients()
        print("address " + listOfClients['list'][0]['address'])
        print("vpn address 1 " + listOfClients['list'][0]['poolAddress'])

        host_result = remote_control.run_command("host test.untangle.com", stdout=True)
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')

        # stop the vpn tunnel on remote box
        remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
        # openvpn takes time to shut down
        time.sleep(3) 

        assert(result==0)
        assert(listOfClients['list'][0]['address'] == global_functions.VPN_CLIENT_IP)

        events = global_functions.get_events('OpenVPN','Connection Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'remote_address', global_functions.VPN_CLIENT_IP,
                                            'client_name', vpnClientName )
        assert( found )

        # Check to see if the faceplate counters have incremented. 
        post_events_connect = global_functions.get_app_metric_value(app, "connect")
        assert(pre_events_connect < post_events_connect)

        #remove Local Directory User
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())        

    def test_075_createClientVPNTunnelRadiusUserPass(self):
        global appData, vpnServerResult, vpnClientResult, appDC
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest.SkipTest("No paried VPN client available")

        pre_events_connect = global_functions.get_app_metric_value(app,"connect")

        if (radiusResult != 0):
            raise unittest.SkipTest("No RADIUS server available")
        appNameDC = "directory-connector"
        if (uvmContext.appManager().isInstantiated(appNameDC)):
            print("App %s already installed" % appNameDC)
            appDC = uvmContext.appManager().app(appNameDC)
        else:
            appDC = uvmContext.appManager().instantiate(appNameDC, default_policy_id)
        appDC.setSettings(createDirectoryConnectorSettings(radius_enable=True))
        
        running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP,)
        loopLimit = 5
        while ((running == 0) and (loopLimit > 0)):
            # OpenVPN is running, wait 5 sec to see if openvpn is done
            loopLimit -= 1
            time.sleep(5)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if loopLimit == 0:
            # try killing the openvpn session as it is probably stuck
            remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
            time.sleep(2)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if running == 0:
            raise unittest.SkipTest("OpenVPN test machine already in use")
            
        appData = app.getSettings()
        appData["serverEnabled"]=True
        siteName = appData['siteName']
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData['remoteClients']['list'][:] = []  
        appData['remoteClients']['list'].append(setUpClient())
        #enable user/password authentication, set to RADIUS directory
        appData['authUserPass']=True
        appData["authenticationType"]="RADIUS"
        app.setSettings(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnClientName,"zip")

        #download, unzip, move config to correct directory
        result = configureVPNClientForConnection(clientLink)
        assert(result == 0)
        
        #create credentials file containing username/password
        remote_control.run_command("echo " + global_functions.RADIUS_USER + " > /tmp/authUserPassFile; echo " + global_functions.RADIUS_PASSWORD + " >> /tmp/authUserPassFile", host=global_functions.VPN_CLIENT_IP)
        #connect to openvpn using the file
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn --config " + siteName + ".conf --auth-user-pass /tmp/authUserPassFile >/dev/null 2>&1 &", host=global_functions.VPN_CLIENT_IP)

        timeout = waitForClientVPNtoConnect()
        # fail if tunnel doesn't connect
        assert(timeout > 0)
        # ping the test host behind the Untangle from the remote testbox
        result = remote_control.run_command("ping -c 2 " + remote_control.client_ip, host=global_functions.VPN_CLIENT_IP)
        
        listOfClients = app.getActiveClients()
        print("address " + listOfClients['list'][0]['address'])
        print("vpn address 1 " + listOfClients['list'][0]['poolAddress'])

        host_result = remote_control.run_command("host test.untangle.com", stdout=True)
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')

        # stop the vpn tunnel on remote box
        remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
        # openvpn takes time to shut down
        time.sleep(3) 

        assert(result==0)
        assert(listOfClients['list'][0]['address'] == global_functions.VPN_CLIENT_IP)

        events = global_functions.get_events('OpenVPN','Connection Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'remote_address', global_functions.VPN_CLIENT_IP,
                                            'client_name', vpnClientName )
        assert( found )

        # Check to see if the faceplate counters have incremented. 
        post_events_connect = global_functions.get_app_metric_value(app, "connect")
        assert(pre_events_connect < post_events_connect)


    def test_079_createClientVPNTunnelADUserPass(self):
        global appData, vpnServerResult, vpnClientResult, appDC
        if (vpnClientResult != 0 or vpnServerResult != 0):
            raise unittest.SkipTest("No paried VPN client available")

        pre_events_connect = global_functions.get_app_metric_value(app,"connect")

        if (adResult != 0):
            raise unittest.SkipTest("No AD server available")
        appNameDC = "directory-connector"
        if (uvmContext.appManager().isInstantiated(appNameDC)):
            print("App %s already installed" % appNameDC)
            appDC = uvmContext.appManager().app(appNameDC)
        else:
            appDC = uvmContext.appManager().instantiate(appNameDC, default_policy_id)
        appDC.setSettings(createDirectoryConnectorSettings(ad_enable=True,ldap_secure=True))
        
        running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP,)
        loopLimit = 5
        while ((running == 0) and (loopLimit > 0)):
            # OpenVPN is running, wait 5 sec to see if openvpn is done
            loopLimit -= 1
            time.sleep(5)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if loopLimit == 0:
            # try killing the openvpn session as it is probably stuck
            remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
            time.sleep(2)
            running = remote_control.run_command("pidof openvpn", host=global_functions.VPN_CLIENT_IP)
        if running == 0:
            raise unittest.SkipTest("OpenVPN test machine already in use")
            
        appData = app.getSettings()
        appData["serverEnabled"]=True
        siteName = appData['siteName']
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData['remoteClients']['list'][:] = []  
        appData['remoteClients']['list'].append(setUpClient())
        #enable user/password authentication, set to AD directory
        appData['authUserPass']=True
        appData["authenticationType"]="ACTIVE_DIRECTORY"
        app.setSettings(appData)
        clientLink = app.getClientDistributionDownloadLink(vpnClientName,"zip")

        #download, unzip, move config to correct directory
        result = configureVPNClientForConnection(clientLink)
        assert(result == 0)
        
        #create credentials file containing username/password
        remote_control.run_command("echo " + global_functions.AD_USER + " > /tmp/authUserPassFile; echo passwd >> /tmp/authUserPassFile", host=global_functions.VPN_CLIENT_IP)
        #connect to openvpn using the file
        remote_control.run_command("cd /etc/openvpn; sudo nohup openvpn --config " + siteName + ".conf --auth-user-pass /tmp/authUserPassFile >/dev/null 2>&1 &", host=global_functions.VPN_CLIENT_IP)

        timeout = waitForClientVPNtoConnect()
        # fail if tunnel doesn't connect
        assert(timeout > 0)
        # ping the test host behind the Untangle from the remote testbox
        result = remote_control.run_command("ping -c 2 " + remote_control.client_ip, host=global_functions.VPN_CLIENT_IP)
        
        listOfClients = app.getActiveClients()
        print("address " + listOfClients['list'][0]['address'])
        print("vpn address 1 " + listOfClients['list'][0]['poolAddress'])

        host_result = remote_control.run_command("host test.untangle.com", stdout=True)
        match = re.search(r'address \d{1,3}.\d{1,3}.\d{1,3}.\d{1,3}', host_result)
        ip_address_testuntangle = (match.group()).replace('address ','')

        # stop the vpn tunnel on remote box
        remote_control.run_command("sudo pkill openvpn", host=global_functions.VPN_CLIENT_IP)
        # openvpn takes time to shut down
        time.sleep(3) 

        assert(result==0)
        assert(listOfClients['list'][0]['address'] == global_functions.VPN_CLIENT_IP)

        events = global_functions.get_events('OpenVPN','Connection Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5,
                                            'remote_address', global_functions.VPN_CLIENT_IP,
                                            'client_name', vpnClientName )
        assert( found )

        # Check to see if the faceplate counters have incremented. 
        post_events_connect = global_functions.get_app_metric_value(app, "connect")
        assert(pre_events_connect < post_events_connect)

    def test_80_OpenVPNTunnelVPNConflict(self):
        """test conflict of OpenVPN and TunnelVPN when 'boundInterfaceId' is set to the first wan IP"""
        global tunnelApp
        vpn_tunnel_file = "http://10.111.56.29/openvpn-ats-test-tunnelvpn-config.zip"
        index_of_wans = global_functions.get_wan_tuples()
        if index_of_wans == []:
            raise unittest.SkipTest("No static or auto WAN")
        # print(index_of_wans[0])

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
                    "tunnelId": vpn_tunnel_id,
                    "boundInterfaceId": index_of_wans[0][0]
            }

        #set up OpenVPN server    
        appData = app.getSettings()
        appData["serverEnabled"]=True
        siteName = appData['siteName']
        appData['exports']['list'].append(create_export("192.0.2.0/24")) # append in case using LXC
        appData['remoteClients']['list'][:] = []  
        appData['remoteClients']['list'].append(setUpClient())
        app.setSettings(appData)
        
        # install TunnelVPN
        tunnelAppName = "tunnel-vpn"
        if (uvmContext.appManager().isInstantiated(tunnelAppName)):
            print('app %s already instantiated' % tunnelAppName)
            tunnelApp = uvmContext.appManager().app(tunnelAppName)
        else:
            tunnelApp = uvmContext.appManager().instantiate(tunnelAppName, default_policy_id)    
        tunnelApp.start()

        #set up TunnelVPN
        result = subprocess.call("wget -o /dev/null -t 1 --timeout=3 " + vpn_tunnel_file + " -O /tmp/config.zip", shell=True)
        if (result != 0):
            raise unittest.SkipTest("Unable to download VPN file: " + vpn_tunnel_file)
        currentWanIP = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
        if (currentWanIP == ""):
            raise unittest.SkipTest("Unable to get WAN IP")
        # print("Original WAN IP: " + currentWanIP)
        tunnelApp.importTunnelConfig("/tmp/config.zip", "Untangle", 200)

        tunnelAppData = tunnelApp.getSettings()
        tunnelAppData['rules']['list'].append(create_tunnel_rule())
        tunnelAppData['tunnels']['list'].append(create_tunnel_profile())
        tunnelApp.setSettings(tunnelAppData)

        # wait for vpn tunnel to form
        timeout = 240
        connected = False
        connectStatus = ""
        newWanIP = currentWanIP
        while (not connected and timeout > 0):
            listOfConnections = tunnelApp.getTunnelStatusList()
            connectStatus = listOfConnections['list'][0]['stateInfo']
            if (connectStatus == "CONNECTED"):
                newWanIP = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
                if (currentWanIP != newWanIP):
                    connected = True
                else:
                    time.sleep(1)
                    timeout-=1
            else:
                time.sleep(1)
                timeout-=1

        # disable the added tunnel
        tunnelAppData['rules']['list'][:] = []
        for i in range(len(tunnelAppData['tunnels']['list'])):
            tunnelAppData['tunnels']['list'][i]['enabled'] = False
            print(tunnelAppData['tunnels']['list'][i]['enabled'])
        tunnelApp.setSettings(tunnelAppData)

        #stop tunnel here
        time.sleep(3)
        tunnelApp.stop()

        # If VPN tunnel has failed to connect, fail the test,
        assert(connected)

    @staticmethod
    def final_tear_down(self):
        global app, appWeb, appDC, tunnelApp
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        if appWeb != None:
            uvmContext.appManager().destroy( appWeb.getAppSettings()["id"] )
            appWeb = None
        if appDC != None:
            uvmContext.appManager().destroy( appDC.getAppSettings()["id"] )
            appDC = None
        if tunnelApp != None:
            uvmContext.appManager().destroy( tunnelApp.getAppSettings()["id"] )
            tunnelApp = None
#            print(tunnelApp.getAppSettings()["id"])

test_registry.register_module("openvpn", OpenVpnTests)
