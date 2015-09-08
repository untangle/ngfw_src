"""
Snort configuration file management
"""
import os
import re

class SnortConf:
    """
    Snort configuration file management
    """
    file_name = "/etc/snort/snort.conf"
    
    # Regex parsing
    comment_regex = re.compile(r'^#\s*(.*)')
    var_regex = re.compile(r'^(ipvar|portvar|var)\s+([^\s+]+)\s+([^\s+]+)')
    include_regex = re.compile(r'^(\#|)\s*include\s+([^\s]+)')
    include_rulepath_regex = re.compile(r'\$(PREPROC_RULE_PATH|SO_RULE_PATH|RULE_PATH)')
    output_regex = re.compile(r'^output\s+([^:]+)')
    preprocessor_normalize_tcp_regex = re.compile(r'^(#|).*preprocessor normalize_tcp: ips ecn stream')
    preprocessor_sfportscan_regex = re.compile(r'(#|).*preprocessor sfportscan:')
    
    def __init__(self, _debug=False):
        self.last_comment = ""
        self.conf = []
        self.variables = []
        self.includes = []
        self.load()
        self.get_variables()
        self.get_includes()

    def load(self):
        """
        Load snort configuration
        """
        conf_file = open( SnortConf.file_name )
        for line in conf_file:
            self.conf.append( line.rstrip( "\n" ) )
        conf_file.close()
        
    def save(self):
        """
        Save snort configuration
        """
        temp_file_name = SnortConf.file_name + ".tmp"
        conf_file = open( temp_file_name, "w" )
        self.save_variables()
        self.save_includes()
        self.save_output()
        self.save_set_options()
        for line in self.conf:
            conf_file.write( line + "\n" )
        conf_file.close()

        backup_file_name = SnortConf.file_name + ".bak"
        if os.path.isfile(backup_file_name):        
            os.remove(backup_file_name)
        os.rename(SnortConf.file_name,backup_file_name)

        if os.path.getsize(temp_file_name) != 0:
            os.rename(temp_file_name, SnortConf.file_name)
            os.remove(backup_file_name)
        else:
            os.rename(backup_file_name,SnortConf.file_name)
        
    def save_variables(self):
        """
        Save snort variables
        """
        saved = {}
        
        for variable in self.variables:
            saved[variable["key"]] = False

        last_ip_or_port_var_position = 0
        
        for i, line in enumerate( self.conf ):
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
            
    def save_includes(self):
        """
        Save snort includes
        """
        saved = {}
        last_positions = {}
        
        for include in self.includes:
            saved[include["file_name"]] = False
            last_positions[include["path"]] = 0
            
        for i, line in enumerate( self.conf ):
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

    def save_output(self):
        """
        Save snort output directives
        """
        unified_found = False
        last_output_position = 0
        for i, line in enumerate( self.conf ):
            match_output = re.search( SnortConf.output_regex, line )
            if match_output:
                last_output_position = i
                
                if match_output.group(1) == "unified2":
                    unified_found = True
                else:
                    self.conf[i] = "#" + self.conf[i]
        
        if unified_found == False:
            self.conf[last_output_position] = self.conf[last_output_position] + "\n" + "output unified2: filename snort.log,limit 128, mpls_event_types, vlan_event_types"
                
    def save_set_options(self):
        """
        Save miscellaneous snort directives
        """
        for i, line in enumerate( self.conf ):
            match_output = re.search( SnortConf.preprocessor_normalize_tcp_regex, line )
            if match_output:
                if match_output.group(1) == "":
                    self.conf[i] = "#" + self.conf[i]

            match_output = re.search( SnortConf.preprocessor_sfportscan_regex, line )
            if match_output:
                self.conf[i] = "preprocessor sfportscan: proto  { all } memcap { 10000000 } sense_level { medium }"

    def get_variables(self):
        """
        Pull default snort variable names, values, and descriptions.
        Used to build up the default settings variable list.
        
        Based on analysis of downloaded rules, they only reference variables 
        defined in the snort.conf template so that's why we are interested in
        them (and not modifying them!)
        """
        if self.variables.count(self) == 0:
            for line in self.conf:
                match_comment = re.search( SnortConf.comment_regex, line )
                if match_comment:
                    self.last_comment = match_comment.group(1)
                match_var = re.search( SnortConf.var_regex, line )
                if match_var:
                    if match_var.group(2) == "HOME_NET":
                        continue
                    self.set_variable( match_var.group(2), match_var.group(3), match_var.group(1), self.last_comment )
        return self.variables

    def get_variable(self, key):
        """
        Get variables
        """
        if self.variables.count(self) == 0:
            self.get_variables()
        for variable in self.variables:
            if variable["key"] == key:
                return variable["value"]

    def set_variable(self, key, value, type="var", description=""):
        """
        type "guess" logic:
        look like an IP address = ipvar, else port
        reference another known type, use that type
        """
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
        """
        Get include list
        """
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
    
    def set_include(self, file_name, enabled = True):
        """
        Set includes
        """
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
