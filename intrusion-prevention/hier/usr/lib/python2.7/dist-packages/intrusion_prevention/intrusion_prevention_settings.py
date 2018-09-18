"""
Intrusion Prevention Settings
"""
import json
import os
import re

from intrusion_prevention.suricata_conf import SuricataConf
from intrusion_prevention.suricata_signature import SuricataSignature
from intrusion_prevention.suricata_signatures import SuricataSignatures
from intrusion_prevention.intrusion_prevention_rule import IntrusionPreventionRule

class IntrusionPreventionSettings:
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
        "signatures": {
            "list": []
        },
        "variables": {
            "list": []
        },
        "rules": {
            "list": []
        },
        "interfaces": {
            "list": []
        },
        "updated": {
            "signatures": {
                "added" : [],
                "modified": [],
                "deleted": []
            }
        },
        # "suricata": {
        #     "outputs": {
        #         "eve-log": {
        #             "enabled": False
        #         },
        #         "fast": {
        #             "enabled": False
        #         },
        #         "stats": {
        #             "enabled": False
        #         }
        #     },
        #     "logging": {
        #         "outputs": {
        #             "file": {
        #                 "enabled": False
        #             },
        #             "syslog":{
        #                 "enabled": True
        #             }
        #         }
        #     }
        # },
        "suricata": {},
        "configured": False,
        "max_scan_size": 1024
    }

    current_version = 2
    
    def __init__(self, app_id):
        self.app_id = app_id
        self.file_name = "@PREFIX@/usr/share/untangle/settings/intrusion-prevention/settings_" + self.app_id + ".js"
            
        self.signatures = SuricataSignatures()
        self.settings = {}

    def json_load_decoder(self,obj):
        if "signature" in obj:
            # Internally we only use a signatureset structure and would otherwise
            # keep the json structure in memory, unused.  This is a 
            # huge waste of memory!  By intercepting we can create the
            # signatureset immediately and incur a much smaller memory footprint(by)
            # creating the signatureset and returning None.
            # It's not 100% wonderful since we're retaining an in-memory 
            # list of thousands of None references but there doesn't seem to be
            # a way around that with this hook (we could free it but garbage collecting
            # doesn't seem to happen fast enough to make a difference).  
            # This is better than nothing and can save up to 40% of memory.
            match_signature = re.search(SuricataSignature.text_regex, obj["signature"])
            if match_signature:
                if "path" in obj:
                    path = obj["path"]
                else:
                    path = "signatures"
                signature = SuricataSignature(match_signature, obj["category"], path)
                signature.set_action(obj["log"], obj["block"])
                signature.set_msg(obj["msg"])
                signature.set_sid(obj["sid"])
                self.signatures.add_signature(signature)
            else:
                print("error with signature:" + obj["signature"])
            return None
        else:
            return obj

    def load(self, file_name=None):
        """
        Load settings
        """
        if file_name == None:
            file_name = self.file_name
            
        settings_file = open( file_name )
        self.settings = json.load( settings_file, object_hook=self.json_load_decoder )
        settings_file.close()
        
    def save(self, file_name=None, key=None):
        """
        Save settings
        """
        if file_name == None:
            file_name = self.file_name
            
        # self.settings["signatures"] = {
        #     "list": []
        # }
        # for signature in self.signatures.get_signatures().values():
        #     msg = signature.options["msg"]
        #     if msg.startswith('"') and msg.endswith('"'):
        #         msg = msg[1:-1]
        #     self.settings["signatures"]["list"].append( { 
        #         "sid": signature.options["sid"],
        #         "log": (signature.enabled == True) and ((signature.action == "alert" or signature.action == "drop")),
        #         "block" : (signature.enabled == True)  and ( signature.action == "drop" ),
        #         "category": signature.category,
        #         "classtype": signature.options["classtype"],
        #         "msg" : msg,
        #         "signature": signature.build(),
        #         "path": signature.path
        #     } )

        if key != None:
            # Only keep settings with the specified key and deferefenced from list.
            # This is only intended for export functions on a functional grid-level
            # basis (eg.,"signatures" or "variables")
            for settings_key in self.settings.keys():
                if settings_key == key:
                    self.settings = self.settings[settings_key]["list"]
                    break
        
        settings_file = open( file_name, "w" )
        json.dump( 
            self.settings, settings_file, 
            False, True, True, True, None, 0 
            )
        settings_file.close()

    def exists(self):
        """
        See if settings exist
        """
        return os.path.exists( self.file_name )

    def create(self):
        """
        Create a new settings file based on the processed
        signature set and default variables from suricata configuration.
        """
        if hasattr( self, 'settings') == False:
            self.settings = IntrusionPreventionSettings.default_settings
        else:
            settings_keys = self.settings.keys()
            for key in IntrusionPreventionSettings.default_settings.keys():
                if not key in settings_keys:
                    self.settings[key] = IntrusionPreventionSettings.default_settings[key]

        SuricataConf.merge_default_settings(self.settings["suricata"])

    def convert(self):
        """
        Convert to current file format
        """
        ## !!! add defaults
        settings_keys = self.settings.keys()
        for key in IntrusionPreventionSettings.default_settings.keys():
            if not key in settings_keys:
                self.settings[key] = IntrusionPreventionSettings.default_settings[key]

        SuricataConf.merge_default_settings(self.settings["suricata"])

        if "active_signatures" in self.settings:
            self.settings["activeGroups"]["classtypes"] = self.settings["active_signatures"]["classtypes_group"]
            self.settings["activeGroups"]["categories"] = self.settings["active_signatures"]["categories_group"]
            if "classtypes" in self.settings["active_signatures"]:
                self.settings["activeGroups"]["classtypesSelected"] = self.settings["active_signatures"]["classtypes"]
            if "categories" in self.settings["active_signatures"]:
                self.settings["activeGroups"]["categoriesSelected"] = self.settings["active_signatures"]["categories"]
            del(self.settings["active_signatures"])

        if not "version" in self.settings:
            self.settings["version"] = self.current_version
            if "rules" in self.settings:
                self.settings["signatures"]["list"] = []
            # !!! convert profiles to rules

    # def set_patch(self, patch, defaults_profile=None):
    def set_patch(self, patch):
        """
        Processing settings patch from UI
        """
        # List deletes first
        for key in patch.settings:
            if key == "signatures":
                for signature_id in patch.settings[key]:
                    if patch.settings[key][signature_id]["op"] == "deleted":
                        self.set_patch_signature(patch.settings[key][signature_id])
            elif key == "variables":
                for var_id in patch.settings[key]:
                    if patch.settings[key][var_id]["op"] == "deleted":
                        self.set_patch_variable(patch.settings[key][var_id])
            elif key == "rules":
                for rule_id in patch.settings[key]:
                    if patch.settings[key][rule_id]["op"] == "deleted":
                        self.set_patch_rule(patch.settings[key][rule_id])

        for key in patch.settings:
            if key == "signatures":
                for signature_id in patch.settings[key]:
                    if patch.settings[key][signature_id]["op"] != "deleted":
                        self.set_patch_signature(patch.settings[key][signature_id])
            elif key == "variables":
                for var_id in patch.settings[key]:
                    if patch.settings[key][var_id]["op"] != "deleted":
                        self.set_patch_variable(patch.settings[key][var_id])
            elif key == "rules":
                for rule_id in patch.settings[key]:
                    if patch.settings[key][rule_id]["op"] != "deleted":
                        self.set_patch_rule(patch.settings[key][rule_id])
            # elif key == "activeGroups":
            #     self.set_patch_active_groups(patch.settings[key], defaults_profile)
            #     self.settings[key] = patch.settings[key] 
            else:
                #
                # Otherwise, just set as-is
                #
                self.settings[key] = patch.settings[key] 

    def set_patch_signature(self, signature):
        """
        Signature diff to add, modify, remove
        """
        suricata_signature = None
        match_signature = re.search( SuricataSignature.text_regex, signature["recData"]["signature"] )
        if match_signature:
            suricata_signature = SuricataSignature( match_signature, signature["recData"]["category"] )

        if suricata_signature == None:
            return

        operation = signature["op"] 
        if operation == "added":
            self.signatures.add_signature( suricata_signature )
        elif operation == "modified":
            self.signatures.modify_signature( suricata_signature )
        elif operation == "deleted":
            self.signatures.delete_signature( suricata_signature.signature_id )

    def set_patch_rule(self, rulePatch):
        """
        Rule diff to add, modify, remove
        """
        rule = {
            "description": rulePatch["recData"]["description"],
            "enabled": rulePatch["recData"]["enabled"],
            "id": rulePatch["recData"]["id"],
            "action": rulePatch["recData"]["action"],
            "conditions": {
                "javaClass": "java.util.LinkedList",
                "list": []
            }
        }
        for condition in rulePatch["recData"]["conditions"]["list"]:
            rule["conditions"]["list"].append({
                "type": condition["type"],
                "comparator": condition["comparator"],
                "value": condition["value"]
            })

        operation = rulePatch["op"] 
        if operation == "added" or operation == "modified":
            modified = False;
            for index, settings_rule in enumerate(self.settings["rules"]["list"]):
                if settings_rule["description"] == rulePatch["recData"]["description"]:
                    self.settings["rules"]["list"][index] = rule
                    modified = True
            if modified is False:
                self.settings["rules"]["list"].append( rule )
        elif operation == "deleted":
            self.settings["rules"]["list"].remove(rule)


    def set_patch_variable(self, variable):
        """
        Variable diff to add, modify, remove
        """
        suricata_variable = { 
            "variable": variable["recData"]["variable"],
            "definition": variable["recData"]["definition"],
            "description": variable["recData"]["description"]
        } 

        operation = variable["op"] 
        if operation == "added" or operation == "modified":
            modified = False;
            for index, settings_variable in enumerate(self.settings["variables"]["list"]):
                if settings_variable["variable"] == variable["recData"]["variable"]:
                    self.settings["variables"]["list"][index] = suricata_variable
                    modified = True
            if modified is False:
                self.settings["variables"]["list"].append( suricata_variable )
        elif operation == "deleted":
            self.settings["variables"]["list"].remove(suricata_variable)

    def set_patch_active_groups(self, active_groups, defaults_profile=None):
        """
        Process active signatures diff.
        """
        self.signatures.filter_group(active_groups, defaults_profile)

    def update_rules(self, rules, id_regex):
        new_rules_list = []
        enabled_rules = []
        rule_id = 1
        for rule in self.settings["rules"]["list"]:
            if rule["enabled"]:
                enabled_rules.append(rule["id"])

            ## !!!! remove any reserved rule....
            match_reserved_rule = re.search( id_regex, str(rule["id"]) )
            if not match_reserved_rule:
                int_value = None
                try:
                    int_value = int(rule["id"])
                except ValueError:
                    int_value = None

                print rule["id"]
                print int_value

                if int_value is not None:
                    rule["id"] = rule_id
                    rule_id += 1
                new_rules_list.append(rule)

        self.settings["rules"]["list"] = new_rules_list
        for rule in rules:
            if rule["id"] in enabled_rules:
                rule["enabled"] = True
            self.settings["rules"]["list"] = [rule] + self.settings["rules"]["list"]

    def apply_rules(self, signatures):
        """
        
        Using rules, set various actions on signatures.

        Arguments:
            signatures {[type]} -- [description]
        """
        for settingsRule in self.settings["rules"]["list"]:
            # print settingsRule
            rule = IntrusionPreventionRule(settingsRule)
            if not rule.get_enabled():
                continue
            print(rule)
            # 
            # rule action precidence
            # default
            # log
            # block
            # disable
            for signature in signatures.get_signatures().values():
                # print(signature)
                # order?
                # print(rule.matches(signature))
                if rule.matches(signature):
                    rule.set_signature_action(signature)
#                break

    def disable_signatures(self, signatures):
        for signature in signatures.get_signatures().values():
            if not signature.get_action_changed():
                signature.set_action(False, False)

    def get_signatures(self):
        """
        Return signatures
        """
        return self.signatures

    def set_signatures(self, signatures):
        """
        Set signatures
        """
        self.signatures.set_signatures(signatures)

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
        Get active signatures categories
        """
        return self.settings["activeGroups"]["categories"]
    
    def get_activegroups_classtypes(self):
        """
        Get active signatures classtypes
        """
        return self.settings["activeGroups"]["classtypes"]

    def get_updated(self):
        """
        Return updated
        """
        return self.settings["updated"]

    def set_updated(self, updated):
        """
        Set signatures
        """
        self.settings["updated"] = updated
