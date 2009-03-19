import mx
import numpy
import pylab
import reports.engine
import sql_helper

from psycopg import DateFromMx
from reports.engine import Column
from reports.engine import KeyStatistic
from reports.engine import Node
from reports.graph import time_of_day_formatter
from reports.graph import even_hours_of_a_day
from sql_helper import print_timing

def _(message): return message

class WebfilterBaseNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-base-webfilter')

    def parents(self):
        return ['untangle-casing-http']

    @print_timing
    def setup(self, start_date, end_date):
        self.__update_n_http_events(start_date, end_date)

        ft = reports.engine.get_fact_table('reports.n_http_totals')

        ft.measures.append(Column('blocks', 'integer',
                                  "count(CASE WHEN webfilter_action = 'P' THEN 1 ELSE NULL END)"))

    def process_graphs(self, end_date, base_directory):
        self.__hourly_web_usage_graph(end_date, base_directory)

    def teardown(self):
        print "TEARDOWN"


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

    @print_timing
    def __hourly_web_usage_graph(self, end_date, base_directory):
        ed = DateFromMx(end_date)
        one_day = DateFromMx(end_date - mx.DateTime.DateTimeDelta(1))
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(7))

        hits_query = """\
SELECT max(hits) AS max_hits, avg(hits) AS avg_hits
FROM reports.n_http_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

        violations_query = """\
SELECT avg(blocks)
FROM (SELECT date_trunc('hour', trunc_time) AS hour, sum(blocks) AS blocks
      FROM reports.n_http_totals
      WHERE trunc_time >= %s AND trunc_time < %s
      GROUP BY hour) AS foo"""

        lks = []

        conn = sql_helper.get_connection()

        try:
            curs = conn.cursor()
            curs.execute(hits_query, (one_day, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('Max Hits (1-day)'), r[0], _('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Hits (1-day)'), r[1], _('hits/minute'))
            lks.append(ks)

            curs = conn.cursor()
            curs.execute(hits_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('Max Hits (1-week)'), r[0], _('hits/minute'))
            lks.append(ks)
            ks = KeyStatistic(_('Avg Hits (1-week)'), r[1], _('hits/minute'))
            lks.append(ks)

            curs = conn.cursor()
            curs.execute(violations_query, (one_day, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('Avg Violations (1-day)'), r[0],
                              _('violations/hour'))
            lks.append(ks)

            curs = conn.cursor()
            curs.execute(violations_query, (one_week, ed))
            r = curs.fetchone()
            ks = KeyStatistic(_('Avg Violations (1-week)'), r[0],
                              _('violations/hour'))
            lks.append(ks)

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
        finally:
            conn.commit()


        fix = pylab.figure()
        axes = pylab.axes()
        axes.xaxis.set_major_formatter(time_of_day_formatter)
        axes.xaxis.set_ticks(even_hours_of_a_day)
        pylab.title(_('Hourly Web Usage'))
        pylab.xlabel(_('Hour of Day'))
        pylab.ylabel(_('Hits per Minute'))
        fix.autofmt_xdate()

        pylab.plot(dates, hits, linestyle='-', label="hits")
        pylab.plot(dates, blocks, linestyle='-', label="violations")


        pylab.legend()
        pylab.savefig('%s/hourly-usage.png' % base_directory)


reports.engine.register_node(WebfilterBaseNode())
