#!/usr/bin/python
# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
# Copyright (c) 2003-2009 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Aaron Read <amread@untangle.com>

import getopt
import logging
import mx
import os
import psycopg
import sys
import tempfile
import shutil

from psycopg import DateFromMx

def usage():
     print """\
usage: %s [options]
Options:
  -h | --help                 help
  -n | --no-migration         skip schema migration
  -g | --no-data-gen          skip graph data processing
  -p | --no-plot-gen          skip graph image processing
  -m | --no-mail              skip mailing
  -a | --attach-csv           attach events as csv
  -t | --trial-report         only report on given trial
  -e | --events-retention     number of days in events schema to keep
  -r | --report-length        number of days to report on
  -l | --locale               locale
  -d y-m-d | --date=y-m-d\
""" % sys.argv[0]

# main

try:
     opts, args = getopt.getopt(sys.argv[1:], "hncgpmave:r:d:l:t:",
                                ['help', 'no-migration', 'no-cleanup',
                                 'no-data-gen', 'no-mail', 'no-plot-gen',
                                 'verbose', 'attach-csv', 'events-retention',
                                 'report-length', 'date=', 'locale=',
                                 'trial-report'])

except getopt.GetoptError, err:
     print str(err)
     usage()
     sys.exit(2)

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

if (PREFIX != ''):
     sys.path.insert(0, REPORTS_PYTHON_DIR)

import reports.i18n_helper
import reports.engine
import reports.mailer
import reports.sql_helper as sql_helper
from reports.log import *
logger = getLogger(__name__)

report_lengths = None
no_migration = False
no_cleanup = False
no_data_gen = False
no_plot_gen = False
no_mail = False
attach_csv = False
attachment_size_limit = 10
events_retention = 3
end_date = mx.DateTime.today()
end_date_forced = False
locale = None
db_retention = None
file_retention = None
trial_report = None

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
     elif k == '-a' or k == '--attach-csv':
          attach_csv = True
     elif k == '-t' or k == '--trial-report':
          trial_report = v
          ## Disable cleanup on trial reports
          no_cleanup = True
     elif k == '-e' or k == '--events-retention':
          events_retention = int(v)
     elif k == '-r' or k == '--report-length':
          report_lengths = [int(v)]
     elif k == '-v' or k == '--verbose':
          setConsoleLogLevel(logging.DEBUG)
     elif k == '-d' or k == '--date':
          end_date_forced = True
          end_date = mx.DateTime.DateFrom(v)
     elif k == '-l' or k == '--locale':
          locale = v

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
            logger.warn("settings_id: %s" % (setting_id,))
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
          logger.warn("could not get db_retention", exc_info=True)


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
if (sql_helper.table_exists('reports', 'daystoadd')
    or sql_helper.table_exists('reports', 'webpages')
    or sql_helper.table_exists('reports', 'emails')):
     try:
          sql_helper.run_sql('DROP SCHEMA reports CASCADE')
     except psycopg.ProgrammingError, e:
          logger.warn(e, exc_info=True)

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
     if end_date_forced:
          sql_helper.clear_partitioned_tables(init_date, end_date)
     reports.engine.setup(init_date, end_date)
     reports.engine.process_fact_tables(init_date, end_date)
     reports.engine.post_facttable_setup(init_date, end_date)
     
mail_reports = []

try:
    for report_days in report_lengths:
         if not no_data_gen:
              logger.info("Generating reports for %s days" % (report_days,))
              mail_reports = reports.engine.generate_reports(reports_output_base,
                                                             end_date, report_days)

         if not no_plot_gen:
              logger.info("Generating plots for %s days" % (report_days,))          
              reports.engine.generate_plots(reports_output_base, end_date,
                                            report_days)

         if not no_mail:
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

if not no_cleanup:
     events_cutoff = end_date - mx.DateTime.DateTimeDelta(events_retention)
     reports.engine.events_cleanup(events_cutoff)

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

