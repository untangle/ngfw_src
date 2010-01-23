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
#
# Aaron Read <amread@untangle.com>
# Sebastien Delafond <seb@untangle.com>

import gettext
import logging
import mx
import reports.colors as colors
import reports.i18n_helper
import reports.sql_helper as sql_helper

from psycopg import DateFromMx
from psycopg import QuotedString
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import Highlight
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
                                  "count(CASE WHEN wf_%s_action = 'B' THEN 1 ELSE null END)"
                                  % self.__vendor_name))

        ft.dimensions.append(Column('wf_%s_category' % self.__vendor_name,
                                    'text'))

        ft.dimensions.append(Column('wf_%s_action' % self.__vendor_name,
                                    'text'))

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [WebHighlight(self.name, self.__vendor_name),
                            HourlyWebUsage(self.__vendor_name),
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
                            TopTenBlockerPolicyViolations(self.__vendor_name)])
        sections.append(s)

        sections.append(WebFilterDetail(self.__vendor_name))
        sections.append(WebFilterDetailAll(self.__vendor_name))
        sections.append(WebFilterDetailDomains(self.__vendor_name))

        return Report(self, sections)

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
       COALESCE(sum(CASE WHEN NULLIF(wf_%s_category,'') IS NULL THEN 0 ELSE hits END), 0)::int AS violations,
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
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        hits_query = """\
SELECT max(hits) AS max_hits,
       COALESCE(sum(hits), 0)::float / (%s * 24 * 60) AS avg_hits
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            hits_query = hits_query + " AND hname = %s"
        elif user:
            hits_query = hits_query + " AND uid = %s"

        violations_query = """\
SELECT COALESCE(sum(wf_%s_blocks), 0)::float / %s,
       max(wf_%s_blocks)
FROM (SELECT date_trunc('hour', trunc_time) AS hour,
             sum(wf_%s_blocks)::int AS wf_%s_blocks
      FROM reports.n_http_totals
      WHERE trunc_time >= %%s AND trunc_time < %%s
""" % (self.__vendor_name, report_days, self.__vendor_name, self.__vendor_name,
       self.__vendor_name)

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
                curs.execute(hits_query, (report_days, one_week, ed, host))
            elif user:
                curs.execute(hits_query, (report_days, one_week, ed, user))
            else:
                curs.execute(hits_query, (report_days, one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('Avg Hits'), r[1], _('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Hits'), r[0], _('hits/minute'))
            lks.append(ks)

            curs = conn.cursor()
            if host:
                curs.execute(violations_query, (one_week, ed, host))
            elif user:
                curs.execute(violations_query, (one_week, ed, user))
            else:
                curs.execute(violations_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('Avg Violations'), r[0],
                              _('violations/hour'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Violations'), r[1],
                              _('violations/hour'))
            lks.append(ks)
        finally:
            conn.commit()

        return lks

    @print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            # per minute
            sums = ["sum(hits)",
                    "sum(wf_%s_blocks)" % (self.__vendor_name,)]

            if host:
                extra_where = [("AND hname = %(host)s", { 'host' : host }),]
            elif user:
                extra_where = [("AND uid = %(user)s" , { 'user' : user }),]
            else:
                extra_where = []

            q, h = sql_helper.get_averaged_query(sums, "reports.n_http_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where)

            curs.execute(q, h)

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

        plot = Chart(type=TIME_SERIES_CHART,
                     title=self.title,
                     xlabel=_('Time'),
                     ylabel=_('Hits per minute'),
                     major_formatter=TIMESTAMP_FORMATTER)
#                     required_points=sql_helper.REQUIRED_TIME_POINTS)

        plot.add_dataset(dates, hits, label=_('hits'),
                         color=colors.goodness)
        plot.add_dataset(dates, blocks, label=_('violations'),
                         color=colors.badness)

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
SELECT max(hits), COALESCE(sum(hits), 0) / %s, max(wf_%s_blocks),
       COALESCE(sum(wf_%s_blocks), 0) / %s
FROM (SELECT date_trunc('day', trunc_time) AS day, sum(hits)::int AS hits,
             sum(wf_%s_blocks)::int as wf_%s_blocks
      FROM reports.n_http_totals
      WHERE trunc_time >= %%s AND trunc_time < %%s
""" % (report_days, self.__vendor_name, self.__vendor_name, report_days,
       self.__vendor_name, self.__vendor_name)
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
            ks = KeyStatistic(_('max hits'), r[0], _('hits/day'))
            lks.append(ks)
            ks = KeyStatistic(_('avg hits'), r[1], _('hits/day'))
            lks.append(ks)
            ks = KeyStatistic(_('max violations'), r[2],
                              _('violations/day'))
            lks.append(ks)
            ks = KeyStatistic(_('avg violations'), r[3],
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
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        conn = sql_helper.get_connection()
        try:
            q = """\
SELECT date_trunc('day', trunc_time) AS day,
       coalesce(sum(hits), 0)::int AS hits,
       coalesce(sum(wf_%s_blocks), 0)::int AS wf_%s_blocks
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

        rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDelta(1))

        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_('Date'),
                     ylabel=_('Hits per Day'),
                     major_formatter=DATE_FORMATTER,
                     required_points=rp)

        plot.add_dataset(dates, hits, label=_('hits'), color=colors.goodness)
        plot.add_dataset(dates, blocks, label=_('violations'),
                         color=colors.badness)

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
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(sum(hits)::int, 0), coalesce(sum(wf_%s_blocks)::int, 0)
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

            hits = r[0]
            violations = r[1]

            ks = KeyStatistic(_('Total Hits'), hits, 'hits')
            lks.append(ks)
            ks = KeyStatistic(_('Total Violations'), violations, 'violations')
            lks.append(ks)
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Date'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset({_('total hits'): hits,
                              _('total violations'): violations},
                             colors={_('total hits'): colors.goodness,
                                     _('total violations'): colors.badness})

        return (lks, plot)

class TopTenWebPolicyViolationsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-categories-of-violations-by-hits',
                       _('Top Ten Categories of Violations (by hits)'))

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
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))
        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"
        query += """\
GROUP BY wf_%s_category ORDER BY blocks_sum DESC
""" % self.__vendor_name

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
                ks = KeyStatistic(cat, r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Policy'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebBlockedPolicyViolationsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-categories-of-blocked-violations-by-hits',
                       _('Top Ten Categories of Blocked Violations (by hits)'))

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
AND wf_%s_category != ''
AND wf_%s_action = 'B'
AND wf_%s_blocks > 0""" % (5 * (self.__vendor_name,))
        if host:
            query = query + " AND hname = %s"
        elif user:
            query = query + " AND uid = %s"
        query += """\
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
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Policy'),
                     ylabel=_('Blocks per Day'))
        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebBrowsingHostsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-browsing-hosts-by-hits',
                       _('Top Ten Web Browsing Hosts (by hits)'))

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

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebBrowsingUsersByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-browsing-users-by-hits',
                       _('Top Ten Web Browsing Users (by hits)'))

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
                ks = KeyStatistic(r[0], r[1], _('hits'),
                                  link_type=reports.USER_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('User'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebBrowsingUsersBySize(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-browsing-users-by-size',
                       _('Top Ten Web Browsing Users (by size)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT uid, COALESCE(sum(s2c_content_length), 0)::bigint as size_sum
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
                ks = KeyStatistic(r[0], r[1], _('bytes'),
                                  link_type=reports.USER_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('User'),
                     ylabel=_('bytes/day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebPolicyViolatorsByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-host-violators-by-hits',
                       _('Top Ten Host Violators (by hits)'))

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
AND wf_%s_action = 'B'
GROUP BY hname
ORDER BY blocks_sum DESC""" % ((self.__vendor_name,)*2)

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))
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

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebPolicyViolatorsADByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-violators-by-hits',
                       _('Top Ten User Violators (by hits)'))

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
AND wf_%s_action = 'B'
AND wf_%s_category != ''
AND wf_%s_blocks > 0
AND uid != ''
GROUP BY uid ORDER BY blocks_sum DESC""" \
            % (4 * (self.__vendor_name,))

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()

            curs.execute(query, (one_week, ed))
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

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebBrowsingHostsBySize(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-web-browsing-hosts-by-size',
                       _('Top Ten Web Browsing Hosts (by size)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hname, COALESCE(sum(s2c_content_length), 0)::bigint as size_sum
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

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebsitesByHits(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-websites-by-hits',
                       _('Top Ten Websites (by hits)'))

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
                ks = KeyStatistic(r[0], r[1], _('hits'), link_type=reports.URL_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenWebsitesBySize(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-websites-by-size',
                       _('Top Ten Websites (by size)'))

        self.__vendor_name = vendor_name

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT host, coalesce(sum(s2c_content_length), 0)::bigint as size_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"
        query += """\
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
                ks = KeyStatistic(r[0], r[1], N_('bytes'), link_type=reports.URL_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]

        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('bytes/day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenPolicyViolations(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-violations',
                       _('Top Ten Violations'))

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
      AND NOT wf_%s_category IS NULL""" % self.__vendor_name
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
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenBlockerPolicyViolations(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-ten-blocked-violations',
                       _('Top Ten Blocked Violations'))

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
AND NOT wf_%s_category IS NULL
AND wf_%s_action = 'B'
AND wf_%s_blocks > 0""" % (self.__vendor_name, self.__vendor_name,
                           self.__vendor_name)
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
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

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
               ColumnDesc('case', _('Blocked')),
               ColumnDesc('url', _('URL'), 'URL'),
               ColumnDesc('s_server_addr', _('Server IP')),
               ColumnDesc('c_client_addr', _('Client IP'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid, wf_%s_category,
       CASE wf_%s_action WHEN 'B' THEN 'True' ELSE 'False' END,
       CASE s_server_port WHEN 443 THEN 'https://' ELSE 'http://' END || host || uri,
       host(s_server_addr), c_client_addr::text
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s
      AND NOT wf_%s_action ISNULL
""" % (self.__vendor_name, self.__vendor_name,
       DateFromMx(start_date), DateFromMx(end_date),
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
               ColumnDesc('case', _('Blocked')),
               ColumnDesc('url', _('URL'), 'URL'),
               ColumnDesc('s_server_addr', _('Server IP')),
               ColumnDesc('c_client_addr', _('Client IP'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid, wf_%s_category,
       CASE wf_%s_action WHEN 'B' THEN 'True' ELSE 'False' END,
       CASE s_server_port WHEN 443 THEN 'https://' ELSE 'http://' END || host || uri,
       host(s_server_addr), c_client_addr::text
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s""" % (self.__vendor_name,
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

        rv = [ColumnDesc('domain', _('Domain')),
              ColumnDesc('hits', _('Hits')),
              ColumnDesc('size', _('Size'))]

        if host:
            rv.append(ColumnDesc('hname', _('Client')))
        else:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('uid', _('User')))
        else:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT regexp_replace(host, E'.*?([^.]+\.[^.]+)(:[0-9]+)?$', E'\\\\1') AS domain,
       count(*) AS count, COALESCE(sum(s2c_content_length) / 10^6, 0)
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
                ks = KeyStatistic(stat_key, r[1], _('hits'))
                lks.append(ks)
                dataset[stat_key] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Category'),
                     ylabel=_('Hits per Day'))
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
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))

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

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)
