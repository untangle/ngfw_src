import datetime
import inspect
import logging
import mx
import psycopg2
import re
import string
import sys
import time
import sqlite3
from psycopg2.extensions import DateFromMx

from reports.log import *
logger = getLogger(__name__)

REQUIRED_TIME_POINTS = [float(s) for s in range(0, 24 * 60 * 60, 30 * 60)]
HOURLY_REQUIRED_TIME_POINTS = [float(s) for s in range(0, 24 * 60 * 60, 60 * 60)]

DEFAULT_TIME_FIELD = 'time_stamp'
DEFAULT_SLICES = 150

DBDRIVER = 'postgresql'
SCHEMA = 'reports'

UNLOGGED_ENABLED = False
EXTRA_INDEXES_ENABLED = True
CONNECTION_STRING = "dbname=uvm user=postgres"

class Cursor(psycopg2.extensions.cursor):
    def execute(self, sql, args=None):
        psycopg2.extensions.cursor.execute(self, sql, args)

    def executemany(self, sql, args=None):
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
        filename = 'unknown'
        line_number = '?'

        for k, v in inspect.getmembers(func):
            if k == 'func_code':
                m = re.search('/(reports/.*).py', v.co_filename)
                filename = m.group(1)
                line_number = v.co_firstlineno

        fun_name = "%s (%s:%s)" % (func.func_name, filename, line_number)

        #logger.debug('%s running...' % (fun_name))

        t1 = time.time()
        res = func(*arg)
        t2 = time.time()

        logger.debug('%s took %0.1f ms' % (fun_name, (t2-t1)*1000))
        return res

    return wrapper

__conn = None

def get_connection():
    global __conn
    global DBDRIVER

    if not __conn and ( DBDRIVER == None or DBDRIVER == "postgresql" ):
        __conn = psycopg2.connect(CONNECTION_STRING, connection_factory=Connection)
    if not __conn and ( DBDRIVER == "sqlite" ):
        __conn = sqlite3.connect('/var/lib/sqlite/reports.db')

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
        __conn = psycopg2.connect(CONNECTION_STRING, connection_factory=Connection)

    return __conn

#
# Runs the sql command and returns true if the sql command returns a "1" and false otherwise
#
def run_sql_one(sql):
    conn = get_connection()

    try:
        curs = conn.cursor()
        curs.execute(sql)
        row = curs.fetchone()
        if row and row[0] == 1:
            return True
    finally:
        conn.commit()
    return False

def run_sql(sql, args=None, connection=None, auto_commit=True, force_propagate=False, debug=False):
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
                logger.warn("Executing: %s" % re.sub(r'\n', ' ', sql))
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
            show_error = True
            
        if show_error:
            raise e

        try:
            connection.rollback()
        except:
            pass

def column_exists( tablename, columnname ):
    if DBDRIVER == "sqlite":
        try:
            run_sql_one("select 1 from %s where %s is null limit 1" % (tablename, columnname))
            column_exists = True
        except:
            column_exists = False
    else:
        column_exists = run_sql_one("select 1 from information_schema.columns where table_schema = '%s' and table_name = '%s' and  column_name = '%s'" % (SCHEMA, tablename, columnname))
    if column_exists:
        return True
    return False

def add_column( tablename, columnname, type ):
    if column_exists( tablename, columnname ):
        return
    sql = "ALTER TABLE %s.%s ADD COLUMN %s %s" % (SCHEMA, tablename, columnname, type)
    logger.info(sql)
    run_sql(sql)

def drop_column( tablename, columnname ):
    if not column_exists( tablename, columnname ):
        return
    sql = "ALTER TABLE %s.%s DROP COLUMN %s" % (SCHEMA+".", tablename, columnname)
    logger.info(sql)
    run_sql(sql)

def rename_column( tablename, oldname, newname ):
    if column_exists( tablename, newname ):
        #logger.debug("rename failed, column already exists: %s.%s" % (tablename, newname))
        return
    if not column_exists( tablename, oldname ):
        #logger.debug("rename failed, column missing: %s.%s" % (tablename, oldname))
        return
    sql = "ALTER TABLE %s.%s RENAME COLUMN %s to %s" % (SCHEMA, tablename, oldname, newname)
    logger.info(sql)
    run_sql(sql)

def rename_table( oldname, newname ):
    if table_exists( newname ):
        return
    if not table_exists( oldname ):
        return
    sql = "ALTER TABLE %s.%s RENAME TO %s" % (SCHEMA, oldname, newname)
    logger.info(sql)
    run_sql(sql)

def convert_column( tablename, columnname, oldtype, newtype ):
    column_type_exists = run_sql_one("select 1 from information_schema.columns where table_schema = '%s' and table_name = '%s' and  column_name = '%s' and data_type = '%s'" % (SCHEMA, tablename, columnname, oldtype))
    if not column_type_exists:
        return
    sql = "ALTER TABLE %s.%s ALTER COLUMN %s TYPE %s" % (SCHEMA, tablename, columnname, newtype)
    logger.info(sql)
    run_sql(sql);

def index_exists( tablename, columnname, unique=False ):
    if DBDRIVER == "sqlite":
        return True
    if unique:
        uniqueStr1="unique_"
    else:
        uniqueStr1=""
    return run_sql_one("select 1 from pg_class where relname = '%s_%s_%sidx'" % (tablename, columnname, uniqueStr1))

def create_index( tablename, columnname, unique=False ):
    if DBDRIVER == "sqlite":
        return
    if (index_exists( tablename, columnname, unique )):
        return
    if unique:
        uniqueStr1="unique_"
        uniqueStr2="UNIQUE "
    else:
        uniqueStr1=""
        uniqueStr2=""
    sql = 'CREATE %sINDEX %s_%s_%sidx ON %s.%s(%s)' % (uniqueStr2, tablename, columnname, uniqueStr1, SCHEMA, tablename, columnname)
    logger.info(sql)
    run_sql(sql)

def drop_index( tablename, columnname, unique=False ):
    if DBDRIVER == "sqlite":
        return True
    if (not index_exists( tablename, columnname, unique)):
        return
    if unique:
        uniqueStr1="unique_"
    else:
        uniqueStr1=""
    sql = 'DROP INDEX %s.%s_%s_%sidx' % (SCHEMA, tablename, columnname, uniqueStr1 )
    logger.info(sql)
    run_sql(sql)
                                    
def rename_index( oldname, newname ):
    already_exists = run_sql_one("select 1 from pg_class where relname = '%s'" % (oldname))
    if not already_exists:
        return
    sql = 'ALTER INDEX %s.%s RENAME TO %s' % (SCHEMA, oldname, newname)
    logger.info(sql)
    run_sql(sql)

def create_schema(schema):
    if DBDRIVER == "sqlite":
        return
    already_exists = run_sql_one("select 1 from pg_namespace where nspname = '%s'" % (schema))
    if already_exists:
        return
    sql = "CREATE SCHEMA %s" % (schema)
    logger.info(sql)
    run_sql(sql)

def clean_table(tablename, cutoff):
    logger.info("clean_table " + str(tablename) + " < " + str(cutoff))
    
    for t, date in find_table_partitions(tablename):
        # if the entire table is before the date specified, just drop it
        # but only if the *entire* table is before the the cutoff
        day_cutoff = mx.DateTime.Parser.DateTimeFromString("%d-%d-%d"%cutoff.timetuple()[:3])
        if date < day_cutoff:
            logger.info("DROP TABLE " + str(t))
            drop_table( t )

    # delete any old events from existing tables
    # this is kept because we used to store events directly in the master table
    # also this is inexpensive because of the partition checks it will only be run
    # on relevent partitions
    if column_exists( tablename, "time_stamp" ):
        sql = "DELETE FROM %s.%s WHERE time_stamp < %%s;" % (SCHEMA, tablename)
        logger.debug( sql % cutoff )
        run_sql(sql, (cutoff,))
    else: 
        logger.warn("Table %s missing time_stamp column!" % tablename)

def create_view( view_sql ):
    run_sql(view_sql)
        
def create_table( table_sql, unique_index_columns=[], other_index_columns=[], create_partitions=True ):
    (schema, tablename) = __get_tablename(table_sql)

    if schema:
        full_tablename = "%s.%s" % (SCHEMA, tablename)
        if DBDRIVER == "sqlite":
            table_sql = table_sql.replace("reports."+tablename,full_tablename)
    else:
        full_tablename = tablename

    # if extra indexes not enabled, just set the list to empty    
    if not EXTRA_INDEXES_ENABLED:
        other_index_columns=[]

    # create root table
    if not table_exists( tablename ):
        logger.info("CREATE %s TABLE %s" % ((UNLOGGED_ENABLED == True and "UNLOGGED" or ""),full_tablename))
        run_sql(table_sql)
        # always create time_stamp index
        if column_exists( tablename, "time_stamp") and not index_exists( tablename, "time_stamp", unique=False):
            create_index( tablename, "time_stamp", unique=False);
        # create other indexes
        for column in unique_index_columns:
            if not index_exists( tablename, column, unique=True ):
                create_index( tablename, column, unique=True );
        for column in other_index_columns:
            if not index_exists( tablename, column, unique=False ):
                create_index( tablename, column, unique=False );


    # sqlite does not support partitions
    if DBDRIVER == "sqlite":
        return

    # for importing old data and testing
    # now = mx.DateTime.strptime(mx.DateTime.now().strftime('%Y-%m-%d'),'%Y-%m-%d') - mx.DateTime.RelativeDateTime(days=30)
    # start_times = [now + mx.DateTime.RelativeDateTime(days=i) for i in range(31)]

    # start with the current day
    now = mx.DateTime.strptime(mx.DateTime.now().strftime('%Y-%m-%d'),'%Y-%m-%d')
    start_times = [now + mx.DateTime.RelativeDateTime(days=i) for i in range(2)]

    # create partition tables
    if create_partitions:
        for table_start_time in start_times:
            partition_tablename = tablename + "_" + table_start_time.strftime('%Y_%m_%d')
            partition_full_tablename = schema + "." + partition_tablename
            partition_table_sql = __sub_tablename( table_sql, partition_full_tablename )

            if not table_exists( partition_tablename ):
                start_time = table_start_time
                end_time = table_start_time + mx.DateTime.RelativeDateTime(days=1)
                partition_table_sql = "CREATE %s TABLE %s (CHECK (time_stamp >= '%s' AND time_stamp < '%s')) INHERITS (%s)" % ((UNLOGGED_ENABLED == True and "UNLOGGED" or ""), partition_full_tablename, start_time, end_time, full_tablename)
                logger.info(partition_table_sql)
                run_sql(partition_table_sql)

                # always create time_stamp index
                if not index_exists( partition_tablename, "time_stamp", unique=False ):
                    create_index( partition_tablename, "time_stamp", unique=False );
                # create other indexes
                for column in unique_index_columns:
                    if not index_exists( partition_tablename, column, unique=True ):
                        create_index( partition_tablename, column, unique=True );
                for column in other_index_columns:
                    if not index_exists( partition_tablename, column, unique=False ):
                        create_index( partition_tablename, column, unique=False );

        # create insert trigger
        # this trigger only includes inserting new events for today and tomorrow 
        # and yesterday if the table exists
        trigger_times = start_times
        yesterday = (now - mx.DateTime.RelativeDateTime(days=1)).strftime('%Y_%m_%d')
        if ( table_exists( tablename + "_" + yesterday ) ):
            trigger_times.insert(0, (now - mx.DateTime.RelativeDateTime(days=1)))

        __make_trigger( tablename, 'time_stamp', trigger_times )

def drop_view( view ):
    tn = '%s.%s' % (SCHEMA, view)
    conn = get_connection()
    try:
        curs = conn.cursor()
        curs.execute('DROP VIEW %s' % (tn,))
        logger.debug("dropped view '%s'" % (view,))
    except psycopg2.ProgrammingError:
        logger.debug('cannot drop view: %s' % (view,))
    finally:
        conn.commit()
        
def drop_table( table ):
    tn = '%s.%s' % (SCHEMA, table)

    conn = get_connection()
    try:
        curs = conn.cursor()
        curs.execute('DROP TABLE %s' % (tn,))
        logger.debug("dropped table '%s'" % (table,))
    except psycopg2.ProgrammingError:
        logger.debug('cannot drop table: %s' % (table,))
    finally:
        conn.commit()

def table_exists( tablename ):
    if DBDRIVER == "sqlite":
        return run_sql_one("SELECT 1 FROM sqlite_master where name = '%s'" % (tablename))
    else:
        return run_sql_one("SELECT 1 FROM pg_catalog.pg_tables WHERE schemaname = '%s' AND tablename = '%s'" % (SCHEMA, tablename))

def get_tables( prefix ):
    conn = get_connection()

    try:
        curs = conn.cursor()

        curs.execute("SELECT tablename FROM pg_catalog.pg_tables WHERE tablename LIKE '" + str(prefix) + "%'" )

        rows = curs.fetchall()
        rv = [rows[i][0] for i in range(len(rows))]
    finally:
        conn.commit()

    return rv

def find_table_partitions(tablename=None):
    if not tablename:
        prefix = ''
    else:
        prefix = '%s_' % tablename
        
    tables = []
    for t in get_tables( prefix ):
        m = re.search('%s(\d+)_(\d+)_(\d+)' % prefix, t)
        if m:
            d = mx.DateTime.Date(*map(int, m.groups()))
            tables.append((t, d))
    return tables
    
def get_date_range(start_date, hours):
    #mx.DateTime.today() + mx.DateTime.RelativeDateTime(days=1)
    #l = int(round((end_date - start_date).days+1))
    return [start_date + mx.DateTime.RelativeDateTime(hours=i) for i in range(hours)]

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

def __tablename_for_date(tablename, date):
    return "%s_%d_%02d_%02d" % ((tablename,) + date.timetuple()[0:3])

def __get_tablename(table_ddl):
    m = re.search('create\s+table\s+(\S+)', table_ddl, re.I | re.M)

    if m:
      s = m.group(1).split('.')
      return (string.join(s[0:-1], '.'), s[-1])
    else:
      raise ValueError("Cannot find table in: %s" % table_ddl)

def __sub_tablename(sql,new_name):
    m = re.search('(create\s+table\s+)(\S+)(.*)', sql.replace('\n',''), re.I | re.M)

    if m:
        start = m.group(1)
        end = m.group(3)
        return start + new_name + end
    else:
      raise ValueError("Cannot find table in: %s" % table_ddl)

def __make_trigger( tablename, timestamp_column, all_dates ):
    full_tablename = '%s.%s' % (SCHEMA, tablename)

    trigger_function_name = '%s_insert_trigger()' % tablename

    trigger_function = """\
CREATE OR REPLACE FUNCTION %s
RETURNS TRIGGER AS $$
BEGIN
""" % trigger_function_name

    first = True
    for d in all_dates:
        partition_table_name = SCHEMA + "." + tablename + "_" + d.strftime('%Y_%m_%d')

        trigger_function += """\
    %s (NEW.%s >= '%s' AND NEW.%s < '%s') THEN
        INSERT INTO %s VALUES (NEW.*);""" % ('IF' if first else "ELSIF",
                                             timestamp_column, d,
                                             timestamp_column, d + mx.DateTime.RelativeDateTime(days=1),
                                             partition_table_name)
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

    if not __trigger_exists( tablename, trigger_name ):
        run_sql("""\
CREATE TRIGGER %s
    BEFORE INSERT ON %s.%s
    FOR EACH ROW EXECUTE PROCEDURE %s
""" % (trigger_name, SCHEMA, tablename, trigger_function_name))

def __trigger_exists( tablename, trigger_name ):
    conn = get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""
SELECT 1 FROM information_schema.triggers
WHERE trigger_schema = %s AND event_object_table = %s AND trigger_name = %s
""", (SCHEMA, tablename, trigger_name))

        rv = curs.rowcount
    finally:
        conn.commit()

    return rv

def date_convert(t, delay=0):
    try:
        return mx.DateTime.DateTime(t.year,t.month,t.day,t.hour,t.minute,t.second+1e-6*t.microsecond) + mx.DateTime.DateTimeDeltaFromSeconds(delay)
    except Exception, e:
        return t + mx.DateTime.DateTimeDeltaFromSeconds(delay)
