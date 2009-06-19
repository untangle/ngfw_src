#!/usr/bin/python

import sys
import logging
import mx
import psycopg
import getopt

def usage():
     print """\
usage: %s [options]
Options:
  -h | --help                 help
  -n | --no-migration         skip schema migration
  -c | --no-cleanup           skip cleanups
  -g | --no-data-gen          skip graph data processing
  -p | --no-plot-gen          skip graph image processing
  -d y-m-d | --date=y-m-d\
""" % sys.argv[0]

try:
     opts, args = getopt.getopt(sys.argv[1:], "hncgpvd:",
                                ['help', 'no-migration', 'no-cleanup',
                                 'no-data-gen', 'no-plot-gen', 'verbose',
                                 'date='])
except getopt.GetoptError, err:
     print str(err)
     usage()
     sys.exit(2)

logging.basicConfig(level=logging.INFO)
no_migration = False
no_cleanup = False
no_data_gen = False
no_plot_gen = False
no_mail = True
end_date = mx.DateTime.today()

no_cleanup = False
for opt in opts:
     k, v = opt
     if k == '-h' or k == '--help':
          usage()
          sys.exit(0)
     elif k == '-n' or k == '--no-migration':
          no_migration = True
     elif k == '-c' or k == '--no-cleanup':
          no_cleanup = True
     elif k == '-g' or k == '--no-data-gen':
          no_data_gen = True
     elif k == '-p' or k == '--no-plot-gen':
          no_plot_gen = True
     elif k == '-v' or k == '--verbose':
          logging.basicConfig(level=logging.DEBUG)
     elif k == '-d' or k == '--date':
          end_date = mx.DateTime.DateFrom(v)

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

if (PREFIX != ''):
     sys.path.insert(0, REPORTS_PYTHON_DIR)

import reports.engine

start_date = end_date - mx.DateTime.DateTimeDelta(30)

reports.engine.init_engine(NODE_MODULE_DIR, 'en')
if not no_migration:
     reports.engine.setup(start_date, end_date)
     reports.engine.process_fact_tables(start_date, end_date)
     reports.engine.post_facttable_setup(start_date, end_date)

if not no_data_gen:
     mail_reports = reports.engine.generate_reports(REPORTS_OUTPUT_BASE,
                                                    end_date)
     if not no_mail:
          reports.engine.generate_mail(REPORTS_OUTPUT_BASE, end_date,
                                       mail_reports[0:2])

if not no_plot_gen:
     reports.engine.generate_plots(REPORTS_OUTPUT_BASE, end_date)

if not no_cleanup:
     events_cutoff = end_date - mx.DateTime.DateTimeDelta(2)
     reports.engine.events_cleanup(events_cutoff)

     reports_cutoff = end_date - mx.DateTime.DateTimeDelta(30)
     reports.engine.reports_cleanup(reports_cutoff)
