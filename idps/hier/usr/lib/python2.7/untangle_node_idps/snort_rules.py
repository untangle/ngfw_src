"""
Snort rule set management
"""
import os
import re

from untangle_node_idps.snort_rule import SnortRule

class SnortRules:
    """
    Process a set of snort rules such as downloaded rules.
    """
    var_regex = re.compile(r'^\$(.+)')
    category_regex = re.compile(r'^# \-+ Begin (.+) Rules Category')

    rule_paths = ["rules", "preproc_rules"]
    
    def __init__(self, node_id = 0, path = "", file_name = ""):
        self.node_id = node_id
        self.path = path
        self.file_name = self.path + "/"
        if file_name != "":
            self.file_name = self.file_name + file_name
        else:
            self.file_name = self.file_name + "node_" + self.node_id + ".rules"
        
        self.rules = {}
        self.variables = []

    def load(self, path = False):
        """
        Load ruleset
        """
        if path == True:
            """
            Parse directory trees
            """
            for rule_path in SnortRules.rule_paths:
                parse_path = self.path + "/" + rule_path 
                for file_name in os.listdir( parse_path ):
                    extension = os.path.splitext( file_name )[1]
                    if extension != ".rules":
                        continue
                    self.load_file( parse_path + "/" + file_name, rule_path )
        else:
            self.load_file( self.file_name )
            
    def load_file( self, file_name, rule_path = "rules"):
        """
        Category based on "major" file name separator. 
        e.g., web-cgi = web
        """
        name = os.path.split( file_name )[1]
        name = os.path.splitext( name )[0]
        category = name

        rule_count = 0
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
            
    def check_write_rule(self, rule, classtypes, categories, msgs):
        """
        Determine if rule should be enabled
        """
        if len(classtypes) == 0 or rule.options["classtype"] in classtypes:
            classtype_match = True
        else:
            classtype_match = False
        
        if len(categories) == 0 or rule.category in categories:
            category_match = True
        else:
            category_match = False

        if len(msgs) == 0:
            msgs_match = True
        else:
            msgs_match = False
        
        for msg_substring in msgs:
            if rule.options["msg"].lower().find( msg_substring.lower() ) != -1:
                msgs_match = True
                break
        
        return classtype_match and category_match and msgs_match
        
    def save(self, path = None, classtypes = None, categories = None, msgs = None):
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

        file_name = path + "/" + "node_" + self.node_id + ".rules"
        rule_path = os.path.split( path )[1]

        temp_file_name = file_name + ".tmp"
        rules_file = open( temp_file_name, "w" )
        category = "undefined"
        # ? order by category
        for rule in self.rules.values():
            if self.check_write_rule( rule, classtypes, categories, msgs ) == False:
                continue

            if ( rule.get_enabled() == True ) and ( rule.path == rule_path ):
                if rule.category != category:
                    category = rule.category
                    rules_file.write( "\n\n# ---- Begin " + category +" Rules Category ----#" + "\n\n")
                
                rules_file.write( rule.build() + "\n" )
        rules_file.close()
        
        if os.path.isfile( file_name ):
            os.remove( file_name )
        os.rename( temp_file_name, file_name )

    def add_rule(self, rule):
        """
        Add a new rule to the list and search for variables.
        """
        self.rules[rule.options["sid"] + "_" + rule.options["gid"]] = rule
        for prop, value in vars(rule).iteritems():
            if isinstance( value, str ) == False:
                continue
            match_variable = re.search( SnortRules.var_regex, value )
            if match_variable:
                if self.variables.count( match_variable.group( 1 ) ) == 0:
                    self.variables.append( match_variable.group( 1 ) )
        for key in rule.options.keys():
            value = rule.options[key]
            if isinstance( value, str ) == False:
                continue
            match_variable = re.search( SnortRules.var_regex, value )
            if match_variable:
                if self.variables.count( match_variable.group( 1 ) ) == 0:
                    self.variables.append( match_variable.group( 1 ) )
                    
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
