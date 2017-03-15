import unittest2
import time
import sys
import pdb
import socket
import subprocess
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import base64
import global_functions

defaultRackId = 1
node = None
nodeAD = None
nodeDataRD = None
tunnelUp = False
orig_netsettings = None

# hardcoded for ats testing
radiusHost = "10.112.56.71"
l2tpServerHosts = ["10.111.56.61","10.111.56.49","10.111.56.56","10.112.11.53","10.112.11.55","10.111.56.91","10.111.56.94"]
l2tpClientHost = "10.111.56.83"  # Windows running freeSSHd
l2tpLocalUser = "test"
l2tpLocalPassword = "passwd"
l2tpRadiusUser = "normal"
l2tpRadiusPassword = "passwd"
ipsecHost = "10.111.56.96"
ipsecHostLANIP = "192.168.235.96"
ipsecPcLANIP = "192.168.235.83"
ipsecHostLAN = "192.168.235.0/24"
ipsecHostname = "ipsecsite.untangle.int"
configuredHostIPs = [('10.112.11.55','192.168.2.1','192.168.2.0/24'), # ATS
                     ('10.111.56.49','192.168.10.49','192.168.10.0/24'), # QA 1
                     ('10.111.56.61','192.168.10.61','192.168.10.0/24'), # QA 2
                     ('10.111.56.56','10.111.56.56','10.111.56.15/32'), # QA 3 Bridged
                     ('10.111.56.94','192.168.10.94','192.168.10.0/24'), # QA 4 Dual WAN
                     ('10.111.56.93','192.168.234.93','192.168.234.0/24')] # QA box .93

# pdb.set_trace()

def addIPSecTunnel(remoteIP="", remoteLAN="", localIP="", localLANIP="", localLANRange=""):
    return {
        "active": True, 
        "adapter": "- Custom -", 
        "conntype": "tunnel", 
        "description": "ipsec test profile", 
        "id": 0, 
        "javaClass": "com.untangle.node.ipsec_vpn.IpsecVpnTunnel", 
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

def appendTunnel(newTunnel):
    ipsecSettings = node.getSettings()
    ipsecSettings["tunnels"]["list"].append(newTunnel)
    node.setSettings(ipsecSettings)

def nukeIPSecTunnels():
    ipsecSettings = node.getSettings()
    ipsecSettings["tunnels"]["list"] = []
    node.setSettings(ipsecSettings)

def createL2TPconfig(authType="LOCAL_DIRECTORY"):
    ipsecSettings = node.getSettings()
    ipsecSettings["authenticationType"] = authType
    ipsecSettings["virtualAddressPool"] = "198.18.0.0/16"
    ipsecSettings["virtualSecret"] = "testthis"
    ipsecSettings["vpnflag"] = True
    node.setSettings(ipsecSettings);

def createLocalDirectoryUser():
    return {'javaClass': 'java.util.LinkedList', 
        'list': [{
            'username': l2tpLocalUser, 
            'firstName': '[firstName]', 
            'lastName': '[lastName]', 
            'javaClass': 'com.untangle.uvm.LocalDirectoryUser', 
            'expirationTime': 0, 
            'passwordBase64Hash': base64.b64encode(l2tpLocalPassword),
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
            "domain": "mydomain.int",
            "enabled": False,
            "javaClass": "com.untangle.node.directory_connector.ActiveDirectorySettings",
            "superuser": "Administrator",
            "superuserPass": "mypassword"
        },
        "apiEnabled": True,
        "facebookSettings": {
            "authenticationEnabled": False,
            "javaClass": "com.untangle.node.directory_connector.FacebookSettings"
        },
        "googleSettings": {
            "authenticationEnabled": False,
            "javaClass": "com.untangle.node.directory_connector.GoogleSettings"
        },
        "javaClass": "com.untangle.node.directory_connector.DirectoryConnectorSettings",
        "radiusSettings": {
            "acctPort": 1813,
            "authPort": 1812,
            "authenticationMethod": "MSCHAPV2",
            "enabled": True,
            "javaClass": "com.untangle.node.directory_connector.RadiusSettings",
            "server": radiusHost,
            "sharedSecret": "chakas"
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

class IPsecTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "ipsec-vpn"

    @staticmethod
    def nodeNameAD():
        return "directory-connector"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global node, orig_netsettings, ipsecHostResult, l2tpClientHostResult, nodeAD, nodeDataRD, radiusResult
        tunnelUp = False
        if (uvmContext.appManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.appManager().instantiate(self.nodeName(), defaultRackId)
        if (uvmContext.appManager().isInstantiated(self.nodeNameAD())):
            raise unittest2.SkipTest('node %s already instantiated' % self.nodeName())
        if orig_netsettings == None:
            orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        nodeAD = uvmContext.appManager().instantiate(self.nodeNameAD(), defaultRackId)
        nodeDataRD = nodeAD.getSettings().get('radiusSettings')
        ipsecHostResult = subprocess.call(["ping","-c","1",ipsecHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        l2tpClientHostResult = subprocess.call(["ping","-c","1",l2tpClientHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        radiusResult = subprocess.call(["ping","-c","1",radiusHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_020_createIpsecTunnel(self):
        global tunnelUp
        if (ipsecHostResult != 0):
            raise unittest2.SkipTest("No paried IPSec server available")
        pre_events_enabled = global_functions.get_app_metric_value(node,"enabled")

        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        pairMatchNotFound = True
        listOfPairs = ""
        for hostConfig in configuredHostIPs:
            print hostConfig[0]
            listOfPairs += str(hostConfig[0]) + ", "
            if (wan_IP in hostConfig[0]):
                appendTunnel(addIPSecTunnel(ipsecHost,ipsecHostLAN,hostConfig[0],hostConfig[1],hostConfig[2]))
                pairMatchNotFound = False
        if (pairMatchNotFound):
            raise unittest2.SkipTest("IPsec test only configed for IPs %s" % (listOfPairs))
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
        post_events_enabled = global_functions.get_app_metric_value(node,"enabled")
        assert(pre_events_enabled < post_events_enabled)
               
    def test_030_restartNetworkVerifyIpsecTunnel(self):
        # save a setting in networking and test ipsec tunnel is set connected.
        global tunnelUp
        if (not tunnelUp):
            raise unittest2.SkipTest("Test test_020_createIpsecTunnel success required ")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        uvmContext.networkManager().setNetworkSettings(netsettings)
        time.sleep(10) # wait for networking to restart
        ipsecHostLANResult = remote_control.run_command("wget -q -O /dev/null --no-check-certificate -4 -t 2 --timeout=5 https://%s/" % ipsecHostLANIP)
        ipsecPcLanResult = remote_control.run_command("ping -c 1 %s" % ipsecPcLANIP)
        # delete tunnel
        nukeIPSecTunnels()
        tunnelUp = False
        assert (ipsecHostLANResult == 0)
        assert (ipsecPcLanResult == 0)
        
    def test_040_windowsL2TPlocalDirectory(self):
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (l2tpClientHostResult != 0):
            raise unittest2.SkipTest("l2tpClientHostResult not available")
        if (not wan_IP in l2tpServerHosts):
            raise unittest2.SkipTest("No paried L2TP client available")
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        createL2TPconfig("LOCAL_DIRECTORY")
        timeout = 480
        found = False
        # Send command for Windows VPN connect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_IP,l2tpLocalUser,l2tpLocalPassword), host=l2tpClientHost)
        while not found and timeout > 0:
            timeout -= 1
            time.sleep(1)
            virtUsers = node.getVirtualUsers()
            for user in virtUsers['list']:
                if user['clientUsername'] == l2tpLocalUser:
                    found = True
        # Send command for Windows VPN disconnect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_IP), host=l2tpClientHost)
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        assert(found)

    def test_050_windowsL2TPRadiusDirectory(self):
        global nodeAD
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (radiusResult != 0):
            raise unittest2.SkipTest("No RADIUS server available")
        if (l2tpClientHostResult != 0):
            raise unittest2.SkipTest("l2tpClientHostResult not available")
        if (not wan_IP in l2tpServerHosts):
            raise unittest2.SkipTest("No paried L2TP client available")
        # Configure RADIUS settings
        nodeAD.setSettings(createRadiusSettings())
        createL2TPconfig("RADIUS_SERVER")
        timeout = 480
        found = False
        vpnServerResult = remote_control.run_command("rasdial.exe %s %s %s" % (wan_IP,l2tpRadiusUser,l2tpRadiusPassword), host=l2tpClientHost)
        while not found and timeout > 0:
            timeout -= 1
            time.sleep(1)
            virtUsers = node.getVirtualUsers()
            for user in virtUsers['list']:
                if user['clientUsername'] == l2tpRadiusUser:
                    found = True
        # Send command for Windows VPN disconnect.
        vpnServerResult = remote_control.run_command("rasdial.exe %s /d" % (wan_IP), host=l2tpClientHost)
        assert(found)

    def test_060_createIpsecTunnelHostname(self):
        if (ipsecHostResult != 0):
            raise unittest2.SkipTest("No paried IPSec server available")
        pre_events_enabled = global_functions.get_app_metric_value(node,"enabled")

        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        pairMatchNotFound = True
        listOfPairs = ""
        addDNSRule(createDNSRule(ipsecHost,ipsecHostname))
        # verify L2TP is off  NGFW-7212
        ipsecSettings = node.getSettings()
        ipsecSettings["vpnflag"] = False
        node.setSettings(ipsecSettings)
        for hostConfig in configuredHostIPs:
            print hostConfig[0]
            listOfPairs += str(hostConfig[0]) + ", "
            if (wan_IP in hostConfig[0]):
                appendTunnel(addIPSecTunnel(ipsecHostname,ipsecHostLAN,hostConfig[0],hostConfig[1],hostConfig[2]))
                pairMatchNotFound = False
        if (pairMatchNotFound):
            raise unittest2.SkipTest("IPsec test only configed for IPs %s" % (listOfPairs))
        timeout = 10
        ipsecHostLANResult = 1
        while (ipsecHostLANResult != 0 and timeout > 0):
            timeout -= 1
            time.sleep(1)
            # ping the remote LAN to see if the IPsec tunnel is connected.
            ipsecHostLANResult = remote_control.run_command("wget -q -O /dev/null --no-check-certificate -4 -t 2 --timeout=5 https://%s/" % ipsecHostLANIP)
        post_events_enabled = global_functions.get_app_metric_value(node,"enabled")
        nukeIPSecTunnels()
        assert (ipsecHostLANResult == 0)
        # Check to see if the faceplate counters have incremented. 
        assert(pre_events_enabled < post_events_enabled)

    @staticmethod
    def finalTearDown(self):
        global node, nodeAD
        # Restore original settings to return to initial settings
        # print "orig_netsettings <%s>" % orig_netsettings
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
        if node != None:
            uvmContext.appManager().destroy( node.getAppSettings()["id"] )
            node = None
        if nodeAD != None:
            uvmContext.appManager().destroy( nodeAD.getAppSettings()["id"] )
            nodeAD = None

test_registry.registerNode("ipsec-vpn", IPsecTests)
