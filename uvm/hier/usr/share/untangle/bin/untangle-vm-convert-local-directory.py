#!/usr/bin/python

#
# This script can be removed in 10.x or after
#

import sys
import os
import re
import base64

def get_settings(debug=False):
    str = "{\n"
    str += '\t"javaClass": "java.util.LinkedList",\n'
    str += '\t"list": [\n'
    
    output = []

    uidRegex = re.compile('^.*uid=(.*),.*$')

    child_stdin, child_stdout, child_stderr = os.popen3("/usr/sbin/slapcat -f /etc/untangle-ldap/slapd.conf")
    output = child_stdout.readlines()

    currentUser = {}
    firstUser = True

    for line in output:
        line = line.rstrip()

        if line == "":
            if debug: print "---- "
            # If its a valid user (must have a username) print out the JSON
            if len(currentUser) > 0 and 'username' in currentUser:
                
                if firstUser: 
                    str += '\t{\n'
                    firstUser = False
                else:
                    str += '\t,{\n'
                str += '\t\t"javaClass": "com.untangle.uvm.LocalDirectoryUser",\n'

                count=0
                for k,v in currentUser.iteritems():
                    count=count+1
                    str += '\t\t"%s": "%s"'%(k,v)
                    if (count == len(currentUser)): 
                        str += "\n"
                    else:
                        str += ",\n"

                str += '\t}\n'

            currentUser = {} # reset currentUser for next user
                
        if line.find("uid=") > 0:
            matches = uidRegex.match(line)
            if matches != None and len(matches.groups()) > 0:
                currentUser['username'] = matches.groups()[0]
                if debug: print 'username: "%s"' % currentUser['username']

        if line.startswith("userPassword:: "):
            splits = line.split(" ")
            if len(splits) > 1:
                currentUser['password'] = base64.b64decode(splits[1])
                if debug: print 'password: "%s"' % currentUser['password']

        if line.startswith("mail: "):
            splits = line.split(" ")
            if len(splits) > 1:
                currentUser['email'] = splits[1]
                if debug: print 'email: "%s"' % currentUser['email']

        if line.startswith("cn: "):
            splits = line.split(" ")
            if len(splits) > 1:
                currentUser['firstName'] = splits[1]
                if debug: print 'firstName: "%s"' % currentUser['firstName']
            if len(splits) > 2:
                currentUser['lastName'] = splits[2]
                if debug: print 'lastName: "%s"' % currentUser['lastName']

    str += '\t]\n'
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

    settings_str = get_settings()
    print settings_str
    if filename == None:
        filename = "/usr/share/untangle/settings/untangle-vm/local_directory.js"
    file = open(filename, 'w')
    file.write(settings_str)
    file.close()

    os.system('/etc/init.d/untangle-slapd stop')
    os.system('/etc/init.d/slapd stop')
    os.system('update-rc.d -f untangle-slapd remove')
    os.system('update-rc.d -f slapd remove')

except Exception, e:
    print("could not get result",e);
    sys.exit(1)

sys.exit(0)
