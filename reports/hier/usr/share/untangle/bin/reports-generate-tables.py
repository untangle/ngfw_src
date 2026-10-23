#! /usr/bin/python3
# $Id: reports-generate-reports.py 38486 2014-08-21 22:29:30Z cblaise $

import os
import sys
import traceback
import getopt
from psycopg2 import sql as pg_sql


def usage():
     print("""\
usage: %s [options]
Options:
    -d <driver>     : "postgresql" or "sqlite"
    --roles-only    : configure Reports roles and ownership, then exit
""" % sys.argv[0])


class ArgumentParser(object):
    def __init__(self):
        pass

    def set_driver( self, arg ):
        global DRIVER
        DRIVER = arg

    def parse_args( self ):
        """Parse the driver and optional role-provisioning-only mode."""
        global CONFIGURE_ROLES_ONLY

        handlers = {
            '-d' : self.set_driver,
        }

        try:
             (optlist, args) = getopt.getopt(sys.argv[1:], 'd:', ['roles-only'])
             for opt in optlist:
                  if opt[0] == '--roles-only':
                       CONFIGURE_ROLES_ONLY = True
                  else:
                       handlers[opt[0]](opt[1])
             return args
        except getopt.GetoptError as exc:
             print(exc)
             usage()
             exit(1)


DRIVER = 'postgresql'
CONFIGURE_ROLES_ONLY = False
PYTHON_DIR = '@PREFIX@/usr/lib/python3/dist-packages'
REPORTS_PYTHON_DIR = '%s/reports' % (PYTHON_DIR)

if '@PREFIX@' != '':
     sys.path.insert(0, PYTHON_DIR)

import reports.sql_helper as sql_helper


def transfer_reports_object_ownership(cursor):
     """Transfer existing Reports objects to the restricted owner role.

     The migration is limited to objects in the reports schema. Tables are
     handled before sequences so owned sequences can follow their table's
     owner; routines and user-defined types are transferred afterward. The
     caller must have administrative authority and ngfw_reports must exist.
     """
     relation_commands = {
          "r": "TABLE",
          "p": "TABLE",
          "v": "VIEW",
          "m": "MATERIALIZED VIEW",
          "S": "SEQUENCE",
          "f": "FOREIGN TABLE",
     }

     cursor.execute("""
          SELECT relation.relkind, namespace.nspname, relation.relname
          FROM pg_class AS relation
          JOIN pg_namespace AS namespace ON namespace.oid = relation.relnamespace
          WHERE namespace.nspname = 'reports'
            AND relation.relkind IN ('r', 'p', 'v', 'm', 'S', 'f')
            AND relation.relowner <> (SELECT oid FROM pg_roles WHERE rolname = 'ngfw_reports')
          -- Move table ownership before sequence ownership because an
          -- OWNED BY sequence must have the same owner as its table.
          ORDER BY CASE WHEN relation.relkind = 'S' THEN 1 ELSE 0 END,
                   relation.relname
     """)
     for relkind, schema_name, object_name in cursor.fetchall():
          cursor.execute(
               pg_sql.SQL("ALTER {} {}.{} OWNER TO ngfw_reports").format(
                    pg_sql.SQL(relation_commands[relkind]),
                    pg_sql.Identifier(schema_name),
                    pg_sql.Identifier(object_name)))

     cursor.execute("""
          SELECT procedure.prokind, namespace.nspname, procedure.proname,
                 pg_get_function_identity_arguments(procedure.oid)
          FROM pg_proc AS procedure
          JOIN pg_namespace AS namespace ON namespace.oid = procedure.pronamespace
          WHERE namespace.nspname = 'reports'
            AND procedure.prokind IN ('f', 'p', 'a', 'w')
            AND procedure.proowner <> (SELECT oid FROM pg_roles WHERE rolname = 'ngfw_reports')
          ORDER BY procedure.proname
     """)
     for prokind, schema_name, object_name, identity_arguments in cursor.fetchall():
          command = "AGGREGATE" if prokind == "a" else (
               "PROCEDURE" if prokind == "p" else "FUNCTION")
          cursor.execute(
               pg_sql.SQL("ALTER {} {}.{}({}) OWNER TO ngfw_reports").format(
                    pg_sql.SQL(command),
                    pg_sql.Identifier(schema_name),
                    pg_sql.Identifier(object_name),
                    pg_sql.SQL(identity_arguments)))

     cursor.execute("""
          SELECT type.typtype, namespace.nspname, type.typname
          FROM pg_type AS type
          JOIN pg_namespace AS namespace ON namespace.oid = type.typnamespace
          WHERE namespace.nspname = 'reports'
            AND type.typtype IN ('d', 'e', 'r', 'm')
            AND type.typelem = 0
            AND type.typowner <> (SELECT oid FROM pg_roles WHERE rolname = 'ngfw_reports')
          ORDER BY type.typname
     """)
     for type_kind, schema_name, object_name in cursor.fetchall():
          command = "DOMAIN" if type_kind == "d" else "TYPE"
          cursor.execute(
               pg_sql.SQL("ALTER {} {}.{} OWNER TO ngfw_reports").format(
                    pg_sql.SQL(command),
                    pg_sql.Identifier(schema_name),
                    pg_sql.Identifier(object_name)))


def revoke_role_memberships(cursor, member_name):
     """Revoke every direct role membership held by member_name.

     This removes both inherited privileges and any ability to SET ROLE into
     another role. The caller is responsible for granting back only the
     specific membership required by the Reports restore design.
     """
     cursor.execute("""
          SELECT granted_role.rolname
          FROM pg_auth_members AS membership
          JOIN pg_roles AS granted_role ON granted_role.oid = membership.roleid
          JOIN pg_roles AS member_role ON member_role.oid = membership.member
          WHERE member_role.rolname = %s
     """, (member_name,))
     for granted_role, in cursor.fetchall():
          cursor.execute(
               pg_sql.SQL("REVOKE {} FROM {}").format(
                    pg_sql.Identifier(granted_role),
                    pg_sql.Identifier(member_name)))


def restrict_reports_role_memberships(cursor):
     """Constrain role membership to the Reports restore capability path.

     ngfw_reports cannot switch into other roles, reports_restore cannot
     inherit or switch into other roles, and no other role can assume
     ngfw_reports. configure_reports_roles() grants the single allowed
     reports_restore-to-ngfw_reports membership afterward.
     """
     # The owner role must not itself be able to switch into another role, and
     # only the dedicated restore login may switch into the owner role.
     revoke_role_memberships(cursor, "ngfw_reports")
     revoke_role_memberships(cursor, "reports_restore")
     cursor.execute("""
          SELECT member_role.rolname
          FROM pg_auth_members AS membership
          JOIN pg_roles AS granted_role ON granted_role.oid = membership.roleid
          JOIN pg_roles AS member_role ON member_role.oid = membership.member
          WHERE granted_role.rolname = 'ngfw_reports'
            AND member_role.rolname <> 'reports_restore'
     """)
     for member_name, in cursor.fetchall():
          cursor.execute(
               pg_sql.SQL("REVOKE ngfw_reports FROM {}").format(
                    pg_sql.Identifier(member_name)))


def configure_reports_roles():
     """Provision Reports roles and migrate reports-schema object ownership.

     This idempotent setup expects the reports schema to exist and the
     connection to have trusted administrative privileges. It makes
     ngfw_reports a non-login owner, limits reports_restore to connecting and
     explicitly assuming that role, removes the earlier direct grants, and
     commits the configuration as one transaction.
     """
     connection = sql_helper.get_connection()
     try:
          with connection.cursor() as cursor:
               cursor.execute("SELECT 1 FROM pg_roles WHERE rolname = %s", ("ngfw_reports",))
               if cursor.fetchone() is None:
                    cursor.execute("""
                         CREATE ROLE ngfw_reports
                             NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
                             NOINHERIT NOREPLICATION NOBYPASSRLS
                    """)

               cursor.execute("""
                    ALTER ROLE ngfw_reports
                        NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
                        NOINHERIT NOREPLICATION NOBYPASSRLS
               """)
               cursor.execute("SELECT 1 FROM pg_roles WHERE rolname = %s", ("reports_restore",))
               if cursor.fetchone() is None:
                    cursor.execute("""
                         CREATE ROLE reports_restore
                             LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
                             NOINHERIT NOREPLICATION NOBYPASSRLS
                    """)
               cursor.execute("""
                    ALTER ROLE reports_restore
                        LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
                        NOINHERIT NOREPLICATION NOBYPASSRLS
               """)
               restrict_reports_role_memberships(cursor)
               cursor.execute("GRANT CONNECT ON DATABASE uvm TO reports_restore")
               cursor.execute("GRANT USAGE, CREATE ON SCHEMA reports TO ngfw_reports")
               # Keep this GRANT compatible with PostgreSQL 13 (Bullseye).
               # reports_restore is NOINHERIT, so it must explicitly SET ROLE
               # to use ngfw_reports privileges; membership option syntax was
               # added in PostgreSQL 16.
               cursor.execute("GRANT ngfw_reports TO reports_restore")

               # Remove the direct object/schema grants from the previous
               # reports_restore design. The restore gets these capabilities
               # only after explicitly SET ROLE ngfw_reports.
               cursor.execute("REVOKE CREATE, USAGE ON SCHEMA reports FROM reports_restore")
               cursor.execute("REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA reports FROM reports_restore")
               cursor.execute("REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA reports FROM reports_restore")
               cursor.execute("""
                    ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA reports
                        REVOKE ALL ON TABLES FROM reports_restore
               """)
               cursor.execute("""
                    ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA reports
                        REVOKE ALL ON SEQUENCES FROM reports_restore
               """)
               cursor.execute("""
                    ALTER DEFAULT PRIVILEGES FOR ROLE ngfw_reports IN SCHEMA reports
                        REVOKE ALL ON TABLES FROM reports_restore
               """)
               cursor.execute("""
                    ALTER DEFAULT PRIVILEGES FOR ROLE ngfw_reports IN SCHEMA reports
                        REVOKE ALL ON SEQUENCES FROM reports_restore
               """)

               transfer_reports_object_ownership(cursor)
          connection.commit()
     except Exception:
          connection.rollback()
          raise

parser = ArgumentParser()
parser.parse_args()
sql_helper.DBDRIVER = DRIVER
#sql_helper.PRINT_TIMINGS = True

if DRIVER == "postgresql":
     os.system("createuser -U postgres -dSR untangle >/dev/null 2>&1")
     os.system("createdb -O postgres -U postgres uvm >/dev/null 2>&1")
     os.system("createlang -U postgres plpgsql uvm >/dev/null 2>&1")

if DRIVER == "sqlite":
     if not os.path.exists('/var/lib/sqlite'):
          os.makedirs('/var/lib/sqlite')

try:
     sql_helper.create_schema(sql_helper.schema())
except Exception:
     print("Failed to create schema")
     traceback.print_exc()

if DRIVER == "postgresql":
     # Transfer existing Reports objects and generate future objects as the
     # dedicated owner role. The application connection remains postgres.
     configure_reports_roles()
     if CONFIGURE_ROLES_ONLY:
          sys.exit(0)
     sql_helper.set_session_role("ngfw_reports")

for f in os.listdir(REPORTS_PYTHON_DIR):
     if f.endswith('py'):
          (m, e) = os.path.splitext(f)
          if "__init__" == m:
               continue
          name = 'reports.%s' % m
          obj = __import__('reports.%s' % m)
          app = getattr(obj,m)
          #obj = eval(name)
          try:
               if "generate_tables" in dir(app):
                    # print("%s.generate_tables()" % name)
                    app.generate_tables()
          except:
               print("%s.generate_tables() Exception:" % name)
               traceback.print_exc()

