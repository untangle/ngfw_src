import errno
import json
import os
import re
from netaddr import IPNetwork, IPAddress

from untangle_node_idps.snort_rule import SnortRule
from untangle_node_idps.snort_rules import SnortRules

class IdpsSettings:
    #
    # NGFW settings management
    #
    default_settings = {
        "variables": {
            "list": []
        },
        "rules": {
            "list": []
        },
        "interfaces": {
            "list": []
        },
        "active_rules": {
            "classtypes": [],
            "categories": []
        },
        "nfqueueQueueNum": 2930,
        "configured": False
    }
    
    def __init__( self, nodeId, save_file_name = "" ):
        self.nodeId = nodeId
        self.file_name = "/usr/share/untangle/settings/untangle-node-idps/settings_" + self.nodeId + ".js"
        if save_file_name != "":
            self.save_file_name = save_file_name
        else:
            self.save_file_name = self.file_name
            
        self.rules = SnortRules( nodeId )

    def load( self, file_name = "" ):
        if file_name == "":
            file_name = self.file_name
            
        self.settings_file = open( file_name )
        self.settings = json.load( self.settings_file )
        self.settings_file.close()

        if hasattr( self.settings, "rules" ) == True:
            ## Convert rules to snort rules object
            for settings_rule in self.settings["rules"]["list"]:
                match_rule = re.search( SnortRule.text_regex, settings_rule["rule"] )
                if match_rule:
                    rule = SnortRule( match_rule, settings_rule["category"] )
                    rule.set_action( settings_rule["log"], settings_rule["block"] )
                    rule.set_msg( settings_rule["msg"] )
                    rule.set_sid( settings_rule["sid"] )
                    self.rules.addRule( rule )
                else:
                    print "error with rule:" + settings_rule["text"]
        
    def exists( self ):
        return os.path.exists( self.file_name )

    def initialize( self, conf, rules ):
        #
        # Create a new settings file based on the processed
        # rule set and default variables from snort configuration.
        #
        if hasattr( self, 'settings') == False:
            self.settings = IdpsSettings.default_settings
        else:            
            settings_keys = self.settings.keys()
            for key in IdpsSettings.default_settings.keys():
                if not key in settings_keys:
                    self.settings[key] = IdpsSettings.default_settings[key]
        
        network_settings_file = open( "/usr/share/untangle/settings/untangle-vm/network.js" )
        network_settings = json.load( network_settings_file )
        network_settings_file.close()
        
        default_interfaces = []
        default_home_net = []
        for interface in network_settings["interfaces"]["list"]:
            default_interfaces.append(interface["systemDev"])
            if interface["isWan"] == False and "v4StaticAddress" in interface:
                network = IPNetwork( interface["v4StaticAddress"] + "/" + str(interface["v4StaticPrefix"]) ).cidr
                default_home_net.append( network )
                for alias in interface["v4Aliases"]["list"]:
                    network = IPNetwork( alias["staticAddress"] + "/" + str( alias["staticPrefix"] ) ).cidr
                    default_home_net.append( network )

        self.settings["interfaces"]["list"] = default_interfaces
        default_home_net = set(default_home_net)

        ## new internal format for variables?
        for variable in rules.get_variables():
            definition = "default value"
            description = "default description"
        
            for default_variable in conf.get_variables():
                if default_variable["key"] == variable:
                    if default_variable["key"] == "HOME_NET":
                        definition = ",".join(map(str,default_home_net))
                        if len(default_home_net) > 1:
                            definition = "[" + definition + "]"
                    else:
                        definition = default_variable["value"]
                    description = default_variable["description"]
                    break
        
            self.settings["variables"]["list"].append( { 
                "variable": variable,
                "definition": definition,
                "description": description
            } );
            
        self.rules = rules
        
    def save( self, file_name = "" ):
        if file_name == "":
            file_name = self.save_file_name
            
        self.settings["rules"] = {
            "list": []
        }
        for rule in self.rules.get_rules().values():
            if rule.get_category() == "deleted":
                continue
            msg = rule.options["msg"]
            if msg.startswith('"') and msg.endswith('"'):
                msg = msg[1:-1]
            self.settings["rules"]["list"].append( { 
                "sid": rule.options["sid"],
                "log": rule.enabled == True and rule.action == "alert",
                "block" : False,
                "category": rule.category,
                "classtype": rule.options["classtype"],
                "msg" : msg,
                "rule": rule.build()
            } );
        
        settings_file = open( file_name, "w" )
        json.dump( self.settings, settings_file, False, True, True, True, None, 0 )
        settings_file.close()

    def get_rules( self ):
       return self.rules

    def get_variables( self ):
        return self.settings["variables"]["list"]

    def get_variable( self, key ):
        for variable in self.settings["variables"]["list"]:
            if key == variable["variable"]:
                return variable["definition"]

    def get_interfaces( self ):
        return self.settings["interfaces"]["list"]

    def get_nfqueue_queue_num( self ):
        return self.settings["nfqueueQueueNum"]
    
    def get_active_rules_categories( self ):
        return self.settings["active_rules"]["categories"]
    
    def get_active_rules_classtypes( self ):
        return self.settings["active_rules"]["classtypes"]

