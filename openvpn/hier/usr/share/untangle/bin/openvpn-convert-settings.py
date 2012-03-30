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

def get_client_list(sid, indent, debug=False):
    if (debug):
        print "Getting n_openvpn_client_2 list for SID: ",sid

    list = sql_helper.run_sql("select address, group_name, name, category, description, live, alert, log from n_openvpn_client_2 where settings_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.LinkedList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.LinkedList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        address = settings[0]
        group_name = settings[1]
        name = settings[2]
        category = settings[3]
        description = settings[4]
        live = settings[5]
        alert = settings[6]
        log = settings[7]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"address": "%s",\n' % escape(address) if (address != None) else ''
        str += pad(indent+12) + '"alert": %s,\n' % alert
        str += pad(indent+12) + '"category": "%s",\n' % escape(category) if (category != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else ''
        str += pad(indent+12) + '"groupName": "%s",\n' % escape(group_name) if (group_name != None) else ''
        str += pad(indent+12) + '"javaClass": "com.untangle.node.openvpn.VpnClient",\n'
        str += pad(indent+12) + '"live": %s,\n' % live
        str += pad(indent+12) + '"log": %s,\n' % log
        str += pad(indent+12) + '"name": "%s"\n' % escape(name) if (name != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_exported_address_list(sid, indent, debug=False):
    if (debug):
        print "Getting n_openvpn_s_site_network list for SID: ",sid

    list = sql_helper.run_sql("select network, netmask, name, category, description, live, alert, log from n_openvpn_s_site_network where settings_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.LinkedList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.LinkedList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        network = settings[0]
        netmask = settings[1]
        name = settings[2]
        category = settings[3]
        description = settings[4]
        live = settings[5]
        alert = settings[6]
        log = settings[7]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"alert": %s,\n' % alert
        str += pad(indent+12) + '"category": "%s",\n' % escape(category) if (category != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else ''
        str += pad(indent+12) + '"id": %d,\n' % counter
        str += pad(indent+12) + '"javaClass": "com.untangle.node.openvpn.SiteNetwork",\n'
        str += pad(indent+12) + '"live": %s,\n' % live
        str += pad(indent+12) + '"log": %s,\n' % log
        str += pad(indent+12) + '"name": "%s",\n' % escape(name) if (name != None) else ''
        str += pad(indent+12) + '"netmask": "%s",\n' % escape(netmask) if (netmask != None) else ''
        str += pad(indent+12) + '"network": "%s"\n' % escape(network) if (network != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_group_list(sid, indent, debug=False):
    if (debug):
        print "Getting n_openvpn_group list for SID: ",sid

    list = sql_helper.run_sql("select address, netmask, name, category, description, live, alert, log, use_dns from n_openvpn_group where settings_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.LinkedList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.LinkedList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        address = settings[0]
        netmask = settings[1]
        name = settings[2]
        category = settings[3]
        description = settings[4]
        live = settings[5]
        alert = settings[6]
        log = settings[7]
        use_dns = settings[8]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"address": "%s",\n' % escape(address) if (address != None) else ''
        str += pad(indent+12) + '"alert": %s,\n' % alert
        str += pad(indent+12) + '"category": "%s",\n' % escape(category) if (category != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else''
        str += pad(indent+12) + '"javaClass": "com.untangle.node.openvpn.VpnGroup",\n'
        str += pad(indent+12) + '"live": %s,\n' % live
        str += pad(indent+12) + '"log": %s,\n' % log
        str += pad(indent+12) + '"name": "%s",\n' % escape(name) if (name != None) else''
        str += pad(indent+12) + '"netmask": "%s",\n' % escape(netmask) if (netmask != None) else''
        str += pad(indent+12) + '"useDNS": %s\n' % use_dns

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_site_list(sid, indent, debug=False):
    if (debug):
        print "Getting n_openvpn_site_2 list for SID: ",sid

    list = sql_helper.run_sql("select rule_id, address, group_name, name, category, description, live, alert, log from n_openvpn_site_2 where settings_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.LinkedList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.LinkedList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        rule_id = settings[0]
        address = settings[1]
        group_name = settings[2]
        name = settings[3]
        category = settings[4]
        description = settings[5]
        live = settings[6]
        alert = settings[7]
        log = settings[8]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"address": "%s",\n' % escape(address) if (address != None) else ''
        str += pad(indent+12) + '"alert": %s,\n' % alert
        str += pad(indent+12) + '"category": "%s",\n' % escape(category) if (category != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else ''

        str += pad(indent+12) + '"exportedAddressList": %s\n' % get_site_networks(rule_id, 16, debug=debug)

        str += pad(indent+12) + '"groupName": "%s",\n' % escape(group_name) if (group_name != None) else ''
        str += pad(indent+12) + '"javaClass": "com.untangle.node.openvpn.VpnSite",\n'
        str += pad(indent+12) + '"live": %s,\n' % live
        str += pad(indent+12) + '"log": %s,\n' % log
        str += pad(indent+12) + '"name": "%s"\n' % escape(name) if (name != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_site_networks(rid, indent, debug=False):
    if (debug):
        print "Getting n_openvpn_c_site_network list for RID: ",rid

    list = sql_helper.run_sql("select network, netmask, name, category, description, live, alert, log from n_openvpn_c_site_network where client_id = '%s'" % rid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.ArrayList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.ArrayList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        network = settings[0]
        netmask = settings[1]
        name = settings[2]
        category = settings[3]
        description = settings[4]
        live = settings[5]
        alert = settings[6]
        log = settings[7]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"alert": %s,\n' % alert
        str += pad(indent+12) + '"category": "%s",\n' % escape(category) if (category != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else ''
        str += pad(indent+12) + '"id": %d,\n' % counter
        str += pad(indent+12) + '"javaClass": "com.untangle.node.openvpn.SiteNetwork",\n'
        str += pad(indent+12) + '"live": %s,\n' % live
        str += pad(indent+12) + '"log": %s,\n' % log
        str += pad(indent+12) + '"name": "%s",\n' % escape(name) if (name != None) else ''
        str += pad(indent+12) + '"netmask": "%s",\n' % escape(netmask) if (netmask != None) else ''
        str += pad(indent+12) + '"network": "%s"\n' % escape(network) if (network != None) else''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

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

    str += pad(4) + '"clientList": %s\n' % get_client_list(settings_id, 4, debug=debug)

    str += pad(4) + '"country": "%s",\n' % escape(country) if (country != None) else ''
    str += pad(4) + '"dns1": "%s",\n' % escape(dns_1) if (dns_1 != None) else ''
    str += pad(4) + '"dns2": "%s",\n' % escape(dns_2) if (dns_2 != None) else ''
    str += pad(4) + '"domain": "%s",\n' % escape(domain) if (domain != None) else ''
    str += pad(4) + '"email": "%s",\n' % escape(email) if (email != None) else ''

    str += pad(4) + '"exportedAddressList": %s\n' % get_exported_address_list(settings_id, 4, debug=debug)

    str += pad(4) + '"exposeClients": %s,\n' % expose_clients

    str += pad(4) + '"groupList": %s\n' % get_group_list(settings_id, 4, debug=debug)

    str += pad(4) + '"isDnsOverrideEnabled": %s,\n' % is_dns_override
    str += pad(4) + '"javaClass": "com.untangle.node.openvpn.VpnSettings",\n'
    str += pad(4) + '"keepAlive": %s,\n' % keep_alive
    str += pad(4) + '"keySize": %s,\n' % key_size
    str += pad(4) + '"locality": "%s",\n' % escape(locality) if (locality != None) else ''
    str += pad(4) + '"maxClients": %s,\n' % max_clients
    str += pad(4) + '"organization": "%s",\n' % escape(org) if (org != None) else ''
    str += pad(4) + '"organizationUnit": "%s",\n' % escape(org_unit) if (org_unit != None) else ''
    str += pad(4) + '"province": "%s",\n' % escape(province) if (province != None) else ''
    str += pad(4) + '"serverAddress": "%s",\n' % escape(server_address) if (server_address != None) else ''

    str += pad(4) + '"siteList": %s\n' % get_site_list(settings_id, 4, debug=debug)

    str += pad(4) + '"siteName": "%s",\n' % escape(site_name) if (site_name != None) else ''
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
#debug = True

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
