import gettext
import logging
import mx
import reports.engine
import sql_helper
import string
import sys

from psycopg import DateFromMx
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from sql_helper import print_timing

_ = gettext.gettext
def N_(message): return message

class VirusBaseNode(reports.engine.Node):
    def __init__(self, node_name, vendor_name):
        reports.engine.Node.__init__(self, node_name)
        self.__vendor_name = vendor_name

    def parents(self):
        return ['untangle-casing-http', 'untangle-casing-mail']

    @sql_helper.print_timing
    def setup(self, start_date, end_date):
        self.__update_n_http_events(start_date, end_date)
        self.__update_n_mail_msgs(start_date, end_date)

        ft = reports.engine.get_fact_table('reports.n_http_totals')

        ft.measures.append(Column('viruses_%s_blocked' % self.__vendor_name, 'integer', "count(CASE WHEN virus_%s_clean = false THEN 1 ELSE null END)" % self.__vendor_name))

        ft = reports.engine.get_fact_table('reports.n_mail_msg_totals')

        ft.measures.append(Column('viruses_%s_blocked' % self.__vendor_name, 'integer', "count(CASE WHEN virus_%s_clean = false THEN 1 ELSE null END)" % self.__vendor_name))

        ft = reports.engine.get_fact_table('reports.n_virus_http_totals')

        if not ft:
            ft = FactTable('reports.n_virus_http_totals', 'reports.n_http_events',
                           'time_stamp', [], [])
            reports.engine.register_fact_table(ft)

        ft.dimensions.append(Column('virus_%s_name' % self.__vendor_name,
                                    'text'))
        ft.measures.append(Column('virus_%s_detected' % self.__vendor_name,
                                  'integer', """\
count(CASE WHEN NOT virus_%s_name is null AND virus_%s_name != '' THEN 1 ELSE null END)\
""" % (self.__vendor_name, self.__vendor_name)))

        ft = reports.engine.get_fact_table('reports.n_virus_mail_totals')

        if not ft:
            ft = FactTable('reports.n_virus_mail_totals', 'reports.n_mail_msgs',
                           'time_stamp', [], [])
            reports.engine.register_fact_table(ft)

        ft.dimensions.append(Column('virus_%s_name' % self.__vendor_name,
                                    'text'))
        ft.measures.append(Column('virus_%s_detected' % self.__vendor_name,
                                  'integer', """\
count(CASE WHEN NOT virus_%s_name is null AND virus_%s_name != '' THEN 1 ELSE null END)\
""" % (self.__vendor_name, self.__vendor_name)))

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []

        s = reports.SummarySection('summary', N_('Summary Report'),
                                   [DailyVirusesBlocked(self.__vendor_name),
                                    TopVirusesDetected(self.__vendor_name),
                                    HourlyVirusesBlocked(self.__vendor_name),
                                    TopEmailVirusesDetected(self.__vendor_name),
                                    TopWebVirusesDetected(self.__vendor_name)])
        sections.append(s)

        sections.append(VirusDetail(self.__vendor_name))

        return reports.Report(self.name, sections)

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
        pass

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
  AND reports.n_http_events.request_id = events.n_virus_evt_http.request_line and  events.n_virus_evt_http.vendor_name = %s""", (sd, ed, string.capwords(self.__vendor_name)), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_http_events[%s]' % self.name, ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @sql_helper.print_timing
    def __update_n_mail_msgs(self, start_date, end_date):
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_mail_msgs ADD COLUMN virus_%s_clean boolean""" % self.__vendor_name)
        except: pass
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_mail_msgs ADD COLUMN virus_%s_name text""" % self.__vendor_name)
        except: pass


        sd = DateFromMx(sql_helper.get_update_info('n_mail_msgs[%s]' % self.name, start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()

        try:
            sql_helper.run_sql("""\
UPDATE reports.n_mail_msgs
SET virus_"""+self.__vendor_name+"""_clean = clean,
  virus_"""+self.__vendor_name+"""_name = virus_name
FROM events.n_virus_evt_mail
WHERE reports.n_mail_msgs.time_stamp >= %s
  AND reports.n_mail_msgs.time_stamp < %s
  AND reports.n_mail_msgs.msg_id = events.n_virus_evt_mail.msg_id and events.n_virus_evt_mail.vendor_name = %s""", (sd, ed, string.capwords(self.__vendor_name)), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_mail_msgs[%s]' % self.name, ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

        try:
            sql_helper.run_sql("""\
UPDATE reports.n_mail_msgs
SET virus_"""+self.__vendor_name+"""_clean = clean,
  virus_"""+self.__vendor_name+"""_name = virus_name
FROM events.n_virus_evt_smtp
WHERE reports.n_mail_msgs.time_stamp >= %s
  AND reports.n_mail_msgs.time_stamp < %s
  AND reports.n_mail_msgs.msg_id = events.n_virus_evt_smtp.msg_id and events.n_virus_evt_smtp.vendor_name = %s""", (sd, ed, string.capwords(self.__vendor_name)), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_mail_msgs[%s]' % self.name, ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class VirusDetail(reports.DetailSection):
    def __init__(self, vendor_name):
        reports.DetailSection.__init__(self, 'incidents', N_('Incident Report'))
        self.__vendor_name = vendor_name

    def get_columns(self, host=None, user=None, email=None):
        rv = [reports.ColumnDesc('time_stamp', N_('Time'), 'Date')]

        if not host:
            rv.append(reports.ColumnDesc('hname', N_('Client'), 'HostLink'))
        if not user:
            rv.append(reports.ColumnDesc('uid', N_('User'), 'UserLink'))

        rv = rv + [reports.ColumnDesc('url', N_('URL'), 'URL')]

        return rv


    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        sql = "SELECT time_stamp, "

        if not host:
            sql = sql + "hname, "
        if not user:
            sql = sql + "uid, "

        sql = sql + ("""'http://' || host || uri
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s
      AND virus_%s_clean = false""" % (DateFromMx(start_date),
                                       DateFromMx(end_date),
                                       self.__vendor_name))

        if host:
            sql = sql + (" AND host = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND host = %s" % QuotedString(user))

        return sql


class DailyVirusesBlocked(reports.Graph):
    def __init__(self, vendor_name):
        reports.Graph.__init__(self, 'daily-viruses-blocked', _('Daily Viruses Blocked'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """SELECT avg(viruses_"""+self.__vendor_name+"""_blocked), max(viruses_"""+self.__vendor_name+"""_blocked) FROM (("""

        #if you add a reports table you should also update the tuple in execute below
        avg_max_query = avg_max_query + """\
select date_trunc('day', trunc_time) AS day, sum(viruses_"""+self.__vendor_name+"""_blocked) AS viruses_"""+self.__vendor_name+"""_blocked
      FROM reports.n_http_totals
      WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"

        avg_max_query = avg_max_query + " GROUP BY day)"
        avg_max_query = avg_max_query + " UNION ("

        avg_max_query = avg_max_query + """\
select date_trunc('day', trunc_time) AS day, sum(viruses_"""+self.__vendor_name+"""_blocked) AS viruses_"""+self.__vendor_name+"""_blocked
      FROM reports.n_mail_msg_totals
      WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"

        avg_max_query = avg_max_query + " GROUP BY day)) AS foo"


        conn = sql_helper.get_connection()

        lks = []

        curs = conn.cursor()
        if host:
            curs.execute(avg_max_query, (one_week, ed, host, one_week, ed, host))
        elif user:
            curs.execute(avg_max_query, (one_week, ed, user, one_week, ed, user))
        else:
            curs.execute(avg_max_query, (one_week, ed, one_week, ed))
        r = curs.fetchone()
        ks = reports.KeyStatistic(N_('max (1-week)'), r[1], N_('viruses/day'))
        lks.append(ks)
        ks = reports.KeyStatistic(N_('avg (1-week)'), r[0], N_('viruses/day'))
        lks.append(ks)

        conn.commit()

        return lks

    @sql_helper.print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()

        q = """\
SELECT date_trunc('day', trunc_time) AS time,
       sum(viruses_"""+self.__vendor_name+"""_blocked) as viruses_"""+self.__vendor_name+"""_blocked
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
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

        blocks_by_date = {}

        while 1:
            r = curs.fetchone()
            if not r:
                break

            blocks_by_date[r[0]] = r[1]

        conn.commit()

        conn = sql_helper.get_connection()

        q = """\
SELECT date_trunc('day', trunc_time) AS time,
       sum(viruses_"""+self.__vendor_name+"""_blocked) as viruses_"""+self.__vendor_name+"""_blocked
FROM reports.n_mail_msg_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
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

        while 1:
            r = curs.fetchone()
            if not r:
                break

            if blocks_by_date.has_key(r[0]):
                blocks_by_date[r[0]] =  blocks_by_date[r[0]]+r[1]
            else:
                blocks_by_date[r[0]] = r[1]

        conn.commit()

        dates = []
        blocks = []
        date_list = blocks_by_date.keys()
        date_list.sort()
        for k in date_list:
            dates.append(k)
            blocks.append(blocks_by_date[k])

        plot = reports.Chart(type=reports.STACKED_BAR_CHART,
                     title=_('Daily Virus Blocked'),
                     xlabel=_('Day'),
                     ylabel=_('viruses/day'),
                     major_formatter=reports.DATE_FORMATTER)

        plot.add_dataset(dates, blocks, label=_('viruses blocked'))

        return plot

class HourlyVirusesBlocked(reports.Graph):
    def __init__(self, vendor_name):
        reports.Graph.__init__(self, 'hourly-viruses-blocked', _('Hourly Viruses Blocked'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """SELECT avg(viruses_"""+self.__vendor_name+"""_blocked), max(viruses_"""+self.__vendor_name+"""_blocked) FROM (("""

        #if you add a reports table you should also update the tuple in execute below
        avg_max_query = avg_max_query + """\
select date_trunc('hour', trunc_time) AS day, sum(viruses_"""+self.__vendor_name+"""_blocked) AS viruses_"""+self.__vendor_name+"""_blocked
      FROM reports.n_http_totals
      WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"

        avg_max_query = avg_max_query + " GROUP BY day)"
        avg_max_query = avg_max_query + " UNION ("

        avg_max_query = avg_max_query + """\
select date_trunc('hour', trunc_time) AS day, sum(viruses_"""+self.__vendor_name+"""_blocked) AS viruses_"""+self.__vendor_name+"""_blocked
      FROM reports.n_mail_msg_totals
      WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"

        avg_max_query = avg_max_query + " GROUP BY day)) AS foo"


        conn = sql_helper.get_connection()

        lks = []

        curs = conn.cursor()
        if host:
            curs.execute(avg_max_query, (one_week, ed, host, one_week, ed, host))
        elif user:
            curs.execute(avg_max_query, (one_week, ed, user, one_week, ed, user))
        else:
            curs.execute(avg_max_query, (one_week, ed, one_week, ed))
        r = curs.fetchone()
        ks = reports.KeyStatistic(N_('max (1-week)'), r[1], N_('viruses/hour'))
        lks.append(ks)
        ks = reports.KeyStatistic(N_('avg (1-week)'), r[0], N_('viruses/hour'))
        lks.append(ks)

        conn.commit()

        return lks

    @sql_helper.print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()

        q = """\
SELECT date_trunc('hour', trunc_time) AS time,
       sum(viruses_"""+self.__vendor_name+"""_blocked) as viruses_"""+self.__vendor_name+"""_blocked
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
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

        blocks_by_date = {}

        while 1:
            r = curs.fetchone()
            if not r:
                break

            blocks_by_date[r[0]] = r[1]

        conn.commit()

        conn = sql_helper.get_connection()

        q = """\
SELECT date_trunc('hour', trunc_time) AS time,
       sum(viruses_"""+self.__vendor_name+"""_blocked) as viruses_"""+self.__vendor_name+"""_blocked
FROM reports.n_mail_msg_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
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

        while 1:
            r = curs.fetchone()
            if not r:
                break

            if blocks_by_date.has_key(r[0]):
                blocks_by_date[r[0]] =  blocks_by_date[r[0]]+r[1]
            else:
                blocks_by_date[r[0]] = r[1]

        conn.commit()

        dates = []
        blocks = []
        date_list = blocks_by_date.keys()
        date_list.sort()
        for k in date_list:
            dates.append(k)
            blocks.append(blocks_by_date[k])

        plot = reports.Chart(type=reports.STACKED_BAR_CHART,
                     title=_('Hourly Virus Blocked'),
                     xlabel=_('hour'),
                     ylabel=_('viruses/hour'),
                     major_formatter=reports.TIME_OF_DAY_FORMATTER)

        plot.add_dataset(dates, blocks, label=_('viruses blocked'))

        return plot


class TopWebVirusesDetected(reports.Graph):
    def __init__(self, vendor_name):
        reports.Graph.__init__(self, 'top-web-viruses-detected', _('Top Web Viruses Detected'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        lks = []
        return lks

    @sql_helper.print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()

        q = """\
SELECT virus_"""+self.__vendor_name+"""_name, virus_"""+self.__vendor_name+"""_detected
FROM reports.n_virus_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            q = q + " AND hname = %s"
        elif user:
            q = q + " AND uid = %s"
        q = q + "ORDER BY virus_"+self.__vendor_name+"_detected DESC"

        curs = conn.cursor()

        if host:
            curs.execute(q, (one_week, ed, host))
        elif user:
            curs.execute(q, (one_week, ed, user))
        else:
            curs.execute(q, (one_week, ed))

        dataset = {}

        while 1:
            r = curs.fetchone()
            if not r:
                break

            key_name = r[0]
            if key_name is None or len(key_name) == 0 or key_name == 'unknown':
                key_name = _('unknown')
            dataset[str(key_name)] = r[1]

        conn.commit()

        plot = reports.Chart(type=reports.PIE_CHART,
                     title=_('Top Web Viruses Detected'),
                     xlabel=_('name'),
                     ylabel=_('count'))

        plot.add_pie_dataset(dataset)

        return plot


class TopEmailVirusesDetected(reports.Graph):
    def __init__(self, vendor_name):
        reports.Graph.__init__(self, 'top-email-viruses-detected', _('Top Email Viruses Detected'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        if host or user:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """\
SELECT virus_%s_name, virus_%s_detected
FROM reports.n_virus_mail_totals
WHERE NOT virus_%s_name IS NULL AND virus_%s_name != ''
      AND trunc_time >= %%s AND trunc_time < %%s""" \
            % (4 * (self.__vendor_name,))
        if host:
            avg_max_query += " AND hname = %s"
        elif user:
            avg_max_query += " AND uid = %s"
        avg_max_query += " ORDER BY virus_%s_detected DESC" % self.__vendor_name

        conn = sql_helper.get_connection()

        lks = []

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
            ks = reports.KeyStatistic(r[0], r[1], N_('viruses'))
            lks.append(ks)

        conn.commit()

        return lks

    @sql_helper.print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        if host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()

        avg_max_query = """
SELECT virus_"""+self.__vendor_name+"""_name, virus_"""+self.__vendor_name+"""_detected
FROM reports.n_virus_mail_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"
        avg_max_query = avg_max_query + " ORDER BY virus_"+self.__vendor_name+"_detected DESC"

        curs = conn.cursor()

        if host:
            curs.execute(avg_max_query, (one_week, ed, host))
        elif user:
            curs.execute(avg_max_query, (one_week, ed, user))
        else:
            curs.execute(avg_max_query, (one_week, ed))

        dataset = {}

        for r in curs.fetchall():
            dataset[r[0]] = r[1]

        plot = reports.Chart(type=reports.PIE_CHART,
                             title=_('Top Email Viruses Detected'),
                             xlabel=_('Viruses'),
                             ylabel=_('Count'))

        plot.add_pie_dataset(dataset)

        return plot

class TopVirusesDetected(reports.Graph):
    def __init__(self, vendor_name):
        reports.Graph.__init__(self, 'top-viruses-detected', _('Top Viruses Detected'))
        self.__vendor_name = vendor_name

    @sql_helper.print_timing
    def get_key_statistics(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        avg_max_query = """\
SELECT m.virus_"""+self.__vendor_name+"""_name as name,
       (h.virus_"""+self.__vendor_name+"""_detected + m.virus_"""+self.__vendor_name+"""_detected) AS foo
FROM reports.n_virus_mail_totals AS m, reports.n_virus_http_totals AS h
WHERE m.trunc_time >= %s AND m.trunc_time < %s
AND  h.trunc_time >= %s AND h.trunc_time < %s
AND h.virus_"""+self.__vendor_name+"""_name = m.virus_"""+self.__vendor_name+"""_name
AND m.virus_"""+self.__vendor_name+"""_name != ''"""

        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"

        conn = sql_helper.get_connection()

        lks = []

        curs = conn.cursor()
        if host:
            curs.execute(avg_max_query, (one_week, ed, one_week, ed, host))
        elif user:
            curs.execute(avg_max_query, (one_week, ed, one_week, ed, user))
        else:
            curs.execute(avg_max_query, (one_week, ed, one_week, ed))

        while 1:
            r = curs.fetchone()
            if not r:
                break
            ks = reports.KeyStatistic(r[0], r[1], N_('viruses'))
            lks.append(ks)

        conn.commit()

        return lks

    @sql_helper.print_timing
    def get_plot(self, end_date, report_days, host=None, user=None, email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        conn = sql_helper.get_connection()

        avg_max_query = """\
SELECT m.virus_"""+self.__vendor_name+"""_name as name, count(*) AS foo
FROM reports.n_virus_mail_totals AS m, reports.n_virus_http_totals AS h
WHERE m.trunc_time >= %s AND m.trunc_time < %s
AND  h.trunc_time >= %s AND h.trunc_time < %s
AND h.virus_"""+self.__vendor_name+"""_name = m.virus_"""+self.__vendor_name+"""_name
AND m.virus_"""+self.__vendor_name+"""_name != ''"""

        if host:
            avg_max_query = avg_max_query + " AND hname = %s"
        elif user:
            avg_max_query = avg_max_query + " AND uid = %s"

        avg_max_query = avg_max_query + " GROUP BY name"

        conn = sql_helper.get_connection()

        lks = []

        curs = conn.cursor()
        if host:
            curs.execute(avg_max_query, (one_week, ed, one_week, ed, host))
        elif user:
            curs.execute(avg_max_query, (one_week, ed, one_week, ed, user))
        else:
            curs.execute(avg_max_query, (one_week, ed, one_week, ed))

        dataset = {}

        for r in curs.fetchall():
            dataset[r[0]] = r[1]

        plot = reports.Chart(type=reports.PIE_CHART,
                             title=_('Top Email Viruses Detected'),
                             xlabel=_('Viruses'),
                             ylabel=_('Count'))

        plot.add_pie_dataset(dataset)

        return plot
