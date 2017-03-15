"""
Snort debian conf 
"""
import inspect
import os
import re

class SnortDebianConf:
    """
    Debian snort configuration file management
    """
    file_name = "/etc/snort/snort.debian.conf"
    
    # Regex parsing
    var_regex = re.compile(r'^DEBIAN_SNORT_([^\=]+)="([^\"]*)"')
    
    def __init__(self, _debug=False):
        self._debug = _debug
        self.conf = []
        self.variables = {}
        self.load()
        self.load_variables()
        
    def load(self):
        """
        Load snort debian conf
        """
        self.conf = []
        conf_file = open( SnortDebianConf.file_name )
        for line in conf_file:
            self.conf.append( line.strip() )
        conf_file.close()

    def save(self):
        """
        Save snort debian conf
        """
        temp_file_name = SnortDebianConf.file_name + ".tmp"
        conf_file = open( temp_file_name, "w" )
        for line in self.conf:
            match_var = re.search( SnortDebianConf.var_regex, line )
            if match_var:
                line = "DEBIAN_SNORT_" + match_var.group(1) + "=" + '"' + self.variables[match_var.group(1)] + '"'
            if self._debug:
                print self.__class__.__name__ + ":" + inspect.stack()[0][3] + ":line=" + line
            conf_file.write( line + "\n" )
        conf_file.close()
        
        backup_file_name = SnortDebianConf.file_name + ".bak"
        if os.path.isfile(backup_file_name):        
            os.remove(backup_file_name)
        os.rename(SnortDebianConf.file_name,backup_file_name)

        if os.path.getsize(temp_file_name) != 0:
            os.rename(temp_file_name, SnortDebianConf.file_name)
            os.remove(backup_file_name)
        else:
            os.rename(backup_file_name,SnortDebianConf.file_name)

    def load_variables(self):
        """
        Load variables
        """
        for line in self.conf:
            match_var = re.search( SnortDebianConf.var_regex, line )
            if match_var:
                self.variables[match_var.group(1)] = match_var.group(2)
                
    def get_variable(self, key):
        """
        Get single variable
        """
        return self.variables[key]
    
    def set_variable(self, key, value):
        """
        Set variables
        """
        self.variables[key] = value
