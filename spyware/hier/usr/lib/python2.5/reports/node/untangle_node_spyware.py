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

from psycopg2.extensions import DateFromMx
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
from reports import TIMESTAMP_FORMATTER
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-spyware').lgettext

class Spyware(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-spyware')

    def setup(self, start_date, end_date):
        self.__update_access(start_date, end_date)
        self.__update_blacklist(start_date, end_date)
        self.__update_cookie(start_date, end_date)

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def parents(self):
        return ['untangle-vm', 'untangle-casing-http']

    def events_cleanup(self, cutoff):
        try:
            sql_helper.run_sql("""\
DELETE FROM events.n_spyware_evt_access
 WHERE time_stamp < %s""", (cutoff,))
        except: pass

        try:
            sql_helper.run_sql("""\
DELETE FROM events.n_spyware_evt_activex
 WHERE time_stamp < %s""", (cutoff,))
        except: pass

        try:
            sql_helper.run_sql("""\
DELETE FROM events.n_spyware_evt_blacklist
 WHERE time_stamp < %s""", (cutoff,))
        except: pass

        try:
            sql_helper.run_sql("""\
DELETE FROM events.n_spyware_evt_cookie
 WHERE time_stamp < %s""", (cutoff,))
        except: pass

    def reports_cleanup(self, cutoff):
        pass

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [SpywareHighlight(self.name),
                            HourlyRates(),
                            SpywareUrlsBlocked(),
                            TopTenBlockedSpywareSitesByHits(),
                            TopTenBlockedHostsByHits(),
                            TopTenBlockedCookies(),
                            SpywareCookiesBlocked(),
                            SpywareSubnetsDetected(),
                            TopTenSuspiciousTrafficSubnetsByHits(),
                            TopTenSuspiciousTrafficHostsByHits()])
        sections.append(s)

        sections.append(CookieDetail())
        sections.append(UrlBlockDetail())
        sections.append(SubnetDetail())

        return Report(self, sections)

    @print_timing
    def __update_access(self, start_date, end_date):
        try:
            sql_helper.run_sql("""
ALTER TABLE reports.sessions ADD COLUMN sw_access_ident text""")
        except: pass

        sd = DateFromMx(sql_helper.get_update_info('sessions[spyware-access]',
                                                   start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.sessions
SET sw_access_ident = ident
FROM events.n_spyware_evt_access
WHERE reports.sessions.time_stamp >= %s
  AND reports.sessions.time_stamp < %s
  AND reports.sessions.pl_endp_id = events.n_spyware_evt_access.pl_endp_id""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('sessions[spyware-access]', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

        ft = reports.engine.get_fact_table('reports.session_totals')
        ft.measures.append(Column('sw_accesses', 'integer',
                                  'count(sw_access_ident)'))
        ft.dimensions.append(Column('sw_access_ident', 'text'))

    @print_timing
    def __update_blacklist(self, start_date, end_date):
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN sw_blacklisted boolean""")
        except: pass

        sd = DateFromMx(sql_helper.get_update_info('n_http_events[spyware-blacklist]',
                                                   start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.n_http_events
SET sw_blacklisted = true
FROM events.n_spyware_evt_blacklist
WHERE reports.n_http_events.time_stamp >= %s
  AND reports.n_http_events.time_stamp < %s
  AND reports.n_http_events.request_id = events.n_spyware_evt_blacklist.request_id""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('n_http_events[spyware-blacklist]', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

        ft = reports.engine.get_fact_table('reports.n_http_totals')
        ft.measures.append(Column('sw_blacklisted', 'integer',
                                  'count(sw_blacklisted)'))

    @print_timing
    def __update_cookie(self, start_date, end_date):
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN sw_cookie_ident text""")
        except: pass

        sd = DateFromMx(sql_helper.get_update_info('n_http_events[spyware-cookie]',
                                                   start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.n_http_events
SET sw_cookie_ident = ident
FROM events.n_spyware_evt_cookie
WHERE reports.n_http_events.time_stamp >= %s
  AND reports.n_http_events.time_stamp < %s
  AND reports.n_http_events.request_id = events.n_spyware_evt_cookie.request_id""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('n_http_events[spyware-cookie]', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

        ft = reports.engine.get_fact_table('reports.n_http_totals')
        ft.measures.append(Column('sw_cookies', 'integer',
                                  'count(sw_cookie_ident)'))
        ft.dimensions.append(Column('sw_cookie_ident', 'text'))

class SpywareHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("scanned") + " " + "%(hits)s" + " " +
                           _("web hits and blocked") + " " +
                           "%(blocks)s" + " " + _("activities"))

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(sum(hits), 0)::int AS hits,
       COALESCE(sum(sw_blacklisted+sw_cookies), 0) AS blocks
FROM reports.n_http_totals
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


class HourlyRates(Graph):
    def __init__(self):
        Graph.__init__(self, 'spyware-events',
                       _('Incidents'))

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

            sums = ["coalesce(sum(sw_blacklisted), 0)",
                    "coalesce(sum(sw_cookies), 0)"]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_http_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            sw_blacklisted = []
            sw_cookies = []
            
            for r in curs.fetchall():
                dates.append(r[0])
                sw_blacklisted.append(r[1])
                sw_cookies.append(r[2])

            if not sw_blacklisted:
                sw_blacklisted = [0,]
            if not sw_cookies:
                sw_cookies = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Avg Urls Blocked'),
                              sum(sw_blacklisted) / len(rp),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Urls Blocked'), max(sw_blacklisted),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Cookies Blocked'),
                              sum(sw_cookies) / len(rp),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Cookies Blocked'), max(sw_cookies),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Incidents'),
                         major_formatter=formatter,
                         required_points=rp)

            plot.add_dataset(dates, sw_blacklisted, label=_('Urls'))
            plot.add_dataset(dates, sw_cookies, label=_('Cookies'))

            sums = ["coalesce(sum(sw_accesses), 0)"]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            sw_accesses = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                sw_accesses.append(r[1])

            if not sw_accesses:
                sw_accesses = [0,]

            ks = KeyStatistic(_('Avg Suspicious Traffic'),
                              "%.2f" % (sum(sw_accesses) / len(rp)),
                              _('Detected')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Suspicious Traffic'),
                              max(sw_accesses),
                              _('Detected')+'/'+_(unit))
            lks.append(ks)

            plot.add_dataset(dates, sw_accesses, label=_('Detections'))
        finally:
            conn.commit()

        return (lks, plot)

class SpywareUrlsBlocked(Graph):
    def __init__(self):
        Graph.__init__(self, 'summary-blocked-urls',
                       _('Blocked Urls'))

    @print_timing
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

            sums = ["coalesce(sum(sw_blacklisted), 0)",]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_http_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            sw_blacklisted = []
            
            for r in curs.fetchall():
                dates.append(r[0])
                sw_blacklisted.append(r[1])

            if not sw_blacklisted:
                sw_blacklisted = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Avg Urls Blocked'),
                              sum(sw_blacklisted) / len(rp),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Urls Blocked'), max(sw_blacklisted),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Blocks'),
                         major_formatter=formatter,
                         required_points=rp)

            plot.add_dataset(dates, sw_blacklisted, label=_('Urls'))

        finally:
            conn.commit()

        return (lks, plot)

class TopTenBlockedSpywareSitesByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-blocked-blocked-urls-by-hits', _('Top Ten Blocked Urls (by Hits)'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT host, sum(sw_blacklisted + sw_cookies) as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND (sw_blacklisted + sw_cookies) > 0"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query = query + " GROUP BY host ORDER BY hits_sum DESC"

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
                     xlabel=_('Site'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-blocked-hosts-by-hits', _('Top Ten Blocked Hosts (by Hits)'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hname, sum(sw_blacklisted + sw_cookies) as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND (sw_blacklisted + sw_cookies) > 0"""

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
                ks = KeyStatistic(r[0], r[1], _('Hits'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenBlockedCookies(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-blocked-cookies', _('Top Ten Blocked Cookies'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT sw_cookie_ident, count(*) as hits_sum
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND sw_cookie_ident != ''
AND sw_cookies > 0"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query = query + " GROUP BY sw_cookie_ident ORDER BY hits_sum DESC"

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

class SpywareCookiesBlocked(Graph):
    def __init__(self):
        Graph.__init__(self, 'blocked-cookies', _('Blocked Cookies'))

    @print_timing
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

            sums = ["coalesce(sum(sw_cookies), 0)",]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_http_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            sw_cookies = []
            
            for r in curs.fetchall():
                dates.append(r[0])
                sw_cookies.append(r[1])

            if not sw_cookies:
                sw_cookies = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Avg Cookies Blocked'),
                              sum(sw_cookies) / len(rp),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Cookies Blocked'), max(sw_cookies),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Blocks'),
                         major_formatter=formatter,
                         required_points=rp)

            plot.add_dataset(dates, sw_cookies, label=_('Cookies'))

        finally:
            conn.commit()

        return (lks, plot)

class TopTenSuspiciousTrafficSubnetsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-suspicious-traffic-networks-by-hits',
                       _('Top Suspicious Traffic Networks'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT sw_access_ident, sum(sw_accesses) as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND sw_access_ident != ''
AND sw_accesses > 0"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query += """
GROUP BY sw_access_ident
ORDER BY hits_sum DESC"""

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
                     xlabel=_('Subnet'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenSuspiciousTrafficHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-suspicious-traffic-hosts-by-hits', _('Top Suspicious Traffic Hosts (by Hits)'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hname, sum(sw_accesses) as hits_sum
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND sw_access_ident != ''
AND sw_accesses > 0"""

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
                ks = KeyStatistic(r[0], r[1], _('Hits'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('Blocks Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class SpywareSubnetsDetected(Graph):
    def __init__(self):
        Graph.__init__(self, 'suspicious-traffic-detections', _('Suspicious Traffic Detections'))

    @print_timing
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


            sums = ["COALESCE(SUM(new_sessions), 0)",]

            extra_where = [ ("NOT sw_accesses IS NULL",{}),
                            ("sw_access_ident != ''",{}) ]
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
            sessions = []
            
            for r in curs.fetchall():
                dates.append(r[0])
                sessions.append(r[1])

            if not sessions:
                sessions = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Avg Suspicious Detections'),
                              sum(sessions) / len(rp),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Suspicious Detections'), max(sessions),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Detections'),
                         major_formatter=formatter,
                         required_points=rp)

            plot.add_dataset(dates, sessions, label=_('Detections'))

        finally:
            conn.commit()

        return (lks, plot)

class CookieDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'cookie-events', _('Cookie Events'))

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

        rv += [ColumnDesc('sw_cookie_ident', _('Cookie')),
               ColumnDesc('s_server_addr', _('Server Ip')),
               ColumnDesc('s_server_port', _('Server Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid, sw_cookie_ident, host(s_server_addr), s_server_port
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s
      AND NOT sw_cookie_ident IS NULL AND sw_cookie_ident != ''
""" % (DateFromMx(start_date), DateFromMx(end_date))

        if host:
            sql += " AND hname = %s" % QuotedString(host)
        if user:
            sql += " AND uid = %s" % QuotedString(user)

        return sql + " ORDER BY time_stamp DESC"

class UrlBlockDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'url-events', _('Url Blocklist Events'))

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

        rv += [ColumnDesc('s_server', _('Server')),
               ColumnDesc('uri', _('Uri')),
               ColumnDesc('s_server_addr', _('Server Ip')),
               ColumnDesc('s_server_port', _('Server Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid, 'http://' || host as s_server, uri, host(s_server_addr),
       s_server_port
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s AND sw_blacklisted
""" % (DateFromMx(start_date), DateFromMx(end_date))

        if host:
            sql += " AND hname = %s" % QuotedString(host)
        if user:
            sql += " AND uid = %s" % QuotedString(user)

        return sql

class SubnetDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'subnet-events', _('Subnet Events'))

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

        rv += [ColumnDesc('sw_blacklisted', _('Subnet')),
               ColumnDesc('c_server_addr', _('Server Ip')),
               ColumnDesc('c_server_port', _('Server Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, hname, uid, sw_access_ident, host(c_server_addr), c_server_port
FROM reports.sessions
WHERE time_stamp >= %s AND time_stamp < %s AND NOT sw_access_ident IS NULL
      AND sw_access_ident != ''
""" % (DateFromMx(start_date), DateFromMx(end_date))

        if host:
            sql += " AND hname = %s" % QuotedString(host)
        if user:
            sql += " AND uid = %s" % QuotedString(user)

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(Spyware())
