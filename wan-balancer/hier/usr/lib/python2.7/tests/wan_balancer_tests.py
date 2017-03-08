import unittest2
import os
import time
import sys
reload(sys)
import commands
import re
import subprocess
import socket
sys.setdefaultencoding("utf-8")

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import test_registry
import remote_control
import global_functions

node = None
nodeData = None
nodeWanFailover = None
nodeDataWanFailover = None
orig_netsettings = None
ip_address_testdestination = None
indexOfWans = []
ruleCounter = 0
defaultRackId = 1

def setWeightOfWan(interfaceId, weight):
    if interfaceId == None or interfaceId == 0:
        print "Invalid interface: " + str(interfaceId)
        return
    nodeData = node.getSettings()
    if (interfaceId == "all"):
        i = 0
        for intefaceIndex in nodeData["weights"]:
            nodeData["weights"][i] = weight
            i += 1
    else:
        nodeData["weights"][interfaceId-1] = weight
    node.setSettings(nodeData)

def createRouteRule( networkAddr, netmask, gateway):
    return {
        "description": "wan-balancer test route", 
        "javaClass": "com.untangle.uvm.network.StaticRoute", 
        "network": networkAddr, 
        "nextHop": gateway, 
        "prefix": netmask, 
        "ruleId": 1, 
        "toAddr": True, 
        "toDev": False
         };

def buildNatRule(ruleType="DST_ADDR", ruleValue="1.1.1.1", newSource="1.1.1.1"):
    global ruleCounter
    
    name = "test nat " + str(ruleValue) + " Source " + str(newSource)
    ruleCounter +=1
    ruleCondition = {
        "invert": False, 
        "javaClass": "com.untangle.uvm.network.NatRuleCondition",
        "conditionType": ruleType, 
        "value": ruleValue
    }
    rule = {
        "auto": False, 
        "description": name, 
        "enabled": True, 
        "javaClass": "com.untangle.uvm.network.NatRule", 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": [
            ]
        }, 
        "newSource": newSource, 
        "ruleId": ruleCounter
    }
    rule["conditions"]["list"].append(ruleCondition)
    return rule

def buildSingleWanRouteRule(ruleType="DST_ADDR", ruleValue="1.1.1.1", wanDestination=1):
    print "wanDestination: %s" % wanDestination
    global ruleCounter
    nodeData = node.getSettings()
    name = "test route " + str(ruleValue) + " Wan " + str(wanDestination)
    ruleCounter +=1
    ruleCondition = {
        "invert": False, 
        "javaClass": "com.untangle.node.wan_balancer.RouteRuleCondition", 
        "conditionType": ruleType, 
        "value": ruleValue
    }
    rule = {
        "description": name, 
        "destinationWan": wanDestination, 
        "enabled": True, 
        "javaClass": "com.untangle.node.wan_balancer.RouteRule", 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": []
        }, 
        "ruleId": ruleCounter
    }
    rule["conditions"]["list"].append(ruleCondition)
    nodeData["routeRules"]["list"].append(rule)
    node.setSettings(nodeData)

def nukeWanBalancerRouteRules():
    nodeData = node.getSettings()
    nodeData["routeRules"]["list"] = []
    node.setSettings(nodeData)

def appendRouteRule(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['staticRoutes']['list'].append(newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)

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
    
    nodeDataWanFailover = nodeWanFailover.getSettings()
    wanRuleIndex = None
    # check to see if rule for the same wan exist, if so overwrite rule
    for i in range(len(nodeDataWanFailover['tests']['list'])):
        if nodeDataWanFailover['tests']['list'][i]["interfaceId"] == matchInterface:
            wanRuleIndex=i
            break
    if wanRuleIndex == None:
        nodeDataWanFailover["tests"]["list"].append(rule)
    else:
        nodeDataWanFailover["tests"]["list"][wanRuleIndex] = rule
    nodeWanFailover.setSettings(nodeDataWanFailover)
    
def nukeWanBalancerRules():
    setWeightOfWan("all",50)
    nodeData = node.getSettings()
    nodeData["routeRules"]["list"] = []
    node.setSettings(nodeData)

def nukeFailoverRules():
    nodeDataWanFailover = nodeWanFailover.getSettings()
    nodeDataWanFailover["tests"]["list"] = []
    nodeWanFailover.setSettings(nodeDataWanFailover)
    
def sameWanNetwork(indexWANs):
    previousExtIP = None
    wan_match = False
    for wanIndexTup in indexWANs:
        currentExtIP = wanIndexTup[2]
        if (previousExtIP == currentExtIP):
            wan_match = True
            break
        else:
            previousExtIP = currentExtIP    
    return wan_match
            
class WanBalancerTests(unittest2.TestCase):
    
    @staticmethod
    def nodeName():
        return "untangle-node-wan-balancer"

    @staticmethod
    def nodeNameWanFailover():
        return "untangle-node-wan-failover"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initialSetUp(self):
        global indexOfWans, node, nodeData, nodeWanFailover, nodeDataWanFailover, orig_netsettings, ip_address_testdestination
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), defaultRackId)
        node.start()
        nodeData = node.getSettings()

        if (uvmContext.nodeManager().isInstantiated(self.nodeNameWanFailover())):
            raise Exception('node %s already instantiated' % self.nodeNameWanFailover())
        nodeWanFailover = uvmContext.nodeManager().instantiate(self.nodeNameWanFailover(), defaultRackId)
        nodeWanFailover.start()
        nodeWanFailoverData = nodeWanFailover.getSettings()

        indexOfWans = global_functions.get_wan_tuples()
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        ip_address_testdestination =  socket.gethostbyname("test.untangle.com")

    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)
    
    def test_020_stickySession(self):
        result = global_functions.get_public_ip_address()
        print "Initial IP address %s" % result
        for x in range(0, 8):
            result2 = global_functions.get_public_ip_address()
            print "Test IP address %s" % result2
            assert (result == result2)
    
    def test_030_heavyWeightWan(self):
        # Set the one WAN as 100 weight and the other as zero and vice versa.
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_030_heavyWeightWan")
        setWeightOfWan("all", 0)
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            setWeightOfWan(wanIndex, 100)
            # get the WAN IP address which was weighted to 100
            weightedIP = wanIndexTup[2]
            # Test that only the weighted interface is used 10 times
            for x in range(0, 9):
                result = global_functions.get_public_ip_address()
                print "Weighted IP %s and retrieved IP %s" % (weightedIP, result)
                assert (result == weightedIP)
            # reset weight to zero
            setWeightOfWan(wanIndex, 0)
        # return weight settings to default
        setWeightOfWan("all", 50)
        
    def test_040_balanced(self):
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_040_routedByIPWan")
        # Set weighting to default 
        setWeightOfWan("all", 50)

        # Test balanced
        # send netcat UDP to random IPs
        result = remote_control.run_command("for i in \`seq 100\` ; do echo \"test\" | netcat -u -w1 -q1 1.2.3.\$i 7 >/dev/null ; done",stdout=False)

        events = global_functions.get_events('Network','All Sessions',None,20)
        assert(events != None)
        for wanIndexTup in indexOfWans:
            found = global_functions.check_events( events.get('list'), 20,
                                                "server_intf", wanIndexTup[0],
                                                "c_client_addr", remote_control.clientIP,
                                                "s_server_port", 7)
            assert(found)

    def test_050_routedByIPWan(self):
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_050_routedByIPWan")
        # Set weighting to default 
        setWeightOfWan("all", 50)

        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            nukeWanBalancerRouteRules()
            # Get the external IP of the interface selected.
            routedIP = wanIndexTup[2]
            buildSingleWanRouteRule("DST_ADDR",ip_address_testdestination,wanIndex)
            # Test that only the routed interface is used 10 times
            for x in range(0, 9):
                result = global_functions.get_public_ip_address()
                print "Routed IP %s and retrieved IP %s" % (routedIP, result)
                assert (result == routedIP)
        nukeWanBalancerRouteRules()

    def test_060_routedByPortWan(self):        
        # Test that route rules override weighted rules on 2 WANs
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_060_routedByPortWan")
        nukeWanBalancerRouteRules()

        setWeightOfWan("all", 50)
        # Set all port 80 traffic out the first WAN and 443 on the other
        port80Index = indexOfWans[0][0]
        port80IP = indexOfWans[0][2]
        port443Index = indexOfWans[1][0]
        port443IP = indexOfWans[1][2]
        print "index443 %s" % port443Index
        buildSingleWanRouteRule("DST_PORT",80,(port80Index))
        buildSingleWanRouteRule("DST_PORT",443,(port443Index))

        for x in range(0, 9):
            result80 = global_functions.get_public_ip_address()
            result443 = global_functions.get_public_ip_address(base_URL="https://test.untangle.com",extra_options="--no-check-certificate --secure-protocol=auto")
            print "80 IP %s and 443 IP %s" % (result80, result443)
            assert (result80 == port80IP)
            assert (result443 == port443IP)

    def test_070_weightVsRoutedWan(self):        
        # Test that route rules override weighted rules
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_070_weightVsRoutedWan")
            
        for x in range(0, 2):
            nukeWanBalancerRouteRules()
            setWeightOfWan("all", 0)
            wanIndexTup = indexOfWans[x]
            wanIndex = wanIndexTup[0]
            setWeightOfWan(wanIndex, 100)
            if (x==0):
                y=1
            else:
                y=0
            weightedIndexTup = indexOfWans[x]
            weightedIP = weightedIndexTup[2]
            routedIndexTup = indexOfWans[y]
            routedIP = routedIndexTup[2]
            # WAN index for route rules is +1 from networking list.  Balance is zero            
            buildSingleWanRouteRule("DST_ADDR",ip_address_testdestination,routedIndexTup[0])

            for x in range(0, 9):
                result = global_functions.get_public_ip_address()
                print "Routed IP %s and retrieved IP %s" % (routedIP, result)
                assert (result == routedIP)
                subprocess.check_output("ip route flush cache", shell=True)
        
    def test_080_routedWanVsNetworkRoute(self):    
        # Test that Networking routes override routed rules in WAN Balancer
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for test_080_routedWanVsNetworkRoute")
        netsettings = netsettings = uvmContext.networkManager().getNetworkSettings()
        nukeWanBalancerRouteRules()

        setWeightOfWan("all", 0)
        weightedIndexTup = indexOfWans[0]
        routedIndexTup = indexOfWans[1]
        setWeightOfWan(weightedIndexTup[0], 100)  # set primary to 100%
        # configure route in network which uses the second WAN for test.untangle.com
        routedIP = routedIndexTup[2]
        routedIPGateway = routedIndexTup[3]
        appendRouteRule(createRouteRule(ip_address_testdestination,32,routedIPGateway))
        result1 = global_functions.get_public_ip_address()
        print "Routed IP %s and retrieved IP %s" % (routedIP, result1)
        # Add WAN route IP to Balancer
        buildSingleWanRouteRule("DST_ADDR",ip_address_testdestination,weightedIndexTup[0])
        result2 = global_functions.get_public_ip_address(extra_options="--no-check-certificate --secure-protocol=auto")
        print "Routed IP %s and retrieved IP %s" % (routedIP, result2)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (result1 == routedIP)
        assert (result2 == routedIP)

    def test_090_heavyWeightWanDown(self):
        # Set the one WAN as 100 weight and the other as zero and down the 100 weight wan
        # if there are more than one WAN
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for combination of wan-balancer and wan failover tests")
        # initialize all the weights to 50
        nukeWanBalancerRules()
        # create valid failover tests
        for wanIndexTup in indexOfWans:
            buildWanTestRule(wanIndexTup[0])
        result = remote_control.is_online()
        assert (result == 0)
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            # set the selected wan to 100 and others to zero
            setWeightOfWan("all",0)
            setWeightOfWan(wanIndex, 100)
            # Set the weighted interface with invalid rule
            buildWanTestRule(wanIndex, "ping", "192.168.244.1")
            # get the WAN IP address which was weighted to 100
            weightedIP = wanIndexTup[1]
            # Wait for targeted the WAN to be off line before testing that the WAN is off line.
            timeout = 50000
            online = True
            offlineWanIndex = wanIndex
            while online and timeout > 0:
                timeout -= 1
                wanStatus = nodeWanFailover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if statusInterface['interfaceId'] == offlineWanIndex:
                        online =  statusInterface['online']
            time.sleep(10) # Let WAN balancer see that the heavy interface is down
            # Test that other interfaces are used 10 times
            for x in range(0, 9):
                result = global_functions.get_public_ip_address()
                print "Weighted IP %s and retrieved IP %s" % (weightedIP, result)
                assert (result != weightedIP)
            # reset weight to zero and interface to valid rule
            setWeightOfWan(wanIndex, 0)
            buildWanTestRule(wanIndex)

        # return settings to default
        nukeWanBalancerRules();
        nukeFailoverRules()
                    
    def test_100_wanBalancerRouteWanDown(self):
        # create a source route and then down the wan which the route is set to
        # if there are more than one WAN
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for combination of wan-balancer and wan failover tests")
        if sameWanNetwork(indexOfWans):
            raise unittest2.SkipTest("WANS on same network")

        netsettings = uvmContext.networkManager().getNetworkSettings()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            nukeWanBalancerRules();
            # Get the external IP of the interface selected.
            routedIP = wanIndexTup[2]
            # WAN index for route rules is from networking list.  Balance is zero  
            buildSingleWanRouteRule("DST_ADDR",ip_address_testdestination,wanIndex)
            # Test that only the routed interface is used 5 times2
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
                print "WAN Balancer Routed IP %s and retrieved IP %s" % (routedIP, result)
                assert (result == routedIP)
            # now down the selected wan and see if traffic flows out the other wan
            buildWanTestRule(wanIndex, "ping", "192.168.244.1")
            # Wait for targeted the WAN to be off line before testing that the WAN is off line.
            timeout = 50000
            online = True
            offlineWanIndex = wanIndex
            while online and timeout > 0:
                timeout -= 1
                wanStatus = nodeWanFailover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if statusInterface['interfaceId'] == offlineWanIndex:
                        online =  statusInterface['online']
            time.sleep(10) # Let WAN balancer see that the interface is down
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = global_functions.get_public_ip_address()
                print "WAN Down WAN Balancer Routed IP %s and retrieved IP %s" % (routedIP, result)
                assert (result != routedIP)

        nukeWanBalancerRules();
        nukeFailoverRules()
    
    def test_110_networkRouteWanDown(self):
        # create a network route and then down the wan which the route is set to
        # if there are more than one WAN
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for combination of wan-balancer and wan failover tests")
        netsettings = netsettings = uvmContext.networkManager().getNetworkSettings()
        nukeWanBalancerRules();

        for wanIndexTup in indexOfWans:
            # Add networking route which does not handle re-routing if WAN is down
            # get the WAN IP address which was source routed
            wanIndex = wanIndexTup[0]
            # Get the external IP of the interface selected.
            routedIP = wanIndexTup[2]
            wanGatewayIP = wanIndexTup[3]
            netsettings['staticRoutes']['list']=[]
            netsettings['staticRoutes']['list'].append(createRouteRule(ip_address_testdestination,32,wanGatewayIP))
            uvmContext.networkManager().setNetworkSettings(netsettings)
            # Test that only the routed interface is used 5 times
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
                print "Network Routed IP %s and retrieved IP %s" % (routedIP, result)
                assert (result == routedIP)
            # now down the selected wan and see if traffic flows out the other wan
            buildWanTestRule(wanIndex, "ping", "192.168.244.1")
            # Wait for targeted the WAN to be off line before testing that the WAN is off line.
            timeout = 50000
            online = True
            offlineWanIndex = wanIndex
            while online and timeout > 0:
                timeout -= 1
                wanStatus = nodeWanFailover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if statusInterface['interfaceId'] == offlineWanIndex:
                        online =  statusInterface['online']
            time.sleep(10) # Let WAN balancer see that the interface is down
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = global_functions.get_public_ip_address()
                print "WAN Down Network Routed IP %s and retrieved IP %s" % (routedIP, result)
                assert (result == routedIP)

        nukeFailoverRules()
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

    def test_120_natOneToOneWanDown(self):
        # create a 1:1 NAT and then down the wan which the NAT is set to
        # if there are more than one WAN
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest2.SkipTest("Need at least two WANS for combination of wan-balancer and wan failover tests")

        pre_count = global_functions.get_app_metric_value(nodeWanFailover,"changed")
        
        # raise unittest2.SkipTest('Skipping test_120_natOneToOneWanDown as not possible with current network layout ')
        netsettings = uvmContext.networkManager().getNetworkSettings()
        nukeWanBalancerRules();
        nukeFailoverRules()
        # create valid failover tests
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            buildWanTestRule(wanIndex)

        for wanIndexTup in indexOfWans:
            # get the WAN IP address which was source routed
            wanIndex = wanIndexTup[0]
            wanIP = wanIndexTup[1]
            wanExternalIP = wanIndexTup[2]

            # Add networking route which does not handle re-routing if WAN is down
            netsettings['natRules']['list']=[]
            netsettings['natRules']['list'].append(buildNatRule("DST_ADDR",ip_address_testdestination,wanIP))
            uvmContext.networkManager().setNetworkSettings(netsettings)

            # Test that only the routed interface is used 5 times
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = global_functions.get_public_ip_address()
                print "NAT 1:1 IP %s External IP %s and retrieved IP %s" % (wanIP, wanExternalIP, result)
                assert (result == wanExternalIP)
            # now down the selected wan and see if traffic flows out the other wan
            buildWanTestRule(wanIndex, "ping", "192.168.244.1")
            # Wait for targeted the WAN to be off line before testing that the WAN is off line.
            timeout = 50000
            online = True
            offlineWanIndex = wanIndex
            while online and timeout > 0:
                timeout -= 1
                wanStatus = nodeWanFailover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if statusInterface['interfaceId'] == offlineWanIndex:
                        online =  statusInterface['online']
            time.sleep(10) # Let WAN balancer see that the interface is down
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = global_functions.get_public_ip_address()
                print "WAN Down NAT 1:1 IP %s  External IP %s and retrieved IP %s" % (wanIP, wanExternalIP, result)
                assert (result == wanExternalIP)
            uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        nukeFailoverRules()
        # Check to see if the faceplate counters have incremented. 
        post_count = global_functions.get_app_metric_value(nodeWanFailover,"changed")
        assert(pre_count < post_count)
               
    @staticmethod
    def finalTearDown(self):
        global node, nodeWanFailover
        # Restore original settings to return to initial settings
        if node != None:
            uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
            node = None
        if nodeWanFailover != None:
            uvmContext.nodeManager().destroy( nodeWanFailover.getNodeSettings()["id"] )
            nodeWanFailover = None
        if orig_netsettings != None:
            uvmContext.networkManager().setNetworkSettings(orig_netsettings)


test_registry.registerNode("wan-balancer", WanBalancerTests)
