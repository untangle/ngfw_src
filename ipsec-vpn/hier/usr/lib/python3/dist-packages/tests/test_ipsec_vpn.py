"""ipsec_vpn tests"""
import time
import subprocess
import base64
import unittest
import pytest

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

default_policy_id = 1
appAD = None
appDataRD = None
tunnelUp = False
orig_netsettings = None

# hardcoded for ats testing
l2tpServerHosts = ["10.111.56.61","10.111.56.49","10.111.56.56","10.112.11.53","10.111.0.134","10.111.56.91","10.111.56.94","10.111.56.57"]
l2tpClientHost = "10.111.56.84"  # Windows 10 using builtin OpenSSH
l2tpLocalUser = "test"
l2tpLocalPassword = "passwd"
ipsecHost = "10.111.56.96"
ipsecHostLANIP = "192.168.235.96"
ipsecPcLANIP = "192.168.235.83"
ipsecHostLAN = "192.168.235.0/24"
ipsecHostname = "ipsecsite.untangle.int"
configuredHostIPs = [('10.111.0.134','192.168.2.1','192.168.2.0/24'), # ATS
                     ('10.111.56.49','192.168.10.49','192.168.10.0/24'), # QA 1
                     ('10.111.56.61','192.168.10.61','192.168.10.0/24'), # QA 2
                     ('10.111.56.56','10.111.56.56','10.111.56.15/32'), # QA 3 Bridged
                     ('10.111.56.94','192.168.10.94','192.168.10.0/24'), # QA 4 Dual WAN
                     ('10.111.56.57','192.168.4.1','192.168.4.0/24')] # QA box .57


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
        "secret": "supersecret"
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


@pytest.mark.ipsec_vpn
class IPsecTests(NGFWTestCase):

    @staticmethod
    def module_name():
        global app
        app = IPsecTests._app
        return "ipsec-vpn"

    @staticmethod
    def appNameAD():
        return "directory-connector"

    @staticmethod
    def vendorName():
        return "Untangle"

    @classmethod
    def initial_extra_setup(cls):
        global orig_netsettings, ipsecHostResult, l2tpClientHostResult, appAD, appDataRD, radiusResult

        tunnelUp = False

        if (uvmContext.appManager().isInstantiated(cls.appNameAD())):
            raise unittest.SkipTest('app %s already instantiated' % cls.module_name())
        if orig_netsettings == None:
            orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        appAD = uvmContext.appManager().instantiate(cls.appNameAD(), default_policy_id)
        appDataRD = appAD.getSettings().get('radiusSettings')
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
        global tunnelUp
        if (ipsecHostResult != 0):
            raise unittest.SkipTest("No paried IPSec server available")
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
                pairMatchNotFound = False
        if (pairMatchNotFound):
            raise unittest.SkipTest("IPsec test only configed for IPs %s" % (listOfPairs))
        timeout = 10
        ipsecHostLANResult = 1
        while (ipsecHostLANResult != 0 and timeout > 0):
            timeout -= 1
            time.sleep(1)
            # ping the remote LAN to see if the IPsec tunnel is connected.
            ipsecHostLANResult = remote_control.run_command("wget -q -O /dev/null --no-check-certificate -4 -t 2 --timeout=5 https://%s/" % ipsecHostLANIP)
        assert (ipsecHostLANResult == 0)
        ipsecPcLanResult = remote_control.run_command("ping -c 1 %s" % ipsecPcLANIP)
        assert (ipsecPcLanResult == 0)
        tunnelUp = True

        # Check to see if the faceplate counters have incremented. 
        post_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")
        assert(pre_events_enabled < post_events_enabled)
               
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
            ipsecHostLANResult = remote_control.run_command("wget -q -O /dev/null --no-check-certificate -4 -t 2 --timeout=5 https://%s/" % ipsecHostLANIP)
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
            raise unittest.SkipTest("No paried L2TP client available")
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

    def test_050_windowsL2TPRadiusDirectory(self):
        global appAD
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (radiusResult != 0):
            raise unittest.SkipTest("No RADIUS server available")
        if (l2tpClientHostResult != 0):
            raise unittest.SkipTest("l2tpClientHostResult not available")
        if (not wan_IP in l2tpServerHosts):
            raise unittest.SkipTest("No paried L2TP client available")
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
            raise unittest.SkipTest("No paried IPSec server available")
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
            ipsecHostLANResult = remote_control.run_command("wget -q -O /dev/null --no-check-certificate -4 -t 2 --timeout=5 https://%s/" % ipsecHostLANIP)
        post_events_enabled = global_functions.get_app_metric_value(self._app,"enabled")
        nukeIPSecTunnels(self._app)
        assert (ipsecHostLANResult == 0)
        # Check to see if the faceplate counters have incremented. 
        assert(pre_events_enabled < post_events_enabled)

    @classmethod
    def final_extra_tear_down(cls):
        global appAD
        # Restore original settings to return to initial settings
        # print("orig_netsettings <%s>" % orig_netsettings)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        if appAD != None:
            uvmContext.appManager().destroy( appAD.getAppSettings()["id"] )
            appAD = None


test_registry.register_module("ipsec-vpn", IPsecTests)
