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

from psycopg import DateFromMx
from psycopg import QuotedString
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import SummarySection
from reports import TIMESTAMP_FORMATTER
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.engine import TOP_LEVEL
from sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-shield').lgettext

class Shield(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-shield')

    def setup(self, start_date, end_date):

        ft = FactTable('reports.n_shield_rejection_totals',
                       'events.n_shield_rejection_evt',
                       'time_stamp',
                       [Column('client_addr', 'inet'),
                        Column('client_intf', 'integer'),
                        Column('mode', 'integer')],
                       [Column('limited', 'integer', 'sum(limited)'),
                        Column('dropped', 'integer', 'sum(dropped)'),
                        Column('rejected', 'integer', 'sum(rejected)')])
        reports.engine.register_fact_table(ft)

        ft = FactTable('reports.n_shield_totals',
                       'events.n_shield_statistic_evt',
                       'time_stamp',
                       [],
                       [Column('accepted', 'integer', 'sum(accepted)'),
                        Column('limited', 'integer', 'sum(limited)'),
                        Column('dropped', 'integer', 'sum(dropped)'),
                        Column('rejected', 'integer', 'sum(rejected)')])
        reports.engine.register_fact_table(ft)

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [DailyRequest(), BlockedHosts(), LimitedHosts()])
        sections.append(s)

        sections.append(ShieldDetail())

        return Report(self.name, sections)

class DailyRequest(Graph):
    def __init__(self):
        Graph.__init__(self, 'daily-request', _('Daily Requests'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            query = """\
SELECT sum(accepted) / 1440 as avg_accepted, max(accepted) as max_accepted,
       sum(limited) / 1440 as avg_limited, max(limited) as max_limited,
       sum(dropped + rejected) / 1440 as avg_blocked,
       max(dropped + rejected) as max_blocked
FROM reports.n_shield_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

            curs.execute(query, (one_week, ed))

            r = curs.fetchone()

            lks = []
            ks = KeyStatistic(_('Avg Requests'), r[0],
                              _('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Requests'), r[1],
                              _('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Limited'), r[2],
                              _('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Limited'), r[3],
                              _('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Blocked'), r[4],
                              _('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Blocked'), r[5],
                              _('sessions/minute'))
            lks.append(ks)

            # per minute
            sums = ["coalesce(sum(accepted), 0) * 60",
                    "coalesce(sum(limited), 0) * 60",
                    "coalesce(sum(dropped+rejected), 0) * 60"]

            extra_where = []
            if host:
                extra_where.append(("AND hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("AND uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_shield_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where)
            curs.execute(q, h)

            times = []
            accepted = []
            limited = []
            blocked = []

            for r in curs.fetchall():
                times.append(r[0])
                accepted.append(r[1])
                limited.append(r[2])
                blocked.append(r[3])
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART,
                     title=_('Daily Request'),
                     xlabel=_('Hour of Day'),
                     ylabel=_('Requests per Minute'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(times, accepted, label=_('accepted'))
        plot.add_dataset(times, limited, label=_('limited'))
        plot.add_dataset(times, blocked, label=_('blocked'))

        return (lks, plot)

class BlockedHosts(Graph):
    def __init__(self):
        Graph.__init__(self, 'blocked-hosts', _('Top Blocked Hosts'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_addr, sum(dropped + rejected) AS blocked
FROM reports.n_shield_rejection_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY client_addr
ORDER BY blocked desc"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                host = r[0]
                num = r[1]

                lks.append(KeyStatistic(host, num, _('blocks')))
                pds[host] = num
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Blocked Hosts'))

        plot.add_pie_dataset(pds, display_limit=10)


        return (lks[0:10], plot)

class LimitedHosts(Graph):
    def __init__(self):
        Graph.__init__(self, 'limited-hosts', _('Top Limited Hosts'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_addr, sum(limited) AS limited
FROM reports.n_shield_rejection_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY client_addr
ORDER BY limited desc"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                host = r[0]
                num = r[1]

                lks.append(KeyStatistic(host, num, _('limited'),
                                        link_type=reports.HNAME_LINK))
                pds[host] = num
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=_('Top Limited Hosts'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks[0:10], plot)

class ShieldDetail(DetailSection):

    def __init__(self):
        DetailSection.__init__(self, 'attack-events', _('Attack Events'))

    def get_columns(self, host=None, user=None, email=None):
        if user or email:
            return None

        rv = [ColumnDesc('trunc_time', _('Time'), 'Date')]

        rv = rv + [ColumnDesc('client_addr', _('Client')),
                   ColumnDesc('limited', _('Limited')),
                   ColumnDesc('dropped', _('Dropped')),
                   ColumnDesc('rejected', _('Rejected'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if user or email:
            return None

        sql = "SELECT trunc_time, "

        sql = sql + ("""host(client_addr), limited, dropped, rejected
FROM reports.n_shield_rejection_totals
WHERE trunc_time >= %s AND trunc_time < %s
      AND (limited + dropped + rejected) > 0""" % (DateFromMx(start_date),
                                                   DateFromMx(end_date)))

        if host:
            sql += " AND client_addr = %s" % (QuotedString(host),)

        return sql + " ORDER BY trunc_time DESC"

reports.engine.register_node(Shield())
