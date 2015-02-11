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
import time
import uvm
from uvm import Manager
from uvm import Uvm

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)
	
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
    previous_rules_file_name = ""
    settings_file_name = None
    status_file_name = None
    node_id = "0"
	
    try:
		opts, args = getopt.getopt(argv, "hsrpna:d", ["help", "settings=", "rules=", "previous_rules=", "node_id=", "status=", "debug"] )
    except getopt.GetoptError:
    	usage()
    	sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-d", "--debug"):
             _debug = True
        elif opt in ( "-n", "--node_id"):
            node_id = arg
        elif opt in ( "-r", "--rules"):
            rules_file_name = arg
        elif opt in ( "-p", "--previous_rules"):
            previous_rules_file_name = arg
        elif opt in ( "-s", "--settings"):
            settings_file_name = arg
        elif opt in ( "-a", "--status"):
            status_file_name = arg

    if _debug == True:
        print "rules_file_name = " + rules_file_name
        print "settings_file_name = " + settings_file_name
        print "node = " + node_id
        print "_debug = ",  _debug

    snort_conf = untangle_node_idps.SnortConf()
    snort_rules = untangle_node_idps.SnortRules( node_id, rules_file_name )
    snort_rules.load( True )

    settings = untangle_node_idps.IdpsSettings( node_id )
    if settings.exists() == False:
        settings.initialize( snort_conf, snort_rules )
    else:
        settings.load()
        added_rule_rids = []
        deleted_rule_rids = []
        modified_rule_rids = []
        
        previous_snort_rules = untangle_node_idps.SnortRules( node_id, previous_rules_file_name )
        previous_snort_rules.load( True )
        
        previous_rules = previous_snort_rules.get_rules()
        current_rules = snort_rules.get_rules()
        settings_rules = settings.get_rules().get_rules()
        
        active_rules_classtypes = settings.get_active_rules_classtypes()
        active_rules_categories = settings.get_active_rules_categories()
        
        for rid in previous_rules:
            if current_rules.has_key(rid) == False:
                deleted_rule_rids.append(rid)

        for rid in current_rules:
            if previous_rules.has_key(rid) == False:
                added_rule_rids.append(rid)
            elif current_rules[rid].build() != previous_rules[rid].build():
                modified_rule_rids.append(rid)

        for rid in deleted_rule_rids:
            if settings_rules.has_key(rid):
                settings_rules.remove(rid)
                
        for rid in added_rule_rids:
            if settings_rules.has_key(rid):
                new_rule = current_rules[rid]
                new_rule.enabled = settings_rules[rid].enabled
                new_rule.action = settings_rules[rid].action
                settings_rules[rid] = new_rule
            else:
                settings_rules[rid] = current_rules[rid]
                
                if len( active_rules_classtypes ) == 0 or ( settings_rules[sid].options["classtype"] in active_rules_classtypes ) == False:
                    classtype_enabled = True
                else:
                    classtype_enabled = False
                if len( active_rules_categories ) == 0 or ( settings_rules[sid].options["classtype"] in active_rules_categories ) == False:
                    category_enabled = True
                else:
                    category_enabled == False
                settings_rules[sid].enabled = classtype_enabled and category_enabled
        settings.set_rules( settings_rules )

        settings.set_updated({
            "rules": { 
                "added" : added_rule_rids, 
                "modified" : modified_rule_rids, 
                "deleted": deleted_rule_rids
            }})
        
    settings.save( settings_file_name )
    sys.exit()

if __name__ == "__main__":
	main( sys.argv[1:] )
