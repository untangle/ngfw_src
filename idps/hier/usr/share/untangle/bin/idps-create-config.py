#!/usr/bin/python
import os
import getopt
import sys
import subprocess
import re
import json

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)
	
import untangle_node_idps

def usage():
	print "usage"

def main(argv):
    global _debug
    _debug = False
    settings_file = ""
    nodeId = 0
    classtypes = []
    categories = []
    msgs = []
	
    try:
		opts, args = getopt.getopt(argv, "hsinca:d", ["help", "nodeId=", "classtypes=", "categories=", "msgs=", "debug"] )
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
        elif opt in ( "-c", "--classtypes"):
            classtypes = arg.split(",")
        elif opt in ( "-a", "--categories"):
            categories = arg.split(",")
        elif opt in ( "-m", "--msgs"):
            msgs = arg.split(",")

    if _debug == True:
		print "nodeId = " + nodeId
		print "_debug = ",  _debug

    settings = untangle_node_idps.IdpsSettings( nodeId )
    if settings.exists() == False:
        print "cannot find settings file"
        sys.exit()
    settings.load()

    snort_conf = untangle_node_idps.SnortConf( _debug=_debug )

    rules = settings.get_rules()
    rules.set_path( snort_conf.get_variable( "RULE_PATH" ) )
    rules.save( classtypes, categories, msgs )
    
    idps_event_map = untangle_node_idps.IdpsEventMap( rules )
    idps_event_map.save()
	
    for settings_variable in settings.get_variables():
        snort_conf.set_variable( settings_variable["variable"], settings_variable["definition"] )

    for include in snort_conf.get_includes():
		match_include_rule = re.search( untangle_node_idps.SnortConf.include_rulepath_regex, include["file_name"] )
		if match_include_rule:
			snort_conf.set_include( include["file_name"], False )
    snort_conf.set_include( "$RULE_PATH/" + os.path.basename( rules.get_file_name() ) )
			
    snort_conf.save()
	
    snort_debian_conf = untangle_node_idps.SnortDebianConf( _debug=_debug )
	
    snort_debian_conf.set_variable("HOME_NET", settings.get_variable("HOME_NET") )
    snort_debian_conf.set_variable("OPTIONS", "--daq-dir /usr/lib/daq --daq nfq --daq-var queue=" + str(settings.get_nfqueue_queue_num()) + " -Q" )
    interface_pairs = []
    interface_first = settings.get_interfaces()[0]
    snort_debian_conf.set_variable("INTERFACE", ":".join(settings.get_interfaces()) )
    snort_debian_conf.save()

if __name__ == "__main__":
	main(sys.argv[1:])
