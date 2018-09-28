"""
Suricata configuration file management
"""
import os
import re
#import yaml
import sys
import ruamel.yaml
from ruamel.yaml.compat import text_type

class SuricataConf:
    """
    Suricata configuration file management
    """
    file_name = "/etc/suricata/suricata.yaml"
    
    def __init__(self, _debug=False):
        self.last_comment = ""
        self.conf = []
        self.variables = []
        self.includes = []
        self.load()
        # self.get_variables()
        # self.get_includes()

        self.setup_yaml()

    def setup_yaml(self):
        def represent_bool(yaml_self, data):
            if data:
                value = u'yes'
            else:
                value = u'no'
            return yaml_self.represent_scalar(u'tag:yaml.org,2002:bool', text_type(value))

        ruamel.yaml.emitter.Emitter.write_comment_orig = ruamel.yaml.emitter.Emitter.write_comment
        def strip_empty_lines_write_comment(self, comment):
            comment.value = comment.value.replace('\n', '')
            if comment.value:
                self.write_comment_orig(comment)

        ruamel.yaml.representer.RoundTripRepresenter.add_representer(bool, represent_bool)
        ruamel.yaml.emitter.Emitter.write_comment = strip_empty_lines_write_comment

    def load(self):
        """
        Load suricata configuration
        """
        # conf_file = open( SuricataConf.file_name )
        # for line in conf_file:
        #     self.conf.append( line.rstrip( "\n" ) )
        # conf_file.close()
        with open(SuricataConf.file_name, 'r') as stream:
            try:
                # self.conf = yaml.load(stream)
                self.conf = ruamel.yaml.load(stream, ruamel.yaml.RoundTripLoader, preserve_quotes=True )
            except ruamel.yaml.YAMLError as yaml_error:
                print(yaml_error)
        
    def save(self):
        """
        Save suricata configuration
        """
        temp_file_name = SuricataConf.file_name + ".tmp"
        #conf_file = open( temp_file_name, "w" )
        # self.save_variables()
        # self.save_includes()
        # self.save_output()
        # self.save_set_options()
        # for line in self.conf:
        #     conf_file.write( line + "\n" )
        # conf_file.close()
        with open(temp_file_name, 'w') as stream:
            try:
                #yaml.dump(self.conf, stream, default_flow_style=False)
                ruamel.yaml.dump(self.conf, stream=stream, Dumper=ruamel.yaml.RoundTripDumper, version=(1,1), explicit_start=True)
                #, Dumper=ruamel.yaml.RoundTripDumper)
            except ruamel.yaml.YAMLError as yaml_error:
                print(yaml_error)

        backup_file_name = SuricataConf.file_name + ".bak"
        if os.path.isfile(backup_file_name):        
            os.remove(backup_file_name)
        os.rename(SuricataConf.file_name,backup_file_name)

        if os.path.getsize(temp_file_name) != 0:
            os.rename(temp_file_name, SuricataConf.file_name)
            os.remove(backup_file_name)
        else:
            os.rename(backup_file_name,SuricataConf.file_name)

    def save_variables(self):
        """
        Save suricata variables
        """
        saved = {}
        
        for variable in self.variables:
            saved[variable["key"]] = False

        last_ip_or_port_var_position = 0
        
        for i, line in enumerate( self.conf ):
            match_var = re.search( SuricataConf.var_regex, line )
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
            
    # def save_includes(self):
    #     """
    #     Save suricata includes
    #     """
    #     saved = {}
    #     last_positions = {}
        
    #     for include in self.includes:
    #         saved[include["file_name"]] = False
    #         last_positions[include["path"]] = 0
            
    #     for i, line in enumerate( self.conf ):
    #         match_var = re.search( SuricataConf.include_regex, line )
    #         if match_var:
    #             for include in self.includes:
    #                 if include["file_name"] == match_var.group(2):
    #                     last_positions[include["path"]] = i
                        
    #                     prefix = "include "
    #                     if include["enabled"] == False:
    #                         prefix = "#" + prefix
                            
    #                     self.conf[i] = prefix + " " + match_var.group(2)
    #                     saved[match_var.group(2)] = True
                        
    #     for include in self.includes:
    #         # after last signature path
    #         if saved[include["file_name"]] == False:
    #             prefix = "include "
    #             if include["enabled"] == False:
    #                 prefix = "#" + prefix
                            
    #             self.conf[last_positions[include["path"]]] = self.conf[last_positions[include["path"]]] + "\n" + prefix + include["file_name"]

    # def save_output(self):
    #     """
    #     Save suricata output directives
    #     """
    #     unified_found = False
    #     last_output_position = 0
    #     for i, line in enumerate( self.conf ):
    #         match_output = re.search( SuricataConf.output_regex, line )
    #         if match_output:
    #             last_output_position = i
                
    #             if match_output.group(1) == "unified2":
    #                 unified_found = True
    #             else:
    #                 self.conf[i] = "#" + self.conf[i]
        
    #     if unified_found == False:
    #         self.conf[last_output_position] = self.conf[last_output_position] + "\n" + "output unified2: filename suricata.log,limit 128, mpls_event_types, vlan_event_types"
                
    # def save_set_options(self):
    #     """
    #     Save miscellaneous suricata directives
    #     """
    #     for i, line in enumerate( self.conf ):
    #         match_output = re.search( SuricataConf.preprocessor_normalize_tcp_regex, line )
    #         if match_output:
    #             if match_output.group(1) == "":
    #                 self.conf[i] = "#" + self.conf[i]

    #         match_output = re.search( SuricataConf.preprocessor_sfportscan_regex, line )
    #         if match_output:
    #             self.conf[i] = "preprocessor sfportscan: proto  { all } memcap { 10000000 } sense_level { medium }"

    def get_variables(self):
        """
        Pull default suricata variable names, values, and descriptions.
        Used to build up the default settings variable list.
        
        Based on analysis of downloaded signatures, they only reference variables 
        defined in the suricata.conf template so that's why we are interested in
        them (and not modifying them!)
        """
        # if self.variables.count(self) == 0:
        #     # for line in self.conf:
        #     #     match_comment = re.search( SuricataConf.comment_regex, line )
        #     #     if match_comment:
        #     #         self.last_comment = match_comment.group(1)
        #     #     match_var = re.search( SuricataConf.var_regex, line )
        #     #     if match_var:
        #     #         if match_var.group(2) == "HOME_NET":
        #     #             continue
        #     #         self.set_variable( match_var.group(2), match_var.group(3), match_var.group(1), self.last_comment )
        #     for key in self.conf:
        #         if key == "vars":
        #             print type(self.conf[key])
        #             if type(self.conf[key]) is dict:
        #                 for subkey in self.conf[key]:
        #                     print "\t" + subkey
        return self.conf["vars"]

    def get_variable(self, key):
        """
        Get variables
        """
        print self.conf["vars"]
        for group in self.conf["vars"]:
            for variable in self.conf["vars"][group]:
                 if variable == key:
                    return self.conf["vars"][group][variable]
        return None

    def set_variable(self, key, value):
        """
        type "guess" logic:
        look like an IP address = ipvar, else port
        reference another known type, use that type
        """
        for group in self.conf["vars"]:
            for variable in self.conf["vars"][group]:
                 if variable == key:
                    self.conf["vars"][group][variable] = value

    def set_config_from_path(self, path, value, config=None):
        """Set configuration from path
        
        From a list path, walk down config to set the value.
        
        Arguments:
            path {[list]} -- Path to walk
            value {[dict]} -- value to set last element
        
        Keyword Arguments:
            config {[type]} -- Current position in configuration (default: {None})
        """
        if config is None:
            config = self.conf

        for key in path:
            if issubclass(type(config), list):
                for item in config:
                    self.set_config_from_path(path, value, item)
            elif issubclass(type(config), dict):
                if key in config:
                    if len(path) == 1:
                        config[key] = value
                    else:
                        if config[key] == None and len(path[1:]) == 1:
                            config[key] = {}
                            # Bizzare hack for retaining nfq comments.
                            # The first two items refer to the comment prefix.
                            # The last two are comments after the key.  When adding a dict,
                            # the last two are duplicated.
                            if key in self.conf.ca.items and len(self.conf.ca.items[key]) == 4:
                                self.conf.ca.items[key] = self.conf.ca.items[key][:2]
                        self.set_config_from_path(path[1:], value, config[key])
                else:
                    if len(path) == 1:
                        config[key] = value

    def set(self, config_path, read_path=None):
        """Set configuration from path

        Build path and value used by set_config_from_path.        
        
        Arguments:
            config_path {[dict]} -- Recursive dict where entry is a key pair.
        
        Keyword Arguments:
            read_path {[list]} -- Currend read_path list (default: {None})
        """
        if read_path is None:
            read_path = []

        for key in config_path:
            read_path.append(key)
            if type(config_path[key]) is dict:
                self.set(config_path[key], read_path)
            else:
                self.set_config_from_path(read_path, config_path[key])
            read_path.remove(key)
