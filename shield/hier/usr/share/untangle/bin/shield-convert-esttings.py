#!/usr/bin/python

#
# This script can be removed in 10.x or after
#

import conversion.sql_helper as sql_helper
import sys
import os

def get_rules_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting rules for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select string, live from u_string_rule join n_adblocker_rules using (rule_id) where setting_id = '%s'" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for settings in settings_list:
        string = settings[0]
        enabled = settings[1]
    
        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"enabled": "%s",\n' % enabled
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s"\n' % string
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str

def get_passed_client_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting passed_clients for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select string, live, description from u_string_rule join n_adblocker_passed_clients using (rule_id) where setting_id = '%s'" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for settings in settings_list:
        string = settings[0]
        enabled = settings[1]
        description = settings[2]
    
        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s",\n' % string
        str += '\t\t\t"enabled": "%s",\n' % enabled
        str += '\t\t\t"description": "%s"\n' % description
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str

def get_passed_urls_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting passed_urls for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select string, live, description from u_string_rule join n_adblocker_passed_urls using (rule_id) where setting_id = '%s'" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for settings in settings_list:
        string = settings[0]
        enabled = settings[1]
        description = settings[2]
    
        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s",\n' % string
        str += '\t\t\t"enabled": "%s",\n' % enabled
        str += '\t\t\t"description": "%s"\n' % description
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str
    

def get_settings(tid, debug=False):
    if (debug):
        print "Getting settings for TID: ",tid

    settings_list = sql_helper.run_sql("select  settings_id from n_adblocker_settings where tid = '%s'" % tid, debug=debug)

    if settings_list == None:
        print "WARNING: missing results for TID %s" % tid
        return ""

    if len(settings_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
        
    settings = settings_list[0]

    settings_id = settings[0]

    str = '{\n'
    str += '\t"javaClass": "com.untangle.node.adblocker.AdBlockerSettings",\n'
    str += '\t"version": "1",\n' 
    str += '\t"rules": %s,\n' % get_rules_settings(tid, settings_id, debug=debug)
    str += '\t"passedUrls": %s,\n' % get_passed_urls_settings(tid, settings_id, debug=debug)
    str += '\t"passedClients": %s\n' % get_passed_client_settings(tid, settings_id, debug=debug)
    str += '}\n'
    
    return str
    

filename = None
if len(sys.argv) < 2:
    print "usage: %s TID [filename]" % sys.argv[0]
    sys.exit(1)


if len(sys.argv) > 1:
    tid = sys.argv[1]

if len(sys.argv) > 2:
    filename = sys.argv[2]

try:
    settings_str = get_settings(tid, debug=True)
    print settings_str
    if filename == None:
        filename = "/usr/share/untangle/settings/untangle-node-adblocker/settings_%s.js" % tid
    dir = os.path.dirname(filename)
    if not os.path.exists(dir):
        os.makedirs(dir)
    file = open(filename, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("could not get result",e);
    sys.exit(1)

sys.exit(0)
