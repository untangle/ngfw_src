#!/usr/bin/python
import os
import getopt
import sys
import subprocess
import re
import json

import untangle_node_idps

def usage():
	print "usage"

def main(argv):
    global _debug
    _debug = False
    settings_file = ""
    snort_file = "/etc/snort/snort.conf"
    nodeId = 0
	
    try:
		opts, args = getopt.getopt(argv, "hsin:d", ["help", "nodeId=", "debug"] )
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

    if _debug == True:
		print "nodeId = " + nodeId
		print "_debug = ",  _debug

    settings = untangle_node_idps.IdpsSettings( nodeId )
    if settings.exists() == False:
        print "cannot find settings file"
        sys.exit()
    settings.load()

    snort_conf = untangle_node_idps.SnortConf( _debug=_debug )

    rules = untangle_node_idps.SnortRules( nodeId, snort_conf.get_variable( "RULE_PATH" ) )
    for settings_rule in settings.get_rules():
        match_rule = re.search( untangle_node_idps.SnortRule.text_regex, settings_rule["text"] )
        if match_rule:
            rule = untangle_node_idps.SnortRule( match_rule, settings_rule["category"] )
            rule.set_description( settings_rule["description"] )
            rule.set_action( settings_rule["log"], settings_rule["live"] )
            rule.set_name( settings_rule["name"] )
            rule.set_sid( settings_rule["sid"] )
            rules.addRule( rule )
        else:
            print "error with rule"
    rules.save()
    snort_conf.set_include( "$RULE_PATH/" + os.path.basename( rules.get_file_name() ) )
	
    for settings_variable in settings.get_variables():
        snort_conf.set_variable( settings_variable["variable"], settings_variable["definition"] )

    for include in snort_conf.get_includes():
		match_include_rule = re.search( untangle_node_idps.SnortConf.include_rulepath_regex, include["file_name"] )
		if match_include_rule:
			snort_conf.set_include( include["file_name"], False )
			
    snort_conf.save()
	
    snort_debian_conf = untangle_node_idps.SnortDebianConf( _debug=_debug )
	
    snort_debian_conf.set_variable("HOME_NET", settings.get_variable("HOME_NET") )
    snort_debian_conf.set_variable("OPTIONS", "--daq afpacket --daq-var buffer_size_mb=64 -Q" )
    interface_pairs = []
    interface_first = settings.get_interfaces()[0]
    for interface_second in settings.get_interfaces()[1:]:
        interface_pairs.append(interface_first + ":" + interface_second );
    snort_debian_conf.set_variable("INTERFACE", "::".join( interface_pairs ) )
    snort_debian_conf.save()

if __name__ == "__main__":
	main(sys.argv[1:])
