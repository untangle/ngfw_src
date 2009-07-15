import gettext
import logging
import mx
import reports.engine
import sql_helper

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
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.engine import TOP_LEVEL
from sql_helper import print_timing

_ = gettext.gettext
def N_(message): return message

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

        s = SummarySection('summary', N_('Summary Report'),
                           [DailyRequest(), BlockedHosts(), LimitedHosts()])
        sections.append(s)

        sections.append(ShieldDetail())

        return Report(self.name, sections)

class DailyRequest(Graph):
    def __init__(self):
        Graph.__init__(self, 'daily-request', _('Daily Request'))

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
            ks = KeyStatistic(N_('avg requests/minute (7-days)'), r[0],
                              N_('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('max requests/minute (7-days)'), r[1],
                              N_('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('avg limited/minute (7-days)'), r[2],
                              N_('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('max limited/minute (7-days)'), r[3],
                              N_('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('avg blocked/minute (7-days)'), r[4],
                              N_('sessions/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('max blocked/minute (7-days)'), r[5],
                              N_('sessions/minute'))
            lks.append(ks)

            query = """\
SELECT (date_part('hour', trunc_time) || ':'
        || (date_part('minute', trunc_time)::int / 10 * 10))::time AS time,
       sum(accepted) as accepted, sum(limited) as limited,
       sum(dropped + rejected) as blocked
FROM reports.n_shield_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY time
ORDER BY time asc"""

            curs.execute(query, (one_week, ed))

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
                     major_formatter=TIME_OF_DAY_FORMATTER)

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
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_addr, sum(dropped + rejected) AS blocked
FROM reports.n_shield_rejection_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY client_addr
ORDER BY blocked desc
LIMIT 10"""

        conn = sql_helper.get_connection()

        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                host = r[0]
                num = r[1]

                lks.append(KeyStatistic(host, num, N_('blocks')))
                pds[host] = num
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Blocked Hosts'))

        plot.add_pie_dataset(pds)


        return (lks, plot)

class LimitedHosts(Graph):
    def __init__(self):
        Graph.__init__(self, 'limited-hosts', _('Top Limited Hosts'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT client_addr, sum(limited) AS limited
FROM reports.n_shield_rejection_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY client_addr
ORDER BY limited desc
LIMIT 10"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                host = r[0]
                num = r[1]

                lks.append(KeyStatistic(host, num, N_('limited'),
                                        link_type=reports.HNAME_LINK))
                pds[host] = num
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=_('Top Limited Hosts'))

        plot.add_pie_dataset(pds)

        return (lks, plot)

class ShieldDetail(DetailSection):

    def __init__(self):
        DetailSection.__init__(self, 'incidents', N_('Incident Report'))

    def get_columns(self, host=None, user=None, email=None):
        if host or user or email:
            return None

        rv = [ColumnDesc('trunc_time', N_('Time'), 'Date')]

        rv = rv + [ColumnDesc('client_addr', N_('Client'), 'Client')]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if host or user or email:
            return None

        sql = "SELECT trunc_time, "

        sql = sql + ("""client_addr
FROM reports.n_shield_rejection_totals
WHERE trunc_time >= %s AND trunc_time < %s
      AND (limited + dropped + rejected) > 0""" % (DateFromMx(start_date),
                                                   DateFromMx(end_date)))

        return sql


reports.engine.register_node(Shield())
