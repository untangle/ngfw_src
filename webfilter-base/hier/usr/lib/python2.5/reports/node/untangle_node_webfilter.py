import gettext
import mx
import reports.engine
import sql_helper

from psycopg import DateFromMx
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
        return Report(self.name, 'Web Filter',
                      [SummarySection('summary', N_('Summary Report'),
                                      [HourlyWebUsage()]),
                       DetailSection('incidents', N_('Incident Report'),
                                     sql_template="""\
SELECT * FROM reports.n_http_events
WHERE time_stamp >= '$end_date' - '1 day'::interval
      AND time_stamp < '$end_date'
      AND NOT webfilter_action ISNULL""")])

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

    def get_key_statistics(self, end_date):
        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(7))

        hits_query = """\
SELECT max(hits) AS max_hits, avg(hits) AS avg_hits
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

        violations_query = """\
SELECT avg(blocks)
FROM (select date_trunc('hour', trunc_time) AS hour, sum(blocks) AS blocks
      FROM reports.n_http_totals
      WHERE trunc_time >= %s AND trunc_time < %s
      GROUP BY hour) AS foo"""

        conn = sql_helper.get_connection()

        lks = []

        try:
            curs = conn.cursor()
            curs.execute(hits_query, (one_day, ed))
            r = curs.fetchone()
            ks = KeyStatistic(N_('max hits (1-day)'), r[0], N_('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('avg hits (1-day)'), r[1], N_('hits/minute'))
            lks.append(ks)
        except: pass

        try:
            curs = conn.cursor()
            curs.execute(hits_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(N_('max hits (1-week)'), r[0], N_('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(N_('avg hits (1-week)'), r[1], N_('hits/minute'))
            lks.append(ks)
        except: pass

        try:
            curs = conn.cursor()
            curs.execute(violations_query, (one_day, ed))
            r = curs.fetchone()
            ks = KeyStatistic(N_('avg violations (1-day)'), r[0],
                              N_('violations/hour'))
            lks.append(ks)
        except: pass

        try:
            curs = conn.cursor()
            curs.execute(violations_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(N_('avg violations (1-week)'), r[0],
                              N_('violations/hour'))
            lks.append(ks)
        except: pass

        return lks

    def get_plot(self, end_date):
        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(7))

        conn = sql_helper.get_connection()

        try:
            curs = conn.cursor()
            curs.execute("""\
SELECT (date_part('hour', trunc_time) || ':'
        || (date_part('minute', trunc_time)::int / 10 * 10))::time AS time,
       sum(hits) / 10 AS hits,
       sum(blocks) / 10 AS blocks
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s
GROUP BY time
ORDER BY time asc""", (one_week, ed))

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

reports.engine.register_node(WebFilterBaseNode())
