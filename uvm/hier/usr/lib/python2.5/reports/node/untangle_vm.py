import psycopg
import sql_helper
import reports.engine

from reports.engine import Node

class UvmNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-vm')

    def setup(self, start_time, end_time):
        st = psycopg.TimestampFromMx(start_time)
        et = psycopg.TimestampFromMx(end_time)

        sql_helper.create_table_from_query('reports.users', """\
SELECT DISTINCT username FROM events.u_lookup_evt
WHERE time_stamp >= %s AND time_stamp < %s""", (st, et))

        sql_helper.create_table_from_query('reports_hnames', """\
SELECT DISTINCT hname FROM reports.sessions
WHERE time_stamp >= %s AND time_stamp < %s AND client_intf=1""", (st, et))

    def teardown(self):
        print "TEARDOWN"

reports.engine.register_node(UvmNode())
