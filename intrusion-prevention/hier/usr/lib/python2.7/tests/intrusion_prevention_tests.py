"""
Intrusion Prevention Test Suite
"""
import unittest2
import time
import sys

import json
import pycurl
import ssl
import urllib
import urllib2
import copy
import socket
from datetime import datetime
from StringIO import StringIO

from jsonrpc import JSONRPCException
from global_functions import uvmContext
from uvm import Uvm
import remote_control
import global_functions
import test_registry

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "", sys.version_info[0], sys.version_info[1] )
if ( "" != ''):
    sys.path.insert(0, UNTANGLE_DIR)

default_rack_id = 1
node = None

class IntrusionPreventionInterface:
    """
    Intrusion Prevention management object
    """
    config_url = "https://localhost/webui/download?"
    config_request_arguments_template = {
        "type": "IntrusionPreventionSettings",
        "arg1": "load",
        "arg2" : "0"
    }
    config_request_patch_template = {
        "configured":True,
        "interfaces":{
            "list":[]
        },
        "max_scan_size":1024,
        "profileId":"low_32",
        "profileVersion":1,
        "updated":{
            "rules":{
                "added":[],
                "deleted":[],
                "modified":[]
            }
        }
    }

    def __init__(self, node_id, timeout=120 ):
        self.node_id = node_id

    def config_request(self, action, patch = ""):
        """
        Send a configuration request
        """
        response = StringIO()

        request_arguments = IntrusionPreventionInterface.config_request_arguments_template
        request_arguments["arg1"] = action
        request_arguments["arg2"] = self.node_id

        patch = json.dumps(patch)

        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE

        method = "POST"
        handler = urllib2.HTTPSHandler(context=ctx)
        opener = urllib2.build_opener(handler)
        data = patch.encode('utf-8')
        request = urllib2.Request(IntrusionPreventionInterface.config_url + urllib.urlencode(request_arguments), data=data)
        request.add_header("Content-Type","text/plain; charset=utf-8")
        request.get_method = lambda: method
        try:
            connection = opener.open(request)
        except Exception, e:
            print e
            connection = e

        if connection.code == 200:
            data = connection.read()
            return data
        else:
            raise JSONRPCException( "Unable to get data.  Failed with code [%i]" % connection.code)

        return False

    def create_patch(self, type = None, action = None, extended = None ):
        """
        Create a patch
        """
        patch = IntrusionPreventionInterface.config_request_patch_template

        patch_action = ""
        if action == "add":
            patch_action = "added"
        if action == "modify":
            patch_action = "modified"

        if type == "rule":
            patch_rule = extended
            patch["rules"] = {
                "-1": {
                    "op": patch_action,
                    "recData": patch_rule,
                    "page":1
                }
            }
            patch["variables"] = {}
        return patch

    last_sid = 1999999
    def create_rule(self, sid = None, category="app-detect", classtype="attempted-admin", msg="Msg", log=True, block=False, 
        rule=None, action="alert", type="tcp", source_ip="any", source_port="any", dest_ip="any", dest_port="any", directive="", 
        originalId="", path="rules"):
        """
        Create a rule
        """
        if sid == None:
            sid = str(self.last_sid)
            self.last_sid = self.last_sid - 1

        if log == True:
            action = "alert"
        if block == True:
            action = "drop"

        if rule == None:
            rule = action + " " + type + " " + source_ip + " " + source_port + " -> " + dest_ip + " " + dest_port + " ( msg:\""+msg+"\"; classtype:"+classtype+"; sid:"+sid+"; " + directive + ")"

        rule = {
            "sid": sid,
            "category": category,
            "classtype":classtype,
            "msg": msg,
            "rule": rule,
            "log": log,
            "block": block,
            "originalId":originalId,
            "path": path
        }
        return rule

    def get_log_event(self, rule):
        """
        Get log event
        """
        global node

        time.sleep(35)
        events = global_functions.get_events('Intrusion Prevention','All Events',None,1)

        logged = False
        blocked = False
        for event in events["list"]:
            if event["msg"] == rule["msg"] and str(event["sig_id"]) == rule["sid"]:
                return event

        return None

    ## Change the timeout for receiving a response
    def set_timeout( self, timeout ):
        """
        Set CURL timeout
        """
        self.__curl.setopt( pycurl.TIMEOUT, timeout )

    def setup(self):
        """
        Perform initialization
        """
        settings = json.loads( self.config_request( "load" ) )
        self.set_last_enabled_rules_count(self.count_enabled_rules(settings))
        self.set_last_variables_count(self.count_variables(settings))
        del(settings)

    def count_enabled_rules(self, settings):
        """
        Count enabled rules
        """
        enabled_count = 0
        for rule in settings["rules"]["list"]:
            if rule["log"] == True or rule["block"] == True:
                enabled_count = enabled_count + 1
        return enabled_count

    def set_last_enabled_rules_count(self, enabled_count):
        """
        Set enabled rules
        """
        self.last_enabled_rules_count = enabled_count

    def get_last_enabled_rules_count(self):
        """
        Get enabled rules
        """
        return self.last_enabled_rules_count

    def count_variables(self, settings):
        """
        Count variables
        """
        count = 0
        for rule in settings["variables"]["list"]:
            count = count + 1
        return count

    def set_last_variables_count(self, count):
        """
        Set variable count
        """
        self.last_variables_count = count

    def get_last_variables_count(self):
        """
        Get variable count
        """
        return self.last_variables_count

def flush_events():
    """
    Clear Intrusion Prevention events
    """
    reports = uvmContext.nodeManager().node("untangle-node-reports")
    if (reports != None):
        reports.flushEvents()

def createBypassConditionRule( conditionType, value ):
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

class IntrusionPreventionTests(unittest2.TestCase):
    """
    Tests
    """
    @staticmethod
    def nodeName():
        """
        Get Node name
        """
        return "untangle-node-intrusion-prevention"

    @staticmethod
    def initialSetUp(self):
        global node
        if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
            raise Exception('node %s already instantiated' % self.nodeName())
        node = uvmContext.nodeManager().instantiate(self.nodeName(), default_rack_id)

        self.intrusion_prevention_interface = IntrusionPreventionInterface(node.getNodeSettings()["id"])
        self.intrusion_prevention_interface.setup()

        patch = IntrusionPreventionInterface.config_request_patch_template
        self.intrusion_prevention_interface.config_request( "save", patch )

        node.start() # must be called since intrusion-prevention doesn't auto-start

    def setUp(self):
        self.intrusion_prevention_interface = IntrusionPreventionInterface(node.getNodeSettings()["id"])
        self.intrusion_prevention_interface.setup()
        flush_events()

    def test_010_client_is_online(self):
        """
        Verify client is online
        """
        result = remote_control.isOnline()

        assert (result == 0)

    #
    # Wizard tests just compare before/after of enabled rules.
    #
    def test_021_wizard_classtypes_custom_categories_recommended(self):
        """
        Setup Wizard, custom classtypes, recommended categories
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["activeGroups"] = {
            "classtypes": "custom",
            "classtypesSelected": [
                "+attempted-admin"
            ],
            "categories": "recommended"
        }

        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        enabled_count = self.intrusion_prevention_interface.count_enabled_rules(settings)
        assert(enabled_count != self.intrusion_prevention_interface.get_last_enabled_rules_count() )
        self.intrusion_prevention_interface.set_last_enabled_rules_count(enabled_count)

    def test_022_wizard_classtypes_recommended_categories_custom(self):
        """
        Setup Wizard, recommended classtypes, custom categories
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["activeGroups"] = {
            "classtypes": "recommended",
            "categories": "custom",
            "categoriesSelected": [
                "+app_detect"
            ]
        }

        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        enabled_count = self.intrusion_prevention_interface.count_enabled_rules(settings)
        assert(enabled_count != self.intrusion_prevention_interface.get_last_enabled_rules_count() )
        self.intrusion_prevention_interface.set_last_enabled_rules_count(enabled_count)

    def test_023_wizard_classtypes_custom_categories_custom(self):
        """
        Setup Wiard, custom classtypes, custom categories
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["activeGroups"] = {
            "classtypes": "custom",
            "classtypesSelected": [
                "+attempted-admin"
            ],
            "categories": "custom",
            "categoriesSelected": [
                "+app_detect"
            ]
        }

        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        enabled_count = self.intrusion_prevention_interface.count_enabled_rules(settings)
        assert(enabled_count != self.intrusion_prevention_interface.get_last_enabled_rules_count() )
        self.intrusion_prevention_interface.set_last_enabled_rules_count(enabled_count)

    def test_024_wizard_classtypes_recommended_categories_recommended(self):
        """
        Setup Wizard, recommended classtypes, recommended categories
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["activeGroups"] = {
            "classtypes": "recommended",
            "categories": "recommended"
        }

        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        enabled_count = self.intrusion_prevention_interface.count_enabled_rules(settings)
        assert(enabled_count != self.intrusion_prevention_interface.get_last_enabled_rules_count() )
        self.intrusion_prevention_interface.set_last_enabled_rules_count(enabled_count)

    #
    # Add/modify/delete rules
    #
    def test_030_rule_add(self):
        """
        UI, Add rule
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["rules"] = {
            "-1": {
                "op":"added",
                "recData": {
                    "sid":"1999999",
                    "category":"app-detect",
                    "classtype":"attempted-admin",
                    "msg":"CompanySecret",
                    "rule":"alert tcp any any -> any any ( msg:\"CompanySecret\"; classtype:attempted-admin; sid:1999999; content:\"CompanySecret\"; nocase;)",
                    "log": True,
                    "block": False,
                    "originalId":"",
                    "path":"",
                    "internalId":-1
                },
                "page":1
            }
        }
        patch["variables"] = {}
        self.intrusion_prevention_interface.config_request( "save", patch )

        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        enabled_count = self.intrusion_prevention_interface.count_enabled_rules(settings)
        assert(enabled_count != self.intrusion_prevention_interface.get_last_enabled_rules_count() )
        self.intrusion_prevention_interface.set_last_enabled_rules_count(enabled_count)

    def test_031_rule_modify(self):
        """
        UI, Modify rule
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["rules"] = {
            "1":{
                "op":"modified",
                "recData":{
                    "sid":"1999999",
                    "category":"app-detect",
                    "classtype":"unknown",
                    "msg":"new rule",
                    "rule":"drop tcp any any -> any any ( msg:\"CompanySecret\"; classtype:attempted-admin; sid:1999999; content:\"CompanySecret\"; nocase;)",
                    "log":True,
                    "block":True,
                    "originalId":"1999999_1",
                    "path":"",
                    "internalId":1
                },
                "page":1
            }
        }
        patch["variables"] = {}
        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        enabled_count = self.intrusion_prevention_interface.count_enabled_rules(settings)
        assert(enabled_count == self.intrusion_prevention_interface.get_last_enabled_rules_count() )
        self.intrusion_prevention_interface.set_last_enabled_rules_count(enabled_count)

    def test_032_rule_delete(self):
        """
        UI, delete rule
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["rules"] = {
            "4194":{
                "op":"deleted",
                "recData":{
                    "sid":"1999999",
                    "category":"app-detect",
                    "classtype":"attempted-admin",
                    "msg":"CompanySecret",
                    "rule":"drop tcp any any -> any any (  msg:\"CompanySecret\"; classtype:attempted-admin; sid:1999999; content:\"CompanySecret\"; nocase; )",
                    "log":True,
                    "block":True,
                    "originalId":"1999999_1",
                    "path":"rules",
                    "internalId":4194
                },
                "page":1
            }
        }
        patch["variables"] = {}
        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        enabled_count = self.intrusion_prevention_interface.count_enabled_rules(settings)
        assert(enabled_count != self.intrusion_prevention_interface.get_last_enabled_rules_count() )
        self.intrusion_prevention_interface.set_last_enabled_rules_count(enabled_count)

    #
    # Add/modify/delete variables
    #
    def test_040_variable_add(self):
        """
        UI, add variable
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["variables"] = {
            "-1":{
                "op":"added",
                "recData":{
                    "variable":"newvar",
                    "definition":"192.168.1.1",
                    "description":"description",
                    "originalId":"",
                    "internalId":-1
                },
                "page":1
            }
        }
        patch["rules"] = {}
        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        count = self.intrusion_prevention_interface.count_variables(settings)
        assert(count != self.intrusion_prevention_interface.get_last_variables_count() )
        self.intrusion_prevention_interface.set_last_variables_count(count)

    def test_041_variable_modify(self):
        """
        UI, modify variable
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["variables"] = {
            "1":{
                "op":"modified",
                "recData":{
                    "variable":"newvar",
                    "definition":"192.168.1.2",
                    "description":"description",
                    "originalId":"newvar",
                    "internalId":1
                },
                "page":1
            }
        }
        patch["rules"] = {}
        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        count = self.intrusion_prevention_interface.count_variables(settings)
        assert(count == self.intrusion_prevention_interface.get_last_variables_count() )
        self.intrusion_prevention_interface.set_last_variables_count(count)

    def test_042_variable_delete(self):
        """
        UI, delete variable
        """
        patch = IntrusionPreventionInterface.config_request_patch_template
        patch["variables"] = {
            "8":{
                "op":"deleted",
                "recData":{
                    "variable":"newvar",
                    "definition":"192.168.1.2",
                    "description":"description",
                    "originalId":"newvar",
                    "internalId":8
                },
                "page":1
            }
        }
        patch["rules"] = {}
        self.intrusion_prevention_interface.config_request( "save", patch )
        settings = json.loads( self.intrusion_prevention_interface.config_request( "load" ) )
        count = self.intrusion_prevention_interface.count_variables(settings)
        assert(count != self.intrusion_prevention_interface.get_last_variables_count() )
        self.intrusion_prevention_interface.set_last_variables_count(count)

    #
    # Functional
    #
    def test_050_functional_tcp_log(self):
        """
        Functional, TCP log
        """
        global node
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        rule = self.intrusion_prevention_interface.create_rule(msg="TCP Log", type="tcp", block=False, directive="content:\"CompanySecret\"; nocase;")

        self.intrusion_prevention_interface.config_request( "save", self.intrusion_prevention_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        loopLimit = 10
        result = 4 # Network failure
        # If there is a network error with wget, retry up to ten times.
        while (result == 4 and loopLimit > 0):
            time.sleep(1)
            result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        event = self.intrusion_prevention_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == False )
        
    def test_051_functional_tcp_block(self):
        """
        Functional, TCP block
        """
        global node
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        rule = self.intrusion_prevention_interface.create_rule(msg="TCP Block", type="tcp", block=True, directive="content:\"CompanySecret\"; nocase;")

        self.intrusion_prevention_interface.config_request( "save", self.intrusion_prevention_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        pre_events_scan = global_functions.getStatusValue(node,"scan")
        pre_events_detect = global_functions.getStatusValue(node,"detect")
        pre_events_block = global_functions.getStatusValue(node,"block")
        
        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        event = self.intrusion_prevention_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == True )

        post_events_scan = global_functions.getStatusValue(node,"scan")
        post_events_detect = global_functions.getStatusValue(node,"detect")
        post_events_block = global_functions.getStatusValue(node,"block")

        print "pre_events_scan: %s post_events_scan: %s"%(str(pre_events_scan),str(post_events_scan))
        assert(pre_events_scan < post_events_scan)
        print "pre_events_detect: %s post_events_detect: %s"%(str(pre_events_detect),str(post_events_detect))
        assert(pre_events_detect < post_events_detect)
        print "pre_events_block: %s post_events_block: %s"%(str(pre_events_block),str(post_events_block))
        assert(pre_events_block < post_events_block)
        
    def test_052_functional_udp_log(self):
        """
        Functional, UDP log
        """
        global node
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        rule = self.intrusion_prevention_interface.create_rule(msg="UDP Log", type="udp", block=False, directive="content:\"CompanySecret\"; nocase;")

        self.intrusion_prevention_interface.config_request( "save", self.intrusion_prevention_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("host www.companysecret.com 4.2.2.1 > /dev/null")

        event = self.intrusion_prevention_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == False )

    def test_053_functional_udp_block(self):
        """
        Functional, UDP block
        """
        global node
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        rule = self.intrusion_prevention_interface.create_rule(msg="UDP Block", type="udp", block=True, directive="content:\"CompanySecret\"; nocase;")

        self.intrusion_prevention_interface.config_request( "save", self.intrusion_prevention_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("host www.companysecret.com 4.2.2.1 > /dev/null")

        event = self.intrusion_prevention_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == True )

    def test_054_functional_icmp_log(self):
        """
        Functional, ICMP log
        """
        global node
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        dest_ip_address = remote_control.runCommand("host test.untangle.com | grep 'has address' | cut -d' ' -f4", None, True )
        rule = self.intrusion_prevention_interface.create_rule(msg="ICMP Log", type="icmp", dest_ip=dest_ip_address, block=False)

        self.intrusion_prevention_interface.config_request( "save", self.intrusion_prevention_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("ping -c 5 " + dest_ip_address + " > /dev/null")

        event = self.intrusion_prevention_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == False )

    def test_055_functional_icmp_block(self):
        """
        Functional, ICMP block
        """
        global node
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        dest_ip_address = remote_control.runCommand("host test.untangle.com | grep 'has address' | cut -d' ' -f4", None, True )
        rule = self.intrusion_prevention_interface.create_rule(msg="ICMP Block", type="icmp", dest_ip=dest_ip_address, block=True)

        self.intrusion_prevention_interface.config_request( "save", self.intrusion_prevention_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        pre_events_scan = global_functions.getStatusValue(node,"scan")
        pre_events_detect = global_functions.getStatusValue(node,"detect")
        pre_events_block = global_functions.getStatusValue(node,"block")
        
        result = remote_control.runCommand("ping -c 5 " + dest_ip_address + " > /dev/null")
        
        event = self.intrusion_prevention_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == True )

        post_events_scan = global_functions.getStatusValue(node,"scan")
        post_events_detect = global_functions.getStatusValue(node,"detect")
        post_events_block = global_functions.getStatusValue(node,"block")

        print "pre_events_scan: %s post_events_scan: %s"%(str(pre_events_scan),str(post_events_scan))
        assert(pre_events_scan < post_events_scan)
        print "pre_events_detect: %s post_events_detect: %s"%(str(pre_events_detect),str(post_events_detect))
        assert(pre_events_detect < post_events_detect)
        print "pre_events_block: %s post_events_block: %s"%(str(pre_events_block),str(post_events_block))
        assert(pre_events_block < post_events_block)

    def test_060_bypass_udp_block(self):
        global node
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')
        tracerouteExists = remote_control.runCommand("test -x /usr/sbin/traceroute")
        if tracerouteExists != 0:
            raise unittest2.SkipTest("Traceroute app needs to be installed on client")

        # use IP address instead of hostname to avoid false positive with DNS IPS block.
        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings = copy.deepcopy( orig_netsettings )
        netsettings['bypassRules']['list'].append( createBypassConditionRule("SRC_ADDR",remote_control.clientIP) )
        # netsettings['logBypassedSessions'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)
        test_untangle_com_ip = socket.gethostbyname("test.untangle.com")

        startTime = datetime.now()

        rule = self.intrusion_prevention_interface.create_rule(msg="UDP Block", type="udp", block=True, directive="")
        self.intrusion_prevention_interface.config_request( "save", self.intrusion_prevention_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("/usr/sbin/traceroute -U -m 3 -p 1234 " + test_untangle_com_ip)
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        time.sleep(35)
        events = global_functions.get_events('Intrusion Prevention','All Events',None,500)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 500,
                                               "source_addr", remote_control.clientIP,
                                               "dest_addr", test_untangle_com_ip,
                                               "dest_port", 1234,
                                               "protocol", 17,
                                               "blocked", True,
                                               min_date=startTime )

        print "found: %s"%str(found)
        assert(not found)

    def test_065_bypass_tcp_block(self):
        """
        Functional, UDP block
        """
        global node
        if remote_control.quickTestsOnly:
            raise unittest2.SkipTest('Skipping a time consuming test')

        orig_netsettings = uvmContext.networkManager().getNetworkSettings()
        netsettings = copy.deepcopy( orig_netsettings )
        netsettings['bypassRules']['list'].append( createBypassConditionRule("SRC_ADDR",remote_control.clientIP) )
        # netsettings['logBypassedSessions'] = True
        uvmContext.networkManager().setNetworkSettings(netsettings)
        test_untangle_com_ip = socket.gethostbyname("test.untangle.com")

        startTime = datetime.now()

        rule = self.intrusion_prevention_interface.create_rule(msg="TCP Block", type="tcp", block=True, directive="")

        self.intrusion_prevention_interface.config_request( "save", self.intrusion_prevention_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/")
        uvmContext.networkManager().setNetworkSettings(orig_netsettings)

        time.sleep(35)
        events = global_functions.get_events('Intrusion Prevention','All Events',None,500)
        assert(events != None)
        found = global_functions.check_events( events.get('list'), 500,
                                               "source_addr", remote_control.clientIP,
                                               "dest_addr", test_untangle_com_ip,
                                               "dest_port", 80,
                                               "protocol", 6,
                                               "blocked", True,
                                               min_date=startTime )

        print "found: %s"%str(found)
        assert(not found)

    @staticmethod
    def finalTearDown(self):
        """
        Shut down
        """
        global node
        if node == None:
            return
        uvmContext.nodeManager().destroy( node.getNodeSettings()["id"] )
        node = None
        

test_registry.registerNode("intrusion-prevention", IntrusionPreventionTests)
