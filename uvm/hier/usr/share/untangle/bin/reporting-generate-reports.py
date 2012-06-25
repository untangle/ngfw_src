#! /usr/bin/env python 

import getopt, logging, mx, os, os.path, re, sys, tempfile, time, shutil
from subprocess import Popen, PIPE
from psycopg2.extensions import DateFromMx, TimestampFromMx
from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_node_settings

def usage():
     print """\
usage: %s [options]
Options:
  -h | --help                   help
  -n | --no-migration           skip schema migration
  -g | --no-data-gen            skip graph data processing
  -p | --no-plot-gen            skip graph image processing
  -m | --no-mail                skip mailing
  -c | --create-schemas         create the SQL schemas
  -a | --attach-csv             attach events as csv
  -r | --report-length          number of days to report on
  -s <cs> | --simulate=<cs>     run reports of the remote DB specified by the connection string
  -d <y-m-d> | --date=<y-m-d>   run reports for the specific date
""" % sys.argv[0]

## main
PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR
LOCKFILE = "/var/run/untangle-reports.pid"

if (PREFIX != ''):
     sys.path.insert(0, REPORTS_PYTHON_DIR)

from reports.log import *
logger = getLogger(__name__)

try:
     opts, args = getopt.getopt(sys.argv[1:], "hncgpmiaver:d:l:t:s:b:",
                                ['help', 'no-migration', 'no-cleanup',
                                 'no-data-gen', 'no-mail', 'create-schemas',
                                 'no-plot-gen', 'verbose', 'attach-csv',
                                 'events-retention', 'report-length',
                                 'date=', 'simulate=', 'behave='])

except getopt.GetoptError, err:
     print str(err)
     usage()
     sys.exit(2)

report_lengths = None
no_migration = False
no_cleanup = False
no_data_gen = False
no_plot_gen = False
no_mail = False
create_schemas = False
attach_csv = False
attachment_size_limit = 10
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
     elif k == '-n' or k == '--no-migration':
          no_migration = True
     elif k == '-g' or k == '--no-data-gen':
          no_data_gen = True
     elif k == '-p' or k == '--no-plot-gen':
          no_plot_gen = True
     elif k == '-m' or k == '--no-mail':
          no_mail = True
     elif k == '-c' or k == '--create-schemas':
          create_schemas = True
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
          if create_schemas:
               logger.error("... aborting create-schemas call.")
               sys.exit(1)
          else:
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

f = open(LOCKFILE, "w")
f.write("%s\n" % os.getpid())
f.close()
logger.info("Wrote pidfile (pid: %s)" % os.getpid())

import reports.i18n_helper
import reports.engine
import reports.mailer
import reports.sql_helper as sql_helper

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

    dailySched = get_node_settings_item('untangle-node-reporting','generateDailyReports').lower()
    weeklySched = get_node_settings_item('untangle-node-reporting','generateWeeklyReports').lower()
    monthlySched = get_node_settings_item('untangle-node-reporting','generateMonthlyReports').lower()

    if str(current_day_of_week) in dailySched or get_day_name(current_day_of_week) in dailySched or "any" in dailySched:
         lengths.append(1)
    if str(current_day_of_week) in weeklySched or get_day_name(current_day_of_week) in weeklySched or "any" in weeklySched:
         lengths.append(7)
    if str(current_day_of_week) in monthlySched or get_day_name(current_day_of_week) in monthlySched or "any" in monthlySched:
         lengths.append(30)

    logger.info("Generating reports for the following lengths: %s" % (lengths,))
    return lengths

def write_cutoff_date(date):
     sql_helper.run_sql("""
CREATE TABLE reports.reports_state (
        last_cutoff timestamp NOT NULL)""")

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

running = False
for instance in Popen([PREFIX + "/usr/bin/ucli", "instances"], stdout=PIPE).communicate()[0].split('\n'):
     if re.search(r'untangle-node-reporting.+RUNNING', instance):
          running = True
          break

if not running and not create_schemas:
     logger.error("Reports node is not installed or not running, exiting.")
     sys.exit(0)

if not report_lengths:
     report_lengths = get_report_lengths(end_date)
     
try:
     sql_helper.create_schema(sql_helper.SCHEMA);
except Exception:
     pass

try:
     sql_helper.create_table("reports","report_data_days","""\
CREATE TABLE reports.report_data_days (
        day_name text NOT NULL,
        day_begin date NOT NULL)""")
except Exception:
     pass

try:
     sql_helper.create_table("reports","table_updates","""\
CREATE TABLE reports.table_updates (
    tablename text NOT NULL,
    last_update timestamp NOT NULL,
    PRIMARY KEY (tablename))""")
except Exception:
     pass

if not db_retention:
     db_retention = get_node_settings('untangle-node-reporting').get('dbRetention', 7.0)
if not file_retention:
     file_retention = get_node_settings('untangle-node-reporting').get('fileRetention', 30)
attach_csv = attach_csv or get_node_settings('untangle-node-reporting').get('emailDetail')
attachment_size_limit = get_node_settings('untangle-node-reporting').get('attachmentSizeLimit')

reports_output_base=REPORTS_OUTPUT_BASE

reports.engine.fix_hierarchy(REPORTS_OUTPUT_BASE)

reports.engine.init_engine(NODE_MODULE_DIR)

if not no_migration:
     init_date = end_date - mx.DateTime.DateTimeDelta(max(report_lengths))
     reports.engine.setup()
     if not create_schemas:
          reports.engine.process_fact_tables(init_date, start_time)
          reports.engine.post_facttable_setup(init_date, start_time)

if not create_schemas:
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
                   f = reports.pdf.generate_pdf(reports_output_base, end_date, report_days, mail_reports)
                   reports.mailer.mail_reports(end_date, report_days, f, mail_reports, attach_csv=attach_csv, attachment_size_limit=attachment_size_limit)
                   os.remove(f)
     except Exception, e:
          logger.critical("Exception while building report: %s" % (e,), exc_info=True)
else:
     logger.info("Create schemas mode, not generating reports themselves")

if not no_cleanup and not simulate and not create_schemas:
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

