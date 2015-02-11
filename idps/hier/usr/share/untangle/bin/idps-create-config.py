#!/usr/bin/python
"""
Create snort configuration from settings
"""
import os
import getopt
import sys
import re

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)
	
import untangle_node_idps

def usage():
    """
    Show usage
    """
    print "usage"

def main(argv):
    """
    Main loop
    """
    global _debug
    _debug = False
    node_id = "0"
    classtypes = []
    categories = []
    msgs = []
    iptables_script = ""
	
    try:
        opts, args = getopt.getopt(argv, "hsincaq:d", ["help", "node_id=", "classtypes=", "categories=", "msgs=", "iptables_script=", "debug"] )
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
        elif opt in ( "-c", "--classtypes"):
            classtypes = arg.split(",")
        elif opt in ( "-a", "--categories"):
            categories = arg.split(",")
        elif opt in ( "-m", "--msgs"):
            msgs = arg.split(",")
        elif opt in ( "-i", "--iptables_script"):
            iptables_script = arg

    if _debug == True:
        print "node_id = " + node_id
        print "_debug = ",  _debug

    settings = untangle_node_idps.IdpsSettings( node_id )
    if settings.exists() == False:
        print "cannot find settings file"
        sys.exit()
    settings.load()

    snort_conf = untangle_node_idps.SnortConf( _debug=_debug )

    rules = settings.get_rules()
    rules.save(snort_conf.get_variable( "RULE_PATH" ), classtypes, categories, msgs )
    rules.save(snort_conf.get_variable( "PREPROC_RULE_PATH" ), classtypes, categories, msgs )
    
    idps_event_map = untangle_node_idps.IdpsEventMap( rules )
    idps_event_map.save()
	
    for settings_variable in settings.get_variables():
        snort_conf.set_variable( settings_variable["variable"], settings_variable["definition"] )

    for include in snort_conf.get_includes():
        match_include_rule = re.search( untangle_node_idps.SnortConf.include_rulepath_regex, include["file_name"] )
        if match_include_rule:
            snort_conf.set_include( include["file_name"], False )
    snort_conf.set_include( "$RULE_PATH/" + os.path.basename( rules.get_file_name() ) )
    snort_conf.set_include( "$PREPROC_RULE_PATH/" + os.path.basename( rules.get_file_name() ) )
			
    snort_conf.save()
	
    snort_debian_conf = untangle_node_idps.SnortDebianConf( _debug=_debug )

    queue_num = "0"
    ipf = open( iptables_script )
    for line in ipf:
        line = line.strip()
        setting = line.split("=")
        if setting[0] == "SNORT_QUEUE_NUM":
            queue_num = setting[1] 
    ipf.close()
    
    snort_debian_conf.set_variable("HOME_NET", settings.get_variable("HOME_NET") )
    snort_debian_conf.set_variable("OPTIONS", "--daq-dir /usr/lib/daq --daq nfq --daq-var queue=" + queue_num + " -Q" )
    snort_debian_conf.set_variable("INTERFACE", ":".join(settings.get_interfaces()) )
    snort_debian_conf.save()

if __name__ == "__main__":
    main(sys.argv[1:])