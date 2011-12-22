import gettext
import logging
import mx
import reports.colors as colors
import reports.i18n_helper
import reports.sql_helper as sql_helper

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
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-base-webfilter').lgettext

def N_(message): return message

class WebFilterBaseNode(Node):
    def __init__(self, node_name, title, vendor_name):
        Node.__init__(self, node_name)

        self.__title = title
        self.__vendor_name = vendor_name

    def parents(self):
        return ['untangle-casing-http']

    @print_timing
    def setup(self, start_date, end_date):
        self.__update_n_http_events(start_date, end_date)

        ft = reports.engine.get_fact_table('reports.n_http_totals')

        ft.measures.append(Column('wf_%s_blocks' % self.__vendor_name,
                                  'integer',
                                  "count(CASE WHEN wf_%s_blocked THEN 1 ELSE null END)"
                                  % self.__vendor_name))

        ft.measures.append(Column('wf_%s_violations' % self.__vendor_name,
                                  'integer',
                                  "count(CASE WHEN wf_%s_flagged THEN 1 ELSE null END)"
                                  % self.__vendor_name))

        ft.dimensions.append(Column('wf_%s_category' % self.__vendor_name,
                                    'text'))

        ft.dimensions.append(Column('wf_%s_reason' % self.__vendor_name,
                                    'text'))

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [WebHighlight(self.name, self.__vendor_name),
                            DailyWebUsage(self.__vendor_name),
                            TotalWebUsage(self.__vendor_name),
                            TopTenWebBrowsingHostsByHits(self.__vendor_name),
                            TopTenWebBrowsingHostsBySize(self.__vendor_name),
                            TopTenWebBrowsingUsersByHits(self.__vendor_name),
                            TopTenWebBrowsingUsersBySize(self.__vendor_name),
                            TopTenWebPolicyViolationsByHits(self.__vendor_name),
                            TopTenWebBlockedPolicyViolationsByHits(self.__vendor_name),
                            TopTenWebsitesByHits(self.__vendor_name),
                            TopTenWebsitesBySize(self.__vendor_name),
                            TopTenWebPolicyViolatorsByHits(self.__vendor_name),
                            TopTenWebPolicyViolatorsADByHits(self.__vendor_name),
                            TopTenPolicyViolations(self.__vendor_name),
                            TopTenBlockedPolicyViolations(self.__vendor_name)])
        sections.append(s)

        sections.append(WebFilterDetail(self.__vendor_name))
        sections.append(WebFilterDetailAll(self.__vendor_name))
        sections.append(WebFilterDetailDomains(self.__vendor_name))

        if self.__vendor_name == 'esoft':
            sections.append(WebFilterDetailUnblock(self.__vendor_name))

        return Report(self, sections)

    def events_cleanup(self, cutoff, safety_margin):
        sql_helper.run_sql("""\
DELETE FROM events.n_webfilter_evt 
WHERE request_id IN (SELECT request_id FROM reports.n_http_events)
OR (time_stamp < %s- interval %s)""", (cutoff, safety_margin))

    def reports_cleanup(self, cutoff):
        pass

    @print_timing
    def __update_n_http_events(self, start_date, end_date):
        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.n_http_events
SET wf_%s_blocked = blocked,
    wf_%s_flagged = flagged,
    wf_%s_reason = reason,
    wf_%s_category = category
FROM events.n_webfilter_evt
WHERE events.n_webfilter_evt.vendor_name = %%s
AND reports.n_http_events.request_id = events.n_webfilter_evt.request_id"""
                               % (4 * (self.__vendor_name,)),
                               (self.__vendor_name,), connection=conn,
                               auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class WebHighlight(Highlight):
    def __init__(self, name, vendor_name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("scanned") + " " + "%(hits)s" + " " +
                           _("web hits and detected") + " " +
                           "%(violations)s" + " " + _("violations of which") +
                           " " + "%(blocks)s" + " " + _("were blocked"))
        self.__vendor_name = vendor_name

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(sum(hits), 0)::int AS hits,
       COALESCE(sum(wf_%s_violations), 0)::int AS violations,
       COALESCE(sum(wf_%s_blocks), 0)::int AS blocks
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
""" % (self.__vendor_name, self.__vendor_name)

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

class DailyWebUsage(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'web-usage', _('Web Usage'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []
        
        conn = sql_helper.get_connection()
        curs = conn.cursor()

        try:
            sums = ["COALESCE(SUM(hits), 0)::float",
                    "COALESCE(SUM(wf_%s_blocks), 0)::float" % (self.__vendor_name,),
                    "COALESCE(SUM(wf_%s_violations), 0)::float" % (self.__vendor_name,)]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)" , { 'user' : user }))

            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER
                
            q, h = sql_helper.get_averaged_query(sums, "reports.n_http_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            hits = []
            blocks = []
            violations = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                hits.append(r[1]-r[2])
                blocks.append(r[2])
                violations.append(r[3]-r[2])

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            if not hits:
                hits = [0,]
            if not blocks:
                blocks = [0,]
            if not violations:
                violations = [0,]

            ks = KeyStatistic(_('Avg Hits'), sum(hits) / len(rp), _('Hits')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Hits'), max(hits), _('Hits')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Violations'), sum(violations) / len(rp), _('Violations')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Violations'), max(violations), _('Violations')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Blocks'), sum(blocks) / len(rp), _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Blocks'), max(blocks), _('Blocks')+'/'+_(unit))
            lks.append(ks)

        finally:
            conn.commit()

        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_(unit),
                     ylabel=_('Hits'),
                     major_formatter=formatter,
                     required_points=rp)

        plot.add_dataset(dates, hits, label=_('Clean Hits'), color=colors.goodness)
        plot.add_dataset(dates, violations, label=_('Violations'),
                         color=colors.detected)
        plot.add_dataset(dates, blocks, label=_('Blocks'),
                         color=colors.badness)

        return (lks, plot)

class TotalWebUsage(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'total-web-usage', _('Total Web Usage'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(sum(hits)::int, 0),
       COALESCE(sum(wf_%s_violations), 0)::int AS violations,
       COALESCE(sum(wf_%s_blocks), 0)::int AS blocks
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (self.__vendor_name,
                                                   self.__vendor_name)
        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"

        conn = sql_helper.get_connection()
        try:
            lks = []

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))
            r = curs.fetchone()

            hits = r[0]
            blocks = r[2]
            violations = r[1] - blocks
            
            ks = KeyStatistic(_('Total Clean Hits'), hits-violations-blocks, 'Hits')
            lks.append(ks)
            ks = KeyStatistic(_('Total Violations'), violations, 'Violations')
            lks.append(ks)
            ks = KeyStatistic(_('Total Blocked Violations'), blocks, 'Blocks')
            lks.append(ks)
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Date'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset({_('Total Clean Hits'): hits-violations-blocks,
                              _('Total Violations'): violations,
                              _('Total Blocked Violations'): blocks},
                             colors={_('Total Clean Hits'): colors.goodness,
                                     _('Total Violations'): colors.detected,
                                     _('Total Blocked Violations'): colors.badness})

        return (lks, plot)

class TopTenWebPolicyViolationsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-categories-of-violations-by-hits',
                       _('Top Categories Of Violations (by Hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT wf_%s_category, count(*)::int AS blocks_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_violations > 0
""" % (2 * (self.__vendor_name,))
        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"
        query += """
GROUP BY wf_%s_category ORDER BY blocks_sum DESC
""" % (self.__vendor_name,)

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
                cat = r[0]
                if not cat or cat == '':
                    cat = _('Uncategorized')
                ks = KeyStatistic(cat, r[1], _('Hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Policy'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebBlockedPolicyViolationsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-web-categories-of-blocked-violations-by-hits',
                       _('Top Categories Of Blocked Violations (by Hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT wf_%s_category, sum(wf_%s_blocks)::int AS blocks_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_blocks > 0
""" % (3 * (self.__vendor_name,))
        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"
        query += """
GROUP BY wf_%s_category ORDER BY blocks_sum DESC""" \
            % self.__vendor_name

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
                     xlabel=_('Policy'),
                     ylabel=_('Blocks Per Day'))
        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebBrowsingHostsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-web-browsing-hosts-by-hits',
                       _('Top Web Browsing Hosts (by Hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hname, sum(hits)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY hname ORDER BY hits_sum DESC"""

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('Hits'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebBrowsingUsersByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-web-browsing-users-by-hits',
                       _('Top Web Browsing Users (by Hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT uid, sum(hits)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s AND NOT uid IS NULL AND uid != ''
GROUP BY uid ORDER BY hits_sum DESC"""

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('Hits'),
                                  link_type=reports.USER_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('User'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebBrowsingUsersBySize(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-web-browsing-users-by-size',
                       _('Top Web Browsing Users (by Size)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT uid, COALESCE(sum(s2c_content_length)/1000000, 0)::bigint as size_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s AND NOT uid IS NULL AND uid != ''
GROUP BY uid ORDER BY size_sum DESC"""

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('MB'),
                                  link_type=reports.USER_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('User'),
                     ylabel=_('MB/day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebPolicyViolatorsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-host-violators-by-hits',
                       _('Top Host Violators (by Hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hname, COALESCE(sum(wf_%s_blocks), 0)::int as blocks_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_blocks > 0
GROUP BY hname
ORDER BY blocks_sum DESC""" % ((self.__vendor_name,)*2)

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('Hits'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebPolicyViolatorsADByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-violators-by-hits',
                       _('Top User Violators (by Hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                           email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT uid, sum(wf_%s_blocks)::int as blocks_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_blocks > 0
AND uid != ''
GROUP BY uid ORDER BY blocks_sum DESC""" \
            % (2 * (self.__vendor_name,))

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('Hits'),
                                  link_type=reports.USER_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Uid'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebBrowsingHostsBySize(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-web-browsing-hosts-by-size',
                       _('Top Web Browsing Hosts (by Size)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hname, COALESCE(sum(s2c_content_length)/1000000, 0)::bigint as size_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        query += " GROUP BY hname ORDER BY size_sum DESC"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], N_('MB'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('MB/day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebsitesByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-websites-by-hits',
                       _('Top Websites (by Hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT host, sum(hits)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += " GROUP BY host ORDER BY hits_sum DESC"

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
                ks = KeyStatistic(r[0], r[1], _('Hits'), link_type=reports.URL_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenWebsitesBySize(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-websites-by-size',
                       _('Top Websites (by Size)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT host, coalesce(sum(s2c_content_length)/1000000, 0)::bigint as size_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += """
GROUP BY host ORDER BY size_sum DESC"""

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
                ks = KeyStatistic(r[0], r[1], N_('MB'), link_type=reports.URL_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('MB/day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenPolicyViolations(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-violations',
                       _('Top Violations'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT host, sum(hits)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_violations > 0
""" % (self.__vendor_name,)
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += " GROUP BY host ORDER BY hits_sum DESC"

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
                if len(host) > 25:
                    host = host[:25] + "..."
                ks = KeyStatistic(host, r[1], _('Hits'))
                lks.append(ks)
                dataset[host] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockedPolicyViolations(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-blocked-violations',
                       _('Top Blocked Violations'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT host, COALESCE(sum(hits), 0)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_blocks > 0
""" % (self.__vendor_name,)
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += " GROUP BY host ORDER BY hits_sum DESC"

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
                     xlabel=_('Hosts'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class WebFilterDetail(DetailSection):
    def __init__(self, vendor_name):
        DetailSection.__init__(self, 'violations', _('Violation Events'))

        self.__vendor_name = vendor_name

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hname', _('Client')))
        else:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('uid', _('User')))
        else:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))

        rv += [ColumnDesc('wf_%s_category' % self.__vendor_name, _('Category')),
               ColumnDesc('wf_%s_flagged' % self.__vendor_name, _('Flagged')),
               ColumnDesc('wf_%s_blocked' % self.__vendor_name, _('Blocked')),
               ColumnDesc('url', _('Url'), 'URL'),
               ColumnDesc('s_server_addr', _('Server Ip')),
               ColumnDesc('c_client_addr', _('Client Ip'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid, wf_%s_category,
       wf_%s_flagged, wf_%s_blocked,
       CASE s_server_port WHEN 443 THEN 'https://' ELSE 'http://' END || host || uri,
       host(s_server_addr), c_client_addr::text
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s
AND (wf_%s_flagged OR wf_%s_blocked)
""" % (self.__vendor_name, self.__vendor_name, self.__vendor_name,
       DateFromMx(start_date), DateFromMx(end_date),
       self.__vendor_name, self.__vendor_name)

        if host:
            sql += " AND hname = %s" % QuotedString(host)
        if user:
            sql += " AND uid = %s" % QuotedString(user)

        return sql + " ORDER BY time_stamp DESC"

class WebFilterDetailUnblock(DetailSection):
    def __init__(self, vendor_name):
        DetailSection.__init__(self, 'unblocks', _('Unblock Events'))

        self.__vendor_name = vendor_name

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hname', _('Client')))
        else:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('uid', _('User')))
        else:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))

        rv += [ColumnDesc('url', _('Url'), 'URL'),
               ColumnDesc('s_server_addr', _('Server Ip')),
               ColumnDesc('c_client_addr', _('Client Ip'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid,
       CASE s_server_port WHEN 443 THEN 'https://' ELSE 'http://' END || host || uri,
       host(s_server_addr), c_client_addr::text
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s
AND wf_%s_category = 'unblocked'
""" % (DateFromMx(start_date), DateFromMx(end_date),
       self.__vendor_name)

        if host:
            sql += " AND hname = %s" % QuotedString(host)
        if user:
            sql += " AND uid = %s" % QuotedString(user)

        return sql + " ORDER BY time_stamp DESC"

class WebFilterDetailAll(DetailSection):
    def __init__(self, vendor_name):
        DetailSection.__init__(self, 'events', _('All Events'))

        self.__vendor_name = vendor_name

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hname', _('Client')))
        else:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('uid', _('User')))
        else:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))

        rv += [ColumnDesc('wf_%s_category' % self.__vendor_name, _('Category')),
               ColumnDesc('wf_%s_flagged' % self.__vendor_name, _('Flagged')),
               ColumnDesc('wf_%s_blocked' % self.__vendor_name, _('Blocked')),
               ColumnDesc('url', _('Url'), 'URL'),
               ColumnDesc('s_server_addr', _('Server Ip')),
               ColumnDesc('c_client_addr', _('Client Ip'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid, wf_%s_category,
       wf_%s_flagged, wf_%s_blocked,
       CASE s_server_port WHEN 443 THEN 'https://' ELSE 'http://' END || host || uri,
       host(s_server_addr), c_client_addr::text
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s""" % (self.__vendor_name,
                                                 self.__vendor_name,
                                                 self.__vendor_name,
                                                 DateFromMx(start_date),
                                                 DateFromMx(end_date))

        if host:
            sql += " AND hname = %s" % QuotedString(host)
        if user:
            sql += " AND uid = %s" % QuotedString(user)

        return sql + " ORDER BY time_stamp DESC"

class WebFilterDetailDomains(DetailSection):
    def __init__(self, vendor_name):
        DetailSection.__init__(self, 'domains', _('Site Events'))

        self.__vendor_name = vendor_name

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('domain', _('Site')),
              ColumnDesc('hits', _('Hits')),
              ColumnDesc('size', _('Size (MB)'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT regexp_replace(host, E'.*?([^.]+\.[^.]+)(:[0-9]+)?$', E'\\\\1') AS domain,
       count(*) AS count, round(COALESCE(sum(s2c_content_length) / 10^6, 0)::numeric, 2)::float
FROM reports.n_http_events
WHERE regexp_replace(host, E'[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+(:[0-9]+)?', '') != ''
AND time_stamp >= %s AND time_stamp < %s
"""  % (DateFromMx(start_date),
        DateFromMx(end_date))

        if host:
            sql += " AND hname = %s" % QuotedString(host)
        if user:
            sql += " AND uid = %s" % QuotedString(user)

        sql += " GROUP BY domain"

        return sql + " ORDER BY count DESC"

# Unused reports --------------------------------------------------------------

class WebUsageByCategory(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'web-usage-by-category',
                       _('Web Usage By Category'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT wf_%s_category, count(*) AS count_events
FROM reports.n_http_events
WHERE time_stamp >= %%s AND time_stamp < %%s""" % self.__vendor_name
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += """\
GROUP BY wf_%s_category
ORDER BY count_events DESC""" % self.__vendor_name

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
                stat_key = r[0]
                if stat_key is None:
                    stat_key = _('Uncategorized')
                ks = KeyStatistic(stat_key, r[1], _('Hits'))
                lks.append(ks)
                dataset[stat_key] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Category'),
                     ylabel=_('Hits Per Day'))
        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class ViolationsByCategory(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'violations-by-category',
                       _('Violations By Category'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT wf_%s_category, count(*) as blocks_sum
FROM reports.n_http_events
WHERE time_stamp >= %%s AND time_stamp < %%s
AND wf_%s_flagged """ % (2 * (self.__vendor_name,))
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += """\
GROUP BY wf_%s_category
ORDER BY blocks_sum DESC""" % self.__vendor_name

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
                stat_key = r[0]
                if stat_key is None:
                    stat_key = _('Uncategorized')
                ks = KeyStatistic(stat_key, r[1], _('Hits'))
                lks.append(ks)
                dataset[stat_key] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Category'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)
