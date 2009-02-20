#!/usr/bin/python

import sys
import logging
import psycopg
import mx

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

logging.basicConfig(level=logging.DEBUG)

if (PREFIX != ''):
     sys.path.append(REPORTS_PYTHON_DIR)

import reports.engine

end_date = mx.DateTime.today()
start_date = end_date - mx.DateTime.DateTimeDelta(30)

reports.engine.init_engine(NODE_MODULE_DIR)
reports.engine.setup(start_date, end_date)
