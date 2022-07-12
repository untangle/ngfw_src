#!/usr/bin/python3
import os
import getopt
import sys
import subprocess
import re

def usage():
    print(("""\
usage: %s filename
example: %s network.js reviews network.js changes
""" % (sys.argv[0], sys.argv[0]) ))
    sys.exit(1)

try:
     opts, args = getopt.getopt(sys.argv[1:], "", [])
except getopt.GetoptError as err:
     print(str(err))
     usage()
     sys.exit(2)

if len(args) < 1:
    usage();

filename = args[0]

prefix = "@PREFIX@"
if prefix == "@PREFIX@": prefix = "" # for running in src
prev = None

cmd = "/usr/bin/find %s/usr/share/untangle/settings/ -type f -name '%s*' | xargs ls -1tr " % (prefix, filename)
lines = subprocess.Popen(["/bin/bash","-c",cmd], stdout=subprocess.PIPE, text=True).communicate()[0].split('\n')

print("There are %i changes:" % (len(lines)-2) )
print("\n")
for file in lines:
    print(file)
print("\n")

eval(input("Press Enter to review 1st change..."))

for file in lines:
    if file == None or file.strip() == "":
        continue
    if prev == None:
        prev = file
        continue

    print(" prev = %s, file = %s " % (prev, file))
    m = re.search(r'(\d\d\d\d)-(\d\d)-(\d\d)-(\d\d)(\d\d)', file)
    if m == None or m.group(1) == None or m.group(2) == None or m.group(3) == None or m.group(4) == None or m.group(5) == None :
        print("Unable to find date of file: %s" % file)
        continue

    year = m.group(1)
    month = m.group(2)
    day = m.group(3)
    hour = m.group(4)
    min = m.group(5)

    os.system("/usr/bin/clear")
    print("= The following changes were made at %s-%s-%s at %s:%s =\n" % (year, month, day, hour, min))
    os.system("diff -urN %s %s" % (prev, file))

    print("\n\n")
    eval(input("Press Enter to continue..."))

    prev = file
