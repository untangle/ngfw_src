#!/usr/bin/python

#
# This script can be removed in 10.x or after
#

import conversion.sql_helper as sql_helper
import sys
import os
import re
import base64

def get_users(debug=False):
    if (debug):
        print "Getting user list"

    str = '\t{\n'
    str += '\t\t"javaClass": "java.util.LinkedList",\n'
    str += '\t\t"list": [\n'
    
    user_list = sql_helper.run_sql("select login, email, encode(password,'base64'), name from settings.u_user where write_access = true and admin_setting_id is not null")

    if user_list == None:
        print "WARNING: missing users"
        return
    if len(user_list) < 1:
        print "WARNING: no users"

    first = True
    for user in user_list:
        username = user[0]
        emailAddress = user[1]
        passwordHashBase64 = user[2]
        description = user[3]

        if not first:
            str += '\t\t\t,\n'
        str += '\t\t\t{\n';
        str += '\t\t\t"javaClass": "com.untangle.uvm.AdminUserSettings",\n';
        str += '\t\t\t"username": "%s",\n' % username;
        str += '\t\t\t"emailAddress": "%s",\n' % emailAddress;
        str += '\t\t\t"passwordHashBase64": "%s",\n' % passwordHashBase64;
        str += '\t\t\t"description": "%s"\n' % description;
        str += '\t\t\t}\n';
        first = False
        
    str += '\t\t]\n'
    str += '\t}'

    return str


def get_settings(debug=False):
    str = "{\n"
    str += '\t"javaClass": "com.untangle.uvm.AdminSettings",\n'
    str += '\t"users": %s\n' % get_users(debug)
    str += "}"

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

    settings_str = get_settings()
    print "Settings:" 
    print settings_str
    if filename == None:
        filename = "/usr/share/untangle/settings/untangle-vm/admin.js"
    file = open(filename, 'w')
    file.write(settings_str)
    file.close()

except Exception, e:
    print("could not get result",e);
    sys.exit(1)

sys.exit(0)
