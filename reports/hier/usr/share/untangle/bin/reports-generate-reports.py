#! /usr/bin/env python 
# $Id: reporting-generate-reports.py 39898 2015-03-18 22:55:24Z dmorris $

import getopt, logging, mx, os, os.path, re, sys, tempfile, time, shutil, datetime, traceback
from subprocess import Popen, PIPE
from psycopg2.extensions import DateFromMx, TimestampFromMx
from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_node_settings

def usage():
     print """\
usage: %s [options]
Options:
  -h | --help                   help
  -g | --no-data-gen            skip graph data processing
  -p | --no-plot-gen            skip graph image processing
  -m | --no-mail                skip mailing
  -a | --attach-csv             attach events as csv
  -r | --report-length          number of days to report on
  -s <cs> | --simulate=<cs>     run reports of the remote DB specified by the connection string
  -d <y-m-d> | --date=<y-m-d>   run reports for the specific date
""" % sys.argv[0]

## main
PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python%d.%d' % (PREFIX, sys.version_info[0], sys.version_info[1])
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR
LOCKFILE = "/var/run/untangle-reports.pid"

if (PREFIX != ''):
     sys.path.insert(0, REPORTS_PYTHON_DIR)

from reports.log import *
logger = getLogger(__name__)

try:
     opts, args = getopt.getopt(sys.argv[1:], "hgpmiaver:d:l:t:s:b:",
                                ['help', 'no-cleanup',
                                 'no-data-gen', 'no-mail', 
                                 'no-plot-gen', 'verbose', 'attach-csv',
                                 'events-retention', 'report-length',
                                 'date=', 'simulate=', 'behave='])

except getopt.GetoptError, err:
     print str(err)
     usage()
     sys.exit(2)

report_lengths = None
no_cleanup = False
no_data_gen = False
no_plot_gen = False
no_mail = False
attach_csv = False
attachment_size_limit = None
end_date = mx.DateTime.today()
start_time = mx.DateTime.now()
db_retention = None
file_retention = None
simulate = None
no_cleanup = False

for opt in opts:
     k, v = opt
     if k == '-h' or k == '--help':
          usage()
          sys.exit(0)
     elif k == '-g' or k == '--no-data-gen':
          no_data_gen = True
     elif k == '-p' or k == '--no-plot-gen':
          no_plot_gen = True
     elif k == '-m' or k == '--no-mail':
          no_mail = True
     elif k == '-a' or k == '--attach-csv':
          attach_csv = True
     elif k == '-r' or k == '--report-length':
          report_lengths = [int(v)]
     elif k == '-v' or k == '--verbose':
          setConsoleLogLevel(logging.DEBUG)
     elif k == '-d' or k == '--date':
          end_date = mx.DateTime.strptime(v, "%Y-%m-%d")
     elif k == '-s' or k == '--simulate':
          simulate = v

def verifyPidFile():
     if not os.path.isfile(LOCKFILE):
          return False

     pid = open(LOCKFILE).readline().strip()
     cmdFile = "/proc/%s/cmdline" % pid

     if os.path.isfile(cmdFile) and open(cmdFile).readline().find(sys.argv[0]) > 0:
          return pid
     else:
          return False

# lock 1st
if os.path.isfile(LOCKFILE):
     pid = verifyPidFile()
     if pid:
          logger.warning("Reports are already running (pid %s)..." % pid)
          slept = 0
          while pid:
               if (slept % 60) == 0:
                    logger.warning("... waiting on pid %s" % pid)
               time.sleep(1)
               pid = verifyPidFile()
               slept += 1
     else:
          logger.info("Removing leftover pidfile (pid %s)" % pid)
          os.remove(LOCKFILE)

pidfile = open(LOCKFILE, "w")
pidfile.write("%s\n" % os.getpid())
pidfile.close()
logger.info("Wrote pidfile (pid: %s)" % os.getpid())

import reports.engine
import reports.mailer
import reports.sql_helper as sql_helper
import uvm.i18n_helper

if simulate:
     sql_helper.SCHEMA = 'reports_simulation'
     sql_helper.CONNECTION_STRING = simulate

def get_day_name(num):
     if num == 1:
          return "sunday"
     if num == 2:
          return "monday"
     if num == 3:
          return "tuesday"
     if num == 4:
          return "wednesday"
     if num == 5:
          return "thursday"
     if num == 6:
          return "friday"
     if num == 7:
          return "saturday"

def get_report_lengths(date):
    lengths = []

    current_day_of_week = ((date.day_of_week + 1) % 7) + 1
    logger.debug("current_day_of_week: %s" % (current_day_of_week,))
    conn = sql_helper.get_connection()

    dailySched = get_node_settings_item('untangle-node-reports','generateDailyReports')
    if dailySched != None:
         dailySched = dailySched.lower()
         if str(current_day_of_week) in dailySched or get_day_name(current_day_of_week) in dailySched or "any" in dailySched:
              lengths.append(1)

    weeklySched = get_node_settings_item('untangle-node-reports','generateWeeklyReports')
    if weeklySched != None:
         weeklySched = weeklySched.lower()
         if str(current_day_of_week) in weeklySched or get_day_name(current_day_of_week) in weeklySched or "any" in weeklySched:
              lengths.append(7)

    monthlySched = get_node_settings_item('untangle-node-reports','generateMonthlyReports')
    if monthlySched != None:
         if monthlySched and date.day == 1:              
              prev_month = date - datetime.timedelta(days=1)
              prev_month_days = prev_month.days_in_month
              lengths.append(prev_month_days)

    logger.info("Generating reports for the following lengths: %s" % (lengths,))
    return lengths

def write_cutoff_date(date):
     if not sql_helper.table_exists( 'reports_state' ):
          sql_helper.run_sql("""CREATE TABLE reports.reports_state (last_cutoff timestamp NOT NULL)""")

     update = False

     conn = sql_helper.get_connection()
     try:
          curs = conn.cursor()
          curs.execute('SELECT * FROM reports.reports_state')

          if curs.rowcount > 0:
               update = True
          else:
               update = False

          conn.commit()
     except Exception, e:
          conn.rollback()
          logger.warn("could not get db_retention", exc_info=True)

     conn = sql_helper.get_connection()
     try:
          curs = conn.cursor()

          if update:
               curs.execute('UPDATE reports.reports_state SET last_cutoff = %s',
                            (date,))
          else:
               curs.execute("""\
INSERT INTO reports.reports_state (last_cutoff) VALUES (%s)""", (date,))

          conn.commit()
     except Exception, e:
          conn.rollback()
          logger.warn("could not set reports' last_cutoff", exc_info=True)

## main
total_start_time = time.time()

os.system(PREFIX + "/usr/share/untangle/bin/reporting-generate-tables.py")

if not report_lengths:
     report_lengths = get_report_lengths(end_date)

running = False
for instance in Popen([PREFIX + "/usr/bin/ucli", "instances"], stdout=PIPE, stderr=PIPE).communicate()[0].split('\n'):
     if re.search(r'untangle-node-reports.+RUNNING', instance):
          running = True
          break

if not running:
     # only print this if create schemas wasn't specified
     logger.error("Reports node is not installed or not running, exiting.")
     sys.exit(0)

if not sql_helper.table_exists( "report_data_days" ):
     sql_helper.create_table("""\
CREATE TABLE reports.report_data_days (
        day_name text NOT NULL,
        day_begin date NOT NULL)""", create_partitions=False)

if not sql_helper.table_exists( "table_updates" ):
     sql_helper.create_table("""\
CREATE TABLE reports.table_updates (
    tablename text NOT NULL,
    last_update timestamp NOT NULL,
    PRIMARY KEY (tablename))""", create_partitions=False)

node = get_node_settings('untangle-node-reports')
if not node:
     node = {}

if not db_retention:
     db_retention = node.get('dbRetention', 7.0)
if not file_retention:
     file_retention = node.get('fileRetention', 30)
if not attach_csv:
     attach_csv = node.get('emailDetail',False)
if not attachment_size_limit:
     attachment_size_limit = node.get('attachmentSizeLimit',10)

reports_output_base=REPORTS_OUTPUT_BASE

reports.engine.fix_hierarchy(REPORTS_OUTPUT_BASE)
reports.engine.init_engine(NODE_MODULE_DIR)

if report_lengths != []:
     init_date = end_date - mx.DateTime.DateTimeDelta(max(report_lengths))
     try:
          reports.engine.setup()
     except Exception, e:
          logger.critical("Exception while setting up reports engine: %s" % (e,), exc_info=True)
          sys.exit(1)
     try:
          reports.engine.process_fact_tables(init_date, start_time)
          reports.engine.post_facttable_setup(init_date, start_time)
     except Exception, e:
          logger.critical("Exception while processing fact-tables: %s" % (e,), exc_info=True)
          sys.exit(1)

mail_reports = []

try:
    for report_days in report_lengths:
         reports.engine.export_static_data(reports_output_base, end_date, report_days)

         if not no_data_gen:
              logger.info("Generating reports for %s days" % (report_days,))
              mail_reports = reports.engine.generate_reports(reports_output_base, end_date, report_days)

         if not no_plot_gen:
              logger.info("Generating plots for %s days" % (report_days,))          
              reports.engine.generate_plots(reports_output_base, end_date, report_days)

         if not no_mail and not simulate:
              logger.info("About to email report summaries for %s days" % (report_days,))          
              pdf_file = None
              try:
                   pdf_file = reports.pdf.generate_pdf(reports_output_base, end_date, report_days, mail_reports)
              except Exception, e:
                   logger.warn("Failed to generate PDF")
                   traceback.print_exc(e)
              reports.mailer.mail_reports(end_date, report_days, pdf_file, mail_reports, attach_csv, attachment_size_limit)
              if pdf_file:
                   os.remove( pdf_file )
except Exception, e:
     logger.critical("Exception while building report: %s" % (e,), exc_info=True)

if not no_cleanup and not simulate:
     reports_cutoff = end_date - mx.DateTime.DateTimeDelta(float(db_retention))
     reports.engine.reports_cleanup(reports_cutoff)     
     write_cutoff_date(DateFromMx(reports_cutoff))
    
     files_cutoff = end_date - mx.DateTime.DateTimeDelta(float(file_retention))
     reports.engine.delete_old_reports('%s/data' % REPORTS_OUTPUT_BASE, files_cutoff)

total_end_time = time.time()
logger.debug('%s took %0.1f sec' % (sys.argv[0], (total_end_time-total_start_time)))

if os.path.isfile(LOCKFILE):
  logger.info("Removing pidfile (pid: %s)" % os.getpid())
  os.remove(LOCKFILE)
