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

def get_immutables_list(sid, indent, debug=False):

    if (debug):
        print "Getting n_ips_immutable_variables list for SID: ",sid

    list = sql_helper.run_sql("select variable_id from n_ips_immutable_variables where setting_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.LinkedList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.LinkedList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        variable_id = settings[0]

        item = sql_helper.run_sql("select variable, definition, description from n_ips_variable where variable_id = '%s'" % variable_id, debug=debug)

        if (len(item) != 1):
            print "WARNING: invalid number of n_ips_immutable_variables results (%i) for VID %s" % (len(item),variable_id)
            continue

        variable = item[0][0]
        definition = item[0][1]
        description = item[0][2]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"definition": "%s",\n' % escape(definition) if (definition != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else ''
        str += pad(indent+12) + '"javaClass": "com.untangle.node.ips.IpsVariable",\n'
        str += pad(indent+12) + '"variable": "%s"\n' % escape(variable) if (variable != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_variables_list(sid, indent, debug=False):

    if (debug):
        print "Getting n_ips_mutable_variables list for SID: ",sid

    list = sql_helper.run_sql("select variable_id from n_ips_mutable_variables where setting_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.LinkedList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.LinkedList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        variable_id = settings[0]

        item = sql_helper.run_sql("select variable, definition, description from n_ips_variable where variable_id = '%s'" % variable_id, debug=debug)

        if (len(item) != 1):
            print "WARNING: invalid number of n_ips_immutable_variables results (%i) for VID %s" % (len(item),variable_id)
            continue

        variable = item[0][0]
        definition = item[0][1]
        description = item[0][2]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"definition": "%s",\n' % escape(definition) if (definition != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else ''
        str += pad(indent+12) + '"javaClass": "com.untangle.node.ips.IpsVariable",\n'
        str += pad(indent+12) + '"variable": "%s"\n' % escape(variable) if (variable != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_rules_list(sid, indent, debug=False):

    if (debug):
        print "Getting n_ips_rule list for SID: ",sid

    list = sql_helper.run_sql("select rule_id, rule, sid, name, category, description, live, alert, log from n_ips_rule where settings_id = '%s'" % sid, debug=debug)

    if (len(list) == 0):
        return '{\n' + pad(indent+4) + '"javaClass": "java.util.LinkedList",\n' + pad(indent+4) + '"list": []\n' + pad(indent) + '},'

    str = '{\n'
    str += pad(indent+4) + '"javaClass": "java.util.LinkedList",\n'
    str += pad(indent+4) + '"list": [\n'

    counter = 1

    for settings in list:
        rule_id = settings[0]
        rule = settings[1]
        sid = settings[2]
        name = settings[3]
        category = settings[4]
        description = settings[5]
        live = settings[6]
        alert = settings[7]
        log = settings[8]

        if (counter != 1):
            str += ',\n'
        str += pad(indent+8) + '{\n'

        str += pad(indent+12) + '"alert": %s,\n' % alert
        str += pad(indent+12) + '"category": "%s",\n' % escape(category) if (category != None) else ''
        str += pad(indent+12) + '"description": "%s",\n' % escape(description) if (description != None) else''
        str += pad(indent+12) + '"javaClass": "com.untangle.node.ips.IpsRule",\n'
        str += pad(indent+12) + '"live": %s,\n' % live
        str += pad(indent+12) + '"log": %s,\n' % log
        str += pad(indent+12) + '"name": "%s",\n' % escape(name) if (name != None) else ''
        str += pad(indent+12) + '"sid": %s,\n' % sid
        str += pad(indent+12) + '"text": "%s"\n' % escape(rule) if (rule != None) else ''

        str += pad(indent+8) + '}'

        counter += 1

    str += '\n' + pad(indent+4) + ']\n' + pad(indent) + '},'

    return str

#------------------------------------------------------------------------------

def get_settings(tid, debug=False):

    if (debug):
        print "Getting n_ips_settings for TID: ",tid

    node_settings = sql_helper.run_sql("select settings_id, max_chunks from n_ips_settings where tid = '%s'" % tid, debug=debug)

    if node_settings == None:
        print "WARNING: missing n_ips_settings result for TID %s" % tid

    if len(node_settings) > 1:
        print "WARNING: too many n_ips_settings results (%i) for TID %s" % (len(node_settings),tid)

    settings_id = node_settings[0][0]
    max_chunks = node_settings[0][1]

    str = '{\n'
    str += pad(4) + '"immutables": %s\n' % get_immutables_list(settings_id, 4, debug)

    str += pad(4) + '"javaClass": "com.untangle.node.ips.IpsSettings",\n'

    str += pad(4) + '"maxChunks": %s,\n' % max_chunks
    
    str += pad(4) + '"rules": %s\n' % get_rules_list(settings_id, 4, debug)
    
    str += pad(4) + '"variables": %s\n' % get_variables_list(settings_id, 4, debug)

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
