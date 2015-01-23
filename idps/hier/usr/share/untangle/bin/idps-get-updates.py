#!/usr/bin/python
##
## Get rule updates
##
import errno
import getopt
import json
import os
import re
import sys
import shutil
import subprocess
import tarfile
import urllib2

import uvm
from uvm import Manager
from uvm import NodeManager
from uvm import Uvm

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)

Debug = False
Chunk_size = 1024 * 1024

##
## 
##
class Update:
    debug = False
    errors = []
    settings_file_name_regex = re.compile(r'^settings_(\d+).js$');

    ##
    ## Initialize variables
    ##
    def __init__( self, base_directory, url ):
        self.debug = Debug
        self.base_directory = base_directory
        self.working_directory = self.base_directory + "/update"
        self.rules_working_directory = self.working_directory + "/new"
        self.url = url
        self.rules_file_name = self.url.split( "/" )[-1].split("?")[0]
        self.url_file_name = self.working_directory + "/" + self.rules_file_name
        self.live_rules_file_name = self.base_directory + "/" + self.rules_file_name

    ##
    ## Setup working directory
    ##
    def setup( self ):
        if self.debug == True:
            print ":".join( [self.__class__.__name__ , sys._getframe().f_code.co_name, "cleanup and create work directories" ] )
            
        if os.path.isdir( self.working_directory ) == True:
            try:
                shutil.rmtree( self.working_directory )
            except:
                self.errors.append( {
			        'msg': "Cannot remove existing working directory=" + self.working_directory,
			        'error': "\n".join( str(v) for v in sys.exc_info() )
		        })
                return False
            
        try:
            os.makedirs( self.working_directory )
        except:
            self.errors.append( {
                'msg': "Cannot create working directory=" + self.working_directory,
                'error': "\n".join( str(v) for v in sys.exc_info() )
            })
            return False
        
        return True

    ##
    ## Download
    ##
    def download( self ):
        if self.debug == True:
            print ":".join( [self.__class__.__name__ , sys._getframe().f_code.co_name, "get rule set" ] )
            
        try:
            u = urllib2.urlopen( self.url )
        except:
            self.errors.append( {
		        'msg': "Cannot open url=" + self.url,
			    'error': "\n".join( str(v) for v in sys.exc_info() )
            })
            return False

        if os.path.isfile( self.live_rules_file_name  ) == True:
            live_rules_file_size = os.path.getsize( self.live_rules_file_name )
        else:
            live_rules_file_size = 0
        
        meta = u.info()
        headers = meta.getheaders( "Content-length" )
        if len(headers) == 0:
            self.errors.append( {
			    'msg': "Get headers",
			    'error': "Unable to get Content-Length header"
		    })
            return False
        url_file_size = int( headers[0] )
        if url_file_size == 0:
            self.errors.append( {
			    'msg': "Content length",
			    'error': "Content length is 0"
		    })
            return False

        if live_rules_file_size == url_file_size:
            if self.debug:
                print ":".join( [self.__class__.__name__ , sys._getframe().f_code.co_name, "live and url sizes are the same=" + str( live_rules_file_size ) ] )
            return False
        
        try:
            f = open( self.url_file_name, 'wb' )
        except:
            self.errors.append( {
		        'msg': "Cannot create url_file_name=" + url_file_name,
			    'error': "\n".join( str(v) for v in sys.exc_info() )
            })
            return False
        
        url_bytes_read = 0
        while url_bytes_read < url_file_size:
            try:
                data = u.read( Chunk_size )
            except:
                self.errors.append( {
		            'msg': "Cannot read content at " + str( url_bytes_read ),
			        'error': "\n".join( str(v) for v in sys.exc_info() )
                })
                return False

            url_bytes_read += len(data)

            try:
                f.write(data)
            except:
                self.errors.append( {
		            'msg': "Cannot write content at " + str( url_bytes_read ),
			        'error': "\n".join( str(v) for v in sys.exc_info() )
                })
                return False
        f.close()
        return True

    ##
    ## Extract and perform any validation
    ##
    def validate( self ):
        if self.debug == True:
            print ":".join( [self.__class__.__name__ , sys._getframe().f_code.co_name, "extract rules" ] )
            
        try:
            t = tarfile.open( self.url_file_name )
            t.extractall( path = self.rules_working_directory )
            t.close()
        except:
            self.errors.append( {
                'msg': "Unable to extract downloaded file=" + url_file_name + " to current directory=" + current_directory,
                'error': "\n".join( str(v) for v in sys.exc_info() )
            })
            return False
        
        return True
    
    ##
    ## Install to live
    ##
    def install( self ):
        if self.debug == True:
            print ":".join( [self.__class__.__name__ , sys._getframe().f_code.co_name, "move to live" ] )
            
        live_new_directory =  self.base_directory + "/new"
        live_previous_directory =  self.base_directory + "/previous"
        live_current_directory =  self.base_directory + "/current"
            
        ## Copy to live directory as "new"
        if os.path.isdir( live_new_directory ) == True:
            try:
                shutil.rmtree( live_new_directory )
            except:
                self.errors.append( {
			        'msg': "Cannot remove existing live new directory=" + live_new_directory,
			        'error': "\n".join( str(v) for v in sys.exc_info() )
		        })
                return False
            
        try:
            shutil.copytree( self.rules_working_directory, live_new_directory )
        except:
            self.errors.append( {
                'msg': "Unable to copy tree from working directory " + self.rules_working_directory + " to live directory " + live_new_directory,
                'error': "\n".join( str(v) for v in sys.exc_info() )
            })
            return False
        
        ## remove previous
        if os.path.isdir( live_previous_directory ) == True:
            try:
                shutil.rmtree( live_previous_directory )
            except:
                self.errors.append( {
			        'msg': "Cannot remove existing live previous directory=" + live_previous_directory,
			        'error': "\n".join( str(v) for v in sys.exc_info() )
		        })
                return False
        
        ## rename current to previous
        if os.path.isdir( live_current_directory ) == True:
            try:
                shutil.move( live_current_directory, live_previous_directory )
            except:
                self.errors.append( {
			        'msg': "Cannot rename existing current directory " + live_current_directory + " to previous " + live_previous_directory,
			        'error': "\n".join( str(v) for v in sys.exc_info() )
		        })
                return False
            
        ## rename current.new to current
        try:
            shutil.move( live_new_directory, live_current_directory )
        except:
            self.errors.append( {
                'msg': "Cannot rename existing new directory " + live_new_directory + " to currret " + live_current_directory,
                'error': "\n".join( str(v) for v in sys.exc_info() )
            })
            return False

        # move tarball to live
        if os.path.isfile( self.live_rules_file_name ) == True:
            try:
                os.remove( self.live_rules_file_name )
            except:
                self.errors.append( {
			        'msg': "Cannot remove existing live rules file=" + self.live_rules_file_name,
			        'error': "\n".join( str(v) for v in sys.exc_info() )
		        })
                return False
        
        try:
            shutil.copyfile( self.url_file_name, self.live_rules_file_name )
        except:
            self.errors.append( {
                'msg': "Cannot copy new rules file " + self.url_file_name + " to live " + self.live_rules_file_name,
                'error': "\n".join( str(v) for v in sys.exc_info() )
            })
            return False
        
        return True

    ##
    ## Synchronize configuration
    ##
    def synchronize( self ):
        nodeIds = []
        uvmContext = Uvm().getUvmContext( hostname="localhost", username=None, password=None, timeout=60 )
        nodeManager = uvmContext.nodeManager()
        
        ##
        ## Easier to parse the api for instances than reproducing what 
        ## it does with object.
        ##
        node_manager = NodeManager(uvmContext)
        for instance in node_manager.get_instances():
            if instance[1] == "untangle-node-idps":
                nodeIds.append(instance[0])
        
        for nodeId in nodeIds:
            node = nodeManager.node(nodeId)
            
            temp_settings_file_name = "/tmp/" + str(nodeId) + ".js"
            ## Syncronize
            args = [
                "@PREFIX@/usr/share/untangle/bin/idps-sync-settings.py",
                "--nodeId", str(nodeId),
                "--previous_rules", "/usr/share/untangle-snort-config/previous/rules",
                "--rules", "/usr/share/untangle-snort-config/current/rules",
                "--settings", temp_settings_file_name
            ]
            try: 
                process = subprocess.Popen( args )
                process.wait()
            except:
                self.errors.append( {
                    'msg': "Unable to run sync script with arguments = " + ",".join(args),
                    'error': "\n".join( str(v) for v in sys.exc_info() )
                })
                return False
            
            node.saveSettings( temp_settings_file_name )
            node.setUpdatedSettingsFlag( True )
            
            ## Reconfigure snort
            args = [
                "@PREFIX@/usr/share/untangle/bin/idps-create-config.py",
                "--nodeId", str( nodeId ),
                "--iptablesScript", "/etc/untangle-netd/iptables-rules.d/740-snort"
            ]
            try: 
                process = subprocess.Popen( args )
                process.wait()
            except:
                self.errors.append( {
                    'msg': "Unable to run config creation script with arguments = " + ",".join(args),
                    'error': "\n".join( str(v) for v in sys.exc_info() )
                })
                return False

            node.stop()
            node.start()
        return True

def usage():
    print "usage"
    print "help\t\tusage"
    print "rules_template_directory\t\tSnort rule template directory"
        
def main(argv):
    global Debug
    rules_template_directory = ""
    url = "https://ids.untangle.com/snortrules.tar.gz"
	
    try:
		opts, args = getopt.getopt(argv, "hru:d", ["help", "rules_template_directory=", "url=", "debug"] )
    except getopt.GetoptError:
    	usage()
    	sys.exit(2)
        
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-d", "--debug"):
             Debug = True
        elif opt in ( "-r", "--rules_template_directory"):
            rules_template_directory = arg
        elif opt in ( "-r", "--url"):
            url = arg

    if Debug == True:
        print "rules_template_directory = " + rules_template_directory
        print "url = " + url

    update = Update( rules_template_directory, url )
    if update.setup() == True and update.download() == True and update.validate() and update.install():
        update.synchronize()
        
    if len( update.errors ):
        print update.errors
    sys.exit()

if __name__ == "__main__":
	main( sys.argv[1:] )
