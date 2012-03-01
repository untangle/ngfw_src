#!/usr/bin/python
import conversion.sql_helper as sql_helper
import sys
import os

#------------------------------------------------------------------------------

def pad(arg):
    str = ""
    for x in range(0,arg):
        str = str + " "
    return str

#------------------------------------------------------------------------------

def escape(arg):

    # first we escape backslash characters
    one = arg.replace('\\', '\\\\')

    # next we escape any double quote characters
    two = one.replace('"', '\\"')

    return two

#------------------------------------------------------------------------------

def get_capture_rules(sid, debug=False):
    if (debug):
        print "Getting n_cpd_capture_rule list for SID: ",sid

    list = sql_helper.run_sql("select name, category, description, live, alert, log, capture_enabled, client_interface, client_address, server_address, start_time, end_time, days from n_cpd_capture_rule where settings_id = '%s'" % sid, debug=debug)

    str = '{\n'
    str += pad(8) + '"javaClass": "java.util.ArrayList",\n'
    str += pad(8) + '"list": [\n'

    first = True

    for settings in list:
        name = settings[0]
        category = settings[1]
        description = settings[2]
        live = settings[3]
        alert = settings[4]
        log = settings[5]
        capture_enabled = settings[6]
        client_interface = settings[7]
        client_address = settings[8]
        server_address = settings[9]
        start_time = settings[10]
        end_time = settings[11]
        days = settings[12]

        if not first:
            str += ',\n'
        str += pad(12) + '{\n'

        str += pad(16) + '"alert": %s,\n' % alert
        str += pad(16) + '"capture": %s,\n' % capture_enabled
        str += pad(16) + '"category": "%s",\n' % category
        str += pad(16) + '"clientAddress": "%s",\n' % client_address
        str += pad(16) + '"clientInterface": "%s",\n' % client_interface
        str += pad(16) + '"days": "%s",\n' % days
        str += pad(16) + '"description": "%s",\n' % escape(description)
        str += pad(16) + '"endTime": "%s",\n' % end_time
        str += pad(16) + '"javaClass": "com.untangle.node.cpd.CaptureRule",\n'
        str += pad(16) + '"live": %s,\n' % live
        str += pad(16) + '"log": %s,\n' % log
        str += pad(16) + '"name": "%s",\n' % name
        str += pad(16) + '"serverAddress": "%s",\n' % server_address
        str += pad(16) + '"startTime": "%s"\n' % start_time

        str += pad(12) + '}'

        first = False

    str += '\n' + pad(8) + ']\n' + pad(4) + '},'

    return str

#------------------------------------------------------------------------------

def get_passed_clients(sid, debug = False):
    if (debug):
        print "Getting n_cpd_passed_client list for SID: ",sid

    list = sql_helper.run_sql("select name, category, description, live, alert, log, address from n_cpd_passed_client where settings_id = '%s'" % sid, debug=debug)

    str = '{\n'
    str += pad(8) + '"javaClass": "java.util.ArrayList",\n'
    str += pad(8) + '"list": [\n'

    first = True

    for settings in list:
        name = settings[0]
        category = settings[1]
        description = settings[2]
        live = settings[3]
        alert = settings[4]
        log = settings[5]
        address = settings[6]

        if not first:
            str += ',\n'
        str += pad(12) + '{\n'

        str += pad(16) + '"address": "%s",\n' % address
        str += pad(16) + '"alert": %s,\n' % alert
        str += pad(16) + '"category": "%s",\n' % category
        str += pad(16) + '"description": "%s",\n' % escape(description)
        str += pad(16) + '"javaClass": "com.untangle.node.cpd.PassedClient",\n'
        str += pad(16) + '"live": %s,\n' % live
        str += pad(16) + '"log": %s,\n' % log
        str += pad(16) + '"name": "%s"\n' % name

        str += pad(12) + '}'

        first = False

    str += '\n' + pad(8) + ']\n' + pad(4) + '},'

    return str

#------------------------------------------------------------------------------

def get_passed_servers(sid, debug = False):
    if (debug):
        print "Getting n_cpd_passed_server list for SID: ",sid

    list = sql_helper.run_sql("select name, category, description, live, alert, log, address from n_cpd_passed_server where settings_id = '%s'" % sid, debug=debug)

    str = '{\n'
    str += pad(8) + '"javaClass": "java.util.ArrayList",\n'
    str += pad(8) + '"list": [\n'

    first = True

    for settings in list:
        name = settings[0]
        category = settings[1]
        description = settings[2]
        live = settings[3]
        alert = settings[4]
        log = settings[5]
        address = settings[6]

        if not first:
            str += ',\n'
        str += pad(12) + '{\n'

        str += pad(16) + '"address": "%s",\n' % address
        str += pad(16) + '"alert": %s,\n' % alert
        str += pad(16) + '"category": "%s",\n' % category
        str += pad(16) + '"description": "%s",\n' % escape(description)
        str += pad(16) + '"javaClass": "com.untangle.node.cpd.PassedServer",\n'
        str += pad(16) + '"live": %s,\n' % live
        str += pad(16) + '"log": %s,\n' % log
        str += pad(16) + '"name": "%s"\n' % name

        str += pad(12) + '}'

        first = False

    str += '\n' + pad(8) + ']\n' + pad(4) + '},'

    return str

#------------------------------------------------------------------------------

def get_settings(tid, debug=False):

    if (debug):
        print "Getting n_cpd_settings for TID: ",tid

    n_cpd_settings = sql_helper.run_sql("select settings_id, capture_bypassed_traffic, authentication_type, idle_timeout, timeout, concurrent_logins, page_type, page_parameters, redirect_url, https_page, redirect_https from n_cpd_settings where tid = '%s'" % tid, debug=debug)

    if n_cpd_settings == None:
        print "WARNING: missing n_cpd_settings result for TID %s" % tid

    if len(n_cpd_settings) > 1:
        print "WARNING: too many n_cpd_settings results (%i) for TID %s" % (len(n_cpd_settings),tid)

    settings_id = n_cpd_settings[0][0]
    capture_bypassed_traffic = n_cpd_settings[0][1]
    authentication_type = n_cpd_settings[0][2]
    idle_timeout = n_cpd_settings[0][3]
    timeout = n_cpd_settings[0][4]
    concurrent_logins = n_cpd_settings[0][5]
    page_type = n_cpd_settings[0][6]
    page_parameters = n_cpd_settings[0][7]
    redirect_url = n_cpd_settings[0][8]
    https_page = n_cpd_settings[0][9]
    redirect_https = n_cpd_settings[0][10]

    str = '{\n'
    str += pad(4) + '"authenticationType": "%s",\n' % authentication_type
    str += pad(4) + '"captureBypassedTraffic": %s,\n' % capture_bypassed_traffic
    str += pad(4) + '"captureRules": %s\n' % get_capture_rules(settings_id, debug=debug)
    str += pad(4) + '"concurrentLoginsEnabled": %s,\n' % concurrent_logins
    str += pad(4) + '"idleTimeout": %s,\n' % idle_timeout
    str += pad(4) + '"javaClass": "com.untangle.node.cpd.CPDSettings",\n'
    str += pad(4) + '"pageParameters": "%s",\n' % escape(page_parameters)
    str += pad(4) + '"pageType": "%s",\n' % page_type
    str += pad(4) + '"passedClients": %s\n' % get_passed_clients(settings_id, debug=debug)
    str += pad(4) + '"passedServers": %s\n' % get_passed_servers(settings_id, debug=debug)
    str += pad(4) + '"redirectHttpsEnabled": %s,\n' % redirect_https
    str += pad(4) + '"redirectUrl": "%s",\n' % redirect_url
    str += pad(4) + '"timeout": %s,\n' % timeout
    str += pad(4) + '"useHttpsPage": %s,\n' % bool(int(https_page))
    str += pad(4) + '"version": 1\n'
    str += '}\n'

    return str

#------------------------------------------------------------------------------

filename = None
if len(sys.argv) < 3:
    print "usage: %s node_id target_filename" % sys.argv[0]
    sys.exit(1)

nodeid = sys.argv[1]
target = sys.argv[2]
debug = True

try:
    (pathname, filename) = os.path.split(target)
    if pathname == "":
        pathname = "."

    fullname = pathname + "/" + filename

    if (debug):
        print("FULL: ",fullname)
        print("PATH: ",pathname)
        print("FILE: ",filename)

    if not os.path.exists(pathname):
        os.makedirs(pathname)

    settings_str = get_settings(nodeid, debug)

    if (debug):
        print settings_str

    file = open(target, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("Could not get result",e)
    sys.exit(1)

sys.exit(0)
