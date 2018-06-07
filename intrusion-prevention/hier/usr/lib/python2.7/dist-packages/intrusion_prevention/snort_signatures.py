"""
Snort signature set management
"""
import os
import re

from intrusion_prevention.snort_signature import SnortSignature

class SnortSignatures:
    """
    Process a set of snort signatures such as downloaded signatures.
    """
    category_regex = re.compile(r'^# \-+ Begin (.+) Rules Category')
    file_name_category_regex =re.compile(r'(/|\-)([^/\-]+)\.rules$')

    signature_paths = ["rules", "preproc_rules", "emerging_rules"]
    
    def __init__(self, app_id="0", path="", file_name=""):
        self.app_id = app_id
        self.path = path
        self.file_name = self.path + "/"
        if file_name != "":
            self.file_name = self.file_name + file_name
        else:
            self.file_name = self.file_name + "app_" + self.app_id + ".rules"
        
        self.signatures = {}
        self.variables = []

    def set_path(self, path=""):
        """
        Path for reading.
        """
        self.path = path

    def load(self, path=False):
        """
        Load signatureset
        """
        if path == True:
            #
            # Parse directory trees
            #
            for signature_path in SnortSignatures.signature_paths:
                parse_path = self.path + "/" + signature_path 
                if os.path.isdir(parse_path) == False:
                    continue
                for file_name in os.listdir( parse_path ):
                    extension = os.path.splitext( file_name )[1]
                    if extension != ".rules":
                        continue
                    self.load_file( parse_path + "/" + file_name, signature_path )
        else:
            self.load_file( self.file_name )
            
    def load_file(self, file_name, signature_path="rules"):
        """
        Category based on "major" file name separator. 
        e.g., web-cgi = web
        """
        name = os.path.split( file_name )[1]
        name = os.path.splitext( name )[0]
        category = name

        signature_count = 0
        # defalt category is from filename, remove prefix
        match_file_name_category = re.search(SnortSignatures.file_name_category_regex, file_name)
        if match_file_name_category:
            category = self.format_category(match_file_name_category.group(2))

        signatures_file = open( file_name )
        for line in signatures_file:
            # Alternate category match from pulledpork output
            match_category = re.search( SnortSignatures.category_regex, line )
            if match_category:
                category = match_category.group(1)
            else:            
                match_signature = re.search( SnortSignature.text_regex, line )
                if match_signature:
                    self.add_signature(SnortSignature( match_signature, category, signature_path))
                    signature_count = signature_count + 1
        signatures_file.close()
            
    def save(self, path=None, classtypes=None, categories=None, msgs=None):
        """
        Save signature set
        """
        if classtypes == None:
            classtypes = []
        if categories == None:
            categories = []
        if msgs == None:
            msgs = []

        if os.path.isdir(path) == False:
            os.makedirs(path)

        file_name = path + "/" + "app_" + self.app_id + ".rules"
        signature_path = os.path.split( path )[1]

        temp_file_name = file_name + ".tmp"
        signatures_file = open( temp_file_name, "w" )
        category = "undefined"
        # ? order by category
        for signature in self.signatures.values():
            if ( signature.get_enabled() == True ) and ( signature.path == signature_path ):
                if signature.category != category:
                    category = signature.category
                    signatures_file.write("\n\n# ---- Begin " + category + " Rules Category ----#" + "\n\n")
                
                signatures_file.write( signature.build() + "\n" )
        signatures_file.close()
        
        if os.path.isfile( file_name ):
            os.remove( file_name )
        os.rename( temp_file_name, file_name )

    def add_signature(self, signature):
        """
        Add a new signature to the list and search for variables.
        """
        self.signatures[signature.signature_id] = signature

    def modify_signature(self, signature, reset_signature=False):
        """
        Alias for add_signature
        """
        if reset_signature is not True:
            # If untantgle_action found in signature metadata, use this signature but copy in the
            # metadata tag and the current enabled and action settings.
            if self.signatures[signature.signature_id] is not None and self.signatures[signature.signature_id].options["metadata"] is not None:
                current_signature = self.signatures[signature.signature_id]
                current_metadata = current_signature.get_metadata()
                if "untangle_action" in current_metadata:
                    new_metadata = signature.get_metadata()
                    for key in current_metadata:
                        if key.startswith("untangle_"):
                            new_metadata[key] = current_metadata[key]

                    signature.enabled = current_signature.enabled
                    signature.action = current_signature.action
                    signature.set_metadata(new_metadata)
        self.add_signature(signature)

    def delete_signature(self, signature_id):
        """
        Remove signature.
        """
        del(self.signatures[signature_id])

    def filter_group(self, profile, defaults_profile=None):
        """
        Filter signatures for enabled/disabled
        """
        if profile["classtypes"] == "recommended":
            classtypes_selected = defaults_profile["activeGroups"]["classtypesSelected"]
        else:
            classtypes_selected = profile["classtypesSelected"]

        if profile["categories"] == "recommended":
            categories_selected = defaults_profile["activeGroups"]["categoriesSelected"]
        else:
            categories_selected = profile["categoriesSelected"]

        signature_ids_selected = []
        if "signaturesSelected" in defaults_profile["activeGroups"]:
            signature_ids_selected = defaults_profile["activeGroups"]["signaturesSelected"]
        elif "signaturesSelected" in profile:
            signature_ids_selected = profile["signaturesSelected"]

        for rid in self.signatures:
            signature = self.signatures[rid]
            # If signature was modified by user, keep those settings instead of disabling the signature.
            if signature.match(classtypes_selected, categories_selected, signature_ids_selected) == False:
                signature_untangle_modified = False
                if signature.options["metadata"] is not None:
                    signature_metadata = signature.get_metadata()
                    if "untangle_action" in signature_metadata:
                        signature_untangle_modified = True

                if signature_untangle_modified is False:
                    # Not modified, so disable signature
                    signature.set_action(False, False)
            self.signatures[rid] = signature

    def update(self, settings, conf, current_signatures=None, previous_signatures=None, reset_signatures=False):
        """
        Determine differences in previous and current signatures.
        If previous is not specified, then the difference will be just
        a populated added_signature_rids list.

        A happy side effect of only comparing old and new Snort signaturesets
        is that custom signatures are preserved (unless their signature identifiers
        conflict, of course).
        """

        #
        # Signature management
        #
        added_signature_rids = []
        deleted_signature_rids = []
        modified_signature_rids = []

        if previous_signatures != None:
            #
            # Deleted signatures: Those only in previous and not in current
            # 
            for rid in previous_signatures.get_signatures():
                if current_signatures.get_signatures().has_key(rid) == False:
                    deleted_signature_rids.append(rid)

        for rid in current_signatures.get_signatures():
            if previous_signatures == None or previous_signatures.get_signatures().has_key(rid) == False:
                # 
                # New signatures: Only in current
                #
                added_signature_rids.append(rid)
            elif previous_signatures != None and ( current_signatures.get_signatures()[rid].build() != previous_signatures.get_signatures()[rid].build() ):
                #
                # Modified signatures: In both but different
                # 
                modified_signature_rids.append(rid)

        #
        # Remove deleted signatures
        # 
        for rid in deleted_signature_rids:
            if self.get_signatures().has_key(rid):
                self.delete_signature(rid)

        #
        # Add/modify signatures
        #
        for rid in added_signature_rids + modified_signature_rids:
            if self.get_signatures().has_key(rid):
                #
                # Replace modified signature
                # 
                self.modify_signature( current_signatures.get_signatures()[rid], reset_signatures )
            else:
                # 
                # Add new signature
                #
                self.add_signature( current_signatures.get_signatures()[rid] )

                # Variable management
                # Only interested in adding from current set.
                # Clearly we don't want to modify values and deletion could
                # be problematic if a custom signature is using it.
                # 
                for variable in current_signatures.get_signatures()[rid].get_variables():
                    if settings.get_variable(variable) == None:
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
        
                        settings.settings["variables"]["list"].append( { 
                            "variable": variable,
                            "definition": definition,
                            "description": description
                        } )

        if ( len(added_signature_rids) > 0 and len(added_signature_rids) != len(current_signatures.get_signatures()) ) or len(modified_signature_rids) > 0 or len(deleted_signature_rids) > 0:
            #
            # Only record updated signature identifiers
            # if there was a change and that change
            # doesn't equal the number of initial signature population.
            #
            settings.set_updated({
                "signatures": { 
                    "added" : added_signature_rids, 
                    "modified" : modified_signature_rids, 
                    "deleted": deleted_signature_rids
                }
            })


    def update_categories(self, defaults, sync_enabled = False):
        """
        Update category for each signature
        """
        categories = defaults.get_categories()

        #
        # Reset all to original
        #
        for signature_id in self.signatures.keys():
            signature = self.get_signatures()[signature_id]
            signature.set_category(defaults.get_original_category(signature.get_category()))
            self.modify_signature(signature)
        #
        # Modify to new
        #
        for category in categories:
            for signature_id in category["ids"]:
                if signature_id in self.signatures.keys():
                    signature = self.get_signatures()[signature_id]
                    signature.set_category(category["category"])
                    if sync_enabled == True:
                        signature.set_action(True,False)
                    self.modify_signature(signature)

    def format_category(self, category):
        category = category.replace("_","-")
        return category

    def get_signatures(self):
        """
        Get signatures
        """
        return self.signatures

    def set_signatures(self, signatures):
        """
        Set signatures
        """
        self.signatures = signatures
    
    def get_variables(self):
        """
        Get variables
        """
        return self.variables

    def get_file_name(self):
        """
        Get filename
        """
        return self.file_name
