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

import gettext
import logging
import mx
import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper
import sys

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
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-firewall').lgettext

class Firewall(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-firewall')

    @sql_helper.print_timing
    def setup(self, start_date, end_date):
        self.__update_sessions(start_date, end_date)

        ft = reports.engine.get_fact_table('reports.session_totals')

        ft.measures.append(Column('firewall_blocks', 'integer',
                                  "count(CASE WHEN NOT firewall_was_blocked ISNULL THEN 1 ELSE null END)"))

        ft.dimensions.append(Column('firewall_rule_index', 'integer'))

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []
        s = reports.SummarySection('summary', _('Summary Report'),
                                   [FirewallHighlight(self.name),
                                    DailyRules(),
                                    TopTenBlockingRulesByHits(),
                                    TopTenBlockedHostsByHits(),
                                    TopTenBlockedUsersByHits()])
        sections.append(s)

        sections.append(FirewallDetail())

        return reports.Report(self, sections)


    def events_cleanup(self, cutoff):
        try:
            sql_helper.run_sql("""\
DELETE FROM events.n_firewall_evt WHERE time_stamp < %s""", (cutoff,))
            sql_helper.run_sql("""\
DELETE FROM events.n_firewall_statistic_evt WHERE time_stamp < %s""", (cutoff,))
        except: pass

    def reports_cleanup(self, cutoff):
        pass

    @sql_helper.print_timing
    def __update_sessions(self, start_date, end_date):
        try:
            sql_helper.run_sql("""
ALTER TABLE reports.sessions ADD COLUMN firewall_was_blocked boolean""")
        except: pass
        try:
            sql_helper.run_sql("""
ALTER TABLE reports.sessions ADD COLUMN firewall_rule_index integer""")
        except: pass

        sd = DateFromMx(sql_helper.get_update_info('sessions[firewall]',
                                                   start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.sessions
SET firewall_was_blocked = was_blocked, firewall_rule_index = rule_index
FROM events.n_firewall_evt
WHERE reports.sessions.time_stamp >= %s
  AND reports.sessions.time_stamp < %s
  AND reports.sessions.pl_endp_id = events.n_firewall_evt.pl_endp_id""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('sessions[firewall]', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class FirewallHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("scanned") + " " + "%(sessions)s" + " " +
                           _("sessions and blocked") + " " +
                           "%(blocks)s" + " " + _("according to the rules"))

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(SUM(new_sessions),0)::int AS sessions,
       COALESCE(sum(firewall_blocks), 0) AS blocks
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h

class DailyRules(reports.Graph):
    def __init__(self):
        reports.Graph.__init__(self, 'daily-rules', _('Daily Rules'))

    @sql_helper.print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """\
SELECT avg(sessions_logged) as avg_sessions_logged, max(sessions_logged) as max_sessions_logged,
       avg(sessions_blocked) as avg_sessions_blocked, max(sessions_blocked) as max_sessions_blocked
FROM (select date_trunc('day', time_stamp) AS day,
      count(CASE WHEN firewall_rule_index IS NOT NULL THEN 1 ELSE null END) AS sessions_logged,
      count(CASE WHEN firewall_was_blocked THEN 1 ELSE null END) AS sessions_blocked
      FROM reports.sessions
      WHERE time_stamp >= %s AND time_stamp < %s"""

        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"

        avg_max_query = avg_max_query + " GROUP BY day) AS foo"

        conn = sql_helper.get_connection()
        try:
            lks = []

            curs = conn.cursor()
            if host:
                curs.execute(avg_max_query, (one_week, ed, host))
            elif user:
                curs.execute(avg_max_query, (one_week, ed, user))
            else:
                curs.execute(avg_max_query, (one_week, ed))
            r = curs.fetchone()
            ks = reports.KeyStatistic(_('Avg'), r[0], _('logged/day'))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max'), r[1], _('logged/day'))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Avg'), r[2], _('blocked/day'))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max'), r[3], _('blocked/day'))
            lks.append(ks)
        finally:
            conn.commit()

        return lks

    @sql_helper.print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        conn = sql_helper.get_connection()
        try:
            q = """\
SELECT date_trunc('day', time_stamp) AS time,
      count(CASE WHEN firewall_rule_index IS NOT NULL THEN 1 ELSE null END) AS sessions_logged,
      count(CASE WHEN firewall_was_blocked THEN 1 ELSE null END) AS sessions_blocked
FROM reports.sessions
WHERE time_stamp >= %s AND time_stamp < %s"""
            if host:
                q = q + " AND hname = %s"
            elif user:
                q = q + " AND uid = %s"
            q = q + """
GROUP BY time
ORDER BY time asc"""

            curs = conn.cursor()

            if host:
                curs.execute(q, (one_week, ed, host))
            elif user:
                curs.execute(q, (one_week, ed, user))
            else:
                curs.execute(q, (one_week, ed))

            dates = []
            logs = []
            blocks = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                logs.append(r[1])
                blocks.append(r[2])
        finally:
            conn.commit()

        rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDelta(1))

        plot = reports.Chart(type=reports.STACKED_BAR_CHART,
                             title=_('Daily Rules'),
                             xlabel=_('Day'),
                             ylabel=_('sessions/day'),
                             major_formatter=reports.DATE_FORMATTER,
                             required_points=rp)

        plot.add_dataset(dates, logs, label=_('logged'))
        plot.add_dataset(dates, blocks, label=_('blocked'))

        return plot

class TopTenBlockedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-firewall-blocked-hosts-by-hits', _('Top Ten Firewall Blocked Hosts By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hname, count(*) as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND firewall_blocks > 0
AND firewall_rule_index IS NOT NULL"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query = query + " GROUP BY hname ORDER BY hits_sum DESC"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

                for r in curs.fetchall():
                    ks = KeyStatistic(r[0], r[1], _('hits'), link_type=reports.HNAME_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Firewall Blocked Hosts (by hits)'),
                     xlabel=_('Host'),
                     ylabel=_('Blocks per Day'))
        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenBlockingRulesByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-firewall-blocking-rules-by-hits', _('Top Ten Firewall Blocking Rules By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT firewall_rule_index, count(*) as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND firewall_blocks > 0
AND firewall_rule_index IS NOT NULL"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query = query + " GROUP BY firewall_rule_index ORDER BY hits_sum DESC"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Firewall Blocking Rules (by hits)'),
                     xlabel=_('Rule #'),
                     ylabel=_('Blocks per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenBlockedUsersByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-firewall-blocked-users-by-hits', _('Top Ten Firewall Blocked Users By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT uid, count(*) as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND uid != ''
AND firewall_blocks > 0
AND firewall_rule_index IS NOT NULL"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query += " GROUP BY uid ORDER BY hits_sum DESC"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'), link_type=reports.USER_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Firewall Blocked Users (by hits)'),
                     xlabel=_('User'),
                     ylabel=_('Blocks per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class FirewallDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'firewall-events', _('Firewall Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if not host:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))
        if not user:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))

        rv = rv + [ColumnDesc('firewall_rule_index', _('Rule Applied')),
                   ColumnDesc('firewall_was_blocked', _('Action')),
                   ColumnDesc('c_server_addr', _('Destination IP')),
                   ColumnDesc('c_server_port', _('Destination Port')),
                   ColumnDesc('c_client_addr', _('Source IP')),
                   ColumnDesc('c_client_port', _('Source Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = "SELECT time_stamp,"

        if not host:
            sql = sql + "hname, "
        if not user:
            sql = sql + "uid, "

        sql = sql + ("""firewall_rule_index, firewall_was_blocked::text, host(c_server_addr), c_server_port, host(c_client_addr), c_client_port
FROM reports.sessions
WHERE time_stamp >= %s AND time_stamp < %s
AND NOT firewall_rule_index IS NULL""" % (DateFromMx(start_date),
                                         DateFromMx(end_date)))

        if host:
            sql = sql + (" AND hname = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND uid = %s" % QuotedString(user))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(Firewall())
