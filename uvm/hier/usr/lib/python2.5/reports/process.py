#!/usr/bin/python

import getopt
import logging
import mx
import os
import psycopg
import sys

def usage():
     print """\
usage: %s [options]
Options:
  -h | --help                 help
  -n | --no-migration         skip schema migration
  -c | --no-cleanup           skip cleanups
  -g | --no-data-gen          skip graph data processing
  -p | --no-plot-gen          skip graph image processing
  -m | --no-mail              skip mailing
  -e | --events-days          number of days in events schema to keep
  -r | --reports-days         number of days in reports schema to keep
  -d y-m-d | --date=y-m-d\
""" % sys.argv[0]

try:
     opts, args = getopt.getopt(sys.argv[1:], "hncgpmve:r:d:",
                                ['help', 'no-migration', 'no-cleanup',
                                 'no-data-gen', 'no-mail', 'no-plot-gen',
                                 'verbose', 'events-days', 'reports-days',
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
no_mail = False
events_days = 3
reports_days = None
end_date = mx.DateTime.today()
locale = 'en'

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
     elif k == '-m' or k == '--no-mail':
          no_mail = True
     elif k == '-e' or k == '--events-days':
          events_days = int(v)
     elif k == '-r' or k == '--reports-days':
          reports_days = int(v)
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

import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper
import reports.mailer

start_date = end_date - mx.DateTime.DateTimeDelta(30)

if (sql_helper.table_exists('reports', 'daystoadd')
    or sql_helper.table_exists('reports', 'webpages')
    or sql_helper.table_exists('reports', 'emails')):
     try:
          sql_helper.run_sql('DROP SCHEMA reports CASCADE')
     except psycopg.ProgrammingError, e:
          print e

try:
     sql_helper.run_sql("CREATE SCHEMA reports");
except Exception:
     pass

try:
     sql_helper.run_sql("""\
CREATE TABLE reports.report_data_days (
        day_name text NOT NULL,
        day_begin date NOT NULL)""")
except Exception:
     pass

try:
     sql_helper.run_sql("""\
CREATE TABLE reports.table_updates (
    tablename text NOT NULL,
    last_update timestamp NOT NULL,
    PRIMARY KEY (tablename))""")
except Exception:
     pass

if not reports_days:
     conn = sql_helper.get_connection()

     try:
          curs = conn.cursor()
          curs.execute("""\
SELECT days_to_keep FROM settings.n_reporting_settings
JOIN u_node_persistent_state USING (tid)
WHERE target_state = 'running' OR target_state = 'initialized'
""")
          r = curs.fetchone()
          if r:
               reports_days = r[0]
          else:
               reports_days = 7
     except Exception, e:
          logging.warn("could not get report_days %s" % e)

if not reports_days:
     reports_days = 7

reports.engine.init_engine(NODE_MODULE_DIR)
if not no_migration:
     reports.engine.setup(start_date, end_date)
     reports.engine.process_fact_tables(start_date, end_date)
     reports.engine.post_facttable_setup(start_date, end_date)

mail_reports = []

if not no_data_gen:
     mail_reports = reports.engine.generate_reports(REPORTS_OUTPUT_BASE,
                                                    end_date)

if not no_plot_gen:
     reports.engine.generate_plots(REPORTS_OUTPUT_BASE, end_date)

if not no_mail:
     f = reports.pdf.generate_pdf(REPORTS_OUTPUT_BASE, end_date, mail_reports)
     reports.mailer.mail_reports(end_date, f)
     os.remove(f)

if not no_cleanup:
     events_cutoff = end_date - mx.DateTime.DateTimeDelta(events_days)
     reports.engine.events_cleanup(events_cutoff)

     reports_cutoff = end_date - mx.DateTime.DateTimeDelta(2 * reports_days)
     reports.engine.reports_cleanup(reports_cutoff)

     reports_cutoff = end_date - mx.DateTime.DateTimeDelta(reports_days)
     reports.engine.delete_old_reports(REPORTS_OUTPUT_BASE, reports_cutoff)
