import unittest2
import time
import sys
import pdb
import os
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
qaHostVPN = "10.5.6.57"
qaHostLANIP = "192.168.234.57"
tunnelUp = False

#pdb.set_trace()

def flushEvents():
    reports = uvmContext.nodeManager().node("untangle-node-ipsec")
    if (reports != None):
        reports.flushEvents()

class OpenVpnTests(unittest2.TestCase):

    @staticmethod
    def nodeName():
        return "untangle-node-openvpn"

    @staticmethod
    def vendorName():
        return "Untangle"

    def setUp(self):
        global node, nodeData, vpnHostResult
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
            node.start()
            nodeData = node.getSettings()
            nodeData["serverEnabled"]=True
            node.setSettings(nodeData)
            vpnHostResult = subprocess.call(["ping","-c","1",qaHostVPN],stdout=subprocess.PIPE,stderr=subprocess.PIPE)

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
        remoteHostResult = subprocess.call(["ping","-c","1",qaHostLANIP],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        assert (remoteHostResult == 0)
        tunnelUp = True
        
        
    def test_999_finalTearDown(self):
        global node
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None

TestDict.registerNode("openvpn", OpenVpnTests)

