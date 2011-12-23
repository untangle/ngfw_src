import gettext
import logging
import mx
import reports.colors as colors
import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper
import string
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
from reports.sql_helper import print_timing

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
                                    TopVirusesDetected(self.__vendor_name),
                                    TopEmailVirusesDetected(self.__vendor_name),
                                    TopWebVirusesDetected(self.__vendor_name)])
        sections.append(s)

        sections.append(VirusWebDetail(self.__vendor_name))
        sections.append(VirusMailDetail(self.__vendor_name))

        return Report(self, sections)

    def events_cleanup(self, cutoff, safety_margin):
        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_http
WHERE request_line IN (SELECT request_id FROM reports.n_http_events)""")

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_http
WHERE (time_stamp < %s - interval %s)""", (cutoff, safety_margin))

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_mail 
WHERE msg_id IN (SELECT msg_id FROM reports.n_mail_msgs)""")

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_mail 
WHERE (time_stamp < %s - interval %s)""", (cutoff, safety_margin))

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_smtp
WHERE msg_id IN (SELECT msg_id FROM reports.n_mail_addrs)""")

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt_smtp
WHERE (time_stamp < %s - interval %s)""", (cutoff, safety_margin))

        sql_helper.run_sql("""\
DELETE FROM events.n_virus_evt WHERE time_stamp < %s""", (cutoff,))

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table('n_virus_http_totals', cutoff)
        sql_helper.drop_fact_table('n_virus_mail_totals', cutoff)        

    @sql_helper.print_timing
    def __update_n_http_events(self, start_date, end_date):
        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.n_http_events
SET virus_"""+self.__vendor_name+"""_clean = clean,
  virus_"""+self.__vendor_name+"""_name = virus_name
FROM events.n_virus_evt_http
WHERE reports.n_http_events.request_id = events.n_virus_evt_http.request_line 
AND events.n_virus_evt_http.vendor_name = %s""",
                               (string.capwords(self.__vendor_name),), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @sql_helper.print_timing
    def __update_n_mail_table(self, tablename, start_date, end_date):
        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.%s
SET virus_%s_clean = clean,
  virus_%s_name = virus_name
FROM events.n_virus_evt_mail
WHERE reports.%s.msg_id = events.n_virus_evt_mail.msg_id
AND events.n_virus_evt_mail.vendor_name = %%s""" %
                               (tablename, self.__vendor_name, self.__vendor_name, tablename),
                               (string.capwords(self.__vendor_name),),
                               connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

        try:
            sql_helper.run_sql("""\
UPDATE reports.%s
SET virus_%s_clean = clean,
  virus_%s_name = virus_name
FROM events.n_virus_evt_smtp
WHERE reports.%s.msg_id = events.n_virus_evt_smtp.msg_id
AND events.n_virus_evt_smtp.vendor_name = %%s""" %
                               (tablename, self.__vendor_name, self.__vendor_name, tablename),
                               (string.capwords(self.__vendor_name),),
                               connection=conn, auto_commit=False)
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
        Graph.__init__(self, 'viruses-blocked', _('Viruses Blocked'))
        self.__vendor_name = vendor_name

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

            sums = ["COALESCE(SUM(viruses_%(vendor)s_blocked), 0)::int" %
                     { "vendor" : self.__vendor_name }]

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

            viruses = {}
            
            for r in curs.fetchall():
                viruses[r[0]] = r[1]

            q, h = sql_helper.get_averaged_query(sums, "reports.n_mail_msg_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            for r in curs.fetchall():
                if r[0] in viruses:
                    viruses[r[0]] += r[1]
                else:
                    viruses[r[0]] = r[1]

            dates = []
            blocks = []

            date_list = viruses.keys()
            date_list.sort()
            for k in date_list:
                dates.append(k)
                blocks.append(viruses[k])

            if not blocks:
                blocks = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Avg Viruses Blocked'),
                              sum(blocks) / len(rp),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Viruses Blocked'), max(blocks),
                              _('Blocks')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Blocks'),
                         major_formatter=formatter,
                         required_points=rp)

            plot.add_dataset(dates, blocks, label=_('Viruses'))

        finally:
            conn.commit()

        return (lks, plot)

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
                    ks = KeyStatistic(str(key_name), r[1], _('Viruses'))
                    lks.append(ks)
                    dataset[str(key_name)] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Name'),
                     ylabel=_('Count'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

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
                ks = KeyStatistic(r[0], r[1], _('Viruses'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Viruses'),
                     ylabel=_('Count'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

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

                ks = KeyStatistic(key_name, r[1], _('Viruses'))
                lks.append(ks)
                dataset[key_name] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Viruses'),
                     ylabel=_('Count'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

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
               ColumnDesc('url', _('Url'), 'URL'),
               ColumnDesc('s_server_addr', _('Server Ip')),
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
               ColumnDesc('c_client_addr', _('Client Ip')),
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
