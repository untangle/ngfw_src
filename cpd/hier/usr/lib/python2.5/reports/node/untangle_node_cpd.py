# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
# Copyright (c) 2003-2009 Untangle, Inc.

import gettext
import logging
import mx
import reports.colors as colors
import reports.i18n_helper
import reports.sql_helper as sql_helper

from psycopg import DateFromMx
from psycopg import QuotedString
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import Highlight
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import STACKED_BAR_CHART
from reports import SummarySection
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-cpd').lgettext
def N_(message): return message

LOGIN = _('Login')
UPDATE = _('Update')
LOGOUT = _('Logout')
FAILED = _('Failed')

class Cpd(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-cpd')

    def setup(self, start_date, end_date):
        self.__create_n_cpd_login_events(start_date, end_date)
        self.__create_n_cpd_block_events(start_date, end_date)
        
        ft = FactTable('reports.n_cpd_login_totals',
                       'reports.n_cpd_login_events',
                       'time_stamp', [], [])
        reports.engine.register_fact_table(ft)

        ft.measures.append(Column('logins',
                                    'integer',
                                    "count(CASE WHEN event = 'LOGIN' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('updates',
                                    'integer',
                                    "count(CASE WHEN event = 'UPDATE' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('logouts',
                                    'integer',
                                    "count(CASE WHEN event = 'LOGOUT' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('failures',
                                  'integer',
                                  "count(CASE WHEN event = 'FAILED' THEN 1 ELSE NULL END)"))

        ft = FactTable('reports.n_cpd_block_totals',
                       'reports.n_cpd_block_events',
                       'time_stamp',
                       [Column('proto', 'INT2'),
                        Column('client_intf','INT2'),
                        Column('client_address','INET'),
                        Column('server_address','INET')],
                       [Column('blocks','bigint', 'count(*)')])
        reports.engine.register_fact_table(ft)

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [CpdHighlight(self.name),
                            DailyUsage(),
                            TopUsers(),
                            TopBlockedClients()])
        sections.append(s)

        sections.append(CpdDetail())
        
        return Report(self, sections)

    def events_cleanup(self, cutoff):
        try:
            sql_helper.run_sql("""\
DELETE FROM events.n_cpd_login_events
WHERE time_stamp < %s""", (cutoff,))
        except: pass

    def reports_cleanup(self, cutoff):
        try:
            sql_helper.run_sql("""\
DELETE FROM reports.n_cpd_login_events
WHERE time_stamp < %s""", (cutoff,))
        except: pass

    @print_timing
    def __create_n_cpd_login_events(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.n_cpd_login_events (
    time_stamp timestamp without time zone,
    login_name text,
    event text,
    auth_type text,
    client_addr inet)""",  'time_stamp', start_date, end_date)

        sd = DateFromMx(sql_helper.get_update_info('reports.n_cpd_login_events', start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_cpd_login_events
      (time_stamp, login_name, event, auth_type, client_addr)
SELECT time_stamp, login_name, event, auth_type, client_addr
FROM events.n_cpd_login_evt
WHERE time_stamp >= %s AND time_stamp < %s""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_cpd_login_events', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @print_timing
    def __create_n_cpd_block_events(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.n_cpd_block_events (
    time_stamp timestamp without time zone,
    proto INT2,
    client_intf INT2,
    client_address INET,
    client_port INT4,
    server_address INET,
    server_port INT4)""",  'time_stamp', start_date, end_date)

        sd = DateFromMx(sql_helper.get_update_info('reports.n_cpd_block_events', start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_cpd_block_events
      (time_stamp, proto, client_intf, client_address, client_port, server_address, server_port)
SELECT time_stamp, proto, client_intf, client_address, client_port, server_address, server_port
FROM events.n_cpd_block_evt
WHERE time_stamp >= %s AND time_stamp < %s""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_cpd_block_events', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e


class CpdHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("processed") + " " + "%(logins)s" + " " +
                           _("user login events"))

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(sum(logins), 0) as logins
FROM reports.n_cpd_login_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        
        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h

class DailyUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'daily-usage', _('Daily Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        lks = []
        query = """
SELECT COALESCE(sum(logins), 0) / %s, COALESCE(max(logins), 0),
       COALESCE(sum(updates), 0) / %s, COALESCE(max(updates), 0),
       COALESCE(sum(logouts), 0) / %s, COALESCE(max(logouts), 0),
       COALESCE(sum(failures), 0) / %s, COALESCE(max(failures), 0),
       COALESCE(sum(logins+updates+logouts+failures), 0) / %s,
       COALESCE(max(logins+updates+logouts+failures), 0)
FROM reports.n_cpd_login_totals
WHERE trunc_time >= %s AND trunc_time < %s
"""
        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (report_days,)*5 + (one_week, ed))

            r = curs.fetchone()

            ks = KeyStatistic(_('Average Logins'), r[0], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Logins'), r[1], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Average Updates'), r[2], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Updates'), r[3], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Average Logouts'), r[4], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Logouts'), r[5], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Average Failures'), r[6], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Failures'), r[7], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Average Events'), r[8], N_('events'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Events'), r[9], N_('events'))
            lks.append(ks)
        finally:
            conn.commit()

        query = """
SELECT date_trunc('day', trunc_time) AS day,
sum(logins), sum(updates), sum(logouts),sum(failures),
sum(logins+updates+logouts+failures)
FROM reports.n_cpd_login_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY day
"""
        
        dates = []
        logins = []
        updates = []
        logouts = []
        failures = []
        events = []
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                dates.append(r[0])
                logins.append(r[1])
                updates.append(r[2])
                logouts.append(r[3])
                failures.append(r[4])
                events.append(r[5])
        finally:
            conn.commit()

        rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDelta(1))
        
        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_('Date'),
                     ylabel=_('Login Events'),
                     major_formatter=DATE_FORMATTER,
                     required_points=rp)
        plot.add_dataset(dates, logins, label=_('logins'),
                         color=colors.goodness)
        plot.add_dataset(dates, updates, label=_('updates'),
                         color=colors.detected)
        plot.add_dataset(dates, logouts, label=_('logouts'),
                         color=colors.badness)
        plot.add_dataset(dates, logouts, label=_('failures'),
                         color=colors.badness)

        return (lks, plot)


class TopUsers(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-users', _('Top Captive Portal Users'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT uid, sum(s2c_bytes + c2s_bytes)
FROM reports.session_totals
WHERE NOT uid IS NULL AND trunc_time >= %s AND trunc_time < %s
GROUP BY uid
LIMIT 10"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pie_data = {}

            for r in curs.fetchall():
                uid = r[0]
                bytes = r[1]

                ks = KeyStatistic(uid, bytes, N_('bytes'))
                lks.append(ks)

                pie_data[uid] = bytes
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title)

        plot.add_pie_dataset(pie_data)

        return (lks, plot)

class TopBlockedClients(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-blocked-users', _('Top Blocked Clients'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_address,sum(blocks)::int as blocks
FROM reports.n_cpd_block_totals
WHERE NOT client_address IS NULL AND trunc_time >= %s AND trunc_time < %s
GROUP BY client_address
LIMIT 10"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pie_data = {}

            for r in curs.fetchall():
                client_address = r[0]
                blocks = r[1]

                ks = KeyStatistic(client_address, blocks, _('blocks'))
                lks.append(ks)

                pie_data[client_address] = blocks
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title)

        plot.add_pie_dataset(pie_data)

        return (lks, plot)


class CpdDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'cpd-events', _('CPD Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email or user or host:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date'),
              ColumnDesc('login_name', _('Login name')),
              ColumnDesc('event', _('Type'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email or host or user:
            return None

        sql = """
SELECT time_stamp, login_name,
CASE WHEN event = 'LOGIN' THEN '%s'
     WHEN event = 'UPDATE' THEN '%s'
     WHEN event = 'LOGOUT' THEN '%s'
     WHEN event = 'FAILED' THEN '%s' END
FROM reports.n_cpd_login_events
WHERE time_stamp >= %s AND time_stamp < %s
ORDER BY time_stamp DESC
""" % (LOGIN, UPDATE, LOGOUT, FAILED,
       DateFromMx(start_date),
       DateFromMx(end_date))

        return sql
    
reports.engine.register_node(Cpd())
