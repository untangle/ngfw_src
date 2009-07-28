import gettext
import logging
import mx
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
from reports import STACKED_BAR_CHART
from reports import SummarySection
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.engine import TOP_LEVEL
from sql_helper import print_timing

_ = gettext.gettext
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

        s = SummarySection('summary', N_('Summary Report'),
                           [BandwidthUsage(), TopUsers()])
        sections.append(s)

        sections.append(OpenVpnDetail())

        return Report(self.name, sections)

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
SELECT avg((rx_bytes + tx_bytes) / 1000 / seconds),
       max((rx_bytes + tx_bytes) / 1000 / seconds)
FROM reports.n_openvpn_stats
WHERE time_stamp >= %s AND time_stamp < %s"""

            lks = []

            for n in (1, report_days):
                sd = DateFromMx(end_date - mx.DateTime.DateTimeDelta(n))

                curs = conn.cursor()
                curs.execute(ks_query, (sd, ed))

                r = curs.fetchone()
                if r:
                    ks = KeyStatistic(N_('Avg data rate (%s-day)' % n),
                                      r[0], N_('Kb/s'))
                    lks.append(ks)
                    ks = KeyStatistic(N_('Peak data rate (%s-day)' % n), r[0],
                                      N_('Kb/s'))
                    lks.append(ks)

            plot = Chart(type=TIME_SERIES_CHART,
                         title=_('Bandwidth Usage'),
                         xlabel=_('Hour of Day'),
                         ylabel=_('Throughput (Kb/sec)'),
                         major_formatter=TIME_OF_DAY_FORMATTER)

            plot_query = """\
SELECT (date_part('hour', time_stamp) || ':'
        || (date_part('minute', time_stamp)::int / 10 * 10))::time,
       sum(rx_bytes + tx_bytes) / sum(seconds) / 1000
FROM reports.n_openvpn_stats
WHERE time_stamp >= %s AND time_stamp < %s
GROUP BY time
ORDER BY time"""

            dates = []
            throughput = []

            curs = conn.cursor()
            curs.execute(plot_query, (one_week, ed))

            for r in curs.fetchall():
                dates.append(r[0])
                throughput.append(r[1])

            plot.add_dataset(dates, throughput, _('Usage'))
        finally:
            conn.commit()

        return (lks, plot)

class TopUsers(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-users', _('OpenVPN Top Users'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_name, sum(rx_bytes + tx_bytes) AS throughput
FROM reports.n_openvpn_connect_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY client_name
ORDER BY throughput desc
LIMIT 10"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                client_name = r[0]
                num = r[1]

                lks.append(KeyStatistic(client_name, num, N_('blocks')))
                pds[client_name] = num
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=_('OpenVPN Top Users'))

        plot.add_pie_dataset(pds)

        return (lks, plot)

class OpenVpnDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'incidents', N_('Incident Report'))

    def get_columns(self, host=None, user=None, email=None):
        if host or user or email:
            return None

        rv = [ColumnDesc('trunc_time', N_('Time'), 'Time')]

        rv = rv + [ColumnDesc('client_name', N_('Client'), 'Client')]
        rv = rv + [ColumnDesc('remote_address', N_('Address'), 'Address')]
        rv = rv + [ColumnDesc('remote_port', N_('Port'), 'Port')]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if host or user or email:
            return None

        sql = "SELECT time_stamp, "

        sql = sql + ("""client_name, remote_address, remote_port
FROM reports.sessions
WHERE time_stamp >= %s AND time_stamp < %s""" % (DateFromMx(start_date),
                                                 DateFromMx(end_date)))

reports.engine.register_node(OpenVpn())
