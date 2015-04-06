"""
IDPS Test Suite
"""
import unittest2
import time
import sys

import json
import pycurl
import urllib
from StringIO import StringIO

from jsonrpc import JSONRPCException
from uvm import Uvm
import remote_control
import test_registry

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)

uvmContext = Uvm().getUvmContext()
default_rack_id = 1
node = None

class IdpsInterface:
    """
    IDPS management object
    """
    config_url = "http://localhost/webui/download?"
    config_request_arguments_template = {
        "type": "IdpsSettings",
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
        self.__curl = pycurl.Curl()
        self.__curl.setopt( pycurl.POST, 1 )
        self.__curl.setopt( pycurl.NOSIGNAL, 1 )
        self.__curl.setopt( pycurl.CONNECTTIMEOUT, 60 )
        self.__curl.setopt( pycurl.TIMEOUT, timeout )
        self.__curl.setopt( pycurl.COOKIEFILE, "" )
        self.__curl.setopt( pycurl.FOLLOWLOCATION, 0 )

        self.node_id = node_id

    def config_request(self, action, patch = ""):
        """
        Send a configuration request
        """
        response = StringIO()

        request_arguments = IdpsInterface.config_request_arguments_template
        request_arguments["arg1"] = action
        request_arguments["arg2"] = self.node_id

        patch = json.dumps(patch)

        self.__curl.setopt( pycurl.URL, IdpsInterface.config_url + urllib.urlencode(request_arguments)) 
        self.__curl.setopt( pycurl.POST, 1 )
        self.__curl.setopt( pycurl.HTTPHEADER, ["Content-type: ", "text/plain; charset=utf-8"])
        self.__curl.setopt( pycurl.VERBOSE, False )
        self.__curl.setopt( pycurl.POSTFIELDS, patch.encode('utf-8'))
        self.__curl.setopt( pycurl.WRITEFUNCTION, response.write )
        try:
            self.__curl.perform()
        except Exception, e:
            print "Problem while asking for " + IdpsInterface.config_url + urllib.urlencode(request_arguments)
            raise e

        http_code = self.__curl.getinfo( pycurl.HTTP_CODE ) 
        if ( http_code != 200 ): 
            if http_code == 302:
                raise JSONRPCException( "Invalid username or password [code: %i]" % http_code )
            elif http_code == 500:
                raise JSONRPCException( "Internal server error [code: %i]" % http_code )
            elif http_code == 502 or http_code == 503:
                raise JSONRPCException( "Service unavailable [code: %i]" % http_code )
            else:
                raise JSONRPCException( "An error occurred [code: %i] response: %s" % (http_code, response.getvalue()) )

        return response.getvalue()

    def create_patch(self, type = None, action = None, extended = None ):
        """
        Create a patch
        """
        patch = IdpsInterface.config_request_patch_template

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

        time.sleep(30)
        flush_events()
        query = None
        for q in node.getEventQueries():
            if q['name'] == 'All Events': 
                query = q
        events = uvmContext.getEvents(query['query'], default_rack_id, 1)

        logged = False
        blocked = False
        for event in events["list"]:
            print event
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
        print settings["variables"]["list"]
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
    Clear IDPS events
    """
    reports = uvmContext.nodeManager().node("untangle-node-reporting")
    if (reports != None):
        reports.flushEvents()

class IdpsTests(unittest2.TestCase):
    """
    Tests
    """
    @staticmethod
    def nodeName():
        """
        Get Node name
        """
        return "untangle-node-idps"

    def setUp(self):
        global node
        #raise unittest2.SkipTest("Disable broken tests for now")
        if node == None:
            if (uvmContext.nodeManager().isInstantiated(self.nodeName())):
                print "ERROR: Node %s already installed" % self.nodeName()
                raise Exception('node %s already instantiated' % self.nodeName())
            node = uvmContext.nodeManager().instantiate(self.nodeName(), default_rack_id)
            node.start() # must be called since idps doesn't auto-start
            flush_events()

        self.idps_interface = IdpsInterface(node.getNodeSettings()["id"])
        self.idps_interface.setup()

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
        patch = IdpsInterface.config_request_patch_template
        patch["activeGroups"] = {
            "classtypes": "custom",
            "classtypesSelected": [
                "+attempted-admin"
            ],
            "categories": "recommended"
        }

        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        enabled_count = self.idps_interface.count_enabled_rules(settings)
        assert(enabled_count != self.idps_interface.get_last_enabled_rules_count() )
        self.idps_interface.set_last_enabled_rules_count(enabled_count)

    def test_022_wizard_classtypes_recommended_categories_custom(self):
        """
        Setup Wizard, recommended classtypes, custom categories
        """
        patch = IdpsInterface.config_request_patch_template
        patch["activeGroups"] = {
            "classtypes": "recommended",
            "categories": "custom",
            "categoriesSelected": [
                "+app_detect"
            ]
        }

        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        enabled_count = self.idps_interface.count_enabled_rules(settings)
        assert(enabled_count != self.idps_interface.get_last_enabled_rules_count() )
        self.idps_interface.set_last_enabled_rules_count(enabled_count)

    def test_023_wizard_classtypes_custom_categories_custom(self):
        """
        Setup Wiard, custom classtypes, custom categories
        """
        patch = IdpsInterface.config_request_patch_template
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

        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        enabled_count = self.idps_interface.count_enabled_rules(settings)
        assert(enabled_count != self.idps_interface.get_last_enabled_rules_count() )
        self.idps_interface.set_last_enabled_rules_count(enabled_count)

    def test_024_wizard_classtypes_recommended_categories_recommended(self):
        """
        Setup Wizard, recommended classtypes, recommended categories
        """
        patch = IdpsInterface.config_request_patch_template
        patch["activeGroups"] = {
            "classtypes": "recommended",
            "categories": "recommended"
        }

        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        enabled_count = self.idps_interface.count_enabled_rules(settings)
        assert(enabled_count != self.idps_interface.get_last_enabled_rules_count() )
        self.idps_interface.set_last_enabled_rules_count(enabled_count)

    #
    # Add/modify/delete rules
    #
    def test_030_rule_add(self):
        """
        UI, Add rule
        """
        patch = IdpsInterface.config_request_patch_template
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
                    "internalId":-1,
                    "javaClass":"com.untangle.node.idps.IpsRule"
                },
                "page":1
            }
        }
        patch["variables"] = {}
        self.idps_interface.config_request( "save", patch )

        settings = json.loads( self.idps_interface.config_request( "load" ) )
        enabled_count = self.idps_interface.count_enabled_rules(settings)
        assert(enabled_count != self.idps_interface.get_last_enabled_rules_count() )
        self.idps_interface.set_last_enabled_rules_count(enabled_count)

    def test_031_rule_modify(self):
        """
        UI, Modify rule
        """
        patch = IdpsInterface.config_request_patch_template
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
                    "internalId":1,
                    "javaClass":"com.untangle.node.idps.IpsRule"
                },
                "page":1
            }
        }
        patch["variables"] = {}
        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        enabled_count = self.idps_interface.count_enabled_rules(settings)
        assert(enabled_count == self.idps_interface.get_last_enabled_rules_count() )
        self.idps_interface.set_last_enabled_rules_count(enabled_count)

    def test_032_rule_delete(self):
        """
        UI, delete rule
        """
        patch = IdpsInterface.config_request_patch_template
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
        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        enabled_count = self.idps_interface.count_enabled_rules(settings)
        assert(enabled_count != self.idps_interface.get_last_enabled_rules_count() )
        self.idps_interface.set_last_enabled_rules_count(enabled_count)

    #
    # Add/modify/delete variables
    #
    def test_040_variable_add(self):
        """
        UI, add variable
        """
        patch = IdpsInterface.config_request_patch_template
        patch["variables"] = {
            "-1":{
                "op":"added",
                "recData":{
                    "variable":"newvar",
                    "definition":"192.168.1.1",
                    "description":"description",
                    "originalId":"",
                    "internalId":-1,
                    "javaClass":"com.untangle.node.idps.IpsVariable"
                },
                "page":1
            }
        }
        patch["rules"] = {}
        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        count = self.idps_interface.count_variables(settings)
        assert(count != self.idps_interface.get_last_variables_count() )
        self.idps_interface.set_last_variables_count(count)

    def test_041_variable_modify(self):
        """
        UI, modify variable
        """
        patch = IdpsInterface.config_request_patch_template
        patch["variables"] = {
            "1":{
                "op":"modified",
                "recData":{
                    "variable":"newvar",
                    "definition":"192.168.1.2",
                    "description":"description",
                    "originalId":"newvar",
                    "internalId":1,
                    "javaClass":"com.untangle.node.idps.IpsVariable"
                },
                "page":1
            }
        }
        patch["rules"] = {}
        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        count = self.idps_interface.count_variables(settings)
        assert(count == self.idps_interface.get_last_variables_count() )
        self.idps_interface.set_last_variables_count(count)

    def test_042_variable_delete(self):
        """
        UI, delete variable
        """
        patch = IdpsInterface.config_request_patch_template
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
        self.idps_interface.config_request( "save", patch )
        settings = json.loads( self.idps_interface.config_request( "load" ) )
        count = self.idps_interface.count_variables(settings)
        assert(count != self.idps_interface.get_last_variables_count() )
        self.idps_interface.set_last_variables_count(count)

    #
    # Functional
    #
    def test_050_functional_tcp_log(self):
        """
        Functional, TCP log
        """
        global node

        rule = self.idps_interface.create_rule(msg="TCP Log", type="tcp", block=False, directive="content:\"CompanySecret\"; nocase;")

        self.idps_interface.config_request( "save", self.idps_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        event = self.idps_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == False )

    def test_051_functional_tcp_block(self):
        """
        Functional, TCP block
        """
        global node

        rule = self.idps_interface.create_rule(msg="TCP Block", type="tcp", block=True, directive="content:\"CompanySecret\"; nocase;")

        self.idps_interface.config_request( "save", self.idps_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("wget -q -O /dev/null -t 1 --timeout=3 http://test.untangle.com/CompanySecret")

        event = self.idps_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == True )

    def test_052_functional_udp_log(self):
        """
        Functional, UDP log
        """
        global node

        rule = self.idps_interface.create_rule(msg="UDP Log", type="udp", block=False, directive="content:\"CompanySecret\"; nocase;")

        self.idps_interface.config_request( "save", self.idps_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("host @4.2.2.1 www.companysecret.com > /dev/null")

        event = self.idps_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == False )

    def test_053_functional_udp_block(self):
        """
        Functional, UDP block
        """
        global node

        rule = self.idps_interface.create_rule(msg="UDP Block", type="udp", block=True, directive="content:\"CompanySecret\"; nocase;")

        self.idps_interface.config_request( "save", self.idps_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("host @4.2.2.1 www.companysecret.com > /dev/null")

        event = self.idps_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == True )

    def test_054_functional_icmp_log(self):
        """
        Functional, ICMP log
        """
        global node

        dest_ip_address = remote_control.runCommand("host test.untangle.com | grep 'has address' | cut -d' ' -f4", None, True )
        rule = self.idps_interface.create_rule(msg="ICMP Log", type="icmp", dest_ip=dest_ip_address, block=False)

        self.idps_interface.config_request( "save", self.idps_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("ping -c 5 " + dest_ip_address + " > /dev/null")

        event = self.idps_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == False )

    def test_055_functional_icmp_block(self):
        """
        Functional, ICMP block
        """
        global node

        dest_ip_address = remote_control.runCommand("host test.untangle.com | grep 'has address' | cut -d' ' -f4", None, True )
        rule = self.idps_interface.create_rule(msg="ICMP Block", type="icmp", dest_ip=dest_ip_address, block=True)

        self.idps_interface.config_request( "save", self.idps_interface.create_patch( "rule", "add", rule ) )
        node.reconfigure()

        result = remote_control.runCommand("ping -c 5 " + dest_ip_address + " > /dev/null")
        
        event = self.idps_interface.get_log_event(rule)
        assert( event != None and event["blocked"] == True )

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
        

test_registry.registerNode("idps", IdpsTests)
