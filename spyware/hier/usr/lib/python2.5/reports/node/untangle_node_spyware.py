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
from reports import TIMESTAMP_FORMATTER
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from sql_helper import print_timing

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
                           [HourlyRates(),
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

        return Report(self.name, sections)

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


class HourlyRates(Graph):
    def __init__(self):
        Graph.__init__(self, 'hourly-spyware-events',
                       _('Hourly Blocked Incidents'))

    @print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email:
            return None

        lks = []

        ed = DateFromMx(end_date)

        url_query = """\
SELECT COALESCE(sum(sw_blacklisted), 0) AS sw_total_blacklisted,
       max(sw_blacklisted)::int AS sw_max_blacklisted,
       COALESCE(sum(sw_cookies), 0)  AS sw_total_cookies,
       max(sw_cookies)::int AS sw_max_cookies
FROM (SELECT date_trunc('hour', trunc_time) AS time,
      COALESCE(sum(sw_blacklisted), 0) AS sw_blacklisted,
      COALESCE(sum(sw_cookies), 0) AS sw_cookies
      FROM reports.n_http_totals
      WHERE trunc_time >= %s AND trunc_time < %s
      GROUP BY time) AS foo"""

        if host:
            url_query += " WHERE hname = %s"
        elif user:
            url_query += " WHERE uid = %s"

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()
            sd = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

            if host:
                curs.execute(url_query, (sd, ed, host))
            elif user:
                curs.execute(url_query, (sd, ed, user))
            else:
                curs.execute(url_query, (sd, ed))

            r = curs.fetchone()
            total_blacklisted = r[0]
            max_blacklisted = r[1]
            total_cookies = r[2]
            max_cookies = r[3]

            ks = KeyStatistic(_('Avg URLs Blocked'),
                              total_blacklisted / float(report_days * 24),
                              _('blocks/hour'))
            lks.append(ks)
            ks = KeyStatistic(_('Max URLs Blocked'), max_blacklisted,
                              _('blocks/hour'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Cookies Blocked'),
                              total_cookies / float(report_days * 24),
                              _('blocks/hour'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Cookies Blocked'), max_blacklisted,
                              _('blocks/hour'))
            lks.append(ks)

            sessions_query = """\
SELECT COALESCE(sum(sw_accesses), 0) AS total_accesses,
       max(sw_accesses)::int AS max_accesses
FROM (SELECT date_trunc('hour', trunc_time) AS time,
      COALESCE(sum(sw_accesses), 0) AS sw_accesses
      FROM reports.session_totals
      WHERE trunc_time >= %s AND trunc_time < %s
      GROUP BY time) AS foo"""

            if host:
                sessions_query = sessions_query + " AND hname = %s"
            elif user:
                sessions_query = sessions_query + " AND uid = %s"


            curs = conn.cursor()
            sd = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

            if host:
                curs.execute(sessions_query, (sd, ed, host))
            elif user:
                curs.execute(sessions_query, (sd, ed, user))
            else:
                curs.execute(sessions_query, (sd, ed))
            r = curs.fetchone()
            total_accesses = r[0]
            max_accesses = r[1]

            ks = KeyStatistic(_('Avg Suspicious Traffic'),
                              total_accesses / float(report_days * 24),
                              _('detected/hour'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Suspicious Traffic'),
                              max_accesses, _('detected/hour'))
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
        curs = conn.cursor()
        try:
            # per minute
            sums = ["coalesce(sum(sw_blacklisted), 0) * 60",
                    "coalesce(sum(sw_cookies), 0) * 60"]

            extra_where = []
            if host:
                extra_where.append(("AND hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("AND uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_http_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where)
            curs.execute(q, h)

            dates = []
            sw_blacklisted = []
            sw_cookies = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                sw_blacklisted.append(r[1])
                sw_cookies.append(r[2])

            plot = Chart(type=TIME_SERIES_CHART,
                         title=self.title,
                         xlabel=_('Hour of Day'),
                         ylabel=_('Hits per Minute'),
                         major_formatter=TIMESTAMP_FORMATTER)

            plot.add_dataset(dates, sw_blacklisted, label=_('URLs'))
            plot.add_dataset(dates, sw_cookies, label=_('cookies'))

            sums = ["coalesce(sum(sw_accesses), 0)"]

            extra_where = []
            if host:
                extra_where.append(("AND hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("AND uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where)
            curs.execute(q, h)

            dates = []
            sw_accesses = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                sw_accesses.append(r[1])
        finally:
            conn.commit()

        plot.add_dataset(dates, sw_accesses, label=_('Detections'))

        return plot

class SpywareUrlsBlocked(Graph):
    def __init__(self):
        Graph.__init__(self, 'summary-daily-blocked-urls',
                       _('Daily Blocked URLs'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        query = """\
SELECT COALESCE(sum(sw_blacklisted), 0) / %s AS avg_blocked,
       max(sw_blacklisted)::int AS max_blocked
FROM (SELECT date_trunc('day', trunc_time) AS time,
             COALESCE(sum(sw_blacklisted), 0) AS sw_blacklisted,
             COALESCE(sum(sw_cookies), 0) AS sw_cookies
      FROM reports.n_http_totals
      WHERE trunc_time >= %s AND trunc_time < %s
      GROUP BY time) AS foo"""

        if host:
            query += " WHERE hname = %s"
        elif user:
            query += " WHERE uid = %s"

        conn = sql_helper.get_connection()
        try:
            lks = []

            curs = conn.cursor()
            if host:
                curs.execute(query, (report_days, one_week, ed, host))
            elif user:
                curs.execute(query, (report_days, one_week, ed, user))
            else:
                curs.execute(query, (report_days, one_week, ed))

            r = curs.fetchone()
            ks = KeyStatistic(_('Avg URLs Blocked'), r[0],
                              _('hits/day'))
            lks.append(ks)
            ks = KeyStatistic(_('Max URLs Blocked'), r[1],
                              _('hits/day'))
            lks.append(ks)

            q = """\
SELECT date_trunc('day', trunc_time) AS day,
       coalesce(sum(sw_blacklisted), 0)
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

            if host:
                q += " AND hname = %s"
            elif user:
                q += " AND uid = %s"
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
            blocks = []

            for r in curs.fetchall():
                dates.append(r[0])
                blocks.append(r[1])
        finally:
            conn.commit()

        rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDelta(1))

        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_('Date'),
                     ylabel=_('Blocks per Day'),
                     major_formatter=DATE_FORMATTER,
                     required_points=rp)

        plot.add_dataset(dates, blocks, label=_('URLs'))

        return (lks, plot)

class TopTenBlockedSpywareSitesByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-blocked-spyware-sites-by-hits', _('Top Ten Blocked Spyware Sites By Hits'))

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
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Site'),
                     ylabel=_('Blocks per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenBlockedHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-blocked-hosts-by-hits', _('Top Ten Blocked Hosts By Hits'))

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
                ks = KeyStatistic(r[0], r[1], _('hits'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('Blocks per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

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
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Cookie'),
                     ylabel=_('Blocks per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class SpywareCookiesBlocked(Graph):
    def __init__(self):
        Graph.__init__(self, 'blocked-cookies', _('Blocked Cookies'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        sd = DateFromMx(start_date)

        query = """\
SELECT date_trunc('day', trunc_time) AS day, sum(sw_cookies)
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query += """
GROUP BY day
ORDER BY day
"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (sd, ed, host))
            elif user:
                curs.execute(query, (sd, ed, user))
            else:
                curs.execute(query, (sd, ed))

            dates = []
            blocks = []
            blocks = []
            total = 0
            max = 0

            for r in curs.fetchall():
                s = r[1]
                dates.append(r[0])
                blocks.append(s)
                if not s:
                    s = 0
                total += s
                if max < s:
                    max = s
        finally:
            conn.commit()


        lks = []

        ks = KeyStatistic(_('avg cookies blocked'), total / report_days,
                          _('blocks/day'))
        lks.append(ks)
        ks = KeyStatistic(_('max cookies blocked'), max, _('blocks/day'))
        lks.append(ks)

        rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDelta(1))

        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_('Date'),
                     ylabel=_('Blocks per Day'),
                     major_formatter=DATE_FORMATTER,
                     required_points=rp)

        plot.add_dataset(dates, blocks, label=_('cookies'))

        return (lks, plot)

class TopTenSuspiciousTrafficSubnetsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-suspicious-traffic-networks-by-hits',
                       _('Top Ten Suspicious Traffic Networks'))

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
                ks = KeyStatistic(r[0], r[1], _('hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Subnet'),
                     ylabel=_('Blocks per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopTenSuspiciousTrafficHostsByHits(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-ten-suspicious-traffic-hosts-by-hits', _('Top Ten Suspicious Traffic Hosts By Hits'))

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
                ks = KeyStatistic(r[0], r[1], _('hits'),
                                  link_type=reports.HNAME_LINK)
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Host'),
                     ylabel=_('Blocks per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class SpywareSubnetsDetected(Graph):
    def __init__(self):
        Graph.__init__(self, 'suspicious-traffic-detected', _('Suspicious Traffic Detected'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        sd = DateFromMx(start_date)

        query = """\
SELECT date_trunc('day', trunc_time) AS day,
       sum(new_sessions)
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s
AND NOT sw_accesses IS NULL AND sw_access_ident != ''"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query += """
GROUP BY day ORDER BY day ASC"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (sd, ed, host))
            elif user:
                curs.execute(query, (sd, ed, user))
            else:
                curs.execute(query, (sd, ed))

            dates = []
            blocks = []
            blocks = []
            total = 0
            max = 0

            for r in curs.fetchall():
                s = r[1]
                dates.append(r[0])
                blocks.append(s)
                total += s
                if max < s:
                    max = s
        finally:
            conn.commit()


        lks = []

        ks = KeyStatistic(_('avg suspicious detected'), total / report_days,
                          _('/day'))
        lks.append(ks)
        ks = KeyStatistic(_('max suspicious detected'), round(max), _('/day'))
        lks.append(ks)

        rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDelta(1))

        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_('Date'),
                     ylabel=_('Detections per Day'),
                     major_formatter=DATE_FORMATTER,
                     required_points=rp)

        plot.add_dataset(dates, blocks, label=_('detections'))

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
               ColumnDesc('s_server_addr', _('Server IP')),
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
        DetailSection.__init__(self, 'url-events', _('URL Blocklist Events'))

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
               ColumnDesc('uri', _('URI')),
               ColumnDesc('s_server_addr', _('Server IP')),
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
               ColumnDesc('c_server_addr', _('Server IP')),
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
