#!/usr/bin/python

#
# This script can be removed in 10.x or after
#

import conversion.sql_helper as sql_helper
import sys
import os

def build_rule(id, live, description, block, log, protocol_matcher, src_addr_matcher, dst_addr_matcher, src_intf_matcher, dst_intf_matcher, src_port_matcher, dst_port_matcher):
    str = ""
    str += '\t\t\t{\n'
    str += '\t\t\t"javaClass": "com.untangle.node.firewall.FirewallRule",\n'
    str += '\t\t\t"id": "%s",\n' % id
    str += '\t\t\t"live": "%s",\n' % live
    str += '\t\t\t"description": "%s",\n' % description
    str += '\t\t\t"block": "%s",\n' % block
    str += '\t\t\t"log": "%s",\n' % log

    str += '\t\t\t"rules": {\n'
    str += '\t\t\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t\t\t"list": [\n'
    
    if (src_addr_matcher != "any"): str += build_rule_matcher("SRC_ADDR",src_addr_matcher,True)
    if (dst_addr_matcher != "any"): str += build_rule_matcher("DST_ADDR",dst_addr_matcher,True)
    if (src_intf_matcher != "any"): str += build_rule_matcher("SRC_INTF",src_intf_matcher,True)
    if (dst_intf_matcher != "any"): str += build_rule_matcher("DST_INTF",dst_intf_matcher,True)
    if (src_port_matcher != "any"): str += build_rule_matcher("SRC_PORT",src_port_matcher,True)
    if (dst_port_matcher != "any"): str += build_rule_matcher("DST_PORT",dst_port_matcher,True)
    # always add protocol just to deal with the comma
    str += build_rule_matcher("PROTOCOL",protocol_matcher,False)
        
    str += '\t\t\t\t]\n'
    str += '\t\t\t}'
    return str

def build_rule_matcher(matcherType, value, default_pass, comma=True):
    str = ""
    str += '\t\t\t\t\t{\n'
    str += '\t\t\t\t\t"javaClass": "com.untangle.node.firewall.FirewallRuleMatcher",\n'
    str += '\t\t\t\t\t"matcherType": "%s",\n' % matcherType
    str += '\t\t\t\t\t"value": "%s",\n' % value
    if (comma):
        str += '\t\t\t\t\t},\n'
    else:
        str += '\t\t\t\t\t}\n'
    return str

def get_rules(tid, settings_id, default_pass, debug=False):
    if (debug):
        print "Getting rules for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select position, live, name, description, is_traffic_blocker as block, log, protocol_matcher, src_ip_matcher, dst_ip_matcher, src_port_matcher, dst_port_matcher, src_intf_matcher, dst_intf_matcher from n_firewall_rule where settings_id = '%s' order by position" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    first = True
    id = 0
    for settings in settings_list:
        position = settings[0];
        live = settings[1];
        name = settings[2]; #unused?
        description = settings[3];
        block = settings[4];
        log = settings[5];
        protocol_matcher = settings[6];
        src_addr_matcher = settings[7];
        dst_addr_matcher = settings[8];
        src_port_matcher = settings[9];
        dst_port_matcher = settings[10];
        src_intf_matcher = settings[11];
        dst_intf_matcher = settings[12];

        if not first:
            str += ',\n'
        str += build_rule(id, live, description, block, log, protocol_matcher, src_addr_matcher, dst_addr_matcher, src_intf_matcher, dst_intf_matcher, src_port_matcher, dst_port_matcher)

        first = False
        id = id + 1

    # handle the old "default" behavior as a rule at the end of the list
    if not default_pass:
        if not first:
            str += ',\n'
        str += build_rule(id, True, "Default Block", True, True, "any", "any", "any", "any", "any", "any", "any")

    str += '\n\t\t]\n'
    str += '\t\t}'
    
    return str


def get_settings(tid, debug=False):
    if (debug):
        print "Getting settings for TID: ",tid

    settings_list = sql_helper.run_sql("select settings_id, is_default_accept from n_firewall_settings where tid = '%s'" % tid, debug=debug)

    if settings_list == None:
        print "WARNING: missing results for TID %s" % tid
        return ""

    if len(settings_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)
        
    settings_id = settings_list[0][0]
    default_pass = settings_list[0][1]

    str = '{\n'
    str += '\t"javaClass": "com.untangle.node.firewall.FirewallSettings",\n'
    str += '\t"version": "1",\n' 
    str += '\t"rules": %s\n' % get_rules(tid, settings_id, default_pass, debug=debug)
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
        filename = "/usr/share/untangle/settings/untangle-node-firewall/settings_%s.js" % tid
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
