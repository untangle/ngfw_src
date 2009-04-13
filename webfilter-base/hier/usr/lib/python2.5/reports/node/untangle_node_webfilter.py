import gettext
import mx
import reports.engine
import sql_helper

from psycopg import DateFromMx
from psycopg import QuotedString
from reports import ColumnDesc
from reports import DetailSection
from reports import EVEN_HOURS_OF_A_DAY
from reports import Graph
from reports import KeyStatistic
from reports import LinePlot
from reports import Report
from reports import SummarySection
from reports import TIME_OF_DAY_FORMATTER
from reports.engine import Column
from reports.engine import Node
from sql_helper import print_timing

_ = gettext.gettext
def N_(message): return message

class WebFilterBaseNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-base-webfilter')

    def parents(self):
        return ['untangle-casing-http']

    @print_timing
    def setup(self, start_date, end_date):
        self.__update_n_http_events(start_date, end_date)

        ft = reports.engine.get_fact_table('reports.n_http_totals')

        ft.measures.append(Column('blocks', 'integer',
                                  "count(case when webfilter_action = 'p' then 1 else null end)"))

    def get_report(self):
        sections = []

        s = SummarySection('summary', N_('Summary Report'), [HourlyWebUsage()])
        sections.append(s)

        sections.append(WebFilterDetail())

        return Report(self.name, 'Web Filter', sections)

    def teardown(self):
        print "teardown"

    @print_timing
    def __update_n_http_events(self, start_date, end_date):
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN webfilter_action character(1)""")
        except: pass
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN webfilter_reason character(1)""")
        except: pass
        try:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN webfilter_category text""")
        except: pass

        sd = DateFromMx(sql_helper.get_update_info('n_http_events[untangle-base-webfilter]', start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()

        try:
            sql_helper.run_sql("""\
UPDATE reports.n_http_events
SET webfilter_action = action,
  webfilter_reason = reason,
  webfilter_category = category
FROM events.n_webfilter_evt_blk
WHERE reports.n_http_events.time_stamp >= %s
  AND reports.n_http_events.time_stamp < %s
  AND reports.n_http_events.request_id = events.n_webfilter_evt_blk.request_id""", (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_http_events[untangle-base-webfilter]', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

class HourlyWebUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'usage', _('Hourly Usage'))

    @print_timing
    def get_key_statistics(self, end_date, host=None, user=None):
        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(7))

        hits_query = """\
SELECT max(hits) AS max_hits, avg(hits) AS avg_hits
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""
        if host:
            hits_query = hits_query + " AND hname = %s"
        elif user:
            hits_query = hits_query + " AND uid = %s"

        violations_query = """\
SELECT avg(blocks)
FROM (select date_trunc('hour', trunc_time) AS hour, sum(blocks) AS blocks
      FROM reports.n_http_totals
      WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            violations_query = violations_query + " AND hname = %s"
        elif user:
            violations_query = violations_query + " AND uid = %s"

        violations_query = violations_query + " GROUP BY hour) AS foo"

        conn = sql_helper.get_connection()

        lks = []

        try:
            curs = conn.cursor()
            if host:
                curs.execute(hits_query, (one_day, ed, host))
            elif user:
                curs.execute(hits_query, (one_day, ed, user))
            else:
                curs.execute(hits_query, (one_day, ed))
            r = curs.fetchone()
            ks = KeyStatistic(N_('max hits (1-day)'), r[0], N_('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('avg hits (1-day)'), r[1], N_('hits/minute'))
            lks.append(ks)
        except: pass

        try:
            curs = conn.cursor()
            if host:
                curs.execute(hits_query, (one_week, ed, host))
            elif user:
                curs.execute(hits_query, (one_week, ed, user))
            else:
                curs.execute(hits_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(N_('max hits (1-week)'), r[0], N_('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('avg hits (1-week)'), r[1], N_('hits/minute'))
            lks.append(ks)
        except: pass

        try:
            curs = conn.cursor()
            if host:
                curs.execute(violations_query, (one_day, ed, host))
            elif user:
                curs.execute(violations_query, (one_day, ed, user))
            else:
                curs.execute(violations_query, (one_day, ed))
            r = curs.fetchone()
            ks = KeyStatistic(N_('avg violations (1-day)'), r[0],
                              N_('violations/hour'))
            lks.append(ks)
        except: pass

        try:
            curs = conn.cursor()
            if host:
                curs.execute(violations_query, (one_week, ed, host))
            elif user:
                curs.execute(violations_query, (one_week, ed, user))
            else:
                curs.execute(violations_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(N_('avg violations (1-week)'), r[0],
                              N_('violations/hour'))
            lks.append(ks)
        except: pass

        return lks

    @print_timing
    def get_plot(self, end_date, host=None, user=None):
        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(7))

        conn = sql_helper.get_connection()

        try:
            q = """\
SELECT (date_part('hour', trunc_time) || ':'
        || (date_part('minute', trunc_time)::int / 10 * 10))::time AS time,
       sum(hits) / 10 AS hits,
       sum(blocks) / 10 AS blocks
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
        except: pass

        conn.commit()

        plot = LinePlot(title=_('Hourly Web Usage'),
                        xlabel=_('Hour of Day'),
                        ylabel=_('Hits per Minute'),
                        major_formatter=TIME_OF_DAY_FORMATTER,
                        xaxis_ticks=EVEN_HOURS_OF_A_DAY)

        plot.add_dataset(dates, hits, label=_('hits'))
        plot.add_dataset(dates, blocks, label=_('violations'))

        return plot

class WebFilterDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'incidents', N_('Incident Report'))

    def get_columns(self, host=None, user=None):
        rv = [ColumnDesc('time_stamp', N_('Time'), 'Date')]

        if not host:
            rv.append(ColumnDesc('hname', N_('Client'), 'HostLink'))
        if not user:
            rv.append(ColumnDesc('uid', N_('User'), 'UserLink'))

        rv = rv + [ColumnDesc('url', N_('URL'), 'URL')]

        return rv


    def get_sql(self, start_date, end_date, host=None, user=None):
        sql = "SELECT time_stamp, "

        if not host:
            sql = sql + "hname, "
        if not user:
            sql = sql + "uid, "

        sql = sql + ("""'http://' || host || uri
FROM reports.n_http_events
WHERE time_stamp >= %s AND time_stamp < %s
      AND NOT webfilter_action ISNULL""" % (DateFromMx(start_date),
                                            DateFromMx(end_date)))

        if host:
            sql = sql + (" AND host = %s" % QuotedString(host))
        if user:
            sql = sql + (" AND host = %s" % QuotedString(user))

        return sql

reports.engine.register_node(WebFilterBaseNode())
