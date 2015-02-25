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
        
        ## new internal format for variables?
        for variable in rules.get_variables():
            if variable == "HOME_NET":
                ## Ignore HOME_NET
                continue
                
            definition = "default value"
            description = "default description"
        
            for default_variable in conf.get_variables():
                if default_variable["key"] == variable:
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
                "log": (rule.enabled == True) and ((rule.action == "alert" or rule.action == "drop")),
                "block" : (rule.enabled == True)  and ( rule.action == "drop" ),
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

    def patch( self, patch_file_name):
        """
        Processing settings patch from UI
        """
        patch_file = open( patch_file_name )
        patch_settings = json.load( patch_file )
        patch_file.close()

        for key in patch_settings:
            if key == "rules":
                for id in patch_settings[key]:
                    self.patch_rule(patch_settings[key][id])
            elif key == "variables":
                for id in patch_settings[key]:
                    self.patch_variable(patch_settings[key][id])
            elif key == "active_rules":
                self.patch_active_rules(patch_settings[key])
                self.settings[key] = patch_settings[key] 
            else:
                """
                Otherwise, just set as-is
                """
                self.settings[key] = patch_settings[key] 

    def patch_rule( self, rule ):
        """
        Rule diff to add, modify, remove
        """
        snort_rule = None
        match_rule = re.search( SnortRule.text_regex, rule["recData"]["rule"] )
        if match_rule:
            snort_rule = SnortRule( match_rule, rule["recData"]["category"] )

        if snort_rule == None:
            ## !! error message
            return

        operation = rule["op"] 
        if operation == "added":
            self.rules.add_rule( snort_rule )
        elif operation == "modified":
            self.rules.modify_rule( snort_rule )
        elif operation == "deleted":
            self.rules.delete_rule( snort_rule )

    def patch_variable( self, variable):
        """
        Variable diff to add, modify, remove
        """
        snort_variable = { 
            "variable": variable["recData"]["variable"],
            "definition": variable["recData"]["definition"],
            "description": variable["recData"]["description"]
        } 

        ## ??? tie to rule variable management instead
        operation = variable["op"] 
        if operation == "added":
            self.settings["variables"]["list"].append( snort_variable )
        elif operation == "modified":
            for i,v in enumerate(self.settings["variables"]["list"]):
                if operation == "modified" and v["variable"] == variable["recData"]["originalId"]:
                    self.settings["variables"]["list"][i] = snort_variable
        elif operation =="deleted":
            self.settings["variables"]["list"].remove(snort_variable)

    def patch_active_rules(self, active_rules):
        """
        Process active rules diff.
        """
        snort_rules = self.get_rules()
        snort_rules.filter(active_rules)
        self.set_rules(snort_rules.get_rules())

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

