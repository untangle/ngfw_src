#! /usr/bin/env python 
# $Id: reports-generate-reports.py 38486 2014-08-21 22:29:30Z cblaise $

import getopt, logging, mx, os, os.path, re, sys, tempfile, time, shutil, datetime, traceback
from subprocess import Popen, PIPE
from psycopg2.extensions import DateFromMx, TimestampFromMx

def usage():
     print("""\
usage: %s [options] <retention in days>
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
PREFIX = '@PREFIX@'
PYTHON_DIR = '%s/usr/lib/python%d.%d/dist-packages' % (PREFIX, sys.version_info[0], sys.version_info[1])
REPORTS_PYTHON_DIR = '%s/reports' % (PYTHON_DIR)

if (PREFIX != ''):
     sys.path.insert(0, PYTHON_DIR)

import reports.sql_helper as sql_helper

parser = ArgumentParser()
args = parser.parse_args()
sql_helper.DBDRIVER = DRIVER
#sql_helper.PRINT_TIMINGS = True

if len(args) < 1:
     usage()
     sys.exit(1)

db_retention = None
try:
     db_retention = float(args[0])
except:
     usage()
     
cutoff = mx.DateTime.today() - mx.DateTime.DateTimeDelta(db_retention)

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
               if "cleanup_tables" in dir(app):
                    # print("%s.cleanup_tables()" % name)
                    app.cleanup_tables( cutoff )
          except:
               print("%s.cleanup_tables() Exception:" % name)
               traceback.print_exc()
    

