#!/usr/bin/python3

#
# A simple utility to check the validity of a license
# This checks by reading the licenses.js file directly 
#
# If a valid license is found, it exits with a return code of 0
# Otherwise 1
#
#

import simplejson as json
import sys
import time

LICENSES_JSON_OBJ = json.loads(open('/usr/share/untangle/conf/licenses/licenses.js', 'r').read())

def checkLicense(app):
    wans = []

    try:
        if LICENSES_JSON_OBJ == None:
            return False;
        if LICENSES_JSON_OBJ['licenses'] == None:
            return False;
        if LICENSES_JSON_OBJ['licenses']['list'] == None:
            return False;

        for license in LICENSES_JSON_OBJ['licenses']['list']:
            if license['name'] == None:
                continue;
            if license['name'] != app:
                continue;

            validity = license['valid']

            if validity is None:
                print("License found: Invalid")
                return False;

            # if its a string type, convert to boolean
            if (isinstance(validity, str)):
                validity = ((validity == 'True') or (validity == 'true')) 

            if validity == True:
                print("License found: Valid")
                return True;
            if validity == False:
                print("License found: Invalid")
                return False;

    except Exception as e:
        print("Exception:", e)
        return False;

    print("License not found for: " + app)
    return False;


if len(sys.argv) < 2:
    print("usage: %s app" % sys.argv[0])
    sys.exit(1)

isValid = checkLicense(sys.argv[1])

if (isValid):
    sys.exit(0);
else:
    sys.exit(1);
