import gettext
import logging
import mail_helper
import mx
import os
import sets
import sql_helper
import string

from sql_helper import print_timing
from psycopg import DateFromMx
from mx.DateTime import DateTimeDelta

REPORT_JAR_DIR = '@PREFIX@/usr/share/java/reports/'

class Node:
    def __init__(self, name):
        self.__name = name

    def get_report(self):
        return None

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

    logging.info("registering fact table: '%s'" % fact_table)
    __fact_tables[fact_table.name] = fact_table
    logging.info('AFTER: %s' % __fact_tables)

def get_fact_table(name):
    global __fact_tables

    return __fact_tables.get(name, None)

@print_timing
def process_fact_tables(start_date, end_date):
    global __fact_tables

    for ft in __fact_tables.values():
        ft.process(start_date, end_date)

@print_timing
def generate_reports(report_base, end_date, main_only=False):
    global __nodes

    date_base = 'data/%d-%02d-%02d' % (end_date.year, end_date.month, end_date.day)

    mail_reports = []

    for node_name in __get_node_partial_order():
        logging.info('doing process_graphs for: %s' % node_name)
        node = __nodes.get(node_name, None)
        if not node:
            logger.warn("could not get node %s" % node_name)
        else:
            report = node.get_report()
            if report:
                report.generate(report_base, date_base, end_date)
                mail_reports.append(report)
                if not main_only:
                    for u in __get_users(end_date - DateTimeDelta(1)):
                        report.generate(report_base, date_base, end_date,
                                        user=u)
                    for h in __get_hosts(end_date - DateTimeDelta(1)):
                        report.generate(report_base, date_base, end_date,
                                        host=h)
                    for e in __get_emails(end_date - DateTimeDelta(1)):
                        report.generate(report_base, date_base, end_date,
                                        email=e)

    return mail_reports

@print_timing
def generate_plots(report_base, end_date):
    path = []

    path.append('@PREFIX@/usr/share/untangle/lib/untangle-libuvm-bootstrap/')
    path.append('@PREFIX@/usr/share/untangle/lib/untangle-libuvm-api/')
    path.append('@PREFIX@/usr/share/untangle/conf/')

    for f in os.listdir(REPORT_JAR_DIR):
        if f.endswith('.jar'):
            path.append('%s/%s' % (REPORT_JAR_DIR, f))

    date_base = 'data/%d-%02d-%02d' % (end_date.year, end_date.month, end_date.day)

    os.system('java -Dlog4j.configuration=log4j-reporter.xml -cp %s com.untangle.uvm.reports.GraphGenerator %s %s'
              % (string.join(path, ':'), report_base, date_base))

# xxx not used or complete
def generate_mail(report_base, end_date, mail_reports):
    date_base = 'data/%d-%02d-%02d' % (end_date.year, end_date.month, end_date.day)

    writer = mail_helper.HtmlWriter()
    for r in mail_reports:
        r.to_html(writer, report_base, date_base, end_date)
    writer.close()
    writer.generate(end_date)
    writer.mail()
    writer.cleanup()

@print_timing
def events_cleanup(cutoff):
    co = DateFromMx(cutoff)

    for name in __get_node_partial_order():
        node = __nodes.get(name, None)
        node.events_cleanup(co)


@print_timing
def reports_cleanup(cutoff):
    co = DateFromMx(cutoff)

    for name in __get_node_partial_order():
        node = __nodes.get(name, None)
        node.reports_cleanup(co)


@print_timing
def init_engine(node_module_dir, locale):
    gettext.bindtextdomain('untangle-node-reporting')
    gettext.textdomain('untangle-node-reporting')

    try:
        lang = gettext.translation('untangle-node-reporting',
                                   languages=[locale])
        lang.install()
    except Exception, e:
        logging.warn(e)

    __get_nodes(node_module_dir)

@print_timing
def setup(start_date, end_date):
    global __nodes

    for name in __get_node_partial_order():
        logging.info('doing setup for: %s' % name)
        node = __nodes.get(name, None)
        if not node:
            logger.warn("could not get node %s" % name)
        else:
            node.setup(start_date, end_date)

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

def __get_node_partial_order():
    global __nodes

    available = sets.Set(__nodes.keys());
    list = []

    while len(available):
        name = available.pop()
        __add_node(name, list, available)

    return list

def __add_node(name, list, available):
    global __nodes

    node = __nodes.get(name, None)
    if not node:
        logging.warn("node not found %s" % name)
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
