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

import inspect
import logging
import mx
import psycopg
import re
import string
import md5
import time

from sets import Set
from psycopg import DateFromMx

REQUIRED_TIME_POINTS = [float(s) for s in range(0, 24 * 60 * 60, 30 * 60)]
HOURLY_REQUIRED_TIME_POINTS = [float(s) for s in range(0, 24 * 60 * 60, 60 * 60)]

DEFAULT_TIME_FIELD = 'trunc_time'
DEFAULT_SLICES = 150

def print_timing(func):
    def wrapper(*arg):
        t1 = time.time()
        res = func(*arg)
        t2 = time.time()

        filename = 'unknown'
        line_number = '?'

        for k, v in inspect.getmembers(func):
            if k == 'func_code':
                m = re.search('/(reports/.*).py', v.co_filename)
                filename = m.group(1)
                line_number = v.co_firstlineno

        fun_name = "%s (%s:%s)" % (func.func_name, filename, line_number)


        logging.debug('%s took %0.3f ms' % (fun_name, (t2-t1)*1000.0))
        return res

    return wrapper

__conn = None

def get_connection():
    global __conn

    if not __conn:
        __conn = psycopg.connect("dbname=uvm user=postgres")

    try:
        curs = __conn.cursor()
        curs.execute("SELECT 1")
        __conn.commit()
    except:
        logging.warn("could not access database, getting new connection",
                     exc_info=True)
        try:
            __conn.close()
        except:
            pass
        __conn = psycopg.connect("dbname=uvm user=postgres")

    return __conn

def create_table_from_query(tablename, query, args=None):
    drop_table(tablename)
    create_table_as_sql(tablename, query, args)

def drop_table(tablename):
    conn = get_connection()

    try:
        curs = conn.cursor()
        curs.execute("DROP TABLE %s" % tablename)
    except:
        logging.debug('did not drop table: %s' % tablename)
    finally:
        conn.commit()

def create_index(table, columns):
    col_str = string.join(columns, ',')

    index = md5.new(table + columns).hexdigest()

    run_sql("CREATE INDEX %s ON %s (%s)" % (index, table, col_str))

def create_table_as_sql(tablename, query, args):
    run_sql("CREATE TABLE %s AS %s" % (tablename, query), args)

def run_sql(sql, args=None, connection=get_connection(), auto_commit=True):
    try:
        curs = connection.cursor()
        if args:
            curs.execute(sql, args)
        else:
            curs.execute(sql)

        if auto_commit:
            connection.commit()

    except Exception, e:
        show_error = True
        if not re.search(r'DELETE ', sql) and not re.search(r'already exists', e.message):
            logging.warn("SQL exception begin", exc_info=True)
            logging.warn("SQL exception end")
            show_error = False
            
        if auto_commit:
            connection.rollback()
        elif show_error:
            raise e

def add_column(tablename, column, type):
    sql = "ALTER TABLE %s ADD COLUMN %s %s" % (tablename, column, type)
    run_sql(sql)

def create_partitioned_table(table_ddl, timestamp_column, start_date, end_date,
                             clear_tables=False):

    (schema, tablename) = __get_tablename(table_ddl)

    if schema:
        full_tablename = "%s.%s" % (schema, tablename)
    else:
        full_tablename = tablename

    if not table_exists(schema, tablename):
        run_sql(table_ddl)

    existing_dates = Set()

    for t in get_tables(schema='reports', prefix='%s_' % tablename):
        m=re.search('%s_(\d+)_(\d+)_(\d+)' % tablename, t)
        if m:
            d = mx.DateTime.Date(*map(int, m.groups()))
            if d >= start_date and d < end_date:
                existing_dates.add(d)
            else:
                drop_table(t, schema='reports')
        else:
            logging.warn('ignoring table: %s' % tablename)

    all_dates = Set(get_date_range(start_date, end_date))

    created_tables = []

    for d in all_dates - existing_dates:
        tn = __tablename_for_date(full_tablename, d)
        created_tables.append(tn)

        run_sql("""\
CREATE TABLE %s
(CHECK (%s >= %%s AND %s < %%s))
INHERITS (%s)""" % (tn, timestamp_column, timestamp_column, full_tablename),
                (DateFromMx(d), DateFromMx(d + mx.DateTime.DateTimeDelta(1))))

    if clear_tables:
        for d in all_dates:
            drop_table(__tablename_for_date(full_tablename, d))

    __make_trigger(schema, tablename, timestamp_column, all_dates)

    return created_tables

def get_update_info(tablename, default=None):
    conn = get_connection()
    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT last_update FROM reports.table_updates WHERE tablename = %s
""", (tablename,))

        row = curs.fetchone()

        if row:
            rv = row[0]
        else:
            rv = default

    finally:
        conn.commit()

    return rv

def set_update_info(tablename, last_update, connection=get_connection(),
                    auto_commit=True):
    try:
        curs = connection.cursor()

        curs.execute("""\
SELECT count(*) FROM reports.table_updates WHERE tablename = %s
""", (tablename,))
        row = curs.fetchone()

        if row[0] == 0:
            curs = connection.cursor()
            curs.execute("""\
INSERT INTO reports.table_updates (tablename, last_update) VALUES (%s, %s)
""", (tablename, last_update))
        else:
            curs = connection.cursor()
            curs.execute("""\
UPDATE reports.table_updates SET last_update = %s WHERE tablename = %s
""", (last_update, tablename))

        if auto_commit:
            connection.commit()
    except Exception, e:
        if auto_commit:
            connection.rollback()
        raise e

def drop_table(table, schema=None):
    if schema:
        tn = '%s.%s' % (schema, table)
    else:
        tn = table

    conn = get_connection()
    try:
        curs = conn.cursor()
        curs.execute('DROP TABLE %s' % tn)
    except psycopg.ProgrammingError:
        logging.debug('cannot drop table: %s' % table)
    finally:
        conn.commit()

def table_exists(schemaname, tablename):
    conn = get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT tablename FROM pg_catalog.pg_tables
WHERE schemaname = %s AND tablename = %s""", (schemaname, tablename))

        rv = curs.rowcount
    finally:
        conn.commit()

    return rv

def get_tables(schema=None, prefix=''):
    conn = get_connection()

    try:
        curs = conn.cursor()

        if schema:
            curs.execute("""\
SELECT tablename FROM pg_catalog.pg_tables
WHERE schemaname = %s AND tablename LIKE %s""", (schema, '%s%%' % prefix))
        else:
            curs.execute("""\
SELECT tablename FROM pg_catalog.pg_tables
WHERE tablename LIKE %s""", '%s%%' % prefix)

        rows = curs.fetchall()
        rv = [rows[i][0] for i in range(len(rows))]
    finally:
        conn.commit()

    return rv

def get_date_range(start_date, end_date):
    l = int(round((end_date - start_date).days))
    return [end_date - mx.DateTime.DateTimeDelta(i + 1) for i in range(l)]

def get_required_points(start, end, interval):
    a = []
    v = start
    while v < end:
        a.append(v)
        v = v + interval
    return a

def get_averaged_query(sums, table_name, start_date, end_date,
                       extra_where = [],
                       avgs = [],
                       extra_fields = [],
                       time_field = DEFAULT_TIME_FIELD, slices = DEFAULT_SLICES):
    time_interval = time.mktime(end_date.timetuple()) - time.mktime(start_date.timetuple())
    time_interval = time_interval / slices

    params_regular = { 'table_name' : table_name,
                     'time_field' : time_field }
    params_to_quote =  { 'start_date' : DateFromMx(start_date),
                         'end_date' : DateFromMx(end_date),
                         'time_interval' : time_interval }
    
    query = """
SELECT date(%%(start_date)s) +
       date_trunc('second',
                  (%(time_field)s - %%(start_date)s) / %%(time_interval)s) * %%(time_interval)s
       AS time"""

    for e in extra_fields:
        query += ", " + e
    for s in sums:
        query += ", " + s + " / %%(time_interval)s"
    for a in avgs:
        query += ", " + a
        
    query += """
FROM %(table_name)s
WHERE %(table_name)s.%(time_field)s >= %%(start_date)s AND %(table_name)s.%(time_field)s < %%(end_date)s"""

    for ex in extra_where: # of the form (strTemplate, dictionary)
        template, h = ex
        query += "\n" + template
        params_to_quote.update(h)

    query += """
GROUP by time"""

    for e in extra_fields:
        query += ", " + e

    query += """
ORDER BY time ASC"""

    logging.info((query % params_regular) % params_to_quote)
    
    return query % params_regular, params_to_quote

def __make_trigger(schema, tablename, timestamp_column, all_dates):
    full_tablename = '%s.%s' % (schema, tablename)

    trigger_function_name = '%s_insert_trigger()' % tablename

    trigger_function = """\
CREATE OR REPLACE FUNCTION %s
RETURNS TRIGGER AS $$
BEGIN
""" % trigger_function_name

    first = True
    for d in all_dates:
        trigger_function += """\
    %s (NEW.%s >= '%s' AND NEW.%s < '%s') THEN
        INSERT INTO %s VALUES (NEW.*);""" % ('IF' if first else "ELSIF",
                                             timestamp_column, d,
                                             timestamp_column,
                                             d + mx.DateTime.DateTimeDelta(1),
                                             __tablename_for_date(full_tablename, d))
        first = False

    trigger_function += """\
    ELSE
        RAISE NOTICE 'Date out of range: %%', NEW.%s;
    END IF;
    RETURN NULL;
END;
$$
LANGUAGE plpgsql;""" % timestamp_column

    run_sql(trigger_function);

    trigger_name = "insert_%s_trigger" % tablename

    if not __trigger_exists(schema, tablename, trigger_name):
        run_sql("""\
CREATE TRIGGER %s
    BEFORE INSERT ON %s.%s
    FOR EACH ROW EXECUTE PROCEDURE %s
""" % (trigger_name, schema, tablename, trigger_function_name))

def __trigger_exists(schema, tablename, trigger_name):
    conn = get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""
SELECT 1 FROM information_schema.triggers
WHERE trigger_schema = %s AND event_object_table = %s AND trigger_name = %s
""", (schema, tablename, trigger_name))

        rv = curs.rowcount
    finally:
        conn.commit()

    return rv


def __tablename_for_date(tablename, date):
    return "%s_%d_%02d_%02d" % ((tablename,) + date.timetuple()[0:3])

def __get_tablename(table_ddl):
    m = re.search('create\s+table\s+(\S+)', table_ddl, re.I | re.M)

    if m:
      s = m.group(1).split('.')
      return (string.join(s[0:-1], '.'), s[-1])
    else:
      raise ValueError("Cannot find table in: %s" % table_ddl)
