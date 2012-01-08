#! /usr/bin/env python 

import getopt, logging, mx, os, os.path, re, sys, tempfile, time, shutil
from subprocess import Popen, PIPE
from psycopg2.extensions import DateFromMx, TimestampFromMx

def usage():
     print """\
usage: %s [options]
Options:
  -h | --help                   help
  -n | --no-migration           skip schema migration
  -g | --no-data-gen            skip graph data processing
  -p | --no-plot-gen            skip graph image processing
  -m | --no-mail                skip mailing
  -i | --incremental            only update fact tables, do not generate reports themselves
  -a | --attach-csv             attach events as csv
  -t | --trial-report           only report on given trial
  -e | --events-retention       number of minutes in events schema to keep (default is 0)
  -r | --report-length          number of days to report on
  -l | --locale                 locale
  -b <load> | --behave <load>   do not run if load is greated than the specified amount
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
                                 'no-data-gen', 'no-mail', 'incremental',
                                 'no-plot-gen', 'verbose', 'attach-csv',
                                 'events-retention', 'report-length',
                                 'date=', 'locale=', 'trial-report=',
                                 'simulate=', 'behave='])

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
incremental = False
attach_csv = False
attachment_size_limit = 10
events_retention = 0
end_date = mx.DateTime.today()
start_time = mx.DateTime.now()
locale = None
maxLoad = None
db_retention = None
file_retention = None
trial_report = None
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
     elif k == '-i' or k == '--incremental':
          incremental = True
     elif k == '-a' or k == '--attach-csv':
          attach_csv = True
     elif k == '-t' or k == '--trial-report':
          trial_report = v
          ## Disable cleanup on trial reports
          no_cleanup = True
     elif k == '-e' or k == '--events-retention':
          events_retention = int(v)
     elif k == '-b' or k == '--behave':
          maxLoad = float(v)
     elif k == '-r' or k == '--report-length':
          report_lengths = [int(v)]
     elif k == '-v' or k == '--verbose':
          setConsoleLogLevel(logging.DEBUG)
     elif k == '-d' or k == '--date':
          end_date = mx.DateTime.strptime(v, "%Y-%m-%d")
     elif k == '-s' or k == '--simulate':
          simulate = v
     elif k == '-l' or k == '--locale':
          locale = v

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
          if incremental:
               logger.error("... we were trying an incremental run, aborting")
               sys.exit(1)
          else:
               slept = 0
               while pid:
                    if (slept % 60) == 0:
                         logger.warning("... we are trying a nightly run, waiting on pid %s" % pid)
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

def getLoad():
     return float(open("/proc/loadavg").read().split(" ")[0])
     
def get_report_lengths(date):
    lengths = []

    day_of_week = ((date.day_of_week + 1) % 7) + 1
    logger.debug("day_of_week: %s" % (day_of_week,))
    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()
        curs.execute("""
SELECT sched.id, daily, monthly_n_daily, monthly_n_day_of_wk, monthly_n_first
FROM settings.n_reporting_sched sched
JOIN settings.n_reporting_settings ON (schedule = sched.id)
JOIN settings.u_node_persistent_state USING (tid)
WHERE target_state = 'running' OR target_state = 'initialized'
""")
        r = curs.fetchone()
        if r:
            setting_id = r[0]
            daily = r[1]
            monthly_n_daily = r[2]
            monthly_n_day_of_wk = r[3]
            monthly_n_first = r[4]

            if daily:
                lengths.append(1)

            if monthly_n_daily:
                lengths.append(30)
            elif monthly_n_day_of_wk == day_of_week:
                lengths.append(30)
            elif monthly_n_first and date.day == 1:
                lengths.append(30)
            conn.commit()

            curs = conn.cursor()
            curs.execute("""
SELECT day
FROM settings.n_reporting_wk_sched_rule rule
JOIN settings.n_reporting_wk_sched sched ON (sched.rule_id = rule.id)
WHERE sched.setting_id = %s AND day = %s
""", (setting_id, day_of_week))
            r = curs.fetchone()
            if r:
                lengths.append(7)
            conn.commit()
    except Exception, e:
        conn.rollback()
        raise e

    logger.info("Generating reports for the following lengths: %s" % (lengths,))
    return lengths

def get_locale():
     locale = None

     conn = sql_helper.get_connection()
     try:
          curs = conn.cursor()
          curs.execute("SELECT language FROM settings.u_language_settings")
          r = curs.fetchone()
          if r:
               locale = r[0]
     except Exception, e:
          logger.warn("could not get locale");

     return locale

def get_settings():
     settings = {}

     conn = sql_helper.get_connection()
     try:
          curs = conn.cursor()
          curs.execute("""\
SELECT db_retention, file_retention, email_detail, attachment_size_limit
FROM settings.n_reporting_settings
JOIN settings.u_node_persistent_state USING (tid)
WHERE target_state = 'running' OR target_state = 'initialized'
""")
          r = curs.fetchone()
          if r:
               settings['db_retention'] = r[0]
               settings['file_retention'] = r[1]
               settings['email_detail'] = r[2]
               settings['attachment_size_limit'] = r[3]
          conn.commit()
     except Exception, e:
          conn.rollback()
          logger.critical("Could not get report settings", exc_info=True)
          sys.exit(3)

     logger.info("db_settings: %s" % (settings,))
     return settings

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

currentLoad = getLoad()
if maxLoad is not None:
     if currentLoad >= maxLoad:
          logger.warning("Current load (%.2f) is higher than %.2f, exiting" % (currentLoad, maxLoad))
          sys.exit(0)
     else:
          logger.info("Current load (%.2f) is lower than %.2f, going ahead" % (currentLoad, maxLoad))

running = False
for instance in Popen(["ucli", "instances"], stdout=PIPE).communicate()[0].split('\n'):
     if re.search(r'untangle-node-reporting.+RUNNING', instance):
          running = True
          break

if not running:
     logger.error("Reports node is not installed or not running, exiting.")
     sys.exit(0)

if not report_lengths:
     report_lengths = get_report_lengths(end_date)
     
if not locale:
     locale = get_locale()

# set locale
if locale:
     logger.info('using locale: %s' % locale);
     os.environ['LANGUAGE'] = locale
else:
     logger.info('locale not set')

# if old reports schema detected, drop the schema
if not simulate:
     if (sql_helper.table_exists('reports', 'daystoadd')
         or sql_helper.table_exists('reports', 'webpages')
         or sql_helper.table_exists('reports', 'emails')):
          try:
               sql_helper.run_sql('DROP SCHEMA reports CASCADE')
          except psycopg2.ProgrammingError, e:
               logger.warn(e, exc_info=True)

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

settings = get_settings()

if not db_retention:
     db_retention = settings.get('db_retention', 7)
if not file_retention:
     file_retention = settings.get('file_retention', 30)
attach_csv = attach_csv or settings.get('email_detail')
attachment_size_limit = settings.get('attachment_size_limit')

reports_output_base=REPORTS_OUTPUT_BASE

if trial_report:
     reports_output_base=tempfile.mkdtemp(prefix="trial-report-")
     if db_retention >= 14:
          report_lengths = (14,)
     else:
          report_lengths = (db_retention,)

reports.engine.fix_hierarchy(REPORTS_OUTPUT_BASE)

reports.engine.init_engine(NODE_MODULE_DIR)

if trial_report:
     trial_report = reports.engine.get_node(trial_report)
     reports.engine.limit_nodes(trial_report)

if not no_migration:
     init_date = end_date - mx.DateTime.DateTimeDelta(max(report_lengths))
     reports.engine.setup(init_date, end_date)
     if not incremental:
          reports.engine.process_fact_tables(init_date, start_time)
          reports.engine.post_facttable_setup(init_date, start_time)

if not incremental:
     mail_reports = []

     try:
         for report_days in report_lengths:
              reports.engine.export_static_data(reports_output_base,
                                                end_date, report_days)

              if not no_data_gen:
                   logger.info("Generating reports for %s days" % (report_days,))
                   mail_reports = reports.engine.generate_reports(reports_output_base,
                                                                  end_date, report_days)

              if not no_plot_gen:
                   logger.info("Generating plots for %s days" % (report_days,))          
                   reports.engine.generate_plots(reports_output_base, end_date,
                                                 report_days)

              if not no_mail and not simulate:
                   logger.info("About to email reports for %s days" % (report_days,))          
                   f = reports.pdf.generate_pdf(reports_output_base, end_date,
                                                report_days, mail_reports,
                                                trial_report)
                   reports.mailer.mail_reports(end_date, report_days, f, mail_reports,
                                               attach_csv=attach_csv,
                                               attachment_size_limit=attachment_size_limit)
                   os.remove(f)
     except Exception, e:
          logger.critical("Exception while building report: %s" % (e,),
                      exc_info=True)
else:
     logger.info("Incremental mode, not generating reports themselves")

if not no_cleanup and not simulate:
    events_cutoff = start_time - mx.DateTime.DateTimeDeltaFromSeconds(60 * events_retention)
    reports.engine.events_cleanup(events_cutoff)

    if not incremental:
      reports_cutoff = end_date - mx.DateTime.DateTimeDelta(db_retention)
      reports.engine.reports_cleanup(reports_cutoff)     
      write_cutoff_date(DateFromMx(reports_cutoff))

      files_cutoff = end_date - mx.DateTime.DateTimeDelta(file_retention)
      reports.engine.delete_old_reports('%s/data' % REPORTS_OUTPUT_BASE,
                                        files_cutoff)

# These are only for end of trial, a reboot will delete these files since they are in /tmp
# if trial_report and ( reports_output_base != REPORTS_OUTPUT_BASE ):
#    try:
#          shutil.rmtree(reports_output_base)

if os.path.isfile(LOCKFILE):
  logger.info("Removing pidfile (pid: %s)" % os.getpid())
  os.remove(LOCKFILE)
