import unittest2
import time
import sys
import pdb
import os
import re
import subprocess
import copy
import socket

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Manager
from uvm import Uvm
import remote_control
import global_functions
import test_registry
import global_functions

defaultRackId = 1
app = None
appWF = None
limitedAcceptanceRatio = .3 # 30% - limited severly is 10% by default, anything under 30% will be accepted as successfull
origNetworkSettings = None
origNetworkSettingsWithQoS = None
origNetworkSettingsWithoutQoS = None
wanLimitKbit = None
wanLimitMbit = None
preDownSpeedKbsec = None

def createBandwidthSingleConditionRule( conditionType, value, actionType="SET_PRIORITY", priorityValue=3 ):
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

def createBandwidthPenaltyRule( conditionType, value, actionType="PENALTY_BOX_CLIENT_HOST", tagName="", tagTime=1000 ):
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
            
def createBandwidthQuotaRule( conditionType, value, actionType, quotaValue=100 ):
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
    
def createQoSCustomRule( conditionType, value, priorityValue=3 ):
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
    
def createBypassConditionRule( conditionType, value):
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

def appendRule(newRule):
    rules = app.getRules()
    rules["list"].append(newRule)
    app.setRules(rules)

def nukeRules():
    rules = app.getRules()
    rules["list"] = []
    app.setRules(rules)

def printResults( wget_speed_pre, wget_speed_post, expected_speed, allowed_speed ):
        print "Pre Results   : %s KB/s" % str(wget_speed_pre)
        print "Post Results  : %s KB/s" % str(wget_speed_post)
        print "Expected Post : %s KB/s" % str(expected_speed)
        print "Allowed Post  : %s KB/s" % str(allowed_speed)
        print "Summary: %s < %s = %s" % (wget_speed_post, allowed_speed, str( wget_speed_post < allowed_speed ))

class BandwidthControlTests(unittest2.TestCase):

    @staticmethod
    def appName():
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

    @staticmethod
    def initialSetUp(self):
        global app, appWF, origNetworkSettings, origNetworkSettingsWithQoS, origNetworkSettingsWithoutQoS, preDownSpeedKbsec, wanLimitKbit, wanLimitMbit
        if (uvmContext.appManager().isInstantiated(self.appName())):
            raise Exception('app %s already instantiated' % self.appName())
        app = uvmContext.appManager().instantiate(self.appName(), defaultRackId)
        settings = app.getSettings()
        settings["configured"] = True
        app.setSettings(settings)        
        app.start()
        if (uvmContext.appManager().isInstantiated(self.appNameWF())):
            raise Exception('app %s already instantiated' % self.appNameWF())
        appWF = uvmContext.appManager().instantiate(self.appNameWF(), defaultRackId)
        if origNetworkSettings == None:
            origNetworkSettings = uvmContext.networkManager().getNetworkSettings()

        # disable QoS
        netsettings = copy.deepcopy( origNetworkSettings )
        netsettings['qosSettings']['qosEnabled'] = False
        uvmContext.networkManager().setNetworkSettings( netsettings )

        # measure speed
        preDownSpeedKbsec = global_functions.get_download_speed()

        # calculate QoS limits
        wanLimitKbit = int((preDownSpeedKbsec*8) * .9)
        # set max to 100Mbit, so that other limiting factors dont interfere
        if wanLimitKbit > 100000: wanLimitKbit = 100000 
        wanLimitMbit = round(wanLimitKbit/1024,2)
        # turn on QoS and set wan speed limits
        netsettings = copy.deepcopy( origNetworkSettings )
        netsettings['qosSettings']['qosEnabled'] = True
        i = 0
        for interface in netsettings['interfaces']['list']:
            if interface['isWan']:
                netsettings['interfaces']['list'][i]['downloadBandwidthKbps']=wanLimitKbit
                netsettings['interfaces']['list'][i]['uploadBandwidthKbps']=wanLimitKbit
            i += 1
        netsettings['bypassRules']['list'] = []
        netsettings['qosSettings']['qosRules']['list'] = []

        # These store the "new" defaults with and without QoS
        origNetworkSettingsWithQoS = copy.deepcopy( netsettings )
        origNetworkSettingsWithQoS['qosSettings']['qosEnabled'] = True
        origNetworkSettingsWithoutQoS = copy.deepcopy( netsettings )
        origNetworkSettingsWithoutQoS['qosSettings']['qosEnabled'] = False
        
        uvmContext.networkManager().setNetworkSettings(origNetworkSettingsWithQoS)
        
    def setUp(self):
        pass

    # verify client is online
    def test_010_clientIsOnline(self):
        result = remote_control.is_online()
        assert (result == 0)

    def test_011_qosLimit(self):
        global preDownSpeedKbsec, wanLimitKbit

        print "\nSetting WAN limit: %i Kbps" % (wanLimitKbit)

        postDownSpeedKbsec = global_functions.get_download_speed()

        # since the limit is 90% of first measure, check that second measure is less than first measure
        assert (preDownSpeedKbsec >  postDownSpeedKbsec)

    def test_013_qosBypassCustomRules(self):
        global app
        nukeRules()
        priority_level = 7
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()

        # Create SRC_ADDR based custom Q0S rule to limit bypass QoS
        netsettings = copy.deepcopy( origNetworkSettingsWithQoS )
        netsettings['qosSettings']['qosRules']["list"].append( createQoSCustomRule("SRC_ADDR",remote_control.clientIP,priority_level) )
        netsettings['bypassRules']['list'].append( createBypassConditionRule("SRC_ADDR",remote_control.clientIP) )
        uvmContext.networkManager().setNetworkSettings( netsettings )
        
        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()

        # Restore original network settings
        uvmContext.networkManager().setNetworkSettings( origNetworkSettingsWithQoS )
        
        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        assert ((wget_speed_pre) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

    def test_014_qosBypassCustomRulesUDP(self):
        global wanLimitMbit
        targetSpeedMbit = str(wanLimitMbit)+"M"
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        # We will use iperf server and iperf for this test.
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        iperfAvailable = global_functions.verify_iperf_configuration(wan_IP)
        if (not iperfAvailable):
            raise unittest2.SkipTest("Iperf server and/or iperf not available")

        netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings['bypassRules']['list'].append( createBypassConditionRule("DST_PORT","5000") )
        netsettings['qosSettings']['qosRules']["list"].append( createQoSCustomRule("DST_PORT","5000", 1) )
        uvmContext.networkManager().setNetworkSettings( netsettings )

        pre_UDP_speed = global_functions.get_udp_download_speed( receiverIP=global_functions.iperfServer, senderIP=remote_control.clientIP, targetRate=targetSpeedMbit )

        netsettings['qosSettings']['qosRules']['list'] = []
        netsettings['qosSettings']['qosRules']["list"].append( createQoSCustomRule("DST_PORT","5000", 7) )
        uvmContext.networkManager().setNetworkSettings( netsettings )

        post_UDP_speed = global_functions.get_udp_download_speed( receiverIP=global_functions.iperfServer, senderIP=remote_control.clientIP, targetRate=targetSpeedMbit )
        
        # Restore original network settings

        uvmContext.networkManager().setNetworkSettings( origNetworkSettingsWithQoS )

        printResults( pre_UDP_speed, post_UDP_speed, (wanLimitKbit/8)*0.1, pre_UDP_speed*.9 )
        assert (post_UDP_speed < pre_UDP_speed*.9)

    def test_015_qosNoBypassCustomRules(self):
        global app
        nukeRules()
        priority_level = 7

        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()

        # Create SRC_ADDR based custom Q0S rule to limit bypass QoS
        netsettings = copy.deepcopy( origNetworkSettingsWithQoS )
        netsettings['qosSettings']['qosRules']["list"].append(createQoSCustomRule("SRC_ADDR",remote_control.clientIP,priority_level))
        uvmContext.networkManager().setNetworkSettings( netsettings )
        
        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()

        # Restore original network settings
        uvmContext.networkManager().setNetworkSettings( origNetworkSettingsWithQoS )
        
        # Because the session is NOT bypassed, the QoS rule should not take effect
        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        assert ((wget_speed_pre) and (wget_speed_post))
        assert (not (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post))

    def test_024_srcAddrRule(self):
        global app
        nukeRules()
        priority_level = 7
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()

        # Create SRC_ADDR based rule to limit bandwidth
        appendRule(createBandwidthSingleConditionRule("SRC_ADDR",remote_control.clientIP,"SET_PRIORITY",priority_level))

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()

        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "bandwidth_control_priority", priority_level,
                                            "c_client_addr", remote_control.clientIP)
        assert( found )

    def test_035_dstAddrRule(self):
        global app
        nukeRules()
        priority_level = 7

        # Get the IP address of test.untangle.com.  We could hardcoded this IP.
        test_untangle_IP = socket.gethostbyname("test.untangle.com")
        
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create DST_ADDR based rule to limit bandwidth
        appendRule(createBandwidthSingleConditionRule("DST_ADDR",test_untangle_IP,"SET_PRIORITY",priority_level))

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()

        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "bandwidth_control_priority", priority_level,
                                            "c_client_addr", remote_control.clientIP)
        assert( found )

    def test_045_dstPortRule(self):
        global app
        nukeRules()
        priority_level = 7

        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create DST_PORT based rule to limit bandwidth
        appendRule(createBandwidthSingleConditionRule("DST_PORT","80","SET_PRIORITY",priority_level))

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()
        
        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "bandwidth_control_priority", priority_level,
                                            "c_client_addr", remote_control.clientIP)
        assert( found )

    def test_046_dstPortRuleUDP(self):
        global app, appWF, wanLimitMbit
        # only use 30% because QoS will limit to 10% and we want to make sure it takes effect
        # really high levels will actually be limited by the untangle-vm throughput instead of QoS
        # which can interfere with the test
        targetSpeedMbit = str(wanLimitMbit*.3)+"M"
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        # We will use iperf server and iperf for this test.
        wan_IP = uvmContext.networkManager().getFirstWanAddress()
        iperfAvailable = global_functions.verify_iperf_configuration(wan_IP)
        if (not iperfAvailable):
            raise unittest2.SkipTest("Iperf server and/or iperf not available, skipping alternate port forwarding test")
        # Enabled QoS
        netsettings = uvmContext.networkManager().getNetworkSettings()
        nukeRules()

        appendRule(createBandwidthSingleConditionRule("DST_PORT","5000","SET_PRIORITY",1))
            
        pre_UDP_speed = global_functions.get_udp_download_speed( receiverIP=global_functions.iperfServer, senderIP=remote_control.clientIP, targetRate=targetSpeedMbit )

        # Create DST_PORT based rule to limit bandwidth
        nukeRules()
        appendRule(createBandwidthSingleConditionRule("DST_PORT","5000","SET_PRIORITY",7))

        post_UDP_speed = global_functions.get_udp_download_speed( receiverIP=global_functions.iperfServer, senderIP=remote_control.clientIP, targetRate=targetSpeedMbit )

        printResults( pre_UDP_speed, post_UDP_speed, (wanLimitKbit/8)*0.1, pre_UDP_speed*.9 )
        assert (post_UDP_speed < pre_UDP_speed*.9)

    def test_047_hostnameRule(self):
        global app
        nukeRules()
        priority_level = 7
        # This test might need web filter for http to start
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create HTTP_HOST based rule to limit bandwidth
        appendRule(createBandwidthSingleConditionRule("HTTP_HOST","test.untangle.com","SET_PRIORITY",priority_level))

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()
        
        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "bandwidth_control_priority", priority_level,
                                            "c_client_addr", remote_control.clientIP)
        assert( found )

    def test_048_contentLengthAddrRule(self):
        global app
        nukeRules()
        priority_level = 7

        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create DST_ADDR based rule to limit bandwidth
        appendRule(createBandwidthSingleConditionRule("HTTP_CONTENT_LENGTH",">3000000","SET_PRIORITY",priority_level))

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()

        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "bandwidth_control_priority", priority_level,
                                            "c_client_addr", remote_control.clientIP)
        assert( found )

    def test_050_webFilterFlaggedRule(self):
        global app, appWF
        nukeRules()
        pre_count = global_functions.get_app_metric_value(app,"prioritize")

        priority_level = 7
        # This test might need web filter for http to start
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create WEB_FILTER_FLAGGED based rule to limit bandwidth
        appendRule(createBandwidthSingleConditionRule("WEB_FILTER_FLAGGED","true","SET_PRIORITY",priority_level))

        # Test.untangle.com is listed as Software, Hardware in web filter. As of 1/2014 its in Technology 
        settingsWF = appWF.getSettings()
        i = 0
        untangleCats = ["Software,", "Technology"]
        for webCategories in settingsWF['categories']['list']:
            if any(x in webCategories['name'] for x in untangleCats):
                settingsWF['categories']['list'][i]['flagged'] = "true"
            i += 1
        appWF.setSettings(settingsWF)

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()
        
        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                            "bandwidth_control_priority", priority_level,
                                            "c_client_addr", remote_control.clientIP)
        assert( found )

        # Check to see if the faceplate counters have incremented. 
        post_count = global_functions.get_app_metric_value(app,"prioritize")
        assert(pre_count < post_count)
 
    def test_060_hostquota(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        global app
        nukeRules()
        priority_level = 7 # Severely Limited 
        given_quota = 10000 # 10k 

        # Remove any existing quota
        uvmContext.hostTable().removeQuota(remote_control.clientIP)
        
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create rule to give quota
        appendRule(createBandwidthQuotaRule("HOST_HAS_NO_QUOTA","true","GIVE_HOST_QUOTA",given_quota))
        # Create penalty for exceeding quota
        appendRule(createBandwidthSingleConditionRule("HOST_QUOTA_EXCEEDED","true","SET_PRIORITY",priority_level))

        # Download the file so quota is exceeded
        global_functions.get_download_speed()

        # quota accounting occurs every 60 seconds, so we must wait at least 60 seconds
        time.sleep(60)

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()
        
        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        # Remove quota
        uvmContext.hostTable().removeQuota(remote_control.clientIP)

        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

        events = global_functions.get_events('Bandwidth Control','Quota Events',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "action", 1, #quota given
                                               "size", given_quota,
                                               "entity", remote_control.clientIP)
        assert(found)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "action", 2, #quota exceeded
                                               "entity", remote_control.clientIP)
        assert(found)

        events = global_functions.get_events('Bandwidth Control','Prioritized Sessions',None,5)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 5, 
                                               "bandwidth_control_priority", priority_level,
                                               "c_client_addr", remote_control.clientIP)
        assert( found )

    def test_061_userquota(self):
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        global app
        nukeRules()
        priority_level = 7 # Severely Limited 
        given_quota = 10000 # 10k 

        # Set this host's username
        username = remote_control.run_command("hostname -s", stdout=True)
        entry = uvmContext.hostTable().getHostTableEntry( remote_control.clientIP )
        entry['usernameDirectoryConnector'] = username
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        
        # Remove any existing quota
        uvmContext.userTable().removeQuota(username)
        
        # Record average speed without bandwidth control configured
        wget_speed_pre = global_functions.get_download_speed()
        
        # Create rule to give quota
        appendRule(createBandwidthQuotaRule("USER_HAS_NO_QUOTA","true","GIVE_USER_QUOTA",given_quota))

        # Create penalty for exceeding quota
        appendRule(createBandwidthSingleConditionRule("USER_QUOTA_EXCEEDED","true","SET_PRIORITY",priority_level))

        # Download the file so quota is exceeded
        global_functions.get_download_speed()

        # quota accounting occurs every 60 seconds, so we must wait at least 60 seconds
        time.sleep(60)

        # Download file and record the average speed in which the file was download
        wget_speed_post = global_functions.get_download_speed()
        
        printResults( wget_speed_pre, wget_speed_post, wget_speed_pre*0.1, wget_speed_pre*limitedAcceptanceRatio )

        # Remove quota
        uvmContext.userTable().removeQuota(username)

        # Blank username
        entry['usernameDirectoryConnector'] = None
        uvmContext.hostTable().setHostTableEntry( remote_control.clientIP, entry )
        
        assert ((wget_speed_post) and (wget_speed_post))
        assert (wget_speed_pre * limitedAcceptanceRatio >  wget_speed_post)

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
                                               "c_client_addr", remote_control.clientIP)
        assert( found )
        
    def test_070_penaltyRule(self):
        global app
        nukeRules()
        tag_time = 2000000

        # remove tags
        entry = uvmContext.hostTable().getHostTableEntry(remote_control.clientIP)
        entry['tags']['list'] = []
        entry = uvmContext.hostTable().setHostTableEntry(remote_control.clientIP, entry)
        
        # Create penalty rule
        appendRule(createBandwidthPenaltyRule("SRC_ADDR",remote_control.clientIP,"TAG_HOST","penalty-box",tag_time))
        
        # go to test.untangle.com 
        result = remote_control.is_online()

        # Look for tag
        entry = uvmContext.hostTable().getHostTableEntry(remote_control.clientIP)
        print entry['tags']['list']
        found = False
        for tag in entry['tags']['list']:
            if tag['name'] == 'penalty-box':
                found = True

        assert(found)
                
        # remove tags
        entry['tags']['list'] = []
        entry = uvmContext.hostTable().setHostTableEntry(remote_control.clientIP, entry)
        
        # check penalty box
        events = global_functions.get_events('Hosts','Hosts Events', None, 50)
        assert(events != None)
        event = global_functions.find_event( events.get('list'), 50,
                                             "address", remote_control.clientIP,
                                             "key", "tags" )
        print(event) 
        
        assert((event != None))

    @staticmethod
    def finalTearDown(self):
        global app, appWF, origNetworkSettings
        # Restore original settings to return to initial settings
        if origNetworkSettings != None:
            uvmContext.networkManager().setNetworkSettings( origNetworkSettings )
        if app != None:
            uvmContext.appManager().destroy( app.getAppSettings()["id"] )
            app = None
        if appWF != None:
            uvmContext.appManager().destroy( appWF.getAppSettings()["id"] )
            appWF = None


test_registry.registerApp("bandwidth-control", BandwidthControlTests)

