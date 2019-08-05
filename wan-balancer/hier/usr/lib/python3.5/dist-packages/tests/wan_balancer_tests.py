"""wan_balancer tests"""
import time
import sys
import subprocess
import socket

import unittest
import runtests
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

app = None
app_data = None
app_wan_failover = None
app_data_wan_failover = None
orig_netsettings = None
ip_address_testdestination = None
index_of_wans = []
rule_counter = 0
default_policy_id = 1

def set_wan_weight(interfaceId, weight):
    if interfaceId == None or interfaceId == 0:
        print("Invalid interface: " + str(interfaceId))
        return
    app_data = app.getSettings()
    if (interfaceId == "all"):
        i = 0
        for intefaceIndex in app_data["weights"]:
            app_data["weights"][i] = weight
            i += 1
    else:
        app_data["weights"][interfaceId-1] = weight
    app.setSettings(app_data)

def create_route_rule( networkAddr, netmask, gateway):
    return {
        "description": "wan-balancer test route", 
        "javaClass": "com.untangle.uvm.network.StaticRoute", 
        "network": networkAddr, 
        "nextHop": gateway, 
        "prefix": netmask, 
        "ruleId": 1, 
        "toAddr": True, 
        "toDev": False
         }

def build_nat_rules(ruleType="DST_ADDR", ruleValue="1.1.1.1", newSource="1.1.1.1"):
    global rule_counter
    
    name = "test nat " + str(ruleValue) + " Source " + str(newSource)
    rule_counter +=1
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
        "ruleId": rule_counter
    }
    rule["conditions"]["list"].append(ruleCondition)
    return rule

def build_single_wan_route_rule(ruleType="DST_ADDR", ruleValue="1.1.1.1", wanDestination=1):
    print("wanDestination: %s" % wanDestination)
    global rule_counter
    app_data = app.getSettings()
    name = "test route " + str(ruleValue) + " Wan " + str(wanDestination)
    rule_counter +=1
    ruleCondition = {
        "invert": False, 
        "javaClass": "com.untangle.app.wan_balancer.RouteRuleCondition", 
        "conditionType": ruleType, 
        "value": ruleValue
    }
    rule = {
        "description": name, 
        "destinationWan": wanDestination, 
        "enabled": True, 
        "javaClass": "com.untangle.app.wan_balancer.RouteRule", 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": []
        }, 
        "ruleId": rule_counter
    }
    rule["conditions"]["list"].append(ruleCondition)
    app_data["routeRules"]["list"].append(rule)
    app.setSettings(app_data)

def nuke_wan_balancer_route_rules():
    app_data = app.getSettings()
    app_data["routeRules"]["list"] = []
    app.setSettings(app_data)

def append_route_rule(newRule):
    netsettings = uvmContext.networkManager().getNetworkSettings()
    netsettings['staticRoutes']['list'].append(newRule)
    uvmContext.networkManager().setNetworkSettings(netsettings)

def build_wan_test_rule(matchInterface, testType="ping", pingHost="8.8.8.8", httpURL="http://192.168.244.1/", testInterval=5, testTimeout=2):
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
                "javaClass": "com.untangle.app.wan_failover.WanTestSettings", 
                "pingHostname": pingHost, 
                "testHistorySize": 4, 
                "timeoutMilliseconds": testTimeout, 
                "type": testType
    }
    
    app_data_wan_failover = app_wan_failover.getSettings()
    wanRuleIndex = None
    # check to see if rule for the same wan exist, if so overwrite rule
    for i in range(len(app_data_wan_failover['tests']['list'])):
        if app_data_wan_failover['tests']['list'][i]["interfaceId"] == matchInterface:
            wanRuleIndex=i
            break
    if wanRuleIndex == None:
        app_data_wan_failover["tests"]["list"].append(rule)
    else:
        app_data_wan_failover["tests"]["list"][wanRuleIndex] = rule
    app_wan_failover.setSettings(app_data_wan_failover)
    
def nuke_wan_balancer_rules():
    set_wan_weight("all",50)
    app_data = app.getSettings()
    app_data["routeRules"]["list"] = []
    app.setSettings(app_data)

def nuke_wan_failover_rules():
    app_data_wan_failover = app_wan_failover.getSettings()
    app_data_wan_failover["tests"]["list"] = []
    app_wan_failover.setSettings(app_data_wan_failover)
    
def same_wan_network(indexWANs):
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
            
class WanBalancerTests(unittest.TestCase):
    
    @staticmethod
    def module_name():
        return "wan-balancer"

    @staticmethod
    def appNameWanFailover():
        return "wan-failover"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initial_setup(self):
        global index_of_wans, app, app_data, app_wan_failover, app_data_wan_failover, orig_netsettings, ip_address_testdestination
        if (uvmContext.appManager().isInstantiated(self.module_name())):
            raise Exception('app %s already instantiated' % self.module_name())

        app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)
        app.start()
        app_data = app.getSettings()
            
        if (uvmContext.appManager().isInstantiated(self.appNameWanFailover())):
            raise Exception('app %s already instantiated' % self.appNameWanFailover())
        app_wan_failover = uvmContext.appManager().instantiate(self.appNameWanFailover(), default_policy_id)
        app_wan_failover.start()
        app_wan_failoverData = app_wan_failover.getSettings()

        index_of_wans = global_functions.get_wan_tuples()
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        ip_address_testdestination =  socket.gethostbyname("test.untangle.com")

    def setUp(self):
        print() # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)
    
    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_stickySession(self):
        result = global_functions.get_public_ip_address()
        print("Initial IP address %s" % result)
        for x in range(0, 8):
            result2 = global_functions.get_public_ip_address()
            print("Test IP address %s" % result2)
            assert (result == result2)
    
    def test_030_heavyWeightWan(self):
        # Set the one WAN as 100 weight and the other as zero and vice versa.
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_030_heavyWeightWan")
        set_wan_weight("all", 0)
        for wanIndexTup in index_of_wans:
            wanIndex = wanIndexTup[0]
            set_wan_weight(wanIndex, 100)
            # get the WAN IP address which was weighted to 100
            weightedIP = wanIndexTup[2]
            # Test that only the weighted interface is used 10 times
            for x in range(0, 9):
                result = global_functions.get_public_ip_address()
                print("Weighted IP %s and retrieved IP %s" % (weightedIP, result))
                assert (result == weightedIP)
            # reset weight to zero
            set_wan_weight(wanIndex, 0)
        # return weight settings to default
        set_wan_weight("all", 50)
        
    def test_040_balanced(self):
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_040_routedByIPWan")
        # Set weighting to default 
        set_wan_weight("all", 50)

        # Test balanced
        # send netcat UDP to random IPs
        result = remote_control.run_command("for i in \`seq 100\` ; do echo \"test\" | netcat -u -w1 -q1 1.2.3.\$i 7 >/dev/null ; done",stdout=False)

        events = global_functions.get_events('Network','All Sessions',None,20)
        assert(events != None)
        for wanIndexTup in index_of_wans:
            found = global_functions.check_events( events.get('list'), 20,
                                                "server_intf", wanIndexTup[0],
                                                "c_client_addr", remote_control.client_ip,
                                                "s_server_port", 7)
            assert(found)

    def test_050_routedByIPWan(self):
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_050_routedByIPWan")
        # Set weighting to default 
        set_wan_weight("all", 50)

        for wanIndexTup in index_of_wans:
            wanIndex = wanIndexTup[0]
            nuke_wan_balancer_route_rules()
            # Get the external IP of the interface selected.
            routedIP = wanIndexTup[2]
            build_single_wan_route_rule("DST_ADDR",ip_address_testdestination,wanIndex)
            # Test that only the routed interface is used 10 times
            for x in range(0, 9):
                result = global_functions.get_public_ip_address()
                print("Routed IP %s and retrieved IP %s" % (routedIP, result))
                assert (result == routedIP)
        nuke_wan_balancer_route_rules()

    def test_060_routedByPortWan(self):        
        # Test that route rules override weighted rules on 2 WANs
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_060_routedByPortWan")
        nuke_wan_balancer_route_rules()

        set_wan_weight("all", 50)
        # Set all port 80 traffic out the first WAN and 443 on the other
        port80Index = index_of_wans[0][0]
        port80IP = index_of_wans[0][2]
        port443Index = index_of_wans[1][0]
        port443IP = index_of_wans[1][2]
        print("index443 %s" % port443Index)
        build_single_wan_route_rule("DST_PORT",80,(port80Index))
        build_single_wan_route_rule("DST_PORT",443,(port443Index))

        for x in range(0, 9):
            result80 = global_functions.get_public_ip_address()
            result443 = global_functions.get_public_ip_address(base_URL="https://test.untangle.com",extra_options="--no-check-certificate --secure-protocol=auto")
            print("80 IP %s and 443 IP %s" % (result80, result443))
            assert (result80 == port80IP)
            assert (result443 == port443IP)

    def test_070_weightVsRoutedWan(self):        
        # Test that route rules override weighted rules
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_070_weightVsRoutedWan")
            
        for x in range(0, 2):
            nuke_wan_balancer_route_rules()
            set_wan_weight("all", 0)
            wanIndexTup = index_of_wans[x]
            wanIndex = wanIndexTup[0]
            set_wan_weight(wanIndex, 100)
            if (x==0):
                y=1
            else:
                y=0
            weightedIndexTup = index_of_wans[x]
            weightedIP = weightedIndexTup[2]
            routedIndexTup = index_of_wans[y]
            routedIP = routedIndexTup[2]
            # WAN index for route rules is +1 from networking list.  Balance is zero            
            build_single_wan_route_rule("DST_ADDR",ip_address_testdestination,routedIndexTup[0])

            for x in range(0, 9):
                result = global_functions.get_public_ip_address()
                print("Routed IP %s and retrieved IP %s" % (routedIP, result))
                assert (result == routedIP)
                subprocess.check_output("ip route flush cache", shell=True)
        
    def test_080_routedWanVsNetworkRoute(self):    
        # Test that Networking routes override routed rules in WAN Balancer
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_080_routedWanVsNetworkRoute")
        netsettings = netsettings = uvmContext.networkManager().getNetworkSettings()
        nuke_wan_balancer_route_rules()

        set_wan_weight("all", 0)
        weightedIndexTup = index_of_wans[0]
        routedIndexTup = index_of_wans[1]
        set_wan_weight(weightedIndexTup[0], 100)  # set primary to 100%
        # configure route in network which uses the second WAN for test.untangle.com
        routedIP = routedIndexTup[2]
        routedIPGateway = routedIndexTup[3]
        append_route_rule(create_route_rule(ip_address_testdestination,32,routedIPGateway))
        result1 = global_functions.get_public_ip_address()
        print("Routed IP %s and retrieved IP %s" % (routedIP, result1))
        # Add WAN route IP to Balancer
        build_single_wan_route_rule("DST_ADDR",ip_address_testdestination,weightedIndexTup[0])
        result2 = global_functions.get_public_ip_address(extra_options="--no-check-certificate --secure-protocol=auto")
        print("Routed IP %s and retrieved IP %s" % (routedIP, result2))
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (result1 == routedIP)
        assert (result2 == routedIP)

    def test_090_heavyWeightWanDown(self):
        # Set the one WAN as 100 weight and the other as zero and down the 100 weight wan
        # if there are more than one WAN
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for combination of wan-balancer and wan failover tests")
        # initialize all the weights to 50
        nuke_wan_balancer_rules()
        # create valid failover tests
        for wanIndexTup in index_of_wans:
            build_wan_test_rule(wanIndexTup[0])
        result = remote_control.is_online()
        assert (result == 0)
        for wanIndexTup in index_of_wans:
            wanIndex = wanIndexTup[0]
            # set the selected wan to 100 and others to zero
            set_wan_weight("all",0)
            set_wan_weight(wanIndex, 100)
            # Set the weighted interface with invalid rule
            build_wan_test_rule(wanIndex, "ping", "192.168.244.1")
            # get the WAN IP address which was weighted to 100
            weightedIP = wanIndexTup[1]
            # Wait for targeted the WAN to be off line before testing that the WAN is off line.
            timeout = 50000
            online = True
            offlineWanIndex = wanIndex
            while online and timeout > 0:
                timeout -= 1
                wanStatus = app_wan_failover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if statusInterface['interfaceId'] == offlineWanIndex:
                        online =  statusInterface['online']
            time.sleep(10) # Let WAN balancer see that the heavy interface is down
            # Test that other interfaces are used 10 times
            for x in range(0, 9):
                result = global_functions.get_public_ip_address()
                print("Weighted IP %s and retrieved IP %s" % (weightedIP, result))
                assert (result != weightedIP)
            # reset weight to zero and interface to valid rule
            set_wan_weight(wanIndex, 0)
            build_wan_test_rule(wanIndex)

        # return settings to default
        nuke_wan_balancer_rules()
        nuke_wan_failover_rules()
                    
    def test_100_wanBalancerRouteWanDown(self):
        # create a source route and then down the wan which the route is set to
        # if there are more than one WAN
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for combination of wan-balancer and wan failover tests")
        if same_wan_network(index_of_wans):
            raise unittest.SkipTest("WANS on same network")

        netsettings = uvmContext.networkManager().getNetworkSettings()
        for wanIndexTup in index_of_wans:
            wanIndex = wanIndexTup[0]
            nuke_wan_balancer_rules()
            # Get the external IP of the interface selected.
            routedIP = wanIndexTup[2]
            # WAN index for route rules is from networking list.  Balance is zero  
            build_single_wan_route_rule("DST_ADDR",ip_address_testdestination,wanIndex)
            # Test that only the routed interface is used 5 times2
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
                print("WAN Balancer Routed IP %s and retrieved IP %s" % (routedIP, result))
                assert (result == routedIP)
            # now down the selected wan and see if traffic flows out the other wan
            build_wan_test_rule(wanIndex, "ping", "192.168.244.1")
            # Wait for targeted the WAN to be off line before testing that the WAN is off line.
            timeout = 50000
            online = True
            offlineWanIndex = wanIndex
            while online and timeout > 0:
                timeout -= 1
                wanStatus = app_wan_failover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if statusInterface['interfaceId'] == offlineWanIndex:
                        online =  statusInterface['online']
            time.sleep(10) # Let WAN balancer see that the interface is down
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = global_functions.get_public_ip_address()
                print("WAN Down WAN Balancer Routed IP %s and retrieved IP %s" % (routedIP, result))
                assert (result != routedIP)

        nuke_wan_balancer_rules()
        nuke_wan_failover_rules()
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)
    
    def test_110_networkRouteWanDown(self):
        # create a network route and then down the wan which the route is set to
        # if there are more than one WAN
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for combination of wan-balancer and wan failover tests")
        netsettings = uvmContext.networkManager().getNetworkSettings()
        nuke_wan_balancer_rules()

        for wanIndexTup in index_of_wans:
            # Add networking route which does not handle re-routing if WAN is down
            # get the WAN IP address which was source routed
            wanIndex = wanIndexTup[0]
            print("Testing interface: %s" % str(wanIndexTup))
            sys.stdout.flush()
            # Get the external IP of the interface selected.
            routedIP = wanIndexTup[2]
            wanGatewayIP = wanIndexTup[3]
            netsettings['staticRoutes']['list']=[]
            netsettings['staticRoutes']['list'].append(create_route_rule(ip_address_testdestination,32,wanGatewayIP))
            uvmContext.networkManager().setNetworkSettings(netsettings)
            # Test that only the routed interface is used 5 times
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = ""
                timeout = 60
                while (timeout > 0 and result == ""):
                    time.sleep(1)
                    timeout -= 1
                    result = remote_control.run_command("wget --timeout=4 -q -O - \"$@\" test.untangle.com/cgi-bin/myipaddress.py",stdout=True)
                print("Network Routed IP %s and retrieved IP %s" % (routedIP, result))
                assert (result == routedIP)
            # now down the selected wan and see if traffic flows out the other wan
            build_wan_test_rule(wanIndex, "ping", "192.168.244.1")
            # Wait for targeted the WAN to be off line before testing that the WAN is off line.
            timeout = 60
            online = True
            offlineWanIndex = wanIndex
            while online and timeout > 0:
                time.sleep(1)
                timeout -= 1
                wanStatus = app_wan_failover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if statusInterface['interfaceId'] == offlineWanIndex:
                        print("Waiting on WAN %i to go offline... %s " % (wanIndex,str(statusInterface)))
                        sys.stdout.flush()
                        online =  statusInterface['online']
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = global_functions.get_public_ip_address()
                print("WAN Down Network Routed IP %s and retrieved IP %s" % (routedIP, result))
                assert (result == routedIP)

        # Remove failover rules
        nuke_wan_failover_rules()
        # Remove static routes
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

    def test_120_natOneToOneWanDown(self):
        # create a 1:1 NAT and then down the wan which the NAT is set to
        # if there are more than one WAN
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (len(index_of_wans) < 2):
            raise unittest.SkipTest("Need at least two WANS for combination of wan-balancer and wan failover tests")

        pre_count = global_functions.get_app_metric_value(app_wan_failover,"changed")
        
        # raise unittest.SkipTest('Skipping test_120_natOneToOneWanDown as not possible with current network layout ')
        netsettings = uvmContext.networkManager().getNetworkSettings()
        nuke_wan_balancer_rules()
        nuke_wan_failover_rules()
        # create valid failover tests
        for wanIndexTup in index_of_wans:
            wanIndex = wanIndexTup[0]
            build_wan_test_rule(wanIndex)

        for wanIndexTup in index_of_wans:
            # get the WAN IP address which was source routed
            wanIndex = wanIndexTup[0]
            wanIP = wanIndexTup[1]
            wanExternalIP = wanIndexTup[2]

            # Add networking route which does not handle re-routing if WAN is down
            netsettings['natRules']['list']=[]
            netsettings['natRules']['list'].append(build_nat_rules("DST_ADDR",ip_address_testdestination,wanIP))
            uvmContext.networkManager().setNetworkSettings(netsettings)

            # Test that only the routed interface is used 5 times
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = ""
                timeout = 60
                while (timeout > 0 and result == ""):
                    time.sleep(1)
                    timeout -= 1
                    result = global_functions.get_public_ip_address()
                print("NAT 1:1 IP %s External IP %s and retrieved IP %s" % (wanIP, wanExternalIP, result))
                assert (result == wanExternalIP)
            # now down the selected wan and see if traffic flows out the other wan
            build_wan_test_rule(wanIndex, "ping", "192.168.244.1")
            # Wait for targeted the WAN to be off line before testing that the WAN is off line.
            timeout = 60
            online = True
            offlineWanIndex = wanIndex
            while online and timeout > 0:
                timeout -= 1
                time.sleep(1)
                wanStatus = app_wan_failover.getWanStatus()
                for statusInterface in wanStatus['list']:
                    if statusInterface['interfaceId'] == offlineWanIndex:
                        online =  statusInterface['online']
            subprocess.check_output("ip route flush cache", shell=True)
            for x in range(0, 5):
                result = global_functions.get_public_ip_address()
                print("WAN Down NAT 1:1 IP %s  External IP %s and retrieved IP %s" % (wanIP, wanExternalIP, result))
                assert (result == wanExternalIP)
            uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        nuke_wan_failover_rules()
        # Check to see if the faceplate counters have incremented. 
        post_count = global_functions.get_app_metric_value(app_wan_failover,"changed")
        assert(pre_count < post_count)

    @staticmethod
    def final_tear_down(self):
        global app, app_wan_failover
        # Restore original settings to return to initial settings
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        if app_wan_failover != None:
            uvmContext.appManager().destroy( app_wan_failover.getAppSettings()["id"] )
            app_wan_failover = None
        if orig_netsettings != None:
            uvmContext.networkManager().setNetworkSettings(orig_netsettings)


            

test_registry.register_module("wan-balancer", WanBalancerTests)
