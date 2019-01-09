"""wan_failover tests"""
import time

import unittest
import runtests
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions
import tests.ipaddr as ipaddr
from uvm import Uvm

app = None
orig_netsettings = None
indexOfWans = []
default_policy_id = 1

def all_wans_online():
    online_count = 0
    wanStatus = app.getWanStatus()
    wanCount = len(wanStatus['list'])
    for statusInterface in wanStatus['list']:
        if statusInterface['online']:
            online_count = online_count + 1
    return ( online_count == wanCount )

def online_wan_count():
    online_count = 0
    wanStatus = app.getWanStatus()
    for statusInterface in wanStatus['list']:
        if statusInterface['online']:
            online_count = online_count + 1
    return online_count

def offline_wan_count():
    offline_count = 0
    wanStatus = app.getWanStatus()
    for statusInterface in wanStatus['list']:
        if not statusInterface['online']:
            offline_count = offline_count + 1
    return offline_count

def wait_for_wan_offline(maxWait=120,increment=1):
    offline_count = offline_wan_count()
    original_offline_count = offline_count
    print("original : offline_count: %s"%(str(original_offline_count)))
    while maxWait > 0:
        time.sleep(increment)
        offline_count = offline_wan_count()
        maxWait = maxWait - increment
        print("current  : offline_count: %s"%(str(offline_count)))
        if (offline_count > original_offline_count):
            break
    print("final    : offline_count: %s"%(str(offline_count)))

def set_interface_field( interfaceId, netsettings, fieldName, value ):
    for interface in netsettings['interfaces']['list']:
        if interface.get('interfaceId') == interfaceId:
            interface[fieldName] = value

def build_wan_test(matchInterface, testType="ping", pingHost="8.8.8.8", httpURL="http://192.168.244.1/", testInterval=5, testTimeout=2):
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
    appData = app.getSettings()
    appData["tests"]["list"].append(rule)
    app.setSettings(appData)

def nuke_rules():
    appData = app.getSettings()
    appData["tests"]["list"] = []
    app.setSettings(appData)

class WanFailoverTests(unittest.TestCase):
    
    @staticmethod
    def module_name():
        return "wan-failover"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def initial_setup(self):
        global indexOfWans, appData, app, orig_netsettings
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        if (uvmContext.appManager().isInstantiated(self.module_name())):
            raise Exception('app %s already instantiated' % self.module_name())
        app = uvmContext.appManager().instantiate(self.module_name(), default_policy_id)
        app.start()
        appData = app.getSettings()
        indexOfWans = global_functions.get_wan_tuples()

    def setUp(self):
        print() # verify client is online
        
    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)
    
    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_020_ping_test_wan_online(self):
        nuke_rules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            build_wan_test(wanIndex)

        time.sleep(30)

        assert ( all_wans_online() )
        result = remote_control.is_online()
        assert (result == 0)
        
    def test_025_ping_test_wan_offline(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_025_addPingFailTestForWans")
        nuke_rules()
        orig_offline_count = offline_wan_count()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            build_wan_test(wanIndex, "ping", pingHost="192.168.244.1")

        wait_for_wan_offline()

        offline_count = offline_wan_count()
        assert (offline_count > orig_offline_count)
        
        result = remote_control.is_online()
        assert (result == 0)

        events = global_functions.get_events('WAN Failover','Outage Events',None,1)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 2, "action", "DISCONNECTED" )
        assert( found )
        
    def test_030_arp_test_wan_online(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        nuke_rules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            print("Testing interface : " + str(wanIndex))
            build_wan_test(wanIndex, "arp")

        time.sleep(30)

        assert ( all_wans_online() )
        result = remote_control.is_online()
        assert (result == 0)

    def test_035_arp_test_wan_offline(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_035_addArpFailTestForWans")
        nuke_rules()
        orig_offline_count = offline_wan_count()
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Add a fake gateway for each of the interfaces
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            # set gateway to fake gateway
            set_interface_field( wanIndex, netsettings, 'v4StaticGateway', '192.168.244.' + str(wanIndex))
            set_interface_field( wanIndex, netsettings, 'v4AutoGatewayOverride', '192.168.244.' + str(wanIndex))
            build_wan_test(wanIndex, "arp")

        uvmContext.networkManager().setNetworkSettings(netsettings)
            
        wait_for_wan_offline()

        offline_count = offline_wan_count()

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (offline_count > orig_offline_count)
        result = remote_control.is_online()
        assert (result == 0)

    def test_040_dns_test_wan_online(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        nuke_rules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            build_wan_test(wanIndex, "dns")

        time.sleep(30)

        assert( all_wans_online() )
        result = remote_control.is_online()
        assert (result == 0)        

    def test_045_dns_test_wan_offline(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_045_addDNSFailTestForWans")

        nuke_rules()
        orig_offline_count = offline_wan_count()
        netsettings = uvmContext.networkManager().getNetworkSettings()
        # Add a fake DNS for each of the interfaces
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            # set DNS values to fake DNS 
            set_interface_field( wanIndex, netsettings, 'v4StaticDns1', '192.168.244.' + str(wanIndex))
            set_interface_field( wanIndex, netsettings, 'v4StaticDns2', '192.168.244.' + str(wanIndex))
            set_interface_field( wanIndex, netsettings, 'v4AutoDns1Override', '192.168.244.' + str(wanIndex))
            set_interface_field( wanIndex, netsettings, 'v4AutoDns2Override', '192.168.244.' + str(wanIndex))

        uvmContext.networkManager().setNetworkSettings(netsettings)

        for wanIndexTup in indexOfWans:
            build_wan_test(wanIndex, "dns")

        wait_for_wan_offline()
        offline_count = offline_wan_count()

        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        assert (offline_count > orig_offline_count)

    def test_050_http_test_wan_online(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        nuke_rules()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            build_wan_test(wanIndex, "http", httpURL="http://test.untangle.com/")

        time.sleep(30)

        wansOnline = all_wans_online()

        assert (wansOnline)                        
        result = remote_control.is_online()
        assert (result == 0)        

    def test_055_http_test_wan_offline(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_055_addHTTPFailTestForWans")
        nuke_rules()
        orig_offline_count = offline_wan_count()
        for wanIndexTup in indexOfWans:
            wanIndex = wanIndexTup[0]
            build_wan_test(wanIndex, "http", httpURL="http://192.168.244.1/")

        wait_for_wan_offline()
        offline_count = offline_wan_count()

        assert (offline_count > orig_offline_count)                        
        result = remote_control.is_online()
        assert (result == 0)        

    def test_060_one_wan_offline(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        invalidWanIP = None
        offlineWanIndex = None
        for upWanIndexTup in indexOfWans:
            upWanIndex = upWanIndexTup[0]
            nuke_rules()
            for wanIndexTup in indexOfWans:
                wanIndex = wanIndexTup[0]
                if upWanIndex == wanIndex:
                    # make this interface test disconnected
                    offlineWanIndex = wanIndex
                    build_wan_test(offlineWanIndex, "ping", pingHost="192.168.244.1")
                    invalidWanIP = wanIndexTup[2]
                    print("InvalidIP is %s" % invalidWanIP)
                else:
                    build_wan_test(wanIndex, "ping")

            wait_for_wan_offline()

            offline_count = offline_wan_count()
            assert( offline_count > 0 )

            if (len(indexOfWans) > 1):
                # Skip the WAN IP address check part of the test if test box only has one WAN
                for x in range(0, 8):
                    result = global_functions.get_public_ip_address()
                    print("IP address %s and invalidWanIP %s" % (result,invalidWanIP))
                    assert (result != invalidWanIP)    

    def test_065_all_wan_offline_but_one(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        if (len(indexOfWans) < 2):
            raise unittest.SkipTest("Need at least two WANS for test_065_downAllButOneWan")
        pre_count = global_functions.get_app_metric_value(app,"changed")

        validWanIP = None
        # loop through the WANs keeping one up and the rest down.
        for upWanIndexTup in indexOfWans:
            upWanIndex = upWanIndexTup[0]
            nuke_rules()
            for wanIndexTup in indexOfWans:
                wanIndex = wanIndexTup[0]
                if upWanIndex != wanIndex:
                    # make this interface test disconnected
                    build_wan_test(wanIndex, "ping", pingHost="192.168.244.1")
                else:
                    validWanIP = wanIndexTup[2]
                    build_wan_test(upWanIndex, "ping", pingHost="8.8.8.8")
                    print("validIP is %s" % validWanIP)

            wait_for_wan_offline()

            online_count = online_wan_count()
            offline_count = offline_wan_count()

            assert( online_count == 1 )
            assert( offline_count > 0 )

            for x in range(0, 8):
                result = global_functions.get_public_ip_address()
                print("IP address %s and validWanIP %s" % (result,validWanIP))
                assert (result == validWanIP)    

        # Check to see if the faceplate counters have incremented. 
        post_count = global_functions.get_app_metric_value(app,"changed")
        assert(pre_count < post_count)

    @staticmethod
    def final_tear_down(self):
        global app
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None


test_registry.register_module("wan-failover", WanFailoverTests)
