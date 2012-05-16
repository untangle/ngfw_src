#!/usr/bin/python

#
# This script can be removed in 10.x or after
#

import conversion.sql_helper as sql_helper
import sys
import os
import string
import traceback

def get_users(userlist, debug=False):
    if (debug):
        print "Getting user list from: ",userlist

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    id = 0
    for email in userlist.split(","):
        if not first:
            str += '\t\t\t,\n'
        str += '\t\t\t{\n';
        str += '\t\t\t"javaClass": "com.untangle.node.reporting.ReportingUser",\n';
        str += '\t\t\t"emailAddress": "%s",\n' % email;
        str += '\t\t\t"emailSummaries": "True",\n';
        str += '\t\t\t"onlineAccess": "False",\n';
        str += '\t\t\t"password": ""\n';
        str += '\t\t\t}\n';
        first = False
        
    str += '\t\t]\n'
    str += '\t}'

    return str


def get_hostname_map(ipmaddr_dir_id, debug=False):
    if (debug):
        print "Getting hostname map for ipmaddr_dir_id: ",ipmaddr_dir_id

    settings_list = sql_helper.run_sql("select ipmaddr, name from settings.u_ipmaddr_dir_entries join settings.u_ipmaddr_rule using (rule_id) where ipmaddr_dir_id = '%s'" % ipmaddr_dir_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    id = 0
    for settings in settings_list:
        ipmaddr = settings[0];
        name = settings[1];

        if not first:
            str += '\t\t\t,\n'
        str += '\t\t\t{\n';
        str += '\t\t\t"javaClass": "com.untangle.node.reporting.ReportingHostnameMapEntry",\n';
        str += '\t\t\t"hostname": "%s",\n' % name;
        str += '\t\t\t"address": "%s"\n' % ipmaddr;
        str += '\t\t\t}\n';
        first = False
        
    str += '\t\t]\n'
    str += '\t}'

    return str


def get_settings(tid, debug=False):
    if (debug):
        print "Getting settings for TID: ",tid

    settings_list = sql_helper.run_sql("select email_detail, attachment_size_limit, network_directory, schedule, nightly_hour, nightly_minute, db_retention, file_retention, reporting_users from n_reporting_settings where tid = '%s'" % tid, debug=debug)

    if settings_list == None:
        print "WARNING: missing results for TID %s" % tid
        return ""

    if len(settings_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
        
    email_detail = settings_list[0][0]
    attachment_size_limit = settings_list[0][1]
    network_directory = settings_list[0][2]
    schedule = settings_list[0][3]
    generation_hour = settings_list[0][4]
    generation_minute = settings_list[0][5]
    db_retention = settings_list[0][6]
    file_retention = settings_list[0][7]
    reporting_users = settings_list[0][8]

    str = '{\n'
    str += '\t"javaClass": "com.untangle.node.reporting.ReportingSettings",\n'
    str += '\t"emailDetail": "%s",\n' % email_detail
    str += '\t"attachmentSizeLimit": "%s",\n' % attachment_size_limit
    str += '\t"generationHour": "%s",\n' % generation_hour
    str += '\t"generationMinute": "%s",\n' % generation_minute
    str += '\t"hostnameMap": %s,\n' % get_hostname_map(network_directory, debug)
    str += '\t"reportingUsers": %s,\n' % get_users(reporting_users, debug)
    str += '\t"dbRetention": "%s",\n' % db_retention
    str += '\t"fileRetention": "%s"\n' % file_retention
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
        filename = "/usr/share/untangle/settings/untangle-node-reporting/settings_%s.js" % tid
    dir = os.path.dirname(filename)
    if not os.path.exists(dir):
        os.makedirs(dir)

    file = open(filename, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("could not get result",e);
    traceback.print_exc(file=sys.stdout)
    sys.exit(1)

sys.exit(0)
