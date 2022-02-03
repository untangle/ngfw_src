#! /usr/bin/python3
# $Id: reports-generate-reports.py 38486 2014-08-21 22:29:30Z cblaise $

import datetime
import getopt
import os
import os.path
import sys
import traceback


def usage():
     print("""\
usage: %s [options] <retention in days>
Options:
    -d <driver>     : "postgresql" or "sqlite"
    -h <int>        : hourly retention NOTE: Using this param will overwrite any daily retention passed in
""" % sys.argv[0])


class ArgumentParser(object):
    def __init__(self):
        pass

    def set_driver( self, arg ):
        global DRIVER
        DRIVER = arg

    def set_hourly( self, arg):
          global HOURLYRETENTION
          HOURLYRETENTION = float(arg)

    def parse_args( self ):
        handlers = {
            '-d' : self.set_driver,
            '-h' : self.set_hourly
        }

        try:
             (optlist, args) = getopt.getopt(sys.argv[1:], 'd:h:')
             for opt in optlist:
                  handlers[opt[0]](opt[1])
             return args
        except getopt.GetoptError as exc:
             print(exc)
             usage()
             exit(1)


DRIVER = 'postgresql'
PYTHON_DIR = '@PREFIX@/usr/lib/python3/dist-packages'
REPORTS_PYTHON_DIR = '%s/reports' % (PYTHON_DIR)
HOURLYRETENTION = None

if '@PREFIX@' != '':
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

cutoff = datetime.date.today() - datetime.timedelta(days=db_retention)

# If we receive anything in the hourly retention, use this instead of the daily cutoff
if HOURLYRETENTION > 0:
     cutoff = datetime.datetime.now() - datetime.timedelta(hours=HOURLYRETENTION)

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
