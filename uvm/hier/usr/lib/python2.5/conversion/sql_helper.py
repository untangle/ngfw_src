# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
# Dirk Morris <dmorris@untangle.com>

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
from psycopg2.extensions import DateFromMx


DEFAULT_SCHEMA = 'uvm'
SCHEMA = 'uvm'
CONNECTION_STRING = "dbname=uvm user=postgres"
EVIL_CHARACTERS = [ '"', "'", "\015" , "\r" , "\n" , "\f", "\t", "\b", "\\", "/"  ]

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


def sanitize_string(str):
    if str ==  None:
        return None
    global EVIL_CHARACTERS
    for char in EVIL_CHARACTERS:
        str=str.replace(char,"")
    return str

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


        print('%s took %0.3f ms' % (fun_name, (t2-t1)*1000.0))
        return res

    return wrapper

__conn = None

def get_connection():

    global __conn

    if not __conn:
        __conn = psycopg2.connect(CONNECTION_STRING, connection_factory=Connection)

    try:
        __conn.commit()
        curs = __conn.cursor()
        curs.execute("SELECT 1")
        __conn.commit()
    except:
        print("could not access database, getting new connection", exc_info)
        try:
            __conn.close()
        except:
            pass
        __conn = psycopg2.connect(CONNECTION_STRING, connection_factory=Connection)

    return __conn

def run_sql(sql, args=None, connection=None, auto_commit=True, force_propagate=False, debug=False):
    
    result = None

    if not connection:
        connection = get_connection()

    try:
        curs = connection.cursor()
        if args:
            if debug:
                print("Executing: %s" % re.sub(r'\#012\s*', ' ', curs.mogrify(sql, args)))
            curs.execute(sql, args)
            result = curs.fetchall()
            if debug:
                print("Executing: %s" % re.sub(r'\#012\s*', ' ', curs.mogrify(sql, args)))
        else:
            if debug:
                print("Executing: %s" % (sql,))
            curs.execute(sql)
            result = curs.fetchall()

        if auto_commit:
            connection.commit()

    except Exception, e:
        if force_propagate:
            if auto_commit:
                connection.rollback()
            raise e
        
        show_error = True
        if re.search(r'^\s*(DELETE|DROP) ', sql, re.I):
            show_error = False
        if re.search(r'^\s*(CREATE|ALTER) ', sql, re.I):
            show_error = False
            
        if show_error:
            raise e

        try:
            connection.rollback()
        except:
            pass
        
    return result

