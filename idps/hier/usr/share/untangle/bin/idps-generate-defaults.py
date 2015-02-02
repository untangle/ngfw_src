#!/usr/bin/python
"""
Generate defaults for Setup Wizard
"""
import os
import getopt
import sys

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)
	
import untangle_node_idps

def usage():
    """
    Show usage
    """
    print "usage..."
    print "help\t\tusage"
    print "settings\tSettings configuration file name"
    print "conf\t\tSnort configuration file name"
    print "rules\t\tSnort rule file name"
    print "node\t\tNode identifier"
    print "debug\t\tEnable debugging"
        
def main(argv):
    """
    Main 
    """
    global _debug
    _debug = False
    rules_dir = ""
    defaults_dir = ""
    templates_dir = ""
    node_id = "0"
    file_name = ""
	
    try:
        opts, args = getopt.getopt(argv, "hsretf:d", ["help", "rules=", "defaults=", "templates=", "filename=", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-d", "--debug"):
            _debug = True
        elif opt in ( "-e", "--defaults"):
            defaults_dir = arg
        elif opt in ( "-r", "--rules"):
            rules_dir = arg
        elif opt in ( "-t", "--templates"):
            templates_dir = arg
        elif opt in ( "-f", "--filename"):
            file_name = arg

    if _debug == True:
        print "rules_dir = " + rules_dir
        print "defaults_dir = " + defaults_dir
        print "templates_dir = " + templates_dir
        print "_debug = ",  _debug

    snort_conf = untangle_node_idps.SnortConf()
    snort_rules = untangle_node_idps.SnortRules( node_id, rules_dir )
    snort_rules.load( True )

    for file_name in os.listdir( templates_dir ):
        if file_name != "" and file_name != file_name:
            continue
        settings = untangle_node_idps.IdpsSettings( node_id )
        settings.load( templates_dir + "/" + file_name )
        settings.initialize( snort_conf, snort_rules )
        settings.save( defaults_dir + "/" + file_name )
        
    sys.exit()

if __name__ == "__main__":
    main( sys.argv[1:] )
