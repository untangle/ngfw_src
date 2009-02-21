import logging
import psycopg
import re

from datetime import date
from datetime import timedelta
from sets import Set

# XXX function timing

__conn = None

def get_connection():
    global __conn

    if not __conn:
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
        logger.debug('did not drop table: %s' % tablename)
    finally:
        conn.commit()

def create_table_as_sql(tablename, query, args):
    run_sql("CREATE TABLE %s AS %s" % (tablename, query), args)

def run_sql(sql, args=None):
    conn = get_connection()

    try:
        curs = conn.cursor()
        curs.execute(sql, args)
    finally:
        conn.commit()

def create_partitioned_table(table_ddl, timestamp_column, start_date, end_date,
                             clear_tables=False):
    (schema, tablename) = __get_tablename(table_ddl)

    existing_dates = Set()

    for t in get_tables(schema='reports', prefix='%s_' % tablename):
        m=re.search('%s_(\d+)_(\d+)_(\d+)' % tablename, t)
        if m:
            d = date(*map(int, m.groups()))
            if d >= start_date and d < end_date:
                existing_dates.add(d)
            else:
                drop_table(t, schema='reports')
        else:
            logging.warn('ignoring table: %s' % tablename)

    interval = (end_date - start_date).days

    all_dates = Set(end_date - timedelta(days=i + 1) for i in range(interval))

    for d in all_dates - existing_dates:
        run_sql("""\
CREATE TABLE %s
(CHECK (%s >= %%s AND %s < %%s))
INHERITS (%s)""" % (__tablename_for_date(tablename, d),
                    timestamp_column, timestamp_column, tablename),
        (d, d + timedelta(days=1)))

    if clear_tables:
        for d in all_dates:
            drop_table(__tablename_for_date(tablename, d))

    __make_trigger(tablename, all_dates)

def get_update_info(tablename):
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
            rv = None

    finally:
        conn.commit()

    return rv

def set_update_info(tablename, last_update):
    conn = get_connection()
    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT count(*) FROM reports.table_updates WHERE table_updates = %s
""", (tablename,))
        row = curs.fetchone()

        if row[0] == 0:
            curs = conn.cursor()
            curs.execute("""\
INSERT INTO reports.table_updates (tablename, last_update) VALUES (%s, %s)
""", (tablename, date))
        else:
            curs = conn.cursor()
            curs.execute("""\
UPDATE reports.table_updates SET last_update = %s WHERE tablename = %s
""", (date, tablename))
    finally:
        conn.commit();

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

def __make_trigger(tablename, all_dates):
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
                                             d + timedelta(days=1),
                                             __get_tablename(tablename, d))
        first = False

    trigger_function += """\
    ELSE
        RAISE NOTICE 'Date out of range: %', NEW.#{timestamp_column};
    END IF;
    RETURN NULL;
END;
$$
LANGUAGE plpgsql;"""

    run_sql(trigger_function);

    trigger_name = "insert_%s_trigger" % tablename

    if not __has_trigger(schema, tablename, trigger_name % tablename):
        run_sql("""\
CREATE TRIGGER trigger_name
    BEFORE INSERT ON %s
    FOR EACH ROW EXECUTE PROCEDURE %s
""" % (trigger_name, tablename, trigger_function_name))

def __tablename_for_date(tablename, date):
    return "%s_%d_%d_%d" % ((tablename,) + date.timetuple()[0:3])

def __get_tablename(table_ddl):
    m = re.search('create\s+table\s+(\S+)', table_ddl, re.I | re.M)

    if m:
      s = m.group(1).split('.')
      return (s[0:-1], s[-1])
    else:
      raise ValueError("Cannot find table in: %s" % table_ddl)
