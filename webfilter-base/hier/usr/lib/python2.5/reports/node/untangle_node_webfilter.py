import sql_helper

import reports.engine
from reports.engine import Column
from reports.engine import Node

class WebfilterNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-webfilter')

    def parents(self):
        return ['untangle-casing-http']

    def setup(self, start_date, end_date):
        sd = sql_helper.get_update_info('n_webfilter_http_events')
        if sd == None:
            sd = start_date
            is_new = True
        else:
            is_new = False

        if is_new:
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN webfilter_action character(1)""")
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN webfilter_reason character(1)""")
            sql_helper.run_sql("""\
ALTER TABLE reports.n_http_events ADD COLUMN webfilter_category text""")

        sql_helper.run_sql("""\
UPDATE reports.n_http_events
SET webfilter_action = action,
  webfilter_reason = reason,
  webfilter_category = category
FROM events.n_webfilter_evt_blk
WHERE reports.n_http_events.time_stamp >= ?
  AND reports.n_http_events.time_stamp < ?
  AND reports.n_http_events.request_id = events.n_webfilter_evt_blk.request_id""",
s                           (sd, end_date))

        ft = report_engine.get_fact_table('reports.n_http_totals')

        ft.measures.add(Column.new('blocks', 'integer',
                                   "count(CASE WHEN webfilter_action = 'P' THEN 1 ELSE NULL END)"))

    def teardown(self):
        print "TEARDOWN"

reports.engine.register_node(WebfilterNode())
