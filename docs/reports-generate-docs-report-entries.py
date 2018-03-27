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
    if 'appProperties.json' in line: continue
    filename = line.strip()

    try:
        json_obj = json.loads(open(filename, 'r').read())

        category = str(json_obj['category'])

        if dict.get(category) == None:
            dict[category] = []
        dict[category].append(json_obj)

    except Exception, e:
        print("Exception: %s" % filename, e)
        traceback.print_exc(e)
        sys.exit(1)

# print(the report entries)
for category in dict:
    list = dict[category]
    list = sorted( list, key=lambda item: item['displayOrder'] )

    print("")
    print("== %s Reports == " % category)
    print("<section begin='%s' />" % category )
    print("{| border=\"1\" cellpadding=\"2\" width=\"85%%\" align=\"center\" ")
    print("!Report Entry")
    print("!Description")
    print("|-")

    for i in list:
        print("| width=\"25%%\" | %s"  % i['title'])
        print("| width=\"60%%\" | %s" % i['description'])
        print("|-")

    print("|}")
    print("<section end='%s' />" % category)
    print("")
