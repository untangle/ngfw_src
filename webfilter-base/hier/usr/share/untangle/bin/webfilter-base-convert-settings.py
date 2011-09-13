#!/usr/bin/python
import conversion.sql_helper as sql_helper
import sys
import os

global nodeName

def get_passed_clients_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting passed_clients for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select ipmaddr, live, description from u_ipmaddr_rule join n_webfilter_passed_clients using (rule_id) where setting_id = '%s'" % settings_id, debug=debug)

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


def get_mimetypes_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting blocked_mimetypes for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select mime_type, name, description, category, live, log from u_mimetype_rule join n_webfilter_mime_types using (rule_id) where setting_id = '%s'" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for settings in settings_list:
        string = settings[0]
        name = settings[1]
        description = settings[2]
        category = settings[3]
        blocked = settings[4]
        flagged = settings[5]

        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s",\n' % string
        str += '\t\t\t"blocked": "%s",\n' % blocked
        str += '\t\t\t"flagged": "%s",\n' % flagged
        str += '\t\t\t"name": "%s",\n' % name
        str += '\t\t\t"description": "%s",\n' % description
        str += '\t\t\t"category": "%s"\n' % category
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str


def get_extensions_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting blocked_extensions for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select string, name, description, category, live, log from u_string_rule join n_webfilter_extensions using (rule_id) where setting_id = '%s'" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for settings in settings_list:
        string = settings[0]
        name = settings[1]
        description = settings[2]
        category = settings[3]
        blocked = settings[4]
        flagged = settings[5]

        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s",\n' % string
        str += '\t\t\t"blocked": "%s",\n' % blocked
        str += '\t\t\t"flagged": "%s",\n' % flagged
        str += '\t\t\t"name": "%s",\n' % name
        str += '\t\t\t"description": "%s",\n' % description
        str += '\t\t\t"category": "%s"\n' % category
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str


def get_category_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting categories for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select name, display_name, block, log, description from n_webfilter_blcat where setting_id = '%s'" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for settings in settings_list:
        string = settings[0]
        display_name = settings[1]
        blocked = settings[2]
        flagged = settings[3]
        description = settings[4]

        # for consistency
        if (nodeName == "webfilter"):
            if string == "Uncategorized":
                string = "uncategorized"
        if (nodeName == "sitefilter"):
            if string == "Uncategorized":
                continue; # no longer have an uncat setting for sitefilter (it has misc instead)

        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s",\n' % string
        str += '\t\t\t"name": "%s",\n' % display_name
        str += '\t\t\t"blocked": "%s",\n' % blocked
        str += '\t\t\t"flagged": "%s",\n' % flagged
        str += '\t\t\t"description": "%s"\n' % description
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str


def get_blocked_urls_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting blocked_URL for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select string, live, log, description from u_string_rule join n_webfilter_blocked_urls using (rule_id) where setting_id = '%s'" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    for settings in settings_list:
        string = settings[0]
        blocked = settings[1]
        flagged = settings[2]
        description = settings[3]
    
        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"string": "%s",\n' % string
        str += '\t\t\t"blocked": "%s",\n' % blocked
        str += '\t\t\t"flagged": "%s",\n' % flagged
        str += '\t\t\t"description": "%s"\n' % description
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str


def get_passed_urls_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting passed_urls for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select string, live, description from u_string_rule join n_webfilter_passed_urls using (rule_id) where setting_id = '%s'" % settings_id, debug=debug)

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

    settings_list = sql_helper.run_sql("select  settings_id, template, block_all_ip_hosts, user_whitelist_mode, enable_https, unblock_password_enabled, unblock_password_admin, unblock_password, enforce_safe_search  from n_webfilter_settings where tid = '%s'" % tid, debug=debug)

    if settings_list == None:
        print "WARNING: missing results for TID %s" % tid
        
    if len(settings_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
        
    settings = settings_list[0]

    settings_id = settings[0]
    # template = settings[1] #unused
    block_all_ip_hosts = settings[2]
    user_whitelist_mode = settings[3]
    enable_https = settings[4]
    unblock_password_enabled = settings[5]
    unblock_password_admin = settings[6]
    unblock_password = settings[7]
    enforce_safe_search = settings[8]
    
    # adjust settings to sanity
    if user_whitelist_mode == "USER_ONLY":
        user_whitelist_mode = "Host"
    if user_whitelist_mode == "USER_AND_GLOBAL":
        user_whitelist_mode = "Global"
    if user_whitelist_mode == "NONE":
        user_whitelist_mode = "None"

    str = '{\n'
    str += '\t"javaClass": "com.untangle.node.webfilter.WebFilterSettings",\n'
    str += '\t"version": "1",\n' 
    str += '\t"scanHttps": "%s",\n' % enable_https
    str += '\t"blockIpHosts": "%s",\n' % block_all_ip_hosts
    str += '\t"unblockMode": "%s",\n' % user_whitelist_mode
    str += '\t"unblockPasswordRequired": "%s",\n' % unblock_password_enabled
    str += '\t"unblockPasswordAdmin": "%s",\n' % unblock_password_admin
    str += '\t"unblockPassword": "%s",\n' % unblock_password
    str += '\t"categories": %s,\n' % get_category_settings(tid, settings_id, debug=debug)
    str += '\t"blockedUrls": %s,\n' % get_blocked_urls_settings(tid, settings_id, debug=debug)
    str += '\t"blockedExtensions": %s,\n' % get_extensions_settings(tid, settings_id, debug=debug)
    str += '\t"blockedMimetypes": %s,\n' % get_mimetypes_settings(tid, settings_id, debug=debug)
    str += '\t"passedUrls": %s,\n' % get_passed_urls_settings(tid, settings_id, debug=debug)
    str += '\t"passedClients": %s\n' % get_passed_clients_settings(tid, settings_id, debug=debug)
    str += '}\n'
    
    return str
    

filename = None
if len(sys.argv) < 3:
    print "usage: %s TID [sitefilter|webfilter] [filename]" % sys.argv[0]
    sys.exit(1)


if len(sys.argv) > 1:
    tid = sys.argv[1]

if len(sys.argv) > 2:
    nodeName = sys.argv[2]

if len(sys.argv) > 3:
    filename = sys.argv[3]

try:
    settings_str = get_settings(tid, debug=True)
    print settings_str
    if filename == None:
        filename = "/usr/share/untangle/settings/untangle-node-webfilter/settings_%s.js" % tid
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
