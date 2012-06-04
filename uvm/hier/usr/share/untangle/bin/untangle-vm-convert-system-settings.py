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
        print "Getting u_access_settings"

    access_settings = sql_helper.run_sql("select allow_insecure, allow_outside_admin, allow_outside_quaran, allow_outside_report, allow_ssh from u_access_settings ", debug=debug)
    if access_settings == None:
        print "WARNING: missing u_access_settings result"
    if len(access_settings) > 1:
        print "WARNING: too many u_access_settings results (%i)" % (len(access_settings))

    address_settings = sql_helper.run_sql("select https_port, is_hostname_public, has_public_address, public_ip_addr, public_port from u_address_settings ", debug=debug)
    if address_settings == None:
        print "WARNING: missing u_address_settings result"
    if len(address_settings) > 1:
        print "WARNING: too many u_address_settings results (%i)" % (len(address_settings))

    inside_http_enabled = access_settings[0][0]
    outside_https_admin_enabled = access_settings[0][1]
    outside_https_quarantine_enabled = access_settings[0][2]
    outside_https_report_enabled = access_settings[0][3]
    support_enabled = access_settings[0][4]
    outside_https_enabled = outside_https_admin_enabled or outside_https_report_enabled or outside_https_quarantine_enabled

    https_port = address_settings[0][0]
    is_hostname_public = address_settings[0][1]
    has_public_address = address_settings[0][2]
    public_ip_addr = address_settings[0][3]
    public_port = address_settings[0][4]

    public_url_method = "external"
    if is_hostname_public:
        public_url_method = "hostname"
    if has_public_address:
        public_url_method = "address_and_port"

    str = '{\n'
    str += pad(4) + '"javaClass": "com.untangle.uvm.SystemSettings",\n'

    str += pad(4) + '"supportEnabled": "%s",\n' % support_enabled
    str += pad(4) + '"insideHttpEnabled": "%s",\n' % inside_http_enabled
    str += pad(4) + '"outsideHttpsEnabled": "%s",\n' % outside_https_enabled
    str += pad(4) + '"outsideHttpsReportingEnabled": "%s",\n' % outside_https_report_enabled
    str += pad(4) + '"outsideHttpsAdministrationEnabled": "%s",\n' % outside_https_admin_enabled
    str += pad(4) + '"outsideHttpsQuarantineEnabled": "%s",\n' % outside_https_quarantine_enabled

    str += pad(4) + '"httpsPort": "%s",\n' % https_port
    str += pad(4) + '"publicUrlMethod": "%s",\n' % public_url_method
    str += pad(4) + '"publicUrlAddress": "%s",\n' % public_ip_addr
    str += pad(4) + '"publicUrlPort": "%s"\n' % public_port

    str += '}\n'

    return str

#------------------------------------------------------------------------------

filename = None
if len(sys.argv) < 2:
    print "usage: %s target_filename" % sys.argv[0]
    sys.exit(1)

target = sys.argv[1]
#debug = False
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
