"""bandwidth_control tests"""
import time
import copy
import unittest
import pytest
import runtests

from tests.common import NGFWTestCase
from tests.global_functions import uvmContext
import runtests.remote_control as remote_control
import runtests.test_registry as test_registry
import tests.global_functions as global_functions

app = None
app_web_filter = None
limited_acceptance_ratio = .3 # 30% - limited severly is 10% by default, anything under 30% will be accepted as successfull
orig_network_settings = None
orig_network_settings_with_qos = None
orig_network_settings_without_qos = None
wan_limit_kbit = None
wan_limit_mbit = None
pre_down_speed_kbit = None


def create_single_condition_rule( conditionType, value, actionType="SET_PRIORITY", priorityValue=3 ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "action": {
            "actionType": actionType,
            "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRuleAction",
            "priority": priorityValue
            },
        "description": "test bandwidth rule",
        "ruleId": 1,
        "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRule",
        "enabled": True,
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRuleCondition",
                    "conditionType": conditionTypeStr,
                    "value": valueStr
                }
            ]                
        }
    }


def create_penalty_rule( conditionType, value, actionType="TAG_HOST", tagName="", tagTime=1000 ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "action": {
            "actionType": actionType,
            "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRuleAction",
            "tagName" : tagName,
            "tagTime": tagTime
        },
        "description": "penalty",
        "ruleId": 1,
        "enabled": True,
        "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRuleCondition",
                    "conditionType": conditionTypeStr,
                    "value": value
                }
            ]
        }
    }

            
def create_quota_rule( conditionType, value, actionType, quotaValue=100 ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "action": {
            "actionType": actionType,
            "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRuleAction",
            "quotaBytes": quotaValue,
            "quotaTime": -3
        },
        "description": "quota",
        "ruleId": 2,
        "enabled": True,
        "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.app.bandwidth_control.BandwidthControlRuleCondition",
                    "conditionType": conditionTypeStr,
                    "value": valueStr
                }
            ]
        },
    }

    
def create_qos_custom_rule( conditionType, value, priorityValue=3 ):
    conditionTypeStr = str(conditionType)
    valueStr = str(value)
    return {
        "description": "bypass " + conditionTypeStr + " ATS",
        "enabled": True,
        "javaClass": "com.untangle.uvm.network.QosRule",
        "conditions": {
            "javaClass": "java.util.LinkedList",
            "list": [
                {
                    "invert": False,
                    "javaClass": "com.untangle.uvm.network.QosRuleCondition",
                    "conditionType": conditionTypeStr,
                    "value": valueStr
                },
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.QosRuleCondition", 
                    "conditionType": "PROTOCOL", 
                    "value": "TCP,UDP"
                }
                
            ]                
        },
        "priority": priorityValue,
        "ruleId": 5
    }

    
def create_bypass_condition_rule( conditionType, value):
    return {
        "bypass": True, 
        "description": "test bypass " + str(conditionType) + " " + str(value), 
        "enabled": True, 
        "javaClass": "com.untangle.uvm.network.BypassRule", 
        "conditions": {
            "javaClass": "java.util.LinkedList", 
            "list": [
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.BypassRuleCondition", 
                    "conditionType": str(conditionType), 
                    "value": str(value)
                }, 
                {
                    "invert": False, 
                    "javaClass": "com.untangle.uvm.network.BypassRuleCondition", 
                    "conditionType": "PROTOCOL", 
                    "value": "TCP,UDP"
                }
            ]
        }, 
        "ruleId": 1
    } 


def append_rule(app, newRule):
    rules = app.getRules()
    rules["list"].append(newRule)
    app.setRules(rules)


def nuke_rules(app):
    rules = app.getRules()
    rules["list"] = []
    app.setRules(rules)


def print_results( wget_speed_pre, wget_speed_post, expected_speed, allowed_speed ):
        print("Pre Results   : %s KB/s" % str(wget_speed_pre))
        print("Post Results  : %s KB/s" % str(wget_speed_post))
        print("Expected Post : %s KB/s" % str(expected_speed))
        print("Allowed Post  : %s KB/s" % str(allowed_speed))
        print("Summary: %s < %s = %s" % (wget_speed_post, allowed_speed, str( wget_speed_post < allowed_speed )))


@pytest.mark.bandwidth_control
class BandwidthControlTests(NGFWTestCase):

    @staticmethod
    def module_name():
        global app
        app = BandwidthControlTests._app
        return "bandwidth-control"

    @staticmethod
    def appNameWF():
        return "web-filter"

    @staticmethod
    def vendorName():
        return "Untangle"

    @staticmethod
    def displayName():
        return "Bandwidth Control"

    @classmethod
    def initial_extra_setup(cls):
        global orig_network_settings, orig_network_settings_with_qos, orig_network_settings_without_qos, pre_down_speed_kbit, wan_limit_kbit, wan_limit_mbit

        settings = cls._app.getSettings()
        settings["configured"] = True
        cls._app.setSettings(settings)        
        cls._app.start()

        cls._app_web_filter = uvmContext.appManager().instantiate(cls.appNameWF(), 1)

        if orig_network_settings == None:
            orig_network_settings = uvmContext.networkManager().getNetworkSettings()

        # disable QoS
        netsettings = copy.deepcopy( orig_network_settings )
        netsettings['qosSettings']['qosEnabled'] = False
        uvmContext.networkManager().setNetworkSettings( netsettings )

        # measure speed
        pre_down_speed_kbit = global_functions.get_download_speed(download_server="test.untangle.com")

        # calculate QoS limits
        wan_limit_kbit = int((pre_down_speed_kbit*8) * .9)
        # set max to 100Mbit, so that other limiting factors dont interfere
        if wan_limit_kbit > 100000: wan_limit_kbit = 100000 
        wan_limit_mbit = round(wan_limit_kbit/1024,2)
        # turn on QoS and set wan speed limits
        netsettings = copy.deepcopy( orig_network_settings )
        netsettings['qosSettings']['qosEnabled'] = True
        i = 0
        for interface in netsettings['interfaces']['list']:
            if interface['isWan']:
                netsettings['interfaces']['list'][i]['downloadBandwidthKbps']=wan_limit_kbit
                netsettings['interfaces']['list'][i]['uploadBandwidthKbps']=wan_limit_kbit
            i += 1
        netsettings['bypassRules']['list'] = []
        netsettings['qosSettings']['qosRules']['list'] = []

        # These store the "new" defaults with and without QoS
        orig_network_settings_with_qos = copy.deepcopy( netsettings )
        orig_network_settings_with_qos['qosSettings']['qosEnabled'] = True
        orig_network_settings_without_qos = copy.deepcopy( netsettings )
        orig_network_settings_without_qos['qosSettings']['qosEnabled'] = False
        
        uvmContext.networkManager().setNetworkSettings(orig_network_settings_with_qos)
        
    # verify client is online
    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_license_valid(self):
        assert(uvmContext.licenseManager().isLicenseValid(self.module_name()))

    def test_012_qos_limit(self):
        global pre_down_speed_kbit, wan_limit_kbit

        print("\nSetting WAN limit: %i Kbps" % (wan_limit_kbit))

        post_down_speed_kbit = global_functions.get_download_speed()

        # since the limit is 90% of first measure, check that second measure is less than first measure
        assert (pre_down_speed_kbit >  post_down_speed_kbit)

    def test_013_qos_bypass_custom_rules_tcp(self):
        nuke_rules(self._app)
        priority_level = 7
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()

        # Create SRC_ADDR based custom Q0S rule to limit bypass QoS
        netsettings = copy.deepcopy( orig_network_settings_with_qos )
        netsettings['qosSettings']['qosRules']["list"].append( create_qos_custom_rule("SRC_ADDR",remote_control.client_ip,priority_level) )
        netsettings['bypassRules']['list'].append( create_bypass_condition_rule("SRC_ADDR",remote_control.client_ip) )
        uvmContext.networkManager().setNetworkSettings( netsettings )
        
        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()

        # Restore original network settings
        uvmContext.networkManager().setNetworkSettings( orig_network_settings_with_qos )
        
        print_results( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limited_acceptance_ratio )

        assert ((wget_speed_pre) and (wget_speed_post))
        assert (wget_speed_pre * limited_acceptance_ratio >  wget_speed_post)

    def test_014_qos_bypass_custom_rules_udp(self):
        global wan_limit_mbit
        targetSpeedMbit = str(wan_limit_mbit)+"M"
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        # We will use iperf server and iperf for this test.
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        iperfAvailable = global_functions.verify_iperf_configuration(wan_IP)
        if (not iperfAvailable):
            raise unittest.SkipTest("Iperf server and/or iperf not available")

        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['bypassRules']['list'].append( create_bypass_condition_rule("DST_PORT","5000") )
        netsettings['qosSettings']['qosRules']["list"].append( create_qos_custom_rule("DST_PORT","5000", 1) )
        uvmContext.networkManager().setNetworkSettings( netsettings )

        pre_UDP_speed = global_functions.get_udp_download_speed( receiverip=global_functions.iperf_server, senderip=remote_control.client_ip, targetRate=targetSpeedMbit )

        netsettings['qosSettings']['qosRules']['list'] = []
        netsettings['qosSettings']['qosRules']["list"].append( create_qos_custom_rule("DST_PORT","5000", 7) )
        uvmContext.networkManager().setNetworkSettings( netsettings )

        post_UDP_speed = global_functions.get_udp_download_speed( receiverip=global_functions.iperf_server, senderip=remote_control.client_ip, targetRate=targetSpeedMbit )
        
        # Restore original network settings

        uvmContext.networkManager().setNetworkSettings( orig_network_settings_with_qos )

        print_results( pre_UDP_speed, post_UDP_speed, (wan_limit_kbit/8)*0.1, pre_UDP_speed*.9 )
        assert (post_UDP_speed < pre_UDP_speed*.9)

    def test_015_qos_nobpass_custom_rules_tcp(self):
        nuke_rules(self._app)
        priority_level = 7

        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()

        # Create SRC_ADDR based custom Q0S rule to limit bypass QoS
        netsettings = copy.deepcopy( orig_network_settings_with_qos )
        netsettings['qosSettings']['qosRules']["list"].append(create_qos_custom_rule("SRC_ADDR",remote_control.client_ip,priority_level))
        uvmContext.networkManager().setNetworkSettings( netsettings )
        
        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()

        # Restore original network settings
        uvmContext.networkManager().setNetworkSettings( orig_network_settings_with_qos )
        
        # Because the session is NOT bypassed, the QoS rule should not take effect
        print_results( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limited_acceptance_ratio )

        assert ((wget_speed_pre) and (wget_speed_post))
        assert (not (wget_speed_pre * limited_acceptance_ratio >  wget_speed_post))

    def test_020_severely_limited_tcp(self):
        nuke_rules(self._app)
        priority_level = 7
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()

        # Create SRC_ADDR based rule to limit bandwidth
        append_rule(self._app, create_single_condition_rule("SRC_ADDR",remote_control.client_ip,"SET_PRIORITY",priority_level))

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()

        print_results( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limited_acceptance_ratio )

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limited_acceptance_ratio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "bandwidth_control_priority", priority_level,
                                            "c_client_addr", remote_control.client_ip)
        assert( found )


    def test_021_severely_limited_udp(self):
        global wan_limit_mbit
        # only use 30% because QoS will limit to 10% and we want to make sure it takes effect
        # really high levels will actually be limited by the untangle-vm throughput instead of QoS
        # which can interfere with the test
        targetSpeedMbit = str(wan_limit_mbit*.3)+"M"
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        # We will use iperf server and iperf for this test.
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        iperfAvailable = global_functions.verify_iperf_configuration(wan_IP)
        if (not iperfAvailable):
            raise unittest.SkipTest("Iperf server and/or iperf not available, skipping alternate port forwarding test")
        # Enabled QoS
        netsettings = uvmContext.networkManager().getNetworkSettings()
        nuke_rules(self._app)

        append_rule(self._app, create_single_condition_rule("DST_PORT","5000","SET_PRIORITY",1))
            
        pre_UDP_speed = global_functions.get_udp_download_speed( receiverip=global_functions.iperf_server, senderip=remote_control.client_ip, targetRate=targetSpeedMbit )

        # Create DST_PORT based rule to limit bandwidth
        nuke_rules(self._app)
        append_rule(self._app, create_single_condition_rule("DST_PORT","5000","SET_PRIORITY",7))

        post_UDP_speed = global_functions.get_udp_download_speed( receiverip=global_functions.iperf_server, senderip=remote_control.client_ip, targetRate=targetSpeedMbit )

        print_results( pre_UDP_speed, post_UDP_speed, (wan_limit_kbit/8)*0.1, pre_UDP_speed*.9 )
        assert (post_UDP_speed < pre_UDP_speed*.9)

    def test_050_severely_limited_web_filter_flagged(self):
        nuke_rules(self._app)
        pre_count = global_functions.get_app_metric_value(self._app,"prioritize")

        priority_level = 7
        # This test might need web filter for http to start
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed(download_server="test.untangle.com")
        
        # Create WEB_FILTER_FLAGGED based rule to limit bandwidth
        append_rule(self._app, create_single_condition_rule("WEB_FILTER_FLAGGED","true","SET_PRIORITY",priority_level))

        # Test.untangle.com is listed as Software, Hardware in web filter. As of 1/2014 its in Technology 
        settingsWF = self._app_web_filter.getSettings()
        i = 0
        untangleCats = ["Computer,", "Security"]
        for webCategories in settingsWF['categories']['list']:
            if any(x in webCategories['name'] for x in untangleCats):
                settingsWF['categories']['list'][i]['flagged'] = "true"
            i += 1
        self._app_web_filter.setSettings(settingsWF)

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed(download_server="test.untangle.com")
        
        print_results( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limited_acceptance_ratio )

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limited_acceptance_ratio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "bandwidth_control_priority", priority_level,
                                            "c_client_addr", remote_control.client_ip)
        assert( found )

        # Check to see if the faceplate counters have incremented. 
        post_count = global_functions.get_app_metric_value(app,"prioritize")
        assert(pre_count < post_count)
 
    def test_060_host_quota(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')

        nuke_rules(self._app)
        priority_level = 7 # Severely Limited 
        given_quota = 10000 # 10k 

        # Remove any existing quota
        uvmContext.hostTable().removeQuota(remote_control.client_ip)
        
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create rule to give quota
        append_rule(self._app, create_quota_rule("HOST_HAS_NO_QUOTA","true","GIVE_HOST_QUOTA",given_quota))
        # Create penalty for exceeding quota
        append_rule(self._app, create_single_condition_rule("HOST_QUOTA_EXCEEDED","true","SET_PRIORITY",priority_level))

        # Download the file so quota is exceeded
        global_functions.get_download_speed(meg=1)

        # quota accounting occurs every 60 seconds, so we must wait at least 60 seconds
        time.sleep(60)

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()
        
        print_results( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limited_acceptance_ratio )

        # Remove quota
        uvmContext.hostTable().removeQuota(remote_control.client_ip)

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limited_acceptance_ratio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Quota Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "action", 1, #quota given
                                               "size", given_quota,
                                               "entity", remote_control.client_ip)
        assert(found)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "action", 2, #quota exceeded
                                               "entity", remote_control.client_ip)
        assert(found)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "bandwidth_control_priority", priority_level,
                                               "c_client_addr", remote_control.client_ip)
        assert( found )

    def test_061_user_quota(self):
        if runtests.quick_tests_only:
            raise unittest.SkipTest('Skipping a time consuming test')
        global app
        nuke_rules(self._app)
        priority_level = 7 # Severely Limited 
        given_quota = 10000 # 10k 

        # Set this host's username
        username = remote_control.run_command("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.client_ip )
        entry['usernameDirectoryConnector'] = username
        uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )
        
        # Remove any existing quota
        uvmContext.userTable().removeQuota(username)
        
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create rule to give quota
        append_rule(self._app, create_quota_rule("USER_HAS_NO_QUOTA","true","GIVE_USER_QUOTA",given_quota))

        # Create penalty for exceeding quota
        append_rule(self._app, create_single_condition_rule("USER_QUOTA_EXCEEDED","true","SET_PRIORITY",priority_level))

        # Download the file so quota is exceeded
        global_functions.get_download_speed(meg=1)

        # quota accounting occurs every 60 seconds, so we must wait at least 60 seconds
        time.sleep(60)

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()
        
        print_results( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limited_acceptance_ratio )

        # Remove quota
        uvmContext.userTable().removeQuota(username)

        # Blank username
        entry['usernameDirectoryConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.client_ip, entry )
        
        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limited_acceptance_ratio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Quota Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "action", 1, #quota given
                                               "size", given_quota,
                                               "entity", username)
        assert(found)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "action", 2, #quota exceeded
                                               "entity", username)
        assert(found)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "bandwidth_control_priority", priority_level,
                                               "c_client_addr", remote_control.client_ip)
        assert( found )
        
    def test_070_penalty_rule(self):
        global app
        nuke_rules(self._app)
        tag_time = 2000000

        # remove tags
        entry = uvmContext.hostTable().getHostTableEntry(remote_control.client_ip)
        entry['tags']['list'] = []
        entry = uvmContext.hostTable().setHostTableEntry(remote_control.client_ip, entry)
        
        # Create penalty rule
        append_rule(self._app, create_penalty_rule("SRC_ADDR",remote_control.client_ip,"TAG_HOST","penalty-box",tag_time))
        
        # go to test.untangle.com 
        result = remote_control.is_online()

        # Look for tag
        entry = uvmContext.hostTable().getHostTableEntry(remote_control.client_ip)
        print(entry['tags']['list'])
        found = False
        for tag in entry['tags']['list']:
            if tag['name'] == 'penalty-box':
                found = True

        assert(found)
                
        # remove tags
        entry['tags']['list'] = []
        entry = uvmContext.hostTable().setHostTableEntry(remote_control.client_ip, entry)
        
        # check penalty box
        events = global_functions.get_events('Hosts','Hosts Events', None, 50)
        assert(events != None)
        event = global_functions.find_event( events.get('list'), 50,
                                             "address", remote_control.client_ip,
                                             "key", "tags" )
        print(event) 
        
        assert((event != None))

    @classmethod
    def final_extra_tear_down(cls):
        global orig_network_settings
        # Restore original settings to return to initial settings
        if orig_network_settings != None:
            uvmContext.networkManager().setNetworkSettings( orig_network_settings )
        if cls._app_web_filter != None:
            uvmContext.appManager().destroy( cls._app_web_filter.getAppSettings()["id"] )
            cls._app_web_filter = None


test_registry.register_module("bandwidth-control", BandwidthControlTests)

