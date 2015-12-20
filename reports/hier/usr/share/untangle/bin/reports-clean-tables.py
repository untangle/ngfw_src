#! /usr/bin/env python 
# $Id: reports-generate-reports.py 38486 2014-08-21 22:29:30Z cblaise $

import getopt, logging, mx, os, os.path, re, sys, tempfile, time, shutil, datetime, traceback
from subprocess import Popen, PIPE
from psycopg2.extensions import DateFromMx, TimestampFromMx
from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_node_settings

def usage():
     print """\
usage: %s [num days of data to keep]
Options:
""" % sys.argv[0]

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python%d.%d' % (PREFIX, sys.version_info[0], sys.version_info[1])
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

if (PREFIX != ''):
     sys.path.insert(0, REPORTS_PYTHON_DIR)

import reports.engine
import reports.sql_helper as sql_helper

from reports.log import *
logger = getLogger(__name__)

if len(sys.argv) < 2:
     usage()
     sys.exit(1)

db_retention = None
try:
     db_retention = float(sys.argv[1])
except:
     usage()
     
reports_cutoff = mx.DateTime.today() - mx.DateTime.DateTimeDelta(db_retention)

reports.engine.init_engine(NODE_MODULE_DIR)
reports.engine.reports_cleanup(reports_cutoff)     
    

