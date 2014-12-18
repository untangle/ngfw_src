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
    key = ""
	
    try:
		opts, args = getopt.getopt(argv, "nk:hd", ["help", "nodeId=", "debug", "key="] )
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
        elif opt in ( "-k", "--key"):
            key = arg

    if _debug == True:
		print "nodeId = " + nodeId
		print "_debug = ",  _debug

    settings = untangle_node_idps.IdpsSettings( nodeId )
    if settings.exists() == False:
        print "cannot find settings file"
        sys.exit()
    settings.load()
    
    if key == "nfqueue_queue_num":
        print settings.get_nfqueue_queue_num()

if __name__ == "__main__":
	main(sys.argv[1:])
