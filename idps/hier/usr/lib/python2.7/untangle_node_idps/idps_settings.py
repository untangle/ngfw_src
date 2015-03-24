"""
IDPS Settings
"""
import json
import os
import re

from untangle_node_idps.snort_rule import SnortRule
from untangle_node_idps.snort_rules import SnortRules

class IdpsSettings:
    """
    NGFW settings management
    """
    default_settings = {
        "profileId": "low_unknown",
        "profileVersion": "0",
        "activeGroups": {
            "classtypes": "recommended",
            "categories": "recommended"
        },
        "rules": {
            "list": []
        },
        "variables": {
            "list": []
        },
        "interfaces": {
            "list": []
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
    
    def __init__(self, node_id):
        self.node_id = node_id
        self.file_name = "/usr/share/untangle/settings/untangle-node-idps/settings_" + self.node_id + ".js"
            
        self.rules = SnortRules(node_id)
        self.settings = {}

    def load(self, file_name=""):
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
                    rule = SnortRule(match_rule, settings_rule["category"], path)
                    rule.set_action( 
                        settings_rule["log"], 
                        settings_rule["block"] 
                        )
                    rule.set_msg(settings_rule["msg"])
                    rule.set_sid(settings_rule["sid"])
                    self.rules.add_rule(rule)
                else:
                    print "error with rule:" + settings_rule["text"]
        
    def exists(self):
        """
        See if settings exist
        """
        return os.path.exists( self.file_name )

    def create(self):
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
                
    def save(self, file_name=None):
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

    def set_patch(self, patch, defaults_profile=None):
        """
        Processing settings patch from UI
        """
        for key in patch.settings:
            if key == "rules":
                for rule_id in patch.settings[key]:
                    self.set_patch_rule(patch.settings[key][rule_id])
            elif key == "variables":
                for var_id in patch.settings[key]:
                    self.set_patch_variable(patch.settings[key][var_id])
            elif key == "activeGroups":
                self.set_patch_active_groups(patch.settings[key], defaults_profile)
                self.settings[key] = patch.settings[key] 
            else:
                #
                # Otherwise, just set as-is
                #
                self.settings[key] = patch.settings[key] 

    def set_patch_rule(self, rule):
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
            self.rules.delete_rule( snort_rule.rule_id )

    def set_patch_variable(self, variable):
        """
        Variable diff to add, modify, remove
        """
        snort_variable = { 
            "variable": variable["recData"]["variable"],
            "definition": variable["recData"]["definition"],
            "description": variable["recData"]["description"]
        } 

        operation = variable["op"] 
        if operation == "added":
            self.settings["variables"]["list"].append( snort_variable )
        elif operation == "modified":
            for index, settings_variable in enumerate(self.settings["variables"]["list"]):
                if operation == "modified" and settings_variable["variable"] == variable["recData"]["originalId"]:
                    self.settings["variables"]["list"][index] = snort_variable
        elif operation == "deleted":
            self.settings["variables"]["list"].remove(snort_variable)

    def set_patch_active_groups(self, active_groups, defaults_profile=None):
        """
        Process active rules diff.
        """
        self.rules.filter_group(active_groups, defaults_profile)

    def convert(self):
        """
        Convert to current file format
        """
        settings_keys = self.settings.keys()
        for key in IdpsSettings.default_settings.keys():
            if not key in settings_keys:
                self.settings[key] = IdpsSettings.default_settings[key]
        if "active_rules" in self.settings:
            self.settings["activeGroups"]["classtypes"] = self.settings["active_rules"]["classtypes_group"]
            self.settings["activeGroups"]["categories"] = self.settings["active_rules"]["categories_group"]
            if "classtypes" in self.settings["active_rules"]:
                self.settings["activeGroups"]["classtypesSelected"] = self.settings["active_rules"]["classtypes"]
            if "categories" in self.settings["active_rules"]:
                self.settings["activeGroups"]["categoriesSelected"] = self.settings["active_rules"]["categories"]
            del(self.settings["active_rules"])

    def get_rules(self):
        """
        Return rules
        """
        return self.rules

    def set_rules(self, rules):
        """
        Set rules
        """
        self.rules.set_rules(rules)

    def get_variables(self):
        """
        Get variables
        """
        return self.settings["variables"]["list"]

    def get_variable(self, key):
        """
        Get single variable
        """
        for variable in self.settings["variables"]["list"]:
            if key == variable["variable"]:
                return variable["definition"]

    def get_interfaces(self):
        """
        Get interfaces
        """
        return self.settings["interfaces"]["list"]

    def get_activegroups_categories(self):
        """
        Get active rules categories
        """
        return self.settings["activeGroups"]["categories"]
    
    def get_activegroups_classtypes(self):
        """
        Get active rules classtypes
        """
        return self.settings["activeGroups"]["classtypes"]

    def get_updated(self):
        """
        Return updated
        """
        return self.settings["updated"]

    def set_updated(self, updated):
        """
        Set rules
        """
        self.settings["updated"] = updated
