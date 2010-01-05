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
from reports.engine import FactTable
from reports.engine import Node
from reports.engine import TOP_LEVEL
from sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-openvpn').lgettext
def N_(message): return message

class OpenVpn(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-openvpn')

    def setup(self, start_date, end_date):
        self.__create_n_openvpn_stats(start_date, end_date)

        ft = FactTable('reports.n_openvpn_connect_totals',
                       'events.n_openvpn_connect_evt',
                       'time_stamp',
                       [Column('client_name', 'text'),
                        Column('remote_address', 'inet'),
                        Column('remote_port', 'integer')],
                       [Column('rx_bytes', 'bigint', 'sum(rx_bytes)'),
                        Column('tx_bytes', 'bigint', 'sum(tx_bytes)')])
        reports.engine.register_fact_table(ft)

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [BandwidthUsage(), TopUsers()])
        sections.append(s)

        sections.append(OpenVpnDetail())

        return Report(self, sections)

    @print_timing
    def __create_n_openvpn_stats(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.n_openvpn_stats (
    time_stamp timestamp without time zone,
    rx_bytes bigint,
    tx_bytes bigint,
    seconds double precision
)""",
                                            'time_stamp', start_date, end_date)

        sd = DateFromMx(sql_helper.get_update_info('reports.n_openvpn_stats',
                                                   start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_openvpn_stats
      (time_stamp, rx_bytes, tx_bytes, seconds)
    SELECT time_stamp, rx_bytes, tx_bytes,
           extract('epoch' from (end_time - start_time)) AS seconds
    FROM events.n_openvpn_statistic_evt evt
    WHERE evt.time_stamp >= %s AND evt.time_stamp < %s""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_openvpn_stats', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class BandwidthUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-usage', _('Bandwidth Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()
        try:
            ks_query = """\
SELECT avg((rx_bytes + tx_bytes) / seconds),
       max((rx_bytes + tx_bytes) / seconds)
FROM reports.n_openvpn_stats
WHERE time_stamp >= %s AND time_stamp < %s"""

            lks = []

            for n in (1, report_days):
                sd = DateFromMx(end_date - mx.DateTime.DateTimeDelta(n))

                curs = conn.cursor()
                curs.execute(ks_query, (sd, ed))

                r = curs.fetchone()
                if r:
                    ks = KeyStatistic(_('Avg Data Rate'), r[0], N_('bytes/s'))
                    lks.append(ks)
                    ks = KeyStatistic(_('Max Data Rate'), r[0], N_('bytes/s'))
                    lks.append(ks)


            # kB
            sums = ["coalesce(sum(rx_bytes + tx_bytes) / 1000, 0)"]

            extra_where = []
            if host:
                extra_where.append(("AND hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("AND uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_openvpn_stats",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_field = "time_stamp")
            curs.execute(q, h)

            dates = []
            throughput = []

            for r in curs.fetchall():
                dates.append(r[0])
                throughput.append(r[1])

            plot = Chart(type=TIME_SERIES_CHART,
                         title=_('Bandwidth Usage'),
                         xlabel=_('Hour of Day'),
                         ylabel=_('Throughput (Kb/sec)'),
                         major_formatter=TIMESTAMP_FORMATTER)

            plot.add_dataset(dates, throughput, _('Usage (KB/sec)'))
        finally:
            conn.commit()

        return (lks, plot)

class TopUsers(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-users', _('Top Users'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_name, sum(rx_bytes + tx_bytes)::int AS throughput
FROM reports.n_openvpn_connect_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY client_name
ORDER BY throughput desc"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                client_name = r[0]
                num = r[1]

                lks.append(KeyStatistic(client_name, num, _('bytes')))
                pds[client_name] = num
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=_('OpenVPN Top Users'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks[0:10], plot)

class OpenVpnDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'login-events', _('Login Events'))

    def get_columns(self, host=None, user=None, email=None):
        if host or user or email:
            return None

        rv = [ColumnDesc('trunc_time', _('Time'), 'Date')]

        rv = rv + [ColumnDesc('client_name', _('Client'))]
        rv = rv + [ColumnDesc('remote_address', _('Address'))]
        rv = rv + [ColumnDesc('remote_port', _('Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if host or user or email:
            return None

        sql = "SELECT trunc_time, client_name, host(remote_address), remote_port"

        sql = sql + ("""
FROM reports.n_openvpn_connect_totals
WHERE trunc_time >= %s AND trunc_time < %s""" % (DateFromMx(start_date),
                                                 DateFromMx(end_date)))

        return sql + " ORDER BY trunc_time DESC"

reports.engine.register_node(OpenVpn())
