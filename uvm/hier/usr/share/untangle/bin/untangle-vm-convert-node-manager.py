#!/usr/bin/python

#
# This script can be removed in 10.x or after
#

import sys
import os
import re
import base64
import conversion.sql_helper as sql_helper

# maps old id in database to new uid
policy_map = None;
# stores all policies
policy_list = None;

#
# Makes a map from old policy_id's (big numbers) to new ones
#
def build_policy_map(debug=False):
    global policy_map
    global policy_list

    if (debug):
        print "Building policy map... "
 
    orig_policy_list = sql_helper.run_sql("select id, is_default, name, notes from u_policy order by id", debug=debug)
    defalut_policy = None
    defalut_policy_id = None

    # find default policy
    id = 0
    for policy in orig_policy_list:
        if policy[1] == True:
            default_policy = list(orig_policy_list.pop(id))
            default_policy.append(1)
        id = id + 1

    if default_policy == None:
        print "ERROR: missing default_policy"
        return

    # number the remaining policys with their new IDs (already sorted by ID)
    id = 0
    for policy in orig_policy_list:
        orig_policy_list[id] = list(policy)
        orig_policy_list[id].append(id+2)
        id = id + 1

    # build the policy map and list
    policy_map = {}
    policy_list = []

    # add the default first
    policy_map[default_policy[0]] = 1
    policy_list.append(default_policy);

    # add the rest
    for policy in orig_policy_list:
        policy_map[policy[0]] = policy[4]
        policy_list.append(policy);


def build_node(nodeId, nodeName, targetState, old_policy_id, debug=False):
    global policy_map
    str = '\t\t{\n'
    str += '\t\t\t"javaClass": "com.untangle.uvm.node.NodeSettings",\n'
    str += '\t\t\t"id": "%s",\n' % nodeId
    str += '\t\t\t"nodeName": "%s",\n' % nodeName
    if old_policy_id != None:
        str += '\t\t\t"policyId": "%s",\n' % policy_map[old_policy_id]
    str += '\t\t\t"targetState": "%s"\n' % targetState
    str += '\t\t}\n'
    return str
    

def get_nodes(debug=False):
    if (debug):
        print "Getting nodes... "

    nodes_list = sql_helper.run_sql("select tid, u_node_persistent_state.name, target_state, policy_id from u_node_persistent_state join u_tid on u_node_persistent_state.tid = u_tid.id left join u_policy on u_tid.policy_id = u_policy.id order by tid", debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'

    first = True
    id = 0
    for node in nodes_list:
        tid = node[0];
        name = node[1];
        target_state = (node[2]).upper();
        policy_id = node[3]

        if not first:
            str += '\t\t,\n'

        str += build_node(tid, name, target_state, policy_id, debug=debug)

        first = False
        id = id + 1

    str += '\n\t\t]\n'
    str += '\t}\n'
    
    return str

def get_settings(debug=False):
    str = "{\n"
    str += '\t"javaClass": "com.untangle.uvm.node.NodeManagerSettings",\n'
    str += '\t"nodes": %s\n' % get_nodes(debug=debug)
    str += '}\n'

    return str

filename = None
if len(sys.argv) < 1:
    print "usage: %s [filename]" % sys.argv[0]
    sys.exit(1)

if len(sys.argv) > 1:
    filename = sys.argv[1]

try:
    dir = "/usr/share/untangle/settings/untangle-vm/"
    if not os.path.exists(dir):
        os.makedirs(dir)

    build_policy_map()

    settings_str = get_settings(debug=True)
    print settings_str
    if filename == None:
        filename = "/usr/share/untangle/settings/untangle-vm/node_manager.js"
    file = open(filename, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("could not get result",e);
    sys.exit(1)

sys.exit(0)
