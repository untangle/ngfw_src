import gettext
import logging
import mx
import reportlab.lib.colors as colors
import reports.i18n_helper
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
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from sql_helper import print_timing

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
                                  "count(CASE WHEN NOT wf_%s_action ISNULL THEN 1 ELSE null END)"
                                  % self.__vendor_name))

        ft.dimensions.append(Column('wf_%s_category' % self.__vendor_name,
                                    'text'))

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [HourlyWebUsage(self.__vendor_name),
                            DailyWebUsage(self.__vendor_name),
                            TotalWebUsage(self.__vendor_name),
                            TopTenWebUsageByHits(self.__vendor_name),
                            TopTenWebUsageBySize(self.__vendor_name),
                            TopTenWebPolicyViolationsByHits(self.__vendor_name),
                            TopTenWebBlockedPolicyViolationsByHits(self.__vendor_name),
                            TopTenWebsitesByHits(self.__vendor_name),
                            TopTenWebsitesBySize(self.__vendor_name),
                            TopTenWebPolicyViolatorsByHits(self.__vendor_name),
                            TopTenWebPolicyViolatorsADByHits(self.__vendor_name),
                            TopTenPolicyViolations(self.__vendor_name),
                            TopTenBlockerPolicyViolations(self.__vendor_name)])
        sections.append(s)

        sections.append(WebFilterDetail(self.__vendor_name))

        return Report(self.name, sections)

    def events_cleanup(self, cutoff):
        sql_helper.run_sql("""\
DELETE FROM events.n_webfilter_evt_blk WHERE time_stamp < %s""", (cutoff,))

    def reports_cleanup(self, cutoff):
        pass

    @print_timing
    def __update_n_http_events(self, start_date, end_date):
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN wf_%s_action character(1)"""
                               % self.__vendor_name)
        except: pass
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN wf_%s_reason character(1)"""
                               % self.__vendor_name)
        except: pass
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN wf_%s_category text"""
                               % self.__vendor_name)
        except: pass

        sd = DateFromMx(sql_helper.get_update_info('n_http_events[%s]'
                                                   % self.name, start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.n_http_events
SET wf_%s_action = action,
  wf_%s_reason = reason,
  wf_%s_category = category
FROM events.n_webfilter_evt_blk
WHERE reports.n_http_events.time_stamp >= %%s
  AND reports.n_http_events.time_stamp < %%s
  AND events.n_webfilter_evt_blk.vendor_name = %%s
  AND reports.n_http_events.request_id = events.n_webfilter_evt_blk.request_id"""
                               % (3 * (self.__vendor_name,)),
                               (sd, ed, self.__vendor_name), connection=conn,
                               auto_commit=False)

            sql_helper.set_update_info('n_http_events[%s]' % self.name,
                                       ed, connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class HourlyWebUsage(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'hourly-usage', _('Hourly Usage'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        hits_query = """\
SELECT max(hits) AS max_hits, avg(hits) AS avg_hits
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            hits_query = hits_query + " AND hname = %s"
        elif user:
            hits_query = hits_query + " AND uid = %s"

        violations_query = """\
SELECT avg(wf_%s_blocks)
FROM (SELECT date_trunc('hour', trunc_time) AS hour,
             sum(wf_%s_blocks)::int AS wf_%s_blocks
      FROM reports.n_http_totals
      WHERE trunc_time >= %%s AND trunc_time < %%s""" \
            % (3 * (self.__vendor_name,))

        if host:
            violations_query = violations_query + " AND hname = %s"
        elif user:
            violations_query = violations_query + " AND uid = %s"

        violations_query = violations_query + " GROUP BY hour) AS foo"

        conn = sql_helper.get_connection()
        try:
            lks = []

            curs = conn.cursor()
            if host:
                curs.execute(hits_query, (one_day, ed, host))
            elif user:
                curs.execute(hits_query, (one_day, ed, user))
            else:
                curs.execute(hits_query, (one_day, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('max hits (1-day)'), r[0], _('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('avg hits (1-day)'), r[1], _('hits/minute'))
            lks.append(ks)

            curs = conn.cursor()
            if host:
                curs.execute(hits_query, (one_week, ed, host))
            elif user:
                curs.execute(hits_query, (one_week, ed, user))
            else:
                curs.execute(hits_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('max hits (1-week)'), r[0], _('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('avg hits (1-week)'), r[1], _('hits/minute'))
            lks.append(ks)

            curs = conn.cursor()
            if host:
                curs.execute(violations_query, (one_day, ed, host))
            elif user:
                curs.execute(violations_query, (one_day, ed, user))
            else:
                curs.execute(violations_query, (one_day, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('avg violations (1-day)'), r[0],
                              _('violations/hour'))
            lks.append(ks)

            curs = conn.cursor()
            if host:
                curs.execute(violations_query, (one_week, ed, host))
            elif user:
                curs.execute(violations_query, (one_week, ed, user))
            else:
                curs.execute(violations_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('avg violations (1-week)'), r[0],
                              _('violations/hour'))
            lks.append(ks)
        finally:
            conn.commit()

        return lks

    @print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()
        try:
            q = """\
SELECT (date_part('hour', trunc_time) || ':'
        || (date_part('minute', trunc_time)::int / 10 * 10))::time AS time,
       sum(hits)::int / 10 AS hits,
       sum(wf_%s_blocks)::int / 10 AS wf_%s_blocks
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))
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
            hits = []
            blocks = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break

                dates.append(r[0].seconds)
                hits.append(r[1])
                blocks.append(r[2])
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART,
                     title=self.title,
                     xlabel=_('Hour of Day'),
                     ylabel=_('Hits per Minute'),
                     major_formatter=TIME_OF_DAY_FORMATTER)

        plot.add_dataset(dates, hits, label=_('hits'), color=colors.green)
        plot.add_dataset(dates, blocks, label=_('violations'), color=colors.red)

        return plot

class DailyWebUsage(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'daily-usage', _('Daily Usage'))

        self.__vendor_name = vendor_name


    @print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT max(hits), avg(hits), max(wf_%s_blocks), avg(wf_%s_blocks)
FROM (select date_trunc('day', trunc_time) AS day, sum(hits)::int AS hits,
             sum(wf_%s_blocks)::int as wf_%s_blocks
      FROM reports.n_http_totals
      WHERE trunc_time >= %%s AND trunc_time < %%s""" \
            % (4 * (self.__vendor_name,))
        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"

        query = query + " GROUP BY day) AS foo"

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
            ks = KeyStatistic(_('max hits (7-days)'), r[0], _('hits/day'))
            lks.append(ks)
            ks = KeyStatistic(_('avg hits (7-days)'), r[1], _('hits/day'))
            lks.append(ks)
            ks = KeyStatistic(_('max violations (7-days)'), r[2],
                              _('violations/day'))
            lks.append(ks)
            ks = KeyStatistic(_('avg violations (7-days)'), r[3],
                              _('violations/day'))
            lks.append(ks)
        finally:
            conn.commit()

        return lks

    @print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()
        try:
            q = """\
SELECT date_trunc('day', trunc_time) AS day,
       sum(hits)::int AS hits,
       sum(wf_%s_blocks)::int AS wf_%s_blocks
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))
            if host:
                q = q + " AND hname = %s"
            elif user:
                q = q + " AND uid = %s"
            q = q + """
GROUP BY day
ORDER BY day asc"""

            curs = conn.cursor()

            if host:
                curs.execute(q, (one_week, ed, host))
            elif user:
                curs.execute(q, (one_week, ed, user))
            else:
                curs.execute(q, (one_week, ed))

            dates = []
            hits = []
            blocks = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                hits.append(r[1])
                blocks.append(r[2])

        finally:
            conn.commit()

        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_('Date'),
                     ylabel=_('Hits per Day'),
                     major_formatter=DATE_FORMATTER)

        plot.add_dataset(dates, hits, label=_('hits'), color=colors.green)
        plot.add_dataset(dates, blocks, label=_('violations'), color=colors.red)

        return plot

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
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT coalesce(sum(hits)::int, 0), coalesce(sum(wf_%s_blocks)::int, 0)
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (self.__vendor_name,)
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

            ks = KeyStatistic(_('total hits (7-days)'), r[0], _('hits'))
            lks.append(ks)
            ks = KeyStatistic(_('total violations (7-days)'), r[1],
                              _('violations'))
            lks.append(ks)

            if host:
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))
            r = curs.fetchone()

            hits = r[0]
            violations = r[1]

            ks = KeyStatistic(_('total hits (1-day)'), hits, _('hits'))
            lks.append(ks)
            ks = KeyStatistic(_('total violations (1-day)'), violations,
                              _('violations'))
            lks.append(ks)
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Date'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset({_('hits'): hits, _('violations'): violations},
                             colors={'hits': colors.green,
                                     'violations': colors.red})

        return (lks, plot)

class TopTenWebPolicyViolationsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-policy-violations-by-hits',
                       _('Top Ten Categories of Blocked Violations (by hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT wf_%s_category, sum(wf_%s_blocks)::int AS blocks_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_category != ''""" % (3 * (self.__vendor_name,))
        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"
        query += """\
GROUP BY wf_%s_category ORDER BY blocks_sum DESC LIMIT 10\
""" % self.__vendor_name

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))

            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Policy'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenWebBlockedPolicyViolationsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-blocked-policy-violations-by-hits',
                       _('Top Ten Categories of Blocked Violations (by hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT wf_%s_category, sum(wf_%s_blocks)::int AS blocks_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_category != ''
AND wf_%s_blocks > 0""" % (4 * (self.__vendor_name,))
        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"
        query += """\
GROUP BY wf_%s_category ORDER BY blocks_sum DESC LIMIT 10""" \
            % self.__vendor_name

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))

            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Policy'),
                     ylabel=_('Blocks per Day'))
        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenWebUsageByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-usage-by-hits',
                       _('Top Ten Web Users By Hits'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT hname, sum(hits)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY hname ORDER BY hits_sum DESC LIMIT 10"""

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_day, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenWebPolicyViolatorsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-policy-violators-by-hits',
                       _('Top Ten Host Policy Violators (by hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT hname, sum(wf_%s_blocks)::int as blocks_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_category != ''
AND wf_%s_blocks > 0
GROUP BY hname ORDER BY blocks_sum DESC LIMIT 10""" \
            % (3 * (self.__vendor_name,))

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_day, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenWebPolicyViolatorsADByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-policy-violator-ad-by-hits',
                       _('Top Ten User Violators (by hits)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                           email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT uid, sum(wf_%s_blocks)::int as blocks_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
AND wf_%s_category != ''
AND wf_%s_blocks > 0
AND uid != ''
GROUP BY uid ORDER BY blocks_sum DESC LIMIT 10""" \
            % (3 * (self.__vendor_name,))

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_day, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'),
                                  link_type=reports.USER_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('UID'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenWebUsageBySize(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-usage-by-size',
                       _('Top Ten Web Users (by size)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT hname, COALESCE(sum(s2c_bytes) + sum(c2s_bytes), 0)::bigint as size_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        query += " GROUP BY hname ORDER BY size_sum DESC LIMIT 10"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_day, ed))
            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], N_('bytes'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('bytes/day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenWebsitesByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-websites-by-hits',
                       _('Top Ten Websites By Hits'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT host, sum(hits)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += " GROUP BY host ORDER BY hits_sum DESC LIMIT 10"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))

            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenWebsitesBySize(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-websites-by-size',
                       _('Top Ten Websites By Size'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT host, coalesce(sum(s2c_bytes) + sum(c2s_bytes), 0)::bigint as size_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += """\
GROUP BY host ORDER BY size_sum DESC LIMIT 10"""

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))

                for r in curs.fetchall():
                    ks = KeyStatistic(r[0], r[1], N_('bytes'),
                                      link_type=reports.HNAME_LINK)
                    lks.append(ks)
                    dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('bytes/day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenPolicyViolations(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-policy-violations',
                       _('Top Ten Policy Violations'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT host, sum(hits)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
      AND NOT wf_%s_category IS NULL""" % self.__vendor_name
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += " GROUP BY host ORDER BY hits_sum DESC LIMIT 10"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))

            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class TopTenBlockerPolicyViolations(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-blocker-policy-violations',
                       _('Top Ten Blocker Violations'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT host, sum(wf_%s_blocks)::int as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % self.__vendor_name
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += " GROUP BY host ORDER BY hits_sum DESC LIMIT 10"

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            if host:
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))

            for r in curs.fetchall():
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)

class WebFilterDetail(DetailSection):
    def __init__(self, vendor_name):
        DetailSection.__init__(self, 'incidents', _('Incident Report'))

        self.__vendor_name = vendor_name

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hname', _('Client'), 'String'))
        else:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('uid', _('User'), 'String'))
        else:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))


        rv += [ColumnDesc('wf_%s_category' % self.__vendor_name, 'String'),
               ColumnDesc('url', _('URL'), 'URL'),
               ColumnDesc('s_server_addr', _('Server IP'), 'ipaddr')]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid, wf_%s_category, 'http://' || host || uri,
       s_server_addr
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s
      AND NOT wf_%s_action ISNULL""" % (self.__vendor_name,
                                        DateFromMx(start_date),
                                        DateFromMx(end_date),
                                        self.__vendor_name)

        if host:
            sql = sql + (" AND host = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND host = %s" % QuotedString(user))

        return sql

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
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

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
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))

            for r in curs.fetchall():
                stat_key = r[0]
                if stat_key is None:
                    stat_key = _('Uncategorized')
                ks = KeyStatistic(stat_key, r[1], _('hits'))
                lks.append(ks)
                dataset[stat_key] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Category'),
                     ylabel=_('Hits per Day'))
        plot.add_pie_dataset(dataset)

        return (lks, plot)

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
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))

        query = """\
SELECT wf_%s_category, count(*) as blocks_sum
FROM reports.n_http_events
WHERE time_stamp >= %%s AND time_stamp < %%s
AND wf_%s_action IS NOT NULL """ % (2 * (self.__vendor_name,))
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
                curs.execute(query, (one_day, ed, host))
            elif user:
                curs.execute(query, (one_day, ed, user))
            else:
                curs.execute(query, (one_day, ed))

            for r in curs.fetchall():
                stat_key = r[0]
                if stat_key is None:
                    stat_key = _('Uncategorized')
                ks = KeyStatistic(stat_key, r[1], _('hits'))
                lks.append(ks)
                dataset[stat_key] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Category'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset)

        return (lks, plot)
