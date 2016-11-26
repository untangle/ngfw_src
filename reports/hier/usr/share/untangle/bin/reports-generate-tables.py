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
