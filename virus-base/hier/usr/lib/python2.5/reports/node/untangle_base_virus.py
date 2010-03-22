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
import reports.colors as colors
import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper
import string
import sys

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
from reports import TIMESTAMP_FORMATTER
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import EMAIL_DRILLDOWN
from reports.engine import FactTable
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-base-virus').lgettext

class VirusBaseNode(Node):
    def __init__(self, node_name, vendor_name):
        Node.__init__(self, node_name)
        self.__vendor_name = vendor_name

    def parents(self):
        return ['untangle-casing-http', 'untangle-casing-mail']

    @sql_helper.print_timing
    def setup(self, start_date, end_date):
        self.__update_n_http_events(start_date, end_date)
        self.__update_n_mail_table('n_mail_msgs', start_date, end_date)
        self.__update_n_mail_table('n_mail_addrs', start_date, end_date)

        ft = reports.engine.get_fact_table('reports.n_http_totals')

        ft.measures.append(Column('viruses_%s_blocked' % self.__vendor_name,
                                  'integer',
                                  """\
count(CASE WHEN virus_%s_clean IS NULL OR virus_%s_clean THEN null ELSE 1 END)
""" % (2 * (self.__vendor_name,))))

        ft = reports.engine.get_fact_table('reports.n_mail_msg_totals')

        ft.measures.append(Column('viruses_%s_blocked' % self.__vendor_name,
                                  'integer', """\
count(CASE WHEN virus_%s_clean IS NULL OR virus_%s_clean THEN null ELSE 1 END)
""" % (2 * (self.__vendor_name,))))

        ft = reports.engine.get_fact_table('reports.n_virus_http_totals')

        if not ft:
            ft = FactTable('reports.n_virus_http_totals', 'reports.n_http_events',
                           'time_stamp', [Column('hname', 'text'),
                                          Column('uid', 'text')], [])
            reports.engine.register_fact_table(ft)

        ft.dimensions.append(Column('virus_%s_name' % self.__vendor_name,
                                    'text'))
        ft.measures.append(Column('virus_%s_detected' % self.__vendor_name,
                                  'integer',
                                  """\
count(CASE WHEN virus_%s_clean IS NULL OR virus_%s_clean THEN null ELSE 1 END)
""" % (2 * (self.__vendor_name,))))

        ft = reports.engine.get_fact_table('reports.n_virus_mail_totals')

        if not ft:
            ft = FactTable('reports.n_virus_mail_totals', 'reports.n_mail_msgs',
                           'time_stamp', [Column('uid', 'text')], [])
            reports.engine.register_fact_table(ft)

        ft.dimensions.append(Column('virus_%s_name' % self.__vendor_name,
                                    'text'))
        ft.measures.append(Column('virus_%s_detected' % self.__vendor_name,
                                  'integer',
                                  """\
count(CASE WHEN virus_%s_clean IS NULL OR virus_%s_clean THEN null ELSE 1 END)
""" % (2 * (self.__vendor_name,))))

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN, EMAIL_DRILLDOWN]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                                   [VirusHighlight(self.name, self.__vendor_name),
                                    DailyVirusesBlocked(self.__vendor_name),
                                    HourlyVirusesBlocked(self.__vendor_name),
                                    TopVirusesDetected(self.__vendor_name),
                                    TopEmailVirusesDetected(self.__vendor_name),
                                    TopWebVirusesDetected(self.__vendor_name)])
        sections.append(s)

        sections.append(VirusWebDetail(self.__vendor_name))
        sections.append(VirusMailDetail(self.__vendor_name))

        return Report(self, sections)

    def events_cleanup(self, cutoff):
        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_http WHERE time_stamp < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_mail WHERE time_stamp < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_smtp WHERE time_stamp < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt WHERE time_stamp < %s""", (cutoff,))

    def reports_cleanup(self, cutoff):
        sql_helper.drop_partitioned_table('n_virus_http_totals', cutoff)
        sql_helper.drop_partitioned_table('n_virus_mail_totals', cutoff)        

    @sql_helper.print_timing
    def __update_n_http_events(self, start_date, end_date):
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN virus_%s_clean boolean""" \
                                   % self.__vendor_name)
        except: pass
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN virus_%s_name text""" \
                                   % self.__vendor_name)
        except: pass

        sd = DateFromMx(sql_helper.get_update_info('n_http_events[%s]' % self.name, start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.n_http_events
SET virus_"""+self.__vendor_name+"""_clean = clean,
  virus_"""+self.__vendor_name+"""_name = virus_name
FROM events.n_virus_evt_http
WHERE reports.n_http_events.time_stamp >= %s
  AND reports.n_http_events.time_stamp < %s
  AND reports.n_http_events.request_id = events.n_virus_evt_http.request_line and events.n_virus_evt_http.vendor_name = %s""", (sd, ed, string.capwords(self.__vendor_name)), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_http_events[%s]' % self.name, ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @sql_helper.print_timing
    def __update_n_mail_table(self, tablename, start_date, end_date):
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.%s ADD COLUMN virus_%s_clean boolean""" % (tablename, self.__vendor_name))
        except: pass
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.%s ADD COLUMN virus_%s_name text""" % (tablename, self.__vendor_name))
        except: pass

        update_name = 'report.%s-mail[%s]' % (tablename, self.name)
        sd = DateFromMx(sql_helper.get_update_info(update_name, start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.%s
SET virus_%s_clean = clean,
  virus_%s_name = virus_name
FROM events.n_virus_evt_mail
WHERE reports.%s.time_stamp >= %%s
  AND reports.%s.time_stamp < %%s
  AND reports.%s.msg_id = events.n_virus_evt_mail.msg_id
  AND events.n_virus_evt_mail.vendor_name = %%s
""" % (tablename, self.__vendor_name, self.__vendor_name, tablename, tablename,
       tablename),
                               (sd, ed, string.capwords(self.__vendor_name)),
                               connection=conn, auto_commit=False)

            sql_helper.set_update_info(update_name, ed, connection=conn,
                                       auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

        try:
            update_name = 'report.%s-smtp[%s]' % (tablename, self.name)
            sd = DateFromMx(sql_helper.get_update_info(update_name, start_date))

            sql_helper.run_sql("""\
UPDATE reports.%s
SET virus_%s_clean = clean,
  virus_%s_name = virus_name
FROM events.n_virus_evt_smtp
WHERE reports.%s.time_stamp >= %%s
  AND reports.%s.time_stamp < %%s
  AND reports.%s.msg_id = events.n_virus_evt_smtp.msg_id
  AND events.n_virus_evt_smtp.vendor_name = %%s
""" % (tablename, self.__vendor_name, self.__vendor_name, tablename, tablename,
       tablename),
                               (sd, ed, string.capwords(self.__vendor_name)),
                               connection=conn, auto_commit=False)

            sql_helper.set_update_info(update_name, ed, connection=conn,
                                       auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class VirusHighlight(Highlight):
    def __init__(self, name, vendor_name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("scanned") + " " + "%(documents)s" + " " +
                           _("documents and detected and blocked") + " " +
                           "%(viruses)s" + " " + _("viruses"))
        self.__vendor_name = vendor_name

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        # FIXME: doing it twice is nasty...
        query_web = """
SELECT COALESCE(sum(hits), 0)::int AS documents,
       COALESCE(sum(viruses_%s_blocked), 0)::int AS viruses
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
""" % (self.__vendor_name,)
        query_mail = """
SELECT COALESCE(sum(msgs), 0)::int AS documents,
       COALESCE(sum(viruses_%s_blocked), 0)::int AS viruses
FROM reports.n_mail_msg_totals
WHERE trunc_time >= %%s AND trunc_time < %%s
""" % (self.__vendor_name,)

        if host:
            query_web += " AND hname = %s"
            query_mail += " AND hname = %s"
        elif user:
            query_web += " AND uid = %s"
            query_mail += " AND uid = %s"

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        h2 = {}
        try:
            if host:
                curs.execute(query_web, (one_week, ed, host))
            elif user:
                curs.execute(query_web, (one_week, ed, user))
            else:
                curs.execute(query_web, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)

            if host:
                curs.execute(query_mail, (one_week, ed, host))
            elif user:
                curs.execute(query_mail, (one_week, ed, user))
            else:
                curs.execute(query_mail, (one_week, ed))

            h2 = sql_helper.get_result_dictionary(curs)

        finally:
            conn.commit()

        for k in h:
            h[k] += h2[k]
            
        return h

class DailyVirusesBlocked(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'daily-viruses-blocked', _('Daily Viruses Blocked'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """\
SELECT COALESCE(sum(viruses_%s_blocked), 0)::int / %%s,
       max(viruses_%s_blocked)::int
FROM ((""" % (2 * (self.__vendor_name,))

        # if you add a reports table you should also update the tuple
        # in execute below
        avg_max_query += """\
SELECT date_trunc('day', trunc_time) AS day,
       COALESCE(sum(viruses_%s_blocked), 0)::int AS viruses_%s_blocked
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))

        if host:
            avg_max_query += " AND hname = %s"
        elif user:
            avg_max_query += " AND uid = %s"

        avg_max_query += """
GROUP BY day)

UNION (

SELECT date_trunc('day', trunc_time) AS day,
       COALESCE(sum(viruses_%s_blocked), 0)::int AS viruses_%s_blocked
FROM reports.n_mail_msg_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))

        if host:
            avg_max_query += " AND hname = %s"
        elif user:
            avg_max_query += " AND uid = %s"

        avg_max_query += " GROUP BY day)) AS foo"

        conn = sql_helper.get_connection()
        try:
            lks = []

            curs = conn.cursor()
            if host:
                curs.execute(avg_max_query, (report_days, one_week, ed, host,
                                             one_week, ed, host))
            elif user:
                curs.execute(avg_max_query, (report_days, one_week, ed, user,
                                             one_week, ed, user))
            else:
                curs.execute(avg_max_query, (report_days, one_week, ed,
                                             one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('Avg'), r[0], _('viruses/day'))
            lks.append(ks)
            ks = KeyStatistic(_('Max'), r[1], _('viruses/day'))
            lks.append(ks)
        finally:
            conn.commit()

        return lks

    @sql_helper.print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        conn = sql_helper.get_connection()
        try:
            q = """\
SELECT date_trunc('day', trunc_time) AS time,
       COALESCE(sum(viruses_%s_blocked), 0)::int as viruses_%s_blocked
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))
            if host:
                q += " AND hname = %s"
            elif user:
                q += " AND uid = %s"
            q += """
GROUP BY time
ORDER BY time asc"""

            curs = conn.cursor()

            if host:
                curs.execute(q, (one_week, ed, host))
            elif user:
                curs.execute(q, (one_week, ed, user))
            else:
                curs.execute(q, (one_week, ed))

            blocks_by_date = {}

            while 1:
                r = curs.fetchone()
                if not r:
                    break

                blocks_by_date[r[0]] = r[1]

            q = """\
SELECT date_trunc('day', trunc_time) AS time,
       COALESCE(sum(viruses_%s_blocked), 0)::int as viruses_%s_blocked
FROM reports.n_mail_msg_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))
            if host:
                q += " AND hname = %s"
            elif user:
                q += " AND uid = %s"
            q += """
GROUP BY time
ORDER BY time asc"""

            curs = conn.cursor()

            if host:
                curs.execute(q, (one_week, ed, host))
            elif user:
                curs.execute(q, (one_week, ed, user))
            else:
                curs.execute(q, (one_week, ed))

            while 1:
                r = curs.fetchone()
                if not r:
                    break

                if blocks_by_date.has_key(r[0]):
                    blocks_by_date[r[0]] += r[1]
                else:
                    blocks_by_date[r[0]] = r[1]
        finally:
            conn.commit()

        dates = []
        blocks = []
        date_list = blocks_by_date.keys()
        date_list.sort()
        for k in date_list:
            dates.append(k)
            blocks.append(blocks_by_date[k])

        rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDelta(1))

        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_('Day'),
                     ylabel=_('viruses/day'),
                     major_formatter=DATE_FORMATTER,
                     required_points=rp)

        plot.add_dataset(dates, blocks, label=_('viruses blocked'),
                         color=colors.badness)

        return plot

class HourlyVirusesBlocked(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'hourly-viruses-blocked', _('Hourly Viruses Blocked'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """
SELECT COALESCE(sum(viruses_%s_blocked), 0) / %%s,
       max(viruses_%s_blocked)
FROM ((""" % (2 * (self.__vendor_name,))

        # if you add a reports table you should also update the tuple
        # in execute below
        avg_max_query += """\
SELECT date_trunc('hour', trunc_time) AS day,
       COALESCE(sum(viruses_%s_blocked), 0)::int AS viruses_%s_blocked
FROM reports.n_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))

        if host:
            avg_max_query += " AND hname = %s"
        elif user:
            avg_max_query += " AND uid = %s"

        avg_max_query += """
GROUP BY day)

UNION (

SELECT date_trunc('hour', trunc_time) AS day,
       COALESCE(sum(viruses_%s_blocked), 0)::int AS viruses_%s_blocked
FROM reports.n_mail_msg_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (2 * (self.__vendor_name,))

        if host:
            avg_max_query += " AND hname = %s"
        elif user:
            avg_max_query += " AND uid = %s"

        avg_max_query += " GROUP BY day)) AS foo"

        conn = sql_helper.get_connection()
        try:
            lks = []

            curs = conn.cursor()
            if host:
                curs.execute(avg_max_query, (report_days, one_week, ed, host,
                                             one_week, ed, host))
            elif user:
                curs.execute(avg_max_query, (report_days, one_week, ed, user,
                                             one_week, ed, user))
            else:
                curs.execute(avg_max_query, (report_days, one_week, ed,
                                             one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('Avg'), r[0], _('viruses/hour'))
            lks.append(ks)
            ks = KeyStatistic(_('Max'), r[1], _('viruses/hour'))
            lks.append(ks)
        finally:
            conn.commit()

        return lks

    @sql_helper.print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        ed = DateFromMx(end_date)
        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)
        one_week = DateFromMx(start_date)

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["coalesce(sum(viruses_%s_blocked), 0)::float * 60 * 60" % (self.__vendor_name)]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_http_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where)
            curs.execute(q, h)

            dates = []
            blocks = []

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                dates.append(r[0])
                blocks.append(r[1])

            sums = ["coalesce(sum(viruses_%s_blocked), 0)::float * 60 * 60" % (self.__vendor_name)]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.n_mail_msg_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where)
            curs.execute(q, h)

            i = 0
            while 1:
                r = curs.fetchone()
                if not r:
                    break
                blocks[i] += r[1]
                i += 1

        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART,
                     title=self.title,
                     xlabel=_('Time'),
                     ylabel=_('Viruses'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, blocks, label=_('viruses blocked'),
                         color=colors.badness)

        return plot

class TopWebVirusesDetected(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-web-viruses-detected', _('Top Web Viruses Detected'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()
        try:
            q = """\
SELECT virus_%s_name,
       COALESCE(sum(virus_%s_detected), 0)::int as virus_%s_detected
FROM reports.n_virus_http_totals
WHERE trunc_time >= %%s AND trunc_time < %%s""" % (3 * (self.__vendor_name,))
            if host:
                q += " AND hname = %s"
            elif user:
                q += " AND uid = %s"
            q += """
GROUP BY virus_%s_name
ORDER BY virus_%s_detected DESC
""" % (self.__vendor_name, self.__vendor_name)

            curs = conn.cursor()

            if host:
                curs.execute(q, (one_week, ed, host))
            elif user:
                curs.execute(q, (one_week, ed, user))
            else:
                curs.execute(q, (one_week, ed))

            lks = []
            dataset = {}

            while 1:
                r = curs.fetchone()
                if not r:
                    break

                key_name = r[0]
                if not key_name or key_name == '':
                    key_name = _('Unknown')
                if r[1] > 0:
                    ks = KeyStatistic(str(key_name), r[1], _('viruses'))
                    lks.append(ks)
                    dataset[str(key_name)] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('name'),
                     ylabel=_('count'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopEmailVirusesDetected(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-email-viruses-detected', _('Top Email Viruses Detected'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """\
SELECT virus_%s_name,
       COALESCE(sum(virus_%s_detected), 0)::int as virus_%s_detected
FROM reports.n_virus_mail_totals
WHERE NOT virus_%s_name IS NULL AND virus_%s_name != ''
      AND trunc_time >= %%s AND trunc_time < %%s""" \
            % (5 * (self.__vendor_name,))

        avg_max_query += """
GROUP BY virus_%s_name
ORDER BY virus_%s_detected DESC
""" % (2 * (self.__vendor_name,))

        conn = sql_helper.get_connection()
        try:
            lks = []
            dataset = {}

            curs = conn.cursor()
            if host:
                curs.execute(avg_max_query, (one_week, ed, host))
            elif user:
                curs.execute(avg_max_query, (one_week, ed, user))
            else:
                curs.execute(avg_max_query, (one_week, ed))

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                ks = KeyStatistic(r[0], r[1], _('viruses'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Viruses'),
                     ylabel=_('Count'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class TopVirusesDetected(Graph):
    def __init__(self, vendor_name):
        Graph.__init__(self, 'top-viruses-detected', _('Top Viruses Detected'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):

        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """\
SELECT name, sum(sum)
FROM (SELECT virus_%s_name AS name,
             COALESCE(sum(virus_%s_detected), 0)::int AS sum
      FROM reports.n_virus_mail_totals
      WHERE  trunc_time >= %%s AND trunc_time < %%s AND virus_%s_detected > 0
""" % (3 * (self.__vendor_name,))

        avg_max_query += """
      GROUP BY virus_%s_name

      UNION

      SELECT virus_%s_name AS name,
             COALESCE(sum(virus_%s_detected), 0)::int AS sum
      FROM reports.n_virus_http_totals
      WHERE trunc_time >= %%s AND trunc_time < %%s AND virus_%s_detected > 0
""" % (4 * (self.__vendor_name,))

        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"

        avg_max_query += """
      GROUP BY virus_%s_name) AS foo
GROUP BY name
ORDER BY sum DESC""" % self.__vendor_name

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()
            if host:
                curs.execute(avg_max_query, (one_week, ed, one_week, ed, host))
            elif user:
                curs.execute(avg_max_query, (one_week, ed, one_week, ed, user))
            else:
                curs.execute(avg_max_query, (one_week, ed, one_week, ed))

            lks = []
            dataset = {}

            while 1:
                r = curs.fetchone()
                if not r:
                    break
                key_name = r[0]
                if not key_name or key_name == '':
                    key_name = _('Unknown')

                ks = KeyStatistic(key_name, r[1], _('viruses'))
                lks.append(ks)
                dataset[key_name] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Viruses'),
                     ylabel=_('Count'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks[0:10], plot)

class VirusWebDetail(DetailSection):
    def __init__(self, vendor_name):
        DetailSection.__init__(self, 'web-events',
                                       _('Web Events'))
        self.__vendor_name = vendor_name

    def get_columns(self, host=None, user=None, email=None):
        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hname', _('Client')))
        else:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('uid', _('User')))
        else:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))

        rv += [ColumnDesc('virus_%s_name' % (self.__vendor_name), _('Virus Name')),
               ColumnDesc('url', _('URL'), 'URL'),
               ColumnDesc('s_server_addr', _('Server IP')),
               ColumnDesc('s_server_port', _('Server Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        sql = """\
SELECT time_stamp, hname, uid, virus_%s_name as virus_ident, 'http://' || host || uri as url,
       host(s_server_addr), s_server_port
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s
AND NOT virus_%s_clean
""" % (self.__vendor_name, DateFromMx(start_date), DateFromMx(end_date),
       self.__vendor_name)

        if host:
            sql = sql + (" AND hname = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND uid = %s" % QuotedString(user))

        return sql + " ORDER BY time_stamp DESC"

class VirusMailDetail(DetailSection):
    def __init__(self, vendor_name):
        DetailSection.__init__(self, 'mail-events',
                                       _('Mail Events'))
        self.__vendor_name = vendor_name

    def get_columns(self, host=None, user=None, email=None):
        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hname', _('Client')))
        else:
            rv.append(ColumnDesc('hname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('uid', _('User')))
        else:
            rv.append(ColumnDesc('uid', _('User'), 'UserLink'))

        rv += [ColumnDesc('virus_%s_name' % (self.__vendor_name,), _('Virus Name')),
               ColumnDesc('subject', _('Subject')),
               ColumnDesc('addr', _('Recipient'), 'EmailLink'), # FIXME: or is it sender ?
               ColumnDesc('c_client_addr', _('Client IP')),
               ColumnDesc('c_client_port', _('Client Port'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        sql = """\
SELECT time_stamp, hname, uid, virus_%s_name, subject, addr,
       host(c_client_addr), c_client_port
FROM reports.n_mail_addrs
WHERE time_stamp >= %s AND time_stamp < %s AND addr_kind = 'T'
AND NOT virus_%s_clean
""" % (self.__vendor_name, DateFromMx(start_date), DateFromMx(end_date),
       self.__vendor_name)

        if host:
            sql = sql + (" AND hname = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND uid = %s" % QuotedString(user))
        if email:
            sql = sql + (" AND addr = %s" % QuotedString(email))
            
        return sql + " ORDER BY time_stamp DESC"
