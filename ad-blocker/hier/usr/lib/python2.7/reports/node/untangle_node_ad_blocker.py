import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import TimestampFromMx
from psycopg2.extensions import QuotedString
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
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle').lgettext

class AdBlocker(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-ad-blocker','Ad Blocker')

    def parents(self):
        return ['untangle-casing-http']

    def setup(self):
        ft = reports.engine.get_fact_table('reports.http_totals')
        ft.measures.append(Column('ab_blocks', 'integer', """count(CASE WHEN NOT ad_blocker_action IS NULL AND ad_blocker_action = 'B' THEN 1 ELSE null END)"""))
        ft.dimensions.append(Column('ad_blocker_action', 'text'))
        ft.measures.append(Column('ab_cookies', 'integer', 'count(ad_blocker_cookie_ident)'))
        ft.dimensions.append(Column('ad_blocker_cookie_ident', 'text'))

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [AbHighlight(self.name),
                            DailyUsage(),
                            CookiesBlocked(),
                            TopTenBlockedAdSites(),
                            TopTenBlockedCookies()
                            ])
        sections.append(s)

        sections.append(CookieDetail())
        
        return Report(self, sections)

    def reports_cleanup(self, cutoff):
        pass

class AbHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("scanned") + " " + "%(hits)s" + " " +
                           _("web hits and blocked") + " " +
                           "%(blocks)s" + " " + _("ads and")  + " " +
                           "%(cookies)s" + " " + _("cookies") )

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT COALESCE(sum(hits), 0)::int AS hits,
       COALESCE(sum(ab_blocks), 0) AS blocks,
       COALESCE(sum(ab_cookies), 0) AS cookies
FROM reports.http_totals
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


class DailyUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'blocked-ads', _('Blocked Ads'))

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

            sums = ["COUNT(CASE WHEN ad_blocker_action = 'B' THEN 1 ELSE NULL END)",
                    "COUNT(CASE WHEN ad_blocker_action = 'L' THEN 1 ELSE NULL END)"]

            extra_where = []
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.http_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)
            
            dates = []
            hits = []
            logged = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                hits.append(r[1])
                logged.append(r[2])

            if not hits:
                hits = [0,]
            if not logged:
                logged = [0,]
            
            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Avg Ads Blocked'),
                              round(sum(hits) / len(rp), 2),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Ads Blocked'), max(hits),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Ads Logged'),
                              round(sum(logged) / len(rp), 2),
                              _('Logs')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Ads Logged'), max(logged),
                              _('Logs')+'/'+_(unit))
            lks.append(ks)
        finally:
            conn.commit()
        
        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_(unit),
                     ylabel=_('Ads'),
                     major_formatter=formatter,
                     required_points=rp)

        plot.add_dataset(dates, hits, label=_('Ads Blocked'),
                         color=colors.badness)
        plot.add_dataset(dates, logged, label=_('Ads Logged'),
                         color=colors.detected)

        return (lks, plot)

class CookiesBlocked(Graph):
    def __init__(self):
        Graph.__init__(self, 'blocked-cookies', _('Blocked Cookies'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
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

            sums = ["coalesce(sum(ab_cookies), 0)",]

            extra_where = []
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.http_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            ab_cookies = []
            
            for r in curs.fetchall():
                dates.append(r[0])
                ab_cookies.append(r[1])

            if not ab_cookies:
                ab_cookies = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Avg Cookies Blocked'),
                              sum(ab_cookies) / len(rp),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Cookies Blocked'), max(ab_cookies),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Blocks'),
                         major_formatter=formatter,
                         required_points=rp)

            plot.add_dataset(dates, ab_cookies, label=_('Cookies'))

        finally:
            conn.commit()

        return (lks, plot)   
   

class TopTenBlockedAdSites(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-blocked-ad-sites',
                       _('Top Blocked Ad Sites'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT host, sum(ab_blocks)::int AS blocks_sum
FROM reports.http_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""
        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"
        query += " GROUP BY host ORDER BY blocks_sum DESC"

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
                host = r[0]
                blocks = r[1]
                if blocks > 0:
                    ks = KeyStatistic(host, blocks, _('Blocks'),
                                      link_type=reports.URL_LINK)
                    lks.append(ks)
                    dataset[host] = blocks
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)
    
class TopTenBlockedCookies(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-blocked-cookies', _('Top Ten Blocked Cookies'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT ad_blocker_cookie_ident, count(*) as hits_sum
FROM reports.http_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND ad_blocker_cookie_ident != ''
AND ab_cookies > 0"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query = query + " GROUP BY ad_blocker_cookie_ident ORDER BY hits_sum DESC"

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
                     title=self.title,
                     xlabel=_('Cookie'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class CookieDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'cookie-events', _('Cookie Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hostname', _('Client')))
        else:
            rv.append(ColumnDesc('hostname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('username', _('User')))
        else:
            rv.append(ColumnDesc('username', _('User'), 'UserLink'))

        rv += [ColumnDesc('ad_blocker_cookie_ident', _('Cookie')),
               ColumnDesc('s_server_addr', _('Server Ip')),
               ColumnDesc('s_server_port', _('Server Port'))]

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_http_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT *
FROM reports.http_events
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
      AND NOT ad_blocker_cookie_ident IS NULL AND ad_blocker_cookie_ident != ''
""" % (DateFromMx(start_date), DateFromMx(end_date))

        if host:
            sql += " AND hostname = %s" % QuotedString(host)
        if user:
            sql += " AND username = %s" % QuotedString(user)

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(AdBlocker())
