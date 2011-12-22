import gettext
import logging
import mx
import reports.i18n_helper
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import sys

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
from psycopg2.extensions import TimestampFromMx
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import Highlight
from reports import HOUR_FORMATTER
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import STACKED_BAR_CHART
from reports import SummarySection
from reports import TIME_OF_DAY_FORMATTER
from reports import TIMESTAMP_FORMATTER
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-protofilter').lgettext

class Protofilter(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-protofilter')

    def setup(self, start_date, end_date):
        self.__update_sessions(start_date, end_date)

        ft = reports.engine.get_fact_table('reports.session_totals')

        ft.measures.append(Column('pf_blocks', 'integer',
                                  "count(CASE WHEN pf_blocked THEN 1 ELSE null END)"))

        ft.dimensions.append(Column('pf_protocol', 'text'))

    def parents(self):
        return ['untangle-vm',]

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []
        s = reports.SummarySection('summary', _('Summary Report'),
                                   [ProtocolsHighlight(self.name),
                                    DailyUsage(),
                                    TopTenBlockedProtocolsByHits(),
                                    TopTenDetectedProtocolsByHits(),
                                    TopTenBlockedHostsByHits(),
                                    TopTenLoggedHostsByHits(),
                                    TopTenBlockedUsersByHits(),
                                    TopTenLoggedUsersByHits(),
                                    ])
        sections.append(s)

        sections.append(ProtofilterDetail())

        return reports.Report(self, sections)

    def events_cleanup(self, cutoff, safety_margin):
        sql_helper.run_sql("""\
DELETE FROM events.n_protofilter_evt 
WHERE pl_endp_id IN (SELECT pl_endp_id FROM reports.sessions)
 OR (time_stamp < %s - interval %s)""",
                           (cutoff,safety_margin))

    def reports_cleanup(self, cutoff):
        pass

    @print_timing
    def __update_sessions(self, start_date, end_date):
        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.sessions
SET pf_protocol = protocol,
  pf_blocked = blocked
FROM events.n_protofilter_evt
WHERE reports.sessions.pl_endp_id = events.n_protofilter_evt.pl_endp_id""",
                               (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class ProtocolsHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("scanned") + " " + "%(sessions)s" + " " +
                           _("sessions and detected") + " " +
                           "%(protocols)s" + " " + _("protocols of which") +
                           " " + "%(blocks)s" + " " + _("were blocked"))

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(SUM(new_sessions),0)::int AS sessions,
       COALESCE(sum(CASE WHEN NULLIF(pf_protocol,'') IS NULL THEN 0 ELSE 1 END), 0)::int AS protocols,
       COALESCE(sum(pf_blocks), 0)::int AS blocks
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


class DailyUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'usage', _('Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []
        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER

            sums = ["COUNT(*)"]

            extra_where = [("pf_protocol != ''", {})]
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            detections = []
            
            for r in curs.fetchall():
                dates.append(r[0])
                detections.append(r[1])

            if not detections:
                detections = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Avg Detections'),
                              "%.2f" % (sum(detections) / len(rp)),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Detections'), max(detections),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title, xlabel=_(unit),
                         ylabel=_('Detections'),
                         major_formatter=TIMESTAMP_FORMATTER,
                         required_points=rp)

            plot.add_dataset(dates, detections, label=_('Detections'))
        finally:
            conn.commit()

        return (lks, plot)

class TopTenBlockedProtocolsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-blocked-protocols-by-hits',
                       _('Top Blocked Protocols By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT pf_protocol, COALESCE(sum(pf_blocks), 0)::int as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query += " GROUP BY pf_protocol ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'))
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Blocked Protocols (by Hits)'),
                     xlabel=_('Protocol'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenDetectedProtocolsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-detected-protocols-by-hits', _('Top Detected Protocols By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT pf_protocol, count(*) as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
      AND pf_protocol != ''
"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query = query + " GROUP BY pf_protocol ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'))
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Detected Protocols (by Hits)'),
                     xlabel=_('Protocol'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-blocked-hosts-by-hits', _('Top Blocked Hosts By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hname, COALESCE(sum(pf_blocks), 0)::int as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND NOT pf_protocol IS NULL
AND pf_protocol != ''
"""

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'),
                                      link_type=reports.HNAME_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Blocked Hosts (by Hits)'),
                     xlabel=_('Host'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenLoggedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-logged-hosts-by-hits',
                       _('Top Logged Hosts By Hits'))

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
AND pf_protocol != ''
"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query +=" GROUP BY hname ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.HNAME_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Logged Hosts (by Hits)'),
                     xlabel=_('Host'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockedUsersByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-blocked-users-by-hits', _('Top Blocked Users By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT uid, sum(pf_blocks) as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND uid != ''
AND NOT pf_protocol IS NULL
AND pf_protocol != ''
"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query = query + " GROUP BY uid ORDER BY hits_sum DESC"

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.USER_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Blocked Users (by Hits)'),
                     xlabel=_('User'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenLoggedUsersByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-logged-users-by-hits', _('Top Logged Users By Hits'))

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
AND pf_protocol != ''
"""

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
                if r[1] > 0:
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.USER_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Logged Users (by Hits)'),
                     xlabel=_('User'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class ProtofilterDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'detection-events', _('Detection Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if not host:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))
        if not user:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))

        rv = rv + [ColumnDesc('pf_protocol', _('Protocol')),
                   ColumnDesc('pf_blocked', _('Blocked')),
                   ColumnDesc('c_server_addr', _('Server')),
                   ColumnDesc('c_server_port', _('Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = "SELECT time_stamp,"

        if not host:
            sql = sql + "hname, "
        if not user:
            sql = sql + "uid, "

        sql = sql + ("""pf_protocol, pf_blocked::text, host(c_server_addr), c_server_port
FROM reports.sessions
WHERE time_stamp >= %s AND time_stamp < %s
AND NOT pf_protocol ISNULL
AND pf_protocol != ''""" % (DateFromMx(start_date),
                            DateFromMx(end_date)))

        if host:
            sql = sql + (" AND hname = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND uid = %s" % QuotedString(user))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(Protofilter())
