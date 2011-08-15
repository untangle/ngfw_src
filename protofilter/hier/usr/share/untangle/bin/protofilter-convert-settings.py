#!/usr/bin/python
import sys
import os
sys.path.insert(0,"/work")
import sql_helper

global nodeName

#------------------------------------------------------------------------------

def get_pattern_settings(tid, settings_id, debug=False):
    if (debug):
        print "Getting protofilter_patterns for TID: ",tid, " settings_id: ",settings_id

    settings_list = sql_helper.run_sql("select rule_id, metavize_id, protocol, description, category, definition, quality, blocked, alert, log, position from n_protofilter_pattern where settings_id = '%s'" % settings_id, debug=debug)

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'

    first = True
    for settings in settings_list:
        rule_id = settings[0]
        metavize_id = settings[1]
        protocol = settings[2]
        description = settings[3]
        category = settings[4]
        definition = settings[5]
        quality = settings[6]
        blocked = settings[7]
        alert = settings[8]
        log = settings[9]
        position = settings[10]

        if not first:
            str += ',\n'
        str += '\t\t\t{\n'
        str += '\t\t\t"javaClass": "com.untangle.uvm.node.GenericRule",\n'
        str += '\t\t\t"rule_id": "%s",\n' % rule_id
        str += '\t\t\t"metavize_id": "%s",\n' % metavize_id
        str += '\t\t\t"protocol": "%s"\n' % protocol
        str += '\t\t\t"description": "%s"\n' % description
        str += '\t\t\t"category": "%s"\n' % category
        str += '\t\t\t"definition": "%s"\n' % definition
        str += '\t\t\t"quality": "%s"\n' % quality
        str += '\t\t\t"blocked": "%s"\n' % blocked
        str += '\t\t\t"alert": "%s"\n' % alert
        str += '\t\t\t"log": "%s"\n' % log
        str += '\t\t\t"position": "%s"\n' % position
        str += '\t\t\t}'

        first = False

    str += '\n\t\t]\n'
    str += '\t\t}'

    return str

#------------------------------------------------------------------------------

def get_settings(tid, debug=False):
    if (debug):
        print "Getting settings for TID: ",tid

    settings_list = sql_helper.run_sql("select settings_id, bytelimit, chunklimit, unknownstring, stripzeros from n_protofilter_settings where tid = '%s'" % tid, debug=debug)

    if settings_list == None:
        print "WARNING: missing results for TID %s" % tid

    if len(settings_list) > 1:
        print "WARNING: too many results (%i) for TID %s" % (len(settings),tid)

    settings = settings_list[0]

    settings_id = settings[0]
    byte_limit = settings[1]
    chunk_limit = settings[2]
    unknown_string = settings[3]
    strip_zeros = settings[4]

    str = '{\n'
    str += '\t"javaClass": "com.untangle.node.protofilter.ProtoFilterSettings",\n'
    str += '\t"version": "1",\n'
    str += '\t"byteLimit": "%s",\n' % byte_limit
    str += '\t"chunkLimit": "%s",\n' % chunk_limit
    str += '\t"unknownString": "%s",\n' % unknown_string
    str += '\t"stripZeros": "%s",\n' % strip_zeros
    str += '\t"patterns": %s,\n' % get_pattern_settings(tid, settings_id, debug=debug)
    str += '}\n'

    return str

#------------------------------------------------------------------------------

filename = None
if len(sys.argv) < 2:
    print "usage: %s TID [filename]" % sys.argv[0]
    sys.exit(1)

if len(sys.argv) > 1:
    tid = sys.argv[1]

if len(sys.argv) > 2:
    filename = sys.argv[2]

try:
    dir = "/usr/share/untangle/settings/untangle-node-protofilter/"
    if not os.path.exists(dir):
        os.makedirs(dir)

    settings_str = get_settings(tid, debug=True)
    print settings_str
    if filename == None:
        filename = "/usr/share/untangle/settings/untangle-node-protofilter/settings_%s.js" % tid
    file = open(filename, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("could not get result",e);
    sys.exit(1)

sys.exit(0)

