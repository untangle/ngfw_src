import unittest2
import os
import sys
import time
reload(sys)
import commands
import subprocess
sys.setdefaultencoding("utf-8")

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import test_registry
import remote_control
import pdb
import global_functions
import system_properties

node = None
orig_netsettings = None
indexOfWans = []
defaultRackId = 1

def allWansOnline():
    onlineCount = 0
    wanStatus = node.getWanStatus()
    wanCount = len(wanStatus['list'])
    for statusInterface in wanStatus['list']:
        if statusInterface['online']:
            onlineCount = onlineCount + 1
    return ( onlineCount == wanCount )

def allWansOffline():
    offlineCount = 0
    wanStatus = node.getWanStatus()
    wanCount = len(wanStatus['list'])
    for statusInterface in wanStatus['list']:
        if not statusInterface['online']:
            offlineCount = offlineCount + 1
    return ( offlineCount == wanCount )

def onlineWanCount():
    onlineCount = 0
    wanStatus = node.getWanStatus()
    for statusInterface in wanStatus['list']:
        if statusInterface['online']:
            onlineCount = onlineCount + 1
    return onlineCount

def offlineWanCount():
    offlineCount = 0
    wanStatus = node.getWanStatus()
    for statusInterface in wanStatus['list']:
        if not statusInterface['online']:
            offlineCount = offlineCount + 1
    return offlineCount

def waitForChangeInStatus(maxWait=120):
    increment = 1
    time.sleep(increment)
    maxWait = maxWait - increment
    originalOnlineCount = onlineWanCount()
    originalOfflineCount = offlineWanCount()
    while maxWait > 0:
        time.sleep(increment)
        maxWait = maxWait - increment
        if (originalOfflineCount != offlineWanCount()):
            break
        if (originalOnlineCount != onlineWanCount()):
            break
    time.sleep( 10 ) # sleep 10 more seconds

def setInterfaceField( interfaceId, netsettings, fieldName, value ):
    for interface in netsettings['interfaces']['list']:
        if interface.get('interfaceId') == interfaceId:
            interface[fieldName] = value

def buildWanTestRule(matchInterface, testType="ping", pingHost="8.8.8.8", httpURL="http://192.168.244.1/", testInterval=5, testTimeout=2):
    name = "test " + str(testType) + " " + str(matchInterface)
    testInterval *= 1000  # convert from secs to millisecs
    testTimeout *= 1000  # convert from secs to millisecs
    rule = {
                "delayMilliseconds": testInterval, 
                "description": name, 
                "enabled": True, 
                "failureThreshold": 3, 
                "httpUrl": httpURL, 
                "interfaceId": matchInterface, 
                "javaClass": "com.untangle.node.wan_failover.WanTestSettings", 
                "pingHostname": pingHost, 
                "testHistorySize": 4, 
                "timeoutMilliseconds": testTimeout, 
                "type": testType
    }
    nodeData = node.getSettings()
    nodeData["tests"]["list"].append(rule)
    node.setSettings(nodeData)

def nukeRules():
    nodeData = node.getSettings()
    nodeData["tests"]["list"] = []
    node.setSettings(nodeData)

class WanFailoverTests(unittest2.TestCase):
    
    @staticmethod
    def nodeName():
        return "untangle-node-wan-failover"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global indexOfWans, nodeData, node, orig_netsettings
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        node.start()
        nodeData = node.getSettings()
        indexOfWans = global_functions.foundWans()

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.isOnline()
        assert (result == 0)
    
    def test_020_addPingTestForWans(self):
        nukeRules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            buildWanTestRule(wanIndex)
        result = remote_control.isOnline()
        assert (result == 0)
        
    def test_025_addPingFailTestForWans(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_025_addPingFailTestForWans")
        nukeRules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            buildWanTestRule(wanIndex, "ping", pingHost="192.168.244.1")

        waitForChangeInStatus()

        offlineCount = offlineWanCount()
        assert (offlineCount > 0)                        

        result = remote_control.isOnline()
        assert (result == 0)

        events = global_functions.get_events('WAN Failover','Outage Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 2, "action", "DISCONNECTED" )
        assert( found )
        
    def test_030_addArpTestForWans(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        nukeRules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            print "Testing interface : " + str(wanIndex)
            buildWanTestRule(wanIndex, "arp")

        waitForChangeInStatus()

        assert ( allWansOnline() )
        result = remote_control.isOnline()
        assert (result == 0)

    def test_035_addArpFailTestForWans(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_035_addArpFailTestForWans")
        nukeRules()
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Add a fake gateway for each of the interfaces
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            # set gateway to fake gateway
            setInterfaceField( wanIndex, netsettings, 'v4StaticGateway', '192.168.244.' + str(wanIndex))
            setInterfaceField( wanIndex, netsettings, 'v4AutoGatewayOverride', '192.168.244.' + str(wanIndex))
            buildWanTestRule(wanIndex, "arp")

        uvmContext.networkManager().setNetworkSettings(netsettings)
            
        waitForChangeInStatus()
        wansOffline = allWansOffline()

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (wansOffline)                        
        result = remote_control.isOnline()
        assert (result == 0)

    def test_040_addDNSTestForWans(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        nukeRules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            buildWanTestRule(wanIndex, "dns")

        waitForChangeInStatus()

        assert( allWansOnline() )
        result = remote_control.isOnline()
        assert (result == 0)        

    def test_045_addDNSFailTestForWans(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_045_addDNSFailTestForWans")

        nukeRules()
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Add a fake DNS for each of the interfaces
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            # set DNS values to fake DNS 
            setInterfaceField( wanIndex, netsettings, 'v4StaticDns1', '192.168.244.' + str(wanIndex))
            setInterfaceField( wanIndex, netsettings, 'v4StaticDns2', '192.168.244.' + str(wanIndex))
            setInterfaceField( wanIndex, netsettings, 'v4AutoDns1Override', '192.168.244.' + str(wanIndex))
            setInterfaceField( wanIndex, netsettings, 'v4AutoDns2Override', '192.168.244.' + str(wanIndex))
            buildWanTestRule(wanIndex, "dns")

        uvmContext.networkManager().setNetworkSettings(netsettings)

        waitForChangeInStatus()
        wansOffline = allWansOffline()

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (wansOffline)

    def test_050_addHTTPTestForWans(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        nukeRules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            buildWanTestRule(wanIndex, "http", httpURL="http://test.untangle.com/")

        waitForChangeInStatus()

        wansOnline = allWansOnline()

        assert (wansOnline)                        
        result = remote_control.isOnline()
        assert (result == 0)        

    def test_055_addHTTPFailTestForWans(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_055_addHTTPFailTestForWans")
        nukeRules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            buildWanTestRule(wanIndex, "http", httpURL="http://192.168.244.1/")

        waitForChangeInStatus()

        wansOffline = allWansOffline()
        assert (wansOffline)                        
        result = remote_control.isOnline()
        assert (result == 0)        

    def test_060_downJustOneWan(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        invalidWanIP = None
        offlineWanIndex = None
        for upWanIndexTup in indexOfWans:
            upWanIndex = upWanIndexTup[0]
            nukeRules()
            for wanIndexTup in indexOfWans:
                wanIndex = wanIndexTup[0]
                if upWanIndex == wanIndex:
                    # make this interface test disconnected
                    offlineWanIndex = wanIndex
                    buildWanTestRule(offlineWanIndex, "ping", pingHost="192.168.244.1")
                    invalidWanIP = wanIndexTup[2]
                    print "InvalidIP is %s" % invalidWanIP
                else:
                    buildWanTestRule(wanIndex, "ping")

            waitForChangeInStatus()

            offlineWans = offlineWanCount()
            assert( offlineWans > 0 )

            if (len(indexOfWans) > 1):
                # Skip the WAN IP address check part of the test if test box only has one WAN
                for x in range(0, 8):
                    result = global_functions.getIpAddress()
                    print "IP address %s and invalidWanIP %s" % (result,invalidWanIP)
                    assert (result != invalidWanIP)    

    def test_065_downAllButOneWan(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_065_downAllButOneWan")
        pre_count = global_functions.getStatusValue(node,"changed")

        validWanIP = None
        # loop through the WANs keeping one up and the rest down.
        for upWanIndexTup in indexOfWans:
            upWanIndex = upWanIndexTup[0]
            nukeRules()
            for wanIndexTup in indexOfWans:
                wanIndex = wanIndexTup[0]
                if upWanIndex != wanIndex:
                    # make this interface test disconnected
                    buildWanTestRule(wanIndex, "ping", pingHost="192.168.244.1")
                else:
                    validWanIP = wanIndexTup[2]
                    buildWanTestRule(upWanIndex, "ping", pingHost="8.8.8.8")
                    print "validIP is %s" % validWanIP

            waitForChangeInStatus()

            onlineWans = onlineWanCount()
            offlineWans = offlineWanCount()

            assert( onlineWans == 1 )
            assert( offlineWans > 0 )

            for x in range(0, 8):
                result = global_functions.getIpAddress()
                print "IP address %s and validWanIP %s" % (result,validWanIP)
                assert (result == validWanIP)    

        # Check to see if the faceplate counters have incremented. 
        post_count = global_functions.getStatusValue(node,"changed")
        assert(pre_count < post_count)

    @staticmethod
    def finalTearDown(self):
        global node
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None


test_registry.registerNode("wan-failover", WanFailoverTests)
