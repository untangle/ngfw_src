"""
IDPS Settings
"""
import json
import os
import re
from netaddr import IPNetwork

from untangle_node_idps.snort_rule import SnortRule
from untangle_node_idps.snort_rules import SnortRules

class IdpsSettings:
    """
    NGFW settings management
    """
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
        "updated": {
            "rules": {
                "added" : [],
                "modified": [],
                "deleted": []
            }
        },
        "configured": False,
        "max_scan_size": 1024
    }
    
    def __init__( self, node_id ):
        self.node_id = node_id
        self.file_name = "/usr/share/untangle/settings/untangle-node-idps/settings_" + self.node_id + ".js"
            
        self.rules = SnortRules( node_id )
        self.settings = {}

    def load( self, file_name = "" ):
        """
        Load settings
        """
        if file_name == "":
            file_name = self.file_name
            
        settings_file = open( file_name )
        self.settings = json.load( settings_file )
        settings_file.close()

        if "rules" in self.settings.keys():
            ## Convert rules to snort rules object
            for settings_rule in self.settings["rules"]["list"]:
                match_rule = re.search( 
                    SnortRule.text_regex, 
                    settings_rule["rule"] 
                    )
                if match_rule:
                    path = "rules"
                    if "path" in settings_rule:
                        path = settings_rule["path"]
                    rule = SnortRule( match_rule, settings_rule["category"], path )
                    rule.set_action( 
                        settings_rule["log"], 
                        settings_rule["block"] 
                        )
                    rule.set_msg( settings_rule["msg"] )
                    rule.set_sid( settings_rule["sid"] )
                    self.rules.add_rule( rule )
                else:
                    print "error with rule:" + settings_rule["text"]
        
    def exists( self ):
        """
        See if settings exist
        """
        return os.path.exists( self.file_name )

    def initialize( self, conf, rules ):
        """
        Create a new settings file based on the processed
        rule set and default variables from snort configuration.
        """
        if hasattr( self, 'settings') == False:
            self.settings = IdpsSettings.default_settings
        else:            
            settings_keys = self.settings.keys()
            for key in IdpsSettings.default_settings.keys():
                if not key in settings_keys:
                    self.settings[key] = IdpsSettings.default_settings[key]
        
        network_settings_file = open( 
            "/usr/share/untangle/settings/untangle-vm/network.js" )
        network_settings = json.load( network_settings_file )
        network_settings_file.close()
        
        default_interfaces = []
        default_home_net = []
        for interface in network_settings["interfaces"]["list"]:
            default_interfaces.append(interface["systemDev"])
            if interface["isWan"] == False and "v4StaticAddress" in interface:
                network = IPNetwork( interface["v4StaticAddress"] + 
                    "/" + 
                    str(interface["v4StaticPrefix"]) ).cidr
                default_home_net.append( network )
                for alias in interface["v4Aliases"]["list"]:
                    network = IPNetwork( alias["staticAddress"] + 
                        "/" + 
                        str( alias["staticPrefix"] ) ).cidr
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
                        definition = ",".join(map(str, default_home_net))
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
            } )
            
        self.rules = rules
        
    def save( self, file_name = None ):
        """
        Save settings
        """
        if file_name == None:
            file_name = self.file_name
            
        self.settings["rules"] = {
            "list": []
        }
        for rule in self.rules.get_rules().values():
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
                "rule": rule.build(),
                "path": rule.path
            } )
        
        settings_file = open( file_name, "w" )
        json.dump( 
            self.settings, settings_file, 
            False, True, True, True, None, 0 
            )
        settings_file.close()

    def get_rules( self ):
        """
        Return rules
        """
        return self.rules

    def set_rules( self, rules ):
        """
        Set rules
        """
        self.rules.set_rules( rules )

    def get_variables( self ):
        """
        Get variables
        """
        return self.settings["variables"]["list"]

    def get_variable( self, key ):
        """
        Get single variable
        """
        for variable in self.settings["variables"]["list"]:
            if key == variable["variable"]:
                return variable["definition"]

    def get_interfaces( self ):
        """
        Get interfaces
        """
        return self.settings["interfaces"]["list"]

    def get_active_rules_categories( self ):
        """
        Get active rules categories
        """
        return self.settings["active_rules"]["categories"]
    
    def get_active_rules_classtypes( self ):
        """
        Get active rules classtypes
        """
        return self.settings["active_rules"]["classtypes"]

    def get_updated( self ):
        """
        Return updated
        """
        return self.settings["updated"]

    def set_updated( self, updated ):
        """
        Set rules
        """
        self.settings["updated"] = updated

