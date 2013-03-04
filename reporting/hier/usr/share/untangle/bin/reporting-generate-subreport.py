#! /usr/bin/env python 
# $Id: reporting-generate-subreport.py,v 1.00 2012/08/30 15:16:16 dmorris Exp $

import getopt
import logging
import mx
import mx.DateTime
import socket
import sys
import time

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.6' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

if (PREFIX != ''):
     sys.path.insert(0, REPORTS_PYTHON_DIR)

import reports.i18n_helper
import reports.engine

def usage():
     print """\
usage: %s [options]
Options:
  -n | --node                 node name
  -e | --end-date             end-date
  -d | --report-days          # of days
  -h | --host                 host [optional]
  -u | --user                 user [optional]
  -e | --email                email [optional]


""" % sys.argv[0]


try:
     opts, args = getopt.getopt(sys.argv[1:], "n:e:d:h:u:e:",
                                ['node=', 'end-date=', 'report-days=', 
                                 'host=', 'user=', 'email='])

except getopt.GetoptError, err:
     print str(err)
     usage()
     sys.exit(2)

node_name = None
end_date = None
report_days = None
host = None
user = None
email = None

for opt in opts:
     k, v = opt
     if k == '-n' or k == '--node':
         node_name = v
     elif k == '-e' or k == '--end-date':
          end_date = mx.DateTime.DateFrom(v)
     elif k == '-d' or k == '--report-days':
          report_days = int(v)
     elif k == '-h' or k == '--host':
          host = v
     elif k == '-u' or k == '--user':
          user = v
     elif k == '-e' or k == '--email':
          email = v
     else:
         usage()
         sys.exit(2)


reports.engine.init_engine(NODE_MODULE_DIR)
reports.engine.generate_sub_report(REPORTS_OUTPUT_BASE, node_name, end_date, report_days, host, user, email)
