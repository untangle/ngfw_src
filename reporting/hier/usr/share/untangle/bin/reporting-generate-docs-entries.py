#!/usr/bin/python -u

import subprocess
import sys
import copy
import json
import traceback

dict = {}

# read the report entries and put in dictionary
p = subprocess.Popen(["sh","-c","/usr/bin/find /usr/share/untangle/lib -path '*/reports/*.js' -print"], stdout=subprocess.PIPE)
for line in iter(p.stdout.readline, ''):
    filename = line.strip()

    try:
        json_obj = json.loads(open(filename, 'r').read())

        category = str(json_obj['category'])

        if dict.get(category) == None:
            dict[category] = []
        dict[category].append(json_obj)

    except Exception, e:
        print "Exception: %s" % filename, e
        traceback.print_exc(e)
        sys.exit(1)

# print the report entries
for category in dict:
    list = dict[category]
    list = sorted( list, key=lambda item: item['displayOrder'] )

    print 
    print "== %s Reports == " % category
    print 
    print "{| border=\"1\" cellpadding=\"2\""
    print "!Report Entry"
    print "!Description"
    print "|-"

    for i in list:
        print "|%s" % i['title']
        print "|%s" % i['description']
        print "|-"

    print "|}"
