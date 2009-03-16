import reports.engine
import sql_helper

from psycopg import DateFromMx
from reports.engine import Column
from reports.engine import Node
from sql_helper import print_timing

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

reports.engine.register_node(WebfilterBaseNode())
