#!/usr/bin/python
import conversion.sql_helper as sql_helper
import simplejson
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
    
    if (len(list) == 0):
        return '{\n' + pad(8) + '"javaClass": "java.util.ArrayList",\n' + pad(8) + '"list": []\n' + pad(4) + '},'

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

    if (len(list) == 0):
        return '{\n' + pad(8) + '"javaClass": "java.util.ArrayList",\n' + pad(8) + '"list": []\n' + pad(4) + '},'

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
        str += pad(16) + '"javaClass": "com.untangle.node.cpd.PassedAddress",\n'
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

    if (len(list) == 0):
        return '{\n' + pad(8) + '"javaClass": "java.util.ArrayList",\n' + pad(8) + '"list": []\n' + pad(4) + '},'

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
        str += pad(16) + '"javaClass": "com.untangle.node.cpd.PassedAddress",\n'
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
        print "Getting n_openvpn_settings for TID: ",tid

    node_settings = sql_helper.run_sql("select id, server_address, domain, key_size, country, province, locality, org, org_unit, email, max_clients, is_edgeguard_client, is_ca_on_usb, is_bridge, expose_clients, keep_alive, is_dns_override, dns_1, dns_2, site_name from n_openvpn_settings where tid = '%s'" % tid, debug=debug)

    if node_settings == None:
        print "WARNING: missing n_openvpn_settings result for TID %s" % tid

    if len(node_settings) > 1:
        print "WARNING: too many n_openvpn_settings results (%i) for TID %s" % (len(node_settings),tid)

    settings_id = node_settings[0][0]
    server_address = node_settings[0][1]
    domain = node_settings[0][2]
    key_size = node_settings[0][3]
    country = node_settings[0][4]
    province = node_settings[0][5]
    locality = node_settings[0][6]
    org = node_settings[0][7]
    org_unit = node_settings[0][8]
    email = node_settings[0][9]
    max_clients = node_settings[0][10]
    is_edgeguard_client = node_settings[0][11]
    is_ca_on_bridge = node_settings[0][12]
    is_bridge = node_settings[0][13]
    expose_clients = node_settings[0][14]
    keep_alive = node_settings[0][15]
    is_dns_override = node_settings[0][16]
    dns_1 = node_settings[0][17]
    dns_2 = node_settings[0][18]
    site_name = node_settings[0][19]

    str = '{\n'
    str += pad(4) + '"bridgeMode": %s,\n' % is_bridge
    str += pad(4) + '"caKeyOnUsb": %s,\n' % is_ca_on_bridge
# clientList
# completeClientList
    str += pad(4) + '"country": "%s",\n' % escape(country)
    
# Create a LinkedList from the two dns entries.  Note that we anticipate
# that either one (or both) may contain values, and handle accordingly

    str += pad(4) + '"dnsServerList": {\n'
    str += pad(8) + '"javaClass": "java.util.LinkedList",\n'
    str += pad(8) + '"list": [\n'
    if dns_1 and dns_2:
        str += pad(12) + '"%s",\n' % dns_1
        str += pad(12) + '"%s"\n' % dns_2
    elif dns_1:
        str += pad(12) + '"%s"\n' % dns_1
    elif dns_2:
        str += pad(12) + '"%s"\n' % dns_2
    str += pad(8) + ']\n'
    str += pad(4) + '},\n'

    str += pad(4) + '"domain": "%s",\n' % escape(domain)
    str += pad(4) + '"email": "%s",\n' % escape(email)
# exportedAddressList
    str += pad(4) + '"exposeClients": %s,\n' % expose_clients
# groupList
    str += pad(4) + '"isDnsOverrideEnabled": %s,\n' % is_dns_override
    str += pad(4) + '"javaClass": "com.untangle.node.openvpn.VpnSettings",\n'
    str += pad(4) + '"keepAlive": %s,\n' % keep_alive
    str += pad(4) + '"keySize": %s,\n' % key_size
    str += pad(4) + '"locality": "%s",\n' % escape(locality)
    str += pad(4) + '"maxClients": %s,\n' % max_clients
    str += pad(4) + '"organization": "%s",\n' % escape(org)
    str += pad(4) + '"organizationUnit": "%s",\n' % escape(org_unit)
    str += pad(4) + '"province": "%s",\n' % escape(province)
    str += pad(4) + '"serverAddress": "%s",\n' % escape(server_address)
# siteList
    str += pad(4) + '"siteName": "%s",\n' % escape(site_name)
    str += pad(4) + '"untanglePlatformClient": %s,\n' % is_edgeguard_client
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
debug = False

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
