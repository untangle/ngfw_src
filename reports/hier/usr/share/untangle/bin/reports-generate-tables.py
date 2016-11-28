#! /usr/bin/env python 
# $Id: reports-generate-reports.py 38486 2014-08-21 22:29:30Z cblaise $

import getopt, logging, mx, os, os.path, re, sys, tempfile, time, shutil, datetime, traceback
from subprocess import Popen, PIPE
from psycopg2.extensions import DateFromMx, TimestampFromMx
from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_node_settings

def usage():
     print """\
usage: %s [options]
Options:
    -d <driver>     : "postgresql" or "sqlite"
""" % sys.argv[0]

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
             print exc
             usage()
             exit(1)

DRIVER = 'postgresql'
PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python%d.%d' % (PREFIX, sys.version_info[0], sys.version_info[1])
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

if (PREFIX != ''):
     sys.path.insert(0, REPORTS_PYTHON_DIR)

import reports.engine
import reports.sql_helper as sql_helper
from reports.log import *
logger = getLogger(__name__)

parser = ArgumentParser()
parser.parse_args()

sql_helper.DBDRIVER = DRIVER

if DRIVER == "postgresql":
     sql_helper.SCHEMA = "reports"
elif DRIVER == "sqlite":
     sql_helper.SCHEMA = "main"
else:
     logger.warn("Unknown driver: " + driver)
     sys.exit(1)

os.system("createuser -U postgres -dSR untangle >/dev/null 2>&1")
os.system("createdb -O postgres -U postgres uvm >/dev/null 2>&1");
os.system("createlang -U postgres plpgsql uvm >/dev/null 2>&1");

reports.engine.init_engine(NODE_MODULE_DIR)

try:
     sql_helper.create_schema(sql_helper.SCHEMA);
except Exception:
     logger.warn("Failed to create schema", exc_info=True)

try:
     reports.engine.create_tables()
except Exception:
     logger.warn("Failed to create tables", exc_info=True)
