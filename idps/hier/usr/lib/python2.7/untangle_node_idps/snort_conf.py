import os
import re

class SnortConf:
    #
    # Snort configuration file management
    #
    file_name = "/etc/snort/snort.conf"
    
    # Regex parsing
    comment_regex = re.compile(r'^#\s*(.*)')
    var_regex = re.compile(r'^(ipvar|portvar|var)\s+([^\s+]+)\s+([^\s+]+)')
    include_regex = re.compile(r'^(\#|)\s*include ([^\s]+)')
    include_rulepath_regex = re.compile(r'\$(PREPROC_RULE_PATH|SO_RULE_PATH|RULE_PATH)')
    
    def __init__( self, _debug = False ):
        self.conf = []
        self.variables = []
        self.includes = []
        self.load()
        self.get_variables()
        self.get_includes()

    def load( self ):
        conf_file = open( SnortConf.file_name )
        self.last_comment = ""
        for line in conf_file:
            self.conf.append( line.rstrip( "\n" ) )
        conf_file.close()
        
    def save( self ):
        temp_file_name = SnortConf.file_name + ".tmp"
        conf_file = open( temp_file_name, "w" )
        self.save_variables()
        self.save_includes()
        for line in self.conf:
            conf_file.write( line + "\n" );
        conf_file.close()
        
        if os.path.isfile( SnortConf.file_name ):
            os.remove( SnortConf.file_name )
        os.rename( temp_file_name, SnortConf.file_name )
        
    def save_variables( self ):
        saved = {}
        
        for variable in self.variables:
            saved[variable["key"]] = False

        last_ip_or_port_var_position = 0
        
        for i,line in enumerate( self.conf ):
            match_var = re.search( SnortConf.var_regex, line )
            if match_var:
                for variable in self.variables:
                    if match_var.group(1) == "ipvar" or match_var.group(1) == "portvar":
                        last_ip_or_port_var_position = i
                        
                    if variable["key"] == match_var.group(2):
                        self.conf[i] = match_var.group(1) + " " + match_var.group(2) + " " + variable["value"]
                        saved[match_var.group(2)] = True
                        
        for variable in self.variables:
            if saved[variable["key"]] == False:
                self.conf[last_ip_or_port_var_position] = self.conf[last_ip_or_port_var_position] + "\n" + variable["type"] + " " + variable["key"] + " " + variable["value"] 
            
    def save_includes( self ):
        saved = {}
        last_positions = {}
        
        for include in self.includes:
            saved[include["file_name"]] = False
            last_positions[include["path"]] = 0
            
        for i,line in enumerate( self.conf ):
            match_var = re.search( SnortConf.include_regex, line )
            if match_var:
                for include in self.includes:
                    if include["file_name"] == match_var.group(2):
                        last_positions[include["path"]] = i
                        
                        prefix = "include "
                        if include["enabled"] == False:
                            prefix = "#" + prefix
                            
                        self.conf[i] = prefix + " " + match_var.group(2)
                        saved[match_var.group(2)] = True
                        
        for include in self.includes:
            # after last rule path
            if saved[include["file_name"]] == False:
                prefix = "include "
                if include["enabled"] == False:
                    prefix = "#" + prefix
                            
                self.conf[last_positions[include["path"]]] = self.conf[last_positions[include["path"]]] + "\n" + prefix + include["file_name"]
        
    def get_variables(self):
        #
        # Pull default snort variable names, values, and descriptions.
        # Used to build up the default settings variable list.
        #
        # Based on analysis of downloaded rules, they only reference variables 
        # defined in the snort.conf template so that's why we are interested in
        # them (and not modifying them!)
        #
        if self.variables.count(self) == 0:
            for line in self.conf:
                match_comment = re.search( SnortConf.comment_regex, line )
                if match_comment:
                    self.last_comment = match_comment.group(1)
                match_var = re.search( SnortConf.var_regex, line )
                if match_var:
                    self.set_variable( match_var.group(2), match_var.group(3), match_var.group(1), self.last_comment )
        return self.variables

    def get_variable(self,key):
        if self.variables.count(self) == 0:
            self.get_variables()
        for variable in self.variables:
            if variable["key"] == key:
                return variable["value"]

    def set_variable( self, key, value, type="var", description="" ):
        ## type "guess" logic:
        ## look like an IP address = ipvar, else port
        ## reference another known type, use that type
        variable_modified = False
        for variable in self.variables:
            if variable["key"] == key:
                variable["value"] = value
                variable_modified = True
        if variable_modified == False:
            self.variables.append({
                "key": key,
                "value": value,
                "type": type,
                "description": description
            })
                        
    def get_includes(self):
        if self.includes.count(self) == 0:
            for line in self.conf:
                match_include = re.search( SnortConf.include_regex, line )
                if match_include:
                    if match_include.group(1) == "#":
                        enabled = False
                    else:
                        enabled = True
                    
                    self.set_include( match_include.group(2), enabled )

        return self.includes
    
    def set_include( self, file_name, enabled = True ):
        include_modified = False
        for include in self.includes:
            if include["file_name"] == file_name:
                include["enabled"] = enabled
                include_modified = True
        if include_modified == False:
            split = os.path.split( file_name )
            self.includes.append({
                "path": split[0],
                "base_name": split[1],
                "file_name": file_name,
                "enabled": enabled
            })
