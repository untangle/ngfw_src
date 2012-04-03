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

def get_settings(debug=False):
    if (debug):
        print "Getting n_http_settings"

    node_settings = sql_helper.run_sql("select enabled, non_http_blocked, max_header_length, block_long_headers, max_uri_length, block_long_uris from n_http_settings ", debug=debug)

    if node_settings == None:
        print "WARNING: missing n_http_settings result"

    if len(node_settings) > 1:
        print "WARNING: too many n_http_settings results (%i)" % (len(node_settings))

    enabled = node_settings[0][0]
    non_http_blocked = node_settings[0][1]
    max_header_length = node_settings[0][2]
    block_long_headers = node_settings[0][3]
    max_uri_length = node_settings[0][4]
    block_long_uris = node_settings[0][5]

    str = '{\n'
    str += pad(4) + '"blockLongHeaders": %s,\n' % block_long_headers
    str += pad(4) + '"blockLongUris": %s,\n' % block_long_uris
    str += pad(4) + '"enabled": %s,\n' % enabled
    str += pad(4) + '"javaClass": "com.untangle.node.http.HttpSettings",\n'
    str += pad(4) + '"maxHeaderLength": %s,\n' % max_header_length
    str += pad(4) + '"maxUriLength": %s,\n' % max_uri_length
    str += pad(4) + '"nonHttpBlocked": %s,\n' % non_http_blocked
    str += pad(4) + '"version": 1\n'
    str += '}\n'

    return str

#------------------------------------------------------------------------------

filename = None
if len(sys.argv) < 2:
    print "usage: %s target_filename" % sys.argv[0]
    sys.exit(1)

target = sys.argv[1]
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

    settings_str = get_settings(debug)

    if (debug):
        print settings_str

    file = open(target, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("Could not get result",e)
    sys.exit(1)

sys.exit(0)
