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

import logging
import mx
import os
import re
import sets
import shutil
import reports.sql_helper as sql_helper
import string

from mx.DateTime import DateTimeDelta
from psycopg import DateFromMx
from reports.pdf import ReportDocTemplate
from sql_helper import print_timing

UVM_JAR_DIR = '@PREFIX@/usr/share/java/uvm/'

TOP_LEVEL = 'top-level'
USER_DRILLDOWN = 'user-drilldown'
HOST_DRILLDOWN = 'host-drilldown'
EMAIL_DRILLDOWN = 'email-drilldown'
MAIL_REPORT_BLACKLIST = ('untangle-node-boxbackup',)

class Node:
    def __init__(self, name):
        self.__name = name

    def get_report(self):
        return None

    def get_toc_membership(self):
        return []

    def setup(self):
        pass

    def post_facttable_setup(self, start_date, end_date):
        pass

    def events_cleanup(self, cutoff):
        pass

    def reports_cleanup(self, cutoff):
        pass

    @property
    def name(self):
        return self.__name

    def parents(self):
        return []

class FactTable:
    def __init__(self, name, detail_table, time_column, dimensions, measures):
        self.__name = name
        self.__detail_table = detail_table
        self.__time_column = time_column
        self.__dimensions = dimensions
        self.__measures = measures

    @property
    def name(self):
        return self.__name

    @property
    def measures(self):
        return self.__measures

    @property
    def dimensions(self):
        return self.__dimensions

    def process(self, start_date, end_date):
        tables = sql_helper.create_partitioned_table(self.__ddl(), 'trunc_time',
                                                     start_date, end_date)

        for c in (self.measures + self.dimensions):
            sql_helper.add_column(self.__name, c.name, c.type)

        sd = DateFromMx(sql_helper.get_update_info(self.__name, start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()

        try:
            sql_helper.run_sql(self.__insert_stmt(), (sd, ed), connection=conn,
                               auto_commit=False)
            sql_helper.set_update_info(self.__name, ed, connection=conn,
                                       auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def __ddl(self):
        ddl = 'CREATE TABLE %s (trunc_time timestamp without time zone' \
            % self.__name
        for c in (self.__dimensions + self.__measures):
            ddl += ", %s %s" % (c.name, c.type)
        ddl += ')'
        return ddl

    def __insert_stmt(self):
        insert_strs = ['trunc_time']
        select_strs = ["date_trunc('minute', %s)" % self.__time_column]
        group_strs = ["date_trunc('minute', %s)" % self.__time_column]

        for c in self.__dimensions:
            insert_strs.append(c.name)
            select_strs.append(c.value_expression)
            group_strs.append(c.name)

        for c in self.__measures:
            insert_strs.append(c.name)
            select_strs.append(c.value_expression)

        return """\
INSERT INTO %s (%s)
    SELECT %s FROM %s
    WHERE %s >= %%s AND %s < %%s
    GROUP BY %s""" % (self.__name, string.join(insert_strs, ','),
                      string.join(select_strs, ','), self.__detail_table,
                      self.__time_column, self.__time_column,
                      string.join(group_strs, ','))

class Column:
    def __init__(self, name, type, value_expression=None):
        self.__name = name
        self.__type = type
        self.__value_expression = value_expression or name

    @property
    def name(self):
        return self.__name

    @property
    def type(self):
        return self.__type

    @property
    def value_expression(self):
        return self.__value_expression

__nodes = {}
__fact_tables = {}

def register_node(node):
    global __nodes

    __nodes[node.name] = node

def register_fact_table(fact_table):
    global __fact_tables

    logging.info("registering fact table: '%s': '%s'" % (fact_table.name,
                                                         fact_table))
    __fact_tables[fact_table.name] = fact_table

def get_fact_table(name):
    global __fact_tables

    return __fact_tables.get(name, None)

@print_timing
def process_fact_tables(start_date, end_date):
    global __fact_tables

    for ft in __fact_tables.values():
        ft.process(start_date, end_date)

@print_timing
def generate_reports(report_base, end_date, report_days):
    global __nodes

    date_base = 'data/%d-%02d-%02d' % (end_date.year, end_date.month,
                                       end_date.day)

    mail_reports = []

    top_level = []
    user_drilldown = []
    host_drilldown = []
    email_drilldown = []

    for node_name in __get_node_partial_order(exclude_uninstalled=True):
        try:
            logging.info('doing process_graphs for: %s' % node_name)
            node = __nodes.get(node_name, None)
            if not node:
                logger.warn('could not get node %s' % node_name)
            else:
                tocs = node.get_toc_membership()
                if TOP_LEVEL in tocs:
                    top_level.append(node_name)
                if USER_DRILLDOWN in tocs:
                    user_drilldown.append(node_name)
                if HOST_DRILLDOWN in tocs:
                    host_drilldown.append(node_name)
                if EMAIL_DRILLDOWN in tocs:
                    email_drilldown.append(node_name)

                report = node.get_report()
                if report:
                    report.generate(report_base, date_base, end_date,
                                    report_days=report_days)
                    if node_name in MAIL_REPORT_BLACKLIST:
                        logging.info('Not including report for %s in emailed reports, since it is blacklisted' % (node_name,))
                    else:
                        logging.info('Including report for %s in emailed reports' % (node_name,))
                        mail_reports.append(report)

        except:
            logging.warn('could not generate reports for: %s' % node_name,
                         exc_info=True)

    __write_toc(report_base, date_base, 'top-level', top_level)
    __write_toc(report_base, date_base, 'user-drilldown', user_drilldown)
    __write_toc(report_base, date_base, 'host-drilldown', host_drilldown)
    __write_toc(report_base, date_base, 'email-drilldown', email_drilldown)

    return mail_reports

@print_timing
def generate_sub_report(report_base, node_name, end_date, report_days=1,
                        host=None, user=None, email=None):
    date_base = 'data/%d-%02d-%02d' % (end_date.year, end_date.month,
                                           end_date.day)

    node = __nodes.get(node_name, None)

    if not node:
        msg = 'UNKNOWN_NODE: %s' % node_name
        logging.warn(msg)
        return msg

    report = node.get_report()

    if user:
        report.generate(report_base, date_base, end_date, user=user)
    if host:
        report.generate(report_base, date_base, end_date, host=host)
    if email:
        report.generate(report_base, date_base, end_date, email=email)

    dir = get_node_base(node_name, date_base, report_days=report_days,
                        host=host, user=user, email=email)

    __generate_plots(report_base, dir)

    return 'DONE'

@print_timing
def generate_plots(report_base, end_date, report_days=1):
    date_base = 'data/%d-%02d-%02d/%s' % (end_date.year, end_date.month,
                                          end_date.day,
                                          report_days_dir(report_days))
    __generate_plots(report_base, date_base)

@print_timing
def events_cleanup(cutoff):
    co = DateFromMx(cutoff)

    for name in __get_node_partial_order():
        try:
            node = __nodes.get(name, None)
            node.events_cleanup(co)
        except:
            logging.warn('count not cleanup events for: %s' % name,
                         exc_info=True)

@print_timing
def reports_cleanup(cutoff):
    co = DateFromMx(cutoff)

    for name in __get_node_partial_order():
        try:
            node = __nodes.get(name, None)
            node.reports_cleanup(co)
        except:
            logging.warn('count not cleanup reports for: %s' % name,
                         exc_info=True)

@print_timing
def delete_old_reports(dir, cutoff):
    for f in os.listdir(dir):
        if re.match('^\d+-\d+-\d+$', f):
            d = mx.DateTime.DateFrom(f)
            if d < cutoff:
                shutil.rmtree('%s/%s' % (dir, f));

@print_timing
def init_engine(node_module_dir):
    __get_nodes(node_module_dir)

@print_timing
def setup(start_date, end_date):
    global __nodes

    for name in __get_node_partial_order():
        try:
            logging.info('doing setup for: %s' % name)
            node = __nodes.get(name, None)

            if not node:
                logger.warn('could not get node %s' % name)
            else:
                node.setup(start_date, end_date)
        except:
            logging.warn('could not setup for: %s' % name, exc_info=True)

@print_timing
def post_facttable_setup(start_date, end_date):
    global __nodes

    for name in __get_node_partial_order():
        try:
            logging.info('doing post_facttable_setup for: %s' % name)
            node = __nodes.get(name, None)
            if not node:
                logger.warn('could not get node %s' % name)
            else:
                node.post_facttable_setup(start_date, end_date)
        except:
            logging.warn('coud not do post factable setup for: %s' % name,
                         exc_info=True)

@print_timing
def fix_hierarchy(output_base):
    base_dir = '%s/data' % output_base

    if not os.path.isdir(base_dir):
        os.makedirs(base_dir);

    for date_dir in os.listdir(base_dir):
        if re.match('^\d+-\d+-\d+$', date_dir):
            one_day_dir = '%s/%s/1-day' % (base_dir, date_dir)
            if not os.path.isdir(one_day_dir):
                os.mkdir(one_day_dir)
                for node_dir in os.listdir('%s/%s' % (base_dir, date_dir)):
                    node_path = '%s/%s/%s' % (base_dir, date_dir, node_dir)
                    if os.path.isdir(node_path) and not re.match('[0-9]+-days?$', node_dir):
                        os.rename(node_path, '%s/%s' % (one_day_dir, node_dir))


def get_node_base(name, date_base, report_days=1, host=None, user=None,
                  email=None):
    days_dir = report_days_dir(report_days)

    if host:
        return '%s/%s/host/%s/%s' % (date_base, days_dir, host, name)
    elif user:
        return '%s/%s/user/%s/%s' % (date_base, days_dir, user, name)
    elif email:
        return '%s/%s/email/%s/%s' % (date_base, days_dir, email, name)
    else:
        return '%s/%s/%s' % (date_base, days_dir, name)

def report_days_dir(report_days):
    if report_days < 2:
        return '%s-day' % report_days
    else:
        return '%s-days' % report_days

def _first_page(canvas, doc):
    canvas.saveState()
    canvas.setFont('Times-Bold',16)
    canvas.drawCentredString(PAGE_WIDTH/2.0, PAGE_HEIGHT-108, "Reports")
    canvas.setFont('Times-Roman',9)
    canvas.drawString(inch, 0.75 * inch, "First Page / foo")
    canvas.restoreState()

def _later_pages(canvas, doc):
    canvas.saveState()
    canvas.setFont('Times-Roman',9)
    canvas.drawString(inch, 0.75 * inch, "Page %d foo" % doc.page)
    canvas.restoreState()

def __generate_plots(report_base, dir):
    path = []

    path.append('@PREFIX@/usr/share/untangle/lib/untangle-libuvm-bootstrap/')
    path.append('@PREFIX@/usr/share/untangle/lib/untangle-libuvm-api/')
    path.append('@PREFIX@/usr/share/untangle/conf/')

    for f in os.listdir(UVM_JAR_DIR):
        if f.endswith('.jar'):
            path.append('%s/%s' % (UVM_JAR_DIR, f))

    os.system("java -Dlog4j.configuration=log4j-reporter.xml -Djava.awt.headless=true -cp %s com.untangle.uvm.reports.GraphGenerator '%s' '%s'"
              % (string.join(path, ':'), report_base, dir))

def __get_users(date):
    conn = sql_helper.get_connection()

    d = DateFromMx(date)

    try:
        curs = conn.cursor()
        curs.execute("SELECT username from reports.users WHERE date = %s", (d,))
        rows = curs.fetchall()
        rv = [rows[i][0] for i in range(len(rows))]
    finally:
        conn.commit()

    return rv

def __get_hosts(date):
    conn = sql_helper.get_connection()

    d = DateFromMx(date)

    try:
        curs = conn.cursor()
        curs.execute("SELECT hname from reports.hnames WHERE date = %s", (d,))
        rows = curs.fetchall()
        rv = [rows[i][0] for i in range(len(rows))]
    finally:
        conn.commit()

    return rv

def __get_emails(date):
    conn = sql_helper.get_connection()

    d = DateFromMx(date)

    try:
        curs = conn.cursor()
        curs.execute("""\
SELECT addr, sum(msgs)
FROM reports.n_mail_addr_totals
WHERE addr_kind = 'T' AND date_trunc('day', trunc_time) = %s
GROUP BY addr
ORDER BY sum DESC
LIMIT 100
""", (d,))
        rows = curs.fetchall()
        rv = [rows[i][0] for i in range(len(rows))]
    finally:
        conn.commit()

    return rv

# do not care about uninstalled nodes anymore
def __get_node_partial_order(exclude_uninstalled=True):
    global __nodes

    if exclude_uninstalled:
        installed = __get_installed_nodes()

    available = sets.Set(__nodes.keys());
    list = []

    while len(available):
        name = available.pop()
        if exclude_uninstalled:
            if name in installed:
                __add_node(name, list, available)
        else:
            __add_node(name, list, available)

    return list

def __get_installed_nodes():
    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()
        curs.execute("""\
SELECT DISTINCT name
FROM settings.u_node_persistent_state
WHERE target_state IN ('initialized', 'running')
""")
        rows = curs.fetchall()
        rv = [rows[i][0] for i in range(len(rows))]
    finally:
        conn.commit()

    return rv

def __add_node(name, list, available):
    global __nodes

    node = __nodes.get(name, None)
    if not node:
        logging.warn('node not found %s' % name)
    else:
        for p in node.parents():
            if p in available:
                available.remove(p)
                __add_node(p, list, available)
        list.append(name)

def __get_nodes(node_module_dir):
    for f in os.listdir(node_module_dir):
        if f.endswith('py'):
            (m, e) = os.path.splitext(f)
            __import__('reports.node.%s' % m)

def __write_toc(report_base, date_base, type, list):
    d = '%s/%s' % (report_base, date_base)
    if not os.path.exists(d):
        os.makedirs(d)

    f = open('%s/%s' % (d, type), 'w')
    try:
        for l in list:
            f.write(l)
            f.write("\n")
    finally:
        f.close()
