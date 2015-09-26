import unittest2
import time
import sys
import pdb
import socket
import subprocess
from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from uvm import Manager
from uvm import Uvm
import remote_control
import test_registry
import base64

uvmContext = Uvm().getUvmContext()
defaultRackId = 1
node = None
tunnelUp = False

# hardcoded for ats testing
l2tpServerHosts = ["10.111.56.49","10.112.11.53"]
l2tpClientHost = "10.111.56.33"  # Windows running freeSSHd
l2tpLocalUser = "test"
l2tpLocalPassword = "passwd"
ipsecHost = "10.111.56.57"
ipsecHostLANIP = "192.168.235.57"
ipsecHostLAN = "192.168.235.0/24"
configuredHostIPs = [('10.112.11.53','192.168.2.1','192.168.2.0/24'), # ATS
                     ('10.111.56.49','192.168.10.49','192.168.10.0/24'), # QA 1
                     ('10.111.56.61','192.168.10.61','192.168.10.0/24'), # QA 2
                     ('10.111.56.56','10.111.56.56','10.111.56.15/32')] # QA 3 Bridged

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
        "runmode": "start", 
        "secret": "supersecret"
    }    

def appendTunnel(newTunnel):
    ipsecSettings = node.getSettings()
    ipsecSettings["tunnels"]["list"].append(newTunnel)
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

class IPsecTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-ipsec"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node, ipsecHostResult, l2tpClientHostResult
        tunnelUp = False
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        ipsecHostResult = subprocess.call(["ping","-c","1",ipsecHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        l2tpClientHostResult = subprocess.call(["ping","-c","1",l2tpClientHost],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
           
    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)

    def test_020_createIpsecTunnel(self):
        global tunnelUp
        if (ipsecHostResult != 0):
            raise unittest2.SkipTest("No paried IPSec server available")
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
            ipsecHostLANResult = remote_control.runCommand(("wget -q -O /dev/null -4 -t 2 --timeout=5 http://%s/" % ipsecHostLANIP))
        assert (ipsecHostLANResult == 0)
        tunnelUp = True

    def test_030_restartNetworkVerifyIpsecTunnel(self):
        # save a setting in networking and test ipsec tunnel is set connected.
        if (not tunnelUp):
            raise unittest2.SkipTest("Test test_020_createIpsecTunnel success required ")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        uvmContext.networkManager().setNetworkSettings(netsettings)
        time.sleep(10) # wait for networking to restart
        ipsecHostLANResult = remote_control.runCommand(("wget -q -O /dev/null -4 -t 2 --timeout=5 http://%s/" % ipsecHostLANIP))
        assert (ipsecHostLANResult == 0)
        
    def test_040_windowsL2TPlocalDirectory(self):
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        if (l2tpClientHostResult != 0):
            raise unittest2.SkipTest("l2tpClientHostResult not available")
        if (not wan_IP in l2tpServerHosts):
            raise unittest2.SkipTest("No paried L2TP client available")
        uvmContext.localDirectory().setUsers(createLocalDirectoryUser())
        createL2TPconfig("LOCAL_DIRECTORY")
        timeout = 360
        found = False
        vpnServerResult = remote_control.runCommand("rasdial.exe %s %s %s" % (wan_IP,l2tpLocalUser,l2tpLocalPassword), host=l2tpClientHost)
        while not found and timeout > 0:
            timeout -= 1
            time.sleep(1)
            virtUsers = node.getVirtualUsers()
            for user in virtUsers['list']:
                if user['clientUsername'] == l2tpLocalUser:
                    found = True
        uvmContext.localDirectory().setUsers(removeLocalDirectoryUser())
        assert(found)

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None

test_registry.registerNode("ipsec", IPsecTests)
