"""
Intrusion Prevention Settings
"""
import json
import os
import re

from intrusion_prevention.snort_signature import SnortSignature
from intrusion_prevention.snort_signatures import SnortSignatures

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
        "configured": False,
        "max_scan_size": 1024
    }
    
    def __init__(self, app_id):
        self.app_id = app_id
        self.file_name = "@PREFIX@/usr/share/untangle/settings/intrusion-prevention/settings_" + self.app_id + ".js"
            
        self.signatures = SnortSignatures(app_id)
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
            match_signature = re.search(SnortSignature.text_regex, obj["signature"])
            if match_signature:
                if "path" in obj:
                    path = obj["path"]
                else:
                    path = "signatures"
                signature = SnortSignature(match_signature, obj["category"], path)
                signature.set_action(obj["log"], obj["block"])
                signature.set_msg(obj["msg"])
                signature.set_sid(obj["sid"])
                self.signatures.add_signature(signature)
            else:
                print("error with signature:" + obj["signature"])
            return None
        else:
            return obj

    def load(self, file_name=""):
        """
        Load settings
        """
        if file_name == "":
            file_name = self.file_name
            
        settings_file = open( file_name )
        self.settings = json.load( settings_file, object_hook=self.json_load_decoder )
        settings_file.close()
        
    def exists(self):
        """
        See if settings exist
        """
        return os.path.exists( self.file_name )

    def create(self):
        """
        Create a new settings file based on the processed
        signature set and default variables from snort configuration.
        """
        if hasattr( self, 'settings') == False:
            self.settings = IntrusionPreventionSettings.default_settings
        else:            
            settings_keys = self.settings.keys()
            for key in IntrusionPreventionSettings.default_settings.keys():
                if not key in settings_keys:
                    self.settings[key] = IntrusionPreventionSettings.default_settings[key]
                
    def save(self, file_name=None, key=None):
        """
        Save settings
        """
        if file_name == None:
            file_name = self.file_name
            
        self.settings["signatures"] = {
            "list": []
        }
        for signature in self.signatures.get_signatures().values():
            msg = signature.options["msg"]
            if msg.startswith('"') and msg.endswith('"'):
                msg = msg[1:-1]
            self.settings["signatures"]["list"].append( { 
                "sid": signature.options["sid"],
                "log": (signature.enabled == True) and ((signature.action == "alert" or signature.action == "drop")),
                "block" : (signature.enabled == True)  and ( signature.action == "drop" ),
                "category": signature.category,
                "classtype": signature.options["classtype"],
                "msg" : msg,
                "signature": signature.build(),
                "path": signature.path
            } )

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

    def set_patch(self, patch, defaults_profile=None):
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

        for key in patch.settings:
            if key == "signatures":
                for signature_id in patch.settings[key]:
                    if patch.settings[key][signature_id]["op"] != "deleted":
                        self.set_patch_signature(patch.settings[key][signature_id])
            elif key == "variables":
                for var_id in patch.settings[key]:
                    if patch.settings[key][var_id]["op"] != "deleted":
                        self.set_patch_variable(patch.settings[key][var_id])
            elif key == "activeGroups":
                self.set_patch_active_groups(patch.settings[key], defaults_profile)
                self.settings[key] = patch.settings[key] 
            else:
                #
                # Otherwise, just set as-is
                #
                self.settings[key] = patch.settings[key] 

    def set_patch_signature(self, signature):
        """
        Signature diff to add, modify, remove
        """
        snort_signature = None
        match_signature = re.search( SnortSignature.text_regex, signature["recData"]["signature"] )
        if match_signature:
            snort_signature = SnortSignature( match_signature, signature["recData"]["category"] )

        if snort_signature == None:
            return

        operation = signature["op"] 
        if operation == "added":
            self.signatures.add_signature( snort_signature )
        elif operation == "modified":
            self.signatures.modify_signature( snort_signature )
        elif operation == "deleted":
            self.signatures.delete_signature( snort_signature.signature_id )

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
        if operation == "added" or operation == "modified":
            modified = False;
            for index, settings_variable in enumerate(self.settings["variables"]["list"]):
                if settings_variable["variable"] == variable["recData"]["variable"]:
                    self.settings["variables"]["list"][index] = snort_variable
                    modified = True
            if modified is False:
                self.settings["variables"]["list"].append( snort_variable )
        elif operation == "deleted":
            self.settings["variables"]["list"].remove(snort_variable)

    def set_patch_active_groups(self, active_groups, defaults_profile=None):
        """
        Process active signatures diff.
        """
        self.signatures.filter_group(active_groups, defaults_profile)

    def convert(self):
        """
        Convert to current file format
        """
        settings_keys = self.settings.keys()
        for key in IntrusionPreventionSettings.default_settings.keys():
            if not key in settings_keys:
                self.settings[key] = IntrusionPreventionSettings.default_settings[key]
        if "active_signatures" in self.settings:
            self.settings["activeGroups"]["classtypes"] = self.settings["active_signatures"]["classtypes_group"]
            self.settings["activeGroups"]["categories"] = self.settings["active_signatures"]["categories_group"]
            if "classtypes" in self.settings["active_signatures"]:
                self.settings["activeGroups"]["classtypesSelected"] = self.settings["active_signatures"]["classtypes"]
            if "categories" in self.settings["active_signatures"]:
                self.settings["activeGroups"]["categoriesSelected"] = self.settings["active_signatures"]["categories"]
            del(self.settings["active_signatures"])

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
