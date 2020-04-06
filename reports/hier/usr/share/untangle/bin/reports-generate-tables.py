#! /usr/bin/env python 
# $Id: reports-generate-reports.py 38486 2014-08-21 22:29:30Z cblaise $

import mx
import os
import re
import sys
import tempfile
import time
import shutil
import datetime
import traceback
import getopt

def usage():
     print("""\
usage: %s [options]
Options:
    -d <driver>     : "postgresql" or "sqlite"
""" % sys.argv[0])

class ArgumentParser(object):
    def __init__(self):
        pass

    def set_driver( self, arg ):
        global DRIVER
        DRIVER = arg

    def parse_args( self ):
        handlers = {
            '-d' : self.set_driver,
        }

        try:
             (optlist, args) = getopt.getopt(sys.argv[1:], 'd:')
             for opt in optlist:
                  handlers[opt[0]](opt[1])
             return args
        except getopt.GetoptError, exc:
             print(exc)
             usage()
             exit(1)

DRIVER = 'postgresql'
PYTHON_DIR = '@PREFIX@/usr/lib/python2.7/dist-packages'
REPORTS_PYTHON_DIR = '%s/reports' % (PYTHON_DIR)

if '@PREFIX@' != '':
     sys.path.insert(0, PYTHON_DIR)

import reports.sql_helper as sql_helper

parser = ArgumentParser()
parser.parse_args()
sql_helper.DBDRIVER = DRIVER
#sql_helper.PRINT_TIMINGS = True

if DRIVER == "postgresql":
     os.system("createuser -U postgres -dSR untangle >/dev/null 2>&1")
     os.system("createdb -O postgres -U postgres uvm >/dev/null 2>&1");
     os.system("createlang -U postgres plpgsql uvm >/dev/null 2>&1");

if DRIVER == "sqlite":
     if not os.path.exists('/var/lib/sqlite'):
          os.makedirs('/var/lib/sqlite')

try:
     sql_helper.create_schema(sql_helper.schema());
except Exception:
     print("Failed to create schema")
     traceback.print_exc()

for f in os.listdir(REPORTS_PYTHON_DIR):
     if f.endswith('py'):
          (m, e) = os.path.splitext(f)
          if "__init__" == m:
               continue
          name = 'reports.%s' % m
          obj = __import__('reports.%s' % m)
          app = getattr(obj,m)
          #obj = eval(name)
          try:
               if "generate_tables" in dir(app):
                    # print("%s.generate_tables()" % name)
                    app.generate_tables()
          except:
               print("%s.generate_tables() Exception:" % name)
               traceback.print_exc()
               
