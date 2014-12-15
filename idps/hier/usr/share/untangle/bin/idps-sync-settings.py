#!/usr/bin/python
#
# Synchronize downloaded snort-formatted rules into idps settings.
#
import errno
import os
import getopt
import sys
import subprocess
import re
import json

import untangle_node_idps

def usage():
    print "usage..."
    print "help\t\tusage"
    print "settings\tSettings configuration file name"
    print "conf\t\tSnort configuration file name"
    print "rules\t\tSnort rule file name"
    print "node\t\tNode identifier"
    print "debug\t\tEnable debugging"
        
def main(argv):
    global _debug
    _debug = False
    rules_file_name = ""
    settings_file_name = ""
    nodeId = 0
	
    try:
		opts, args = getopt.getopt(argv, "hscrn:d", ["help", "settings=", "rules=", "nodeId=", "debug"] )
    except getopt.GetoptError:
    	usage()
    	sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-d", "--debug"):
             _debug = True
        elif opt in ( "-n", "--nodeId"):
            nodeId = arg
        elif opt in ( "-r", "--rules"):
            rules_file_name = arg
        elif opt in ( "-s", "--settings"):
            settings_file_name = arg

    if _debug == True:
        print "rules_file_name = " + rules_file_name
        print "settings_file_name = " + settings_file_name
        print "node = " + nodeId
        print "_debug = ",  _debug

    snort_conf = untangle_node_idps.SnortConf()
    path_filename = os.path.split( rules_file_name ); 
    snort_rules = untangle_node_idps.SnortRules( nodeId, path_filename[0], path_filename[1] )
    snort_rules.load()

    settings = untangle_node_idps.IdpsSettings( nodeId, settings_file_name )
    if settings.exists() == False:
        settings.create( snort_conf, snort_rules )
        settings.save()
    else:
        print "sync forthcoming"

    sys.exit()

if __name__ == "__main__":
	main( sys.argv[1:] )
