"""
Snort rule set management
"""
import os
import re

from intrusion_prevention.snort_rule import SnortRule

class SnortRules:
    """
    Process a set of snort rules such as downloaded rules.
    """
    category_regex = re.compile(r'^# \-+ Begin (.+) Rules Category')
    file_name_category_regex =re.compile(r'(/|\-)([^/\-]+)\.rules$')

    rule_paths = ["rules", "preproc_rules", "emerging_rules"]
    
    def __init__(self, app_id="0", path="", file_name=""):
        self.app_id = app_id
        self.path = path
        self.file_name = self.path + "/"
        if file_name != "":
            self.file_name = self.file_name + file_name
        else:
            self.file_name = self.file_name + "app_" + self.app_id + ".rules"
        
        self.rules = {}
        self.variables = []

    def set_path(self, path=""):
        """
        Path for reading.
        """
        self.path = path

    def load(self, path=False):
        """
        Load ruleset
        """
        if path == True:
            #
            # Parse directory trees
            #
            for rule_path in SnortRules.rule_paths:
                parse_path = self.path + "/" + rule_path 
                if os.path.isdir(parse_path) == False:
                    continue
                for file_name in os.listdir( parse_path ):
                    extension = os.path.splitext( file_name )[1]
                    if extension != ".rules":
                        continue
                    self.load_file( parse_path + "/" + file_name, rule_path )
        else:
            self.load_file( self.file_name )
            
    def load_file(self, file_name, rule_path="rules"):
        """
        Category based on "major" file name separator. 
        e.g., web-cgi = web
        """
        name = os.path.split( file_name )[1]
        name = os.path.splitext( name )[0]
        category = name

        rule_count = 0
        # defalt category is from filename, remove prefix
        match_file_name_category = re.search(SnortRules.file_name_category_regex, file_name)
        if match_file_name_category:
            category = self.format_category(match_file_name_category.group(2))

        rules_file = open( file_name )
        for line in rules_file:
            # Alternate category match from pulledpork output
            match_category = re.search( SnortRules.category_regex, line )
            if match_category:
                category = match_category.group(1)
            else:            
                match_rule = re.search( SnortRule.text_regex, line )
                if match_rule:
                    self.add_rule(SnortRule( match_rule, category, rule_path))
                    rule_count = rule_count + 1
        rules_file.close()
            
    def save(self, path=None, classtypes=None, categories=None, msgs=None):
        """
        Save rule set
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
        rule_path = os.path.split( path )[1]

        temp_file_name = file_name + ".tmp"
        rules_file = open( temp_file_name, "w" )
        category = "undefined"
        # ? order by category
        for rule in self.rules.values():
            if ( rule.get_enabled() == True ) and ( rule.path == rule_path ):
                if rule.category != category:
                    category = rule.category
                    rules_file.write("\n\n# ---- Begin " + category + " Rules Category ----#" + "\n\n")
                
                rules_file.write( rule.build() + "\n" )
        rules_file.close()
        
        if os.path.isfile( file_name ):
            os.remove( file_name )
        os.rename( temp_file_name, file_name )

    def add_rule(self, rule):
        """
        Add a new rule to the list and search for variables.
        """
        self.rules[rule.rule_id] = rule
                    
    def modify_rule(self, rule):
        """
        Alias for add_rule
        """
        self.add_rule(rule)

    def delete_rule(self, rule_id):
        """
        Remove rule.
        """
        del(self.rules[rule_id])

    def filter_group(self, profile, defaults_profile=None):
        """
        Filter rules for enabled/disabled
        """
        if profile["classtypes"] == "recommended":
            classtypes_selected = defaults_profile["activeGroups"]["classtypesSelected"]
        else:
            classtypes_selected = profile["classtypesSelected"]

        if profile["categories"] == "recommended":
            categories_selected = defaults_profile["activeGroups"]["categoriesSelected"]
        else:
            categories_selected = profile["categoriesSelected"]

        rule_ids_selected = []
        if "rulesSelected" in defaults_profile["activeGroups"]:
            rule_ids_selected = defaults_profile["activeGroups"]["rulesSelected"]
        elif "rulesSelected" in profile:
            rule_ids_selected = profile["rulesSelected"]

        for rid in self.rules:
            rule = self.rules[rid]
            if rule.match(classtypes_selected, categories_selected, rule_ids_selected) == False:
                rule.set_action(False, False)
            self.rules[rid] = rule

    def update(self, settings, conf, current_rules=None, previous_rules=None, preserve_action=True):
        """
        Determine differences in previous and current rules.
        If previous is not specified, then the difference will be just
        a populated added_rule_rids list.

        A happy side effect of only comparing old and new Snort rulesets
        is that custom rules are preserved (unless their rule identifiers
        conflict, of course).
        """

        #
        # Rule management
        #
        added_rule_rids = []
        deleted_rule_rids = []
        modified_rule_rids = []

        if previous_rules != None:
            #
            # Deleted rules: Those only in previous and not in current
            # 
            for rid in previous_rules.get_rules():
                if current_rules.get_rules().has_key(rid) == False:
                    deleted_rule_rids.append(rid)

        for rid in current_rules.get_rules():
            if previous_rules == None or previous_rules.get_rules().has_key(rid) == False:
                # 
                # New rules: Only in current
                #
                added_rule_rids.append(rid)
            elif previous_rules != None and ( current_rules.get_rules()[rid].build() != previous_rules.get_rules()[rid].build() ):
                #
                # Modified rules: In both but different
                # 
                modified_rule_rids.append(rid)

        #
        # Remove deleted rules
        # 
        for rid in deleted_rule_rids:
            if self.get_rules().has_key(rid):
                self.delete_rule(rid)

        #
        # Add/modify rules
        #
        for rid in added_rule_rids:
            if self.get_rules().has_key(rid):
                #
                # Replace modified rule
                # 
                new_rule = current_rules.get_rules()[rid]
                if preserve_action == True:
                    new_rule.enabled = self.get_rules()[rid].enabled
                    new_rule.action = self.get_rules()[rid].action
                self.modify_rule( new_rule )
            else:
                # 
                # Add new rule
                #
                self.add_rule( current_rules.get_rules()[rid] )

                # Variable management
                # Only interested in adding from current set.
                # Clearly we don't want to modify values and deletion could
                # be problematic if a custom rule is using it.
                # 
                for variable in current_rules.get_rules()[rid].get_variables():
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

        if ( len(added_rule_rids) > 0 and len(added_rule_rids) != len(current_rules.get_rules()) ) or len(modified_rule_rids) > 0 or len(deleted_rule_rids) > 0:
            #
            # Only record updated rule identifiers
            # if there was a change and that change
            # doesn't equal the number of initial rule population.
            #
            settings.set_updated({
                "rules": { 
                    "added" : added_rule_rids, 
                    "modified" : modified_rule_rids, 
                    "deleted": deleted_rule_rids
                }
            })


    def update_categories(self, defaults, sync_enabled = False):
        """
        Update category for each rule
        """
        categories = defaults.get_categories()

        #
        # Reset all to original
        #
        for rule_id in self.rules.keys():
            rule = self.get_rules()[rule_id]
            rule.set_category(defaults.get_original_category(rule.get_category()))
            self.modify_rule(rule)
        #
        # Modify to new
        #
        for category in categories:
            for rule_id in category["ids"]:
                if rule_id in self.rules.keys():
                    rule = self.get_rules()[rule_id]
                    rule.set_category(category["category"])
                    if sync_enabled == True:
                        rule.set_action(True,False)
                    self.modify_rule(rule)

    def format_category(self, category):
        category = category.replace("_","-")
        return category

    def get_rules(self):
        """
        Get rules
        """
        return self.rules

    def set_rules(self, rules):
        """
        Set rules
        """
        self.rules = rules
    
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
