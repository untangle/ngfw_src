import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

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
from reports import TIME_SERIES_CHART
from reports import TIMESTAMP_FORMATTER
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN

_ = uvm.i18n_helper.get_translation('untangle').lgettext
def N_(message): return message

PRIORITIES = { None : _("Default"),
               1 : _("Very High"),
               2 : _("High"),
               3 : _("Medium"),
               4 : _("Low"),
               5 : _("Limited"),
               6 : _("Limited More"),
               7 : _("Limited Severely") }

class BandwidthControl(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-bandwidth-control','Bandwidth Control')

    @sql_helper.print_timing
    def setup(self):
        ft = reports.engine.get_fact_table('reports.session_totals')
        ft.dimensions.append(Column('bandwidth_control_priority', 'bigint'))
        ft.dimensions.append(Column('bandwidth_control_rule', 'bigint'))

    def reports_cleanup(self, cutoff):
        pass

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [BandwidthHighlight(self.name),
                            BandwidthUsage(),
                            BandwidthByIP(),
                            BandwidthByUser(),
                            BandwidthByPort(),
                            BandwidthByPriority(),
                            SessionsByPriority()])
        sections.append(s)

        sections.append(QuotaDetail())

        return Report(self, sections)

class BandwidthHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("analyzed") + " " + "%(bw)s" + " MB ")

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days, host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000000)::numeric, 2) as bw
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h

class SessionsByPriority(Graph):
    def __init__(self):
        Graph.__init__(self, 'sessions-by-priority', _('Sessions by Priority'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        lks = []

        query = """
SELECT bandwidth_control_priority, COUNT(*) as sessions
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY bandwidth_control_priority ORDER BY sessions DESC"

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                priority = PRIORITIES.get(r[0])
                sessions = r[1]
                ks = KeyStatistic(str(priority), sessions, _('Sessions'))
                lks.append(ks)
                pds[priority] = sessions
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Priority'),
                     ylabel=_('Sessions'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class BandwidthByPriority(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-by-priority', _('Bandwidth by Priority'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        lks = []

        query = """
SELECT bandwidth_control_priority, ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000000)::numeric, 2) as bandwidth
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY bandwidth_control_priority ORDER BY bandwidth DESC"

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                priority = PRIORITIES.get(r[0])
                bandwidth = r[1]
                ks = KeyStatistic(str(priority), bandwidth, _('MB'))
                lks.append(ks)
                pds[priority] = bandwidth
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Priority'),
                     ylabel=_('MB'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class BandwidthUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-usage', _('Bandwidth Usage'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        lks = []

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            # kB
            sums = ["coalesce(sum(s2c_bytes + c2s_bytes), 0) / 1000",]

            extra_where = []
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            time_interval = 3600
            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            throughput = []

            for r in curs.fetchall():
                dates.append(r[0])
                throughput.append(float(r[1]) / time_interval)

            if not throughput:
                throughput = [0,]

            ks = KeyStatistic(_('Avg Data Rate'),
                              sum(throughput)/len(throughput),
                              N_('kB/s'))
            lks.append(ks)
            #ks = KeyStatistic(_('Max Data Rate'), max(throughput), N_('kB/s'))
            #lks.append(ks)
            ks = KeyStatistic(_('Data Transferred'), time_interval * sum(throughput), N_('kB'))
            lks.append(ks)

                
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Date'), ylabel=_('Throughput (kB/s)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, throughput, _('Usage'))

        return (lks, plot)

class BandwidthByIP(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-by-ip', _('Top Bandwidth Hosts'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT hostname, ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000000)::numeric, 2) as bw
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        if host:
            query += " AND hostname = %s"

        query += " GROUP BY hostname ORDER BY bw DESC"

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            else:
                curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                hostname = r[0]
                bw = r[1]
                ks = KeyStatistic(str(hostname), bw, _('MB'), link_type=reports.HNAME_LINK)
                lks.append(ks)
                pds[hostname] = bw
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Host'),
                     ylabel=_('MB'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class BandwidthByPort(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-by-port', _('Top Bandwidth Ports'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT c_server_port, ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000000)::numeric, 2) as bw
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY c_server_port ORDER BY bw DESC"


        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                port = r[0]
                bw = r[1]
                ks = KeyStatistic(str(port), bw, _('MB'))
                lks.append(ks)
                pds[port] = bw
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Port'),
                     ylabel=_('MB'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class BandwidthByUser(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-by-user', _('Top Bandwidth Users'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email or host:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT username, ROUND((COALESCE(SUM(s2c_bytes + c2s_bytes), 0) / 1000000)::numeric, 2) as bw
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND username IS NOT NULL"""

        if user:
            query += " AND username = %s"

        query += " GROUP BY username ORDER BY bw DESC"

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                user = r[0]
                bw = r[1]
                ks = KeyStatistic(str(user), bw, _('MB'), link_type=reports.USER_LINK)
                lks.append(ks)
                pds[user] = bw
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('User'),
                     ylabel=_('MB'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class QuotaDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'quota', _('Quota Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email or user:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if not host:
            rv.append(ColumnDesc('address', _('Client'), 'HostLink'))

        rv.append(ColumnDesc('action_str', _('Action')))
        rv.append(ColumnDesc('reason', _('Reason')))

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email or user:
            return None

        sql = "SELECT time_stamp, "

        if not host:
            sql += "host(address) AS address, "

        sql += """CASE action WHEN 1 THEN 'Quota given' ELSE 'Quota exceeded' END as action_str, reason
FROM reports.quotas
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
""" % (DateFromMx(start_date), DateFromMx(end_date))

        if host:
            sql += " AND address = %s" % QuotedString(host)

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(BandwidthControl())

