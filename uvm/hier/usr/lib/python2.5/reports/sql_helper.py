import datetime
import inspect
import logging
import mx
import psycopg2
import re
import string
import md5
import sys
import time

from sets import Set
from psycopg2.extensions import DateFromMx

from reports.log import *
logger = getLogger(__name__)

REQUIRED_TIME_POINTS = [float(s) for s in range(0, 24 * 60 * 60, 30 * 60)]
HOURLY_REQUIRED_TIME_POINTS = [float(s) for s in range(0, 24 * 60 * 60, 60 * 60)]

DEFAULT_TIME_FIELD = 'trunc_time'
DEFAULT_SLICES = 150

DEFAULT_SCHEMA = 'reports'
SCHEMA = 'reports'

CONNECTION_STRING = "dbname=uvm user=postgres"

class Cursor(psycopg2.extensions.cursor):
    def execute(self, sql, args=None):
        sql = sql.replace('%s.' % (DEFAULT_SCHEMA,), '%s.' % (SCHEMA,))
        psycopg2.extensions.cursor.execute(self, sql, args)

    def executemany(self, sql, args=None):
        sql = sql.replace('%s.' % (DEFAULT_SCHEMA,), '%s.' % (SCHEMA,))
        psycopg2.extensions.cursor.executemany(self, sql, args)

    def fetchone(self):
        try:
            return psycopg2.extensions.cursor.fetchone(self)
        except:
            return None

class Connection(psycopg2.extensions.connection):
    def cursor(self):
        return psycopg2.extensions.connection.cursor(self, cursor_factory=Cursor)

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


        logger.debug('%s took %0.3f ms' % (fun_name, (t2-t1)*1000.0))
        return res

    return wrapper

__conn = None

def get_connection():
    global __conn

    if not __conn:
        __conn = psycopg2.connect(CONNECTION_STRING,
                                  connection_factory=Connection)

    try:
        __conn.commit()
        curs = __conn.cursor()
        curs.execute("SELECT 1")
        __conn.commit()
    except:
        logger.warn("could not access database, getting new connection",
                     exc_info=True)
        try:
            __conn.close()
        except:
            pass
        __conn = psycopg2.connect(CONNECTION_STRING,
                                  connection_factory=Connection)

    return __conn

def create_table_from_query(tablename, query, args=None):
    drop_table(tablename)
    create_table_as_sql(tablename, query, args)

#def create_index(table, columns):
#    col_str = string.join(columns, ',')
#
#    index = md5.new(table + columns).hexdigest()
#
#    run_sql("CREATE INDEX %s ON %s (%s)" % (index, table, col_str))

def create_table_as_sql(tablename, query, args):
    run_sql("CREATE TABLE %s AS %s" % (tablename, query), args)

def run_sql(sql, args=None, connection=None,
            auto_commit=True, force_propagate=False,
            debug=False):
    if not connection:
        connection = get_connection()

    try:
        curs = connection.cursor()
        if args:
            if debug:
                logger.debug("Executing: %s" % re.sub(r'\n', ' ', curs.mogrify(sql, args)))
            curs.execute(sql, args)
        else:
            if debug:
                logger.debug("Executing: %s" % re.sub(r'\n', ' ', sql))
            curs.execute(sql)

        if auto_commit:
            connection.commit()

    except Exception, e:
        if force_propagate:
            if auto_commit:
                connection.rollback()
            raise e
        
        show_error = True
        if re.search(r'^\s*(DROP) ', sql, re.I):
            show_error = False
        if re.search(r'^\s*(CREATE|ALTER) ', sql, re.I):
            show_error = False
            
        if show_error:
            raise e

        try:
            connection.rollback()
        except:
            pass

def add_column(schema, tablename, columnname, type):
    # first verify that the column doesn't already exist
    sql = "select 1 from information_schema.columns where table_schema = '%s' and table_name = '%s' and  column_name = '%s'" % (schema, tablename, columnname)
    conn = get_connection()
    try:
        curs = conn.cursor()
        curs.execute(sql)
        row = curs.fetchone()
        if row and row[0] == 1:
            return # column already exists
    finally:
        conn.commit()
    sql = "ALTER TABLE %s.%s ADD COLUMN %s %s" % (schema, tablename, columnname, type)
    run_sql(sql)

def drop_column(schema, tablename, columnname):
    # first verify that the column exist
    sql = "select 1 from information_schema.columns where table_schema = '%s' and table_name = '%s' and  column_name = '%s'" % (schema, tablename, columnname)
    conn = get_connection()
    try:
        curs = conn.cursor()
        curs.execute(sql)
        row = curs.fetchone()
        if not (row and row[0] == 1):
            return # column doesn exist
    finally:
        conn.commit()
    sql = "ALTER TABLE %s.%s DROP COLUMN %s" % (schema, tablename, column)
    run_sql(sql)

def convert_column(schema, tablename, columnname, oldtype, newtype):
    # first verify that the column is currently of type oldtype
    sql = "select 1 from information_schema.columns where table_schema = '%s' and table_name = '%s' and  column_name = '%s' and data_type = '%s'" % (schema, tablename, columnname, oldtype)
    conn = get_connection()
    try:
        curs = conn.cursor()
        curs.execute(sql)
        row = curs.fetchone()
        if not (row and row[0] == 1):
            return # column of that type doesnt exist
    finally:
        conn.commit()
    # If this part is reached we have verified that the column exists and is of type oldtype
    sql = "ALTER TABLE %s.%s ALTER COLUMN %s TYPE %s" % (schema, tablename, columnname, newtype)
    run_sql(sql);
    return;

def create_index(schema, tablename, columnname):
    # first check to see if the index already exists
    sql = "select 1 from pg_class where relname = '%s_%s_idx'" % (tablename, columnname)
    conn = get_connection()
    try:
        curs = conn.cursor()
        curs.execute(sql)
        row = curs.fetchone()
        if row and row[0] == 1:
            return # index already doesn exist
    finally:
        conn.commit()
    # If this part is reached we have verified that the column exists and is of type oldtype
    sql = 'CREATE INDEX %s_%s_idx ON %s.%s(%s)' % (tablename, columnname, schema, tablename, columnname)
    run_sql(sql)

def drop_partitioned_table(tablename, cutoff_date):
  for t, date in find_partitioned_tables(tablename):
    if date < cutoff_date:
      drop_table(t, schema=SCHEMA)

  tablename = re.sub(r'(\-[a-z]+|reports\.|\[.+\])', '', tablename)
  try:
    run_sql("DELETE FROM reports.%s WHERE time_stamp < %%s" % tablename,
            (cutoff_date,),
            force_propagate=True)
  except:
    try:
      run_sql("DELETE FROM reports.%s WHERE trunc_time < %%s" % tablename,
              (cutoff_date,),
              force_propagate=True)
    except:
      run_sql("DELETE FROM reports.%s WHERE date < %%s" % tablename,
              (cutoff_date,),
              force_propagate=True)

def clear_partitioned_tables(start_date, end_date, tablename=None):
    logger.debug('Forcing removal of existing partitioned...')

    for table, date in find_partitioned_tables(tablename):
        if date >= start_date and date < end_date:
            drop_table(table, 'reports')
    run_sql("UPDATE reports.table_updates SET last_update = %s",
            (DateFromMx(date_convert(start_date)),))

def create_partitioned_table(table_ddl, timestamp_column, start_date, end_date):
    (schema, tablename) = __get_tablename(table_ddl)
    
    if schema:
        schema = SCHEMA
        full_tablename = "%s.%s" % (schema, tablename)
    else:
        full_tablename = tablename

    if not table_exists(schema, tablename):
        run_sql(table_ddl)

    for t, date in find_partitioned_tables(tablename):
        drop_table(t, schema=SCHEMA)

    __drop_trigger(schema, tablename, timestamp_column)

def get_update_info(tablename, default=None, delay=0):
    conn = get_connection()
    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT last_update + interval '10 millisecond' FROM reports.table_updates WHERE tablename = %s
""", (tablename,))
        
        row = curs.fetchone()

        if row:
            rv = row[0]
        else:
            rv = default

    finally:
        conn.commit()

    return date_convert(rv, delay)

def get_max_timestamp_with_interval(table, time_column='date', interval='1 day'):
    connection = get_connection()

    curs = connection.cursor()
    try:
        curs.execute("SELECT max(%s) + interval '%s' FROM %s" % (time_column, interval, table))
        connection.commit()
    except:
        connection.rollback()
        raise
    
    sd = curs.fetchone()
    if not sd or not sd[0]:
        sd = '1-1-1'
    else:
        sd = sd[0]
    return sd

def set_update_info(tablename, last_update, connection=None,
                    auto_commit=True, origin_table=None):
    # FIXME: last_update is now ignored and re-calculated from the
    # info in the corresponding table; change all the calls to this
    # new signature at some point
    if not connection:
        connection = get_connection()
    else:
        try:
            connection.commit()
        except:
            pass

    try:
        curs = connection.cursor()

        if not origin_table:
            origin_tablename = re.sub(r'(\-[a-z]+|\[.+\])', '', tablename)
        else:
            origin_tablename = re.sub(r'(\-[a-z]+|\[.+\])', '', origin_table)

        last_update_origin = last_update
        try:
            curs.execute("SELECT max(time_stamp) FROM %s" % (origin_tablename,))
        except psycopg2.ProgrammingError, e:
            connection.rollback()
            if e.pgerror.find('column "time_stamp" does not exist') > 0:
                try:
                    curs = connection.cursor()
                    curs.execute("SELECT max(trunc_time) FROM %s" % (origin_tablename,))
                except:
                    connection.rollback()

        last_update = curs.fetchone()
        if not last_update or not last_update[0]:
            last_update = last_update_origin
        else:
            last_update = last_update[0]
            
        logger.debug("About to set last_update to %s (last was %s) for %s" % (last_update,
                                                                              last_update_origin,
                                                                              tablename))

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

def drop_table(table, schema=SCHEMA):
    if schema:
        tn = '%s.%s' % (schema, table)
    else:
        tn = table

    conn = get_connection()
    try:
        curs = conn.cursor()
        curs.execute('DROP TABLE %s' % (tn,))
        logger.debug("dropped table '%s'" % (table,))
    except psycopg2.ProgrammingError:
        logger.debug('cannot drop table: %s' % (table,))
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

def find_partitioned_tables(tablename=None):
    if not tablename:
        prefix = ''
    else:
        prefix = '%s_' % tablename
        
    tables = []
    for t in get_tables(schema=SCHEMA, prefix=prefix):
        m = re.search('%s(\d+)_(\d+)_(\d+)' % prefix, t)
        if m:
            d = mx.DateTime.Date(*map(int, m.groups()))
            tables.append((t, d))
    return tables
    
def get_date_range(start_date, end_date):
    l = int(round((end_date - start_date).days+1))
    return [end_date - mx.DateTime.DateTimeDelta(i) for i in range(l)]

def get_required_points(start, end, interval):
    a = []
    v = start
    while v < end:
        a.append(datetime.datetime.fromtimestamp(v))
        v = v + interval
    return a

def get_result_dictionary(curs):
    row = curs.fetchone()

    i = 0
    h = {}
    for desc in curs.description:
        h[desc[0]] = row[i]
        i += 1

    return h

def get_averaged_query(sums, table_name, start_date, end_date,
                       extra_where = [],
                       extra_fields = [],
                       time_field = DEFAULT_TIME_FIELD,
                       time_interval = 60,
                       debug = False):
#     time_interval = time.mktime(end_date.timetuple()) - time.mktime(start_date.timetuple())
#     time_interval = time_interval / slices

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
        query += ", " + s

    query += """
FROM %(table_name)s
WHERE %(table_name)s.%(time_field)s >= %%(start_date)s AND %(table_name)s.%(time_field)s < %%(end_date)s"""

    for ex in extra_where: # of the form (strTemplate, dictionary)
        template, h = ex
        query += "\nAND " + template.replace("%(", "%%(")
        params_to_quote.update(h)

    query += """
GROUP by time"""

    for e in extra_fields:
        query += ", " + e

    query += """
ORDER BY time ASC"""

    if debug:
        logger.debug((query % params_regular) % params_to_quote)
    
    return query % params_regular, params_to_quote

def __drop_trigger(schema, tablename, timestamp_column):
    full_tablename = '%s.%s' % (schema, tablename)
    for trigger in ('%s_insert_trigger', 'insert_%s_trigger'):
        trigger = trigger % tablename
        run_sql("DROP TRIGGER IF EXISTS %s ON %s.%s CASCADE" % (trigger, schema, tablename),
                force_propagate=True)
        run_sql("DROP FUNCTION IF EXISTS %s() CASCADE" % trigger, force_propagate=True)

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

def date_convert(t, delay=0):
    try:
        return mx.DateTime.DateTime(t.year,t.month,t.day,t.hour,t.minute,t.second+1e-6*t.microsecond) + mx.DateTime.DateTimeDeltaFromSeconds(delay)
    except Exception, e:
        return t + mx.DateTime.DateTimeDeltaFromSeconds(delay)
