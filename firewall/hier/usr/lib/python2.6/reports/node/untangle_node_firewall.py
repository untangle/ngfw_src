import gettext
import logging
import mx
import reports.colors as colors
import reports.i18n_helper
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
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-firewall').lgettext

class Firewall(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-firewall')

    @sql_helper.print_timing
    def setup(self):
        ft = reports.engine.get_fact_table('reports.session_totals')
        ft.measures.append(Column('firewall_blocks', 'integer', "count(CASE WHEN firewall_blocked THEN 1 ELSE null END)"))
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

    def reports_cleanup(self, cutoff):
        pass

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
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        if host:
            query = query + " AND hostname = %s"
        elif user:
            query = query + " AND username = %s"

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
        reports.Graph.__init__(self, 'sessions', _('Sessions'))

    @sql_helper.print_timing
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

            sums = ["COUNT(CASE WHEN firewall_rule_index IS NOT NULL AND NOT firewall_flagged AND NOT firewall_blocked THEN 1 ELSE null END)",
                    "COUNT(CASE WHEN firewall_flagged AND NOT firewall_blocked THEN 1 ELSE null END)",
                    "COUNT(CASE WHEN firewall_blocked THEN 1 ELSE null END)"]

            extra_where = []
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.sessions",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval,
                                                 time_field = 'time_stamp')
            curs.execute(q, h)

            dates = []
            scans = []
            flags = []
            blocks = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                scans.append(r[1])
                flags.append(r[2])
                blocks.append(r[3])

            if not scans:
                scans = [0,]
            if not flags:
                flags = [0,]
            if not blocks:
                blocks = [0,]
                
            rp = sql_helper.get_required_points(start_date, end_date,
                                                mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = reports.KeyStatistic(_('Avg Scanned'), sum(scans + flags + blocks)/len(rp),_('Scans')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Scanned'), max(scans + flags + blocks),_('Scans')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Avg Flagged'), sum(flags + blocks)/len(rp),_('Flags')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Flagged'), max(flags + blocks),_('Flags')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Avg Blocked'), sum(blocks)/len(rp),_('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = reports.KeyStatistic(_('Max Blocked'), max(blocks),_('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = reports.Chart(type=reports.STACKED_BAR_CHART,
                                 title=_('Sessions'),
                                 xlabel=_(unit),
                                 ylabel=_('Sessions'),
                                 major_formatter=formatter,
                                 required_points=rp)

            plot.add_dataset(dates, blocks, label=_('Blocked'), color=colors.badness)
            plot.add_dataset(dates, flags, label=_('Flagged'), color=colors.detected)
            plot.add_dataset(dates, scans, label=_('Scanned'), color=colors.goodness)

        finally:
            conn.commit()

        return (lks, plot)

class TopTenBlockedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-firewall-blocked-hosts-by-hits', _('Top Firewall Blocked Hosts By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hostname, count(*) as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND firewall_blocks > 0
AND firewall_rule_index IS NOT NULL"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query = query + " GROUP BY hostname ORDER BY hits_sum DESC"

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
                    ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.HNAME_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Firewall Blocked Hosts (by Hits)'),
                     xlabel=_('Host'),
                     ylabel=_('Blocks Per Day'))
        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockingRulesByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-firewall-blocking-rules-by-hits', _('Top Firewall Blocking Rules By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT firewall_rule_index,
       count(*) as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND firewall_blocks > 0
AND firewall_rule_index IS NOT NULL"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

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
                ks = KeyStatistic(r[0], r[1], _('Hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Firewall Blocking Rules (by Hits)'),
                     xlabel=_('Rule #'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockedUsersByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-firewall-blocked-users-by-hits', _('Top Firewall Blocked Users By Hits'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT username, count(*) as hits_sum
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND username != ''
AND firewall_blocks > 0
AND firewall_rule_index IS NOT NULL"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += " GROUP BY username ORDER BY hits_sum DESC"

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
                ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.USER_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=_('Top Ten Firewall Blocked Users (by Hits)'),
                     xlabel=_('User'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class FirewallDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'firewall-events', _('Firewall Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if not host:
            rv.append(ColumnDesc('hostname', _('Client'), 'HostLink'))
        if not user:
            rv.append(ColumnDesc('username', _('User'), 'UserLink'))

        rv = rv + [ColumnDesc('firewall_rule_index', _('Rule Applied')),
                   ColumnDesc('firewall_blocked', _('Blocked')),
                   ColumnDesc('c_server_addr', _('Destination Ip')),
                   ColumnDesc('c_server_port', _('Destination Port')),
                   ColumnDesc('c_client_addr', _('Source Ip')),
                   ColumnDesc('c_client_port', _('Source Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = "SELECT time_stamp,"

        if not host:
            sql = sql + "hostname, "
        if not user:
            sql = sql + "username, "

        sql = sql + ("""firewall_rule_index, firewall_blocked::text, host(c_server_addr), c_server_port, host(c_client_addr), c_client_port
FROM reports.sessions
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND NOT firewall_rule_index IS NULL""" % (DateFromMx(start_date),
                                         DateFromMx(end_date)))

        if host:
            sql = sql + (" AND hostname = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND username = %s" % QuotedString(user))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(Firewall())
