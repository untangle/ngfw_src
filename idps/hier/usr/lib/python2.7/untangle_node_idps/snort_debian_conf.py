import errno
import inspect
import os
import re

class SnortDebianConf:
    #
    # Debian snort configuration file management
    #
    file_name = "/etc/snort/snort.debian.conf"
    
    # Regex parsing
    var_regex = re.compile(r'^DEBIAN_SNORT_([^\=]+)="([^\"]*)"')
    
    def __init__( self, _debug = False ):
        self._debug = _debug
        self.variables = {}
        self.load()
        self.load_variables()
        
    def load(self):
        self.conf = []
        conf_file = open( SnortDebianConf.file_name )
        for line in conf_file:
            self.conf.append( line.strip() )
        conf_file.close()

    def save(self):
        temp_file_name = SnortDebianConf.file_name + ".tmp"
        conf_file = open( temp_file_name, "w" )
        for line in self.conf:
            match_var = re.search( SnortDebianConf.var_regex, line )
            if match_var:
                line = "DEBIAN_SNORT_" + match_var.group(1) + "=" + '"' + self.variables[match_var.group(1)] + '"'
            if self._debug:
                print self.__class__.__name__ + ":" + inspect.stack()[0][3] + ":line=" + line
            conf_file.write( line + "\n" );
        conf_file.close()
        
        if os.path.isfile( SnortDebianConf.file_name ):
            os.remove( SnortDebianConf.file_name )
        os.rename( temp_file_name, SnortDebianConf.file_name )

    def load_variables(self):
        for line in self.conf:
            match_var = re.search( SnortDebianConf.var_regex, line )
            if match_var:
                self.variables[match_var.group(1)] = match_var.group(2)
                
    def get_variable( self, key ):
        return self.variables[key];
    
    def set_variable( self, key, value ):
        self.variables[key] = value;
