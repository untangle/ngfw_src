import os
import re

from untangle_node_idps.snort_rule import SnortRule

class SnortRules:
    #
    # Process a set of snort rules such as downloaded rules.
    #
    var_regex = re.compile(r'^\$(.+)')
    category_regex = re.compile(r'^# \-+ Begin (.+) Rules Category')
    
    def __init__( self, nodeId = 0, path = "", file_name = "" ):
        self.nodeId = nodeId
        self.path = path
        self.file_name = self.path + "/";
        if file_name != "":
            self.file_name = self.file_name + file_name
        else:
            self.file_name = self.file_name + "node_" + self.nodeId + ".rules"
        
        self.rules = []
        self.variables = []
        
    def load(self):
        category = "undefined"
        
        rules_file = open( self.file_name )
        for line in rules_file:
            match_category = re.search( SnortRules.category_regex, line )
            if match_category:
                category = match_category.group(1)
            else:            
                match_rule = re.search( SnortRule.text_regex, line )
                if match_rule:
                    self.addRule( SnortRule( match_rule, category ) )
        rules_file.close()
        
    def save(self):
        temp_file_name = self.file_name + ".tmp"
        rules_file = open( temp_file_name, "w" )
        category = "undefined"
        # ? order by category
        for rule in self.rules:
            if rule.category != category:
                category = rule.category
                rules_file.write( "\n\n# ---- Begin " + category +" Rules Category ----#" + "\n\n")
            rules_file.write( rule.build() + "\n" );
        rules_file.close()
        
        if os.path.isfile( self.file_name ):
            os.remove( self.file_name )
        os.rename( temp_file_name, self.file_name )

    def addRule( self, rule ):
        #
        # Add a new rule to the list and search for variables.
        #
        self.rules.append( rule )
        for property, value in vars(rule).iteritems():
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
        return self.rules
    
    def get_variables(self):
        return self.variables

    def get_file_name( self ):
        return self.file_name
