#!/usr/bin/python
import conversion.sql_helper as sql_helper
import simplejson
import sys
import os
import string

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
        print "Getting u_mail_settings"

    mail_settings = sql_helper.run_sql(" select smtp_host, from_address, smtp_port, auth_user, auth_pass, use_mx_records from u_mail_settings ", debug=debug)
    if mail_settings == None:
        print "WARNING: missing u_mail_settings result"
    if len(mail_settings) > 1:
        print "WARNING: too many u_mail_settings results (%i)" % (len(mail_settings))

    smtp_host = sql_helper.sanitize_string(mail_settings[0][0])
    from_address = sql_helper.sanitize_string(mail_settings[0][1])
    smtp_port = mail_settings[0][2]
    auth_user = sql_helper.sanitize_string(mail_settings[0][3])
    auth_pass = sql_helper.sanitize_string(mail_settings[0][4])
    use_mx_records = mail_settings[0][5]

    str = '{\n'
    str += pad(4) + '"javaClass": "com.untangle.uvm.MailSettings",\n'

    str += pad(4) + '"useMxRecords": "%s",\n' % use_mx_records
    str += pad(4) + '"smtpHost": "%s",\n' % smtp_host
    str += pad(4) + '"smtpPort": "%s",\n' % smtp_port
    str += pad(4) + '"fromAddress": "%s",\n' % from_address
    str += pad(4) + '"authUser": "%s",\n' % auth_user
    str += pad(4) + '"authPass": "%s"\n' % auth_pass

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
