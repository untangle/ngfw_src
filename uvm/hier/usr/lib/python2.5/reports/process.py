#!/usr/bin/python

import sys
import logging
import mx
import psycopg

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

logging.basicConfig(level=logging.DEBUG)

if (PREFIX != ''):
     sys.path.append(REPORTS_PYTHON_DIR)

import reports.engine

#end_date = mx.DateTime.today()
end_date = mx.DateTime.Date(2009, 2, 11)
start_date = end_date - mx.DateTime.DateTimeDelta(30)

reports.engine.init_engine(NODE_MODULE_DIR, 'en')
reports.engine.setup(start_date, end_date)
reports.engine.process_fact_tables(start_date, end_date)
reports.engine.generate_reports(REPORTS_OUTPUT_BASE, end_date)
