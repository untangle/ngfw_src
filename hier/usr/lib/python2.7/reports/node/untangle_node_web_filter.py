import reports.node.untangle_base_web_filter
import reports.sql_helper as sql_helper
import uvm.i18n_helper
import mx
import sys

from reports.sql_helper import print_timing

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
from reports import TIME_OF_DAY_FORMATTER
from reports import TIMESTAMP_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

from reports.log import *
logger = getLogger(__name__)

_ = uvm.i18n_helper.get_translation('untangle').lgettext

def N_(message): return message

class WebFilterNode(reports.node.untangle_base_web_filter.WebFilterBaseNode):
    def __init__(self, node_name, title, short_name):
	    reports.node.untangle_base_web_filter.WebFilterBaseNode.__init__(self, node_name, title, short_name)
	    self.__short_name = short_name

    @sql_helper.print_timing
    def setup(self):
	    reports.node.untangle_base_web_filter.WebFilterBaseNode.setup(self)

    def create_tables(self):
	    self.__create_http_query_events()

    @sql_helper.print_timing
    def __create_http_query_events(self):
	    sql_helper.create_table("""\
CREATE TABLE reports.http_query_events (
    event_id bigserial,
    time_stamp timestamp without time zone,
    session_id bigint, 
    client_intf smallint,
    server_intf smallint,
    c_client_addr inet, 
    s_client_addr inet, 
    c_server_addr inet, 
    s_server_addr inet,
    c_client_port integer, 
    s_client_port integer, 
    c_server_port integer, 
    s_server_port integer,
    policy_id bigint, 
    username text,
    hostname text,
    request_id bigint, 
    method character(1), 
    uri text,
    term text,
    host text, 
    c2s_content_length bigint,
    s2c_content_length bigint, 
    s2c_content_type text)""",["request_id","event_id"],["session_id","policy_id","time_stamp"])

    def get_report(self):
	    summaryReports = [
	        TopTenQueryTerms(self.__short_name),
	        TopTenQueryHosts(self.__short_name),
	        TopTenQueryUsers(self.__short_name)
	    ]
	    detailReports = [
	          WebFilterDetailUnblock(self.__short_name),
	          WebFilterDetailQueries(self.__short_name)
	    ]
	    return reports.node.untangle_base_web_filter.WebFilterBaseNode.get_report(self, summaryReports, detailReports )

    def reports_cleanup(self, cutoff):
	    sql_helper.clean_table("http_query_events", cutoff)
	    sql_helper.clean_table("http_totals", cutoff)   


class TopTenQueryTerms(Graph):
    def __init__(self, node_name):
        Graph.__init__(self, 'top-query-terms',
                       _('Top Query Terms'))

        self.__short_name = node_name

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT term, count(*) AS term_sum
FROM reports.http_query_events
WHERE time_stamp >= %%s AND time_stamp < %%s
""" % ()
        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"
        query += """\
GROUP BY term
ORDER BY term_sum DESC""" 

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
                ks = KeyStatistic(r[0], r[1], _('Hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenQueryHosts(Graph):
    def __init__(self, node_name):
        Graph.__init__(self, 'top-query-hosts',
                       _('Top Query Hosts'))

        self.__short_name = node_name

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT host, count(*) AS host_sum
FROM reports.http_query_events
WHERE time_stamp >= %%s AND time_stamp < %%s
""" % ()
        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"
        query += """\
GROUP BY host
ORDER BY host_sum DESC""" 

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
                ks = KeyStatistic(r[0], r[1], _('Hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class TopTenQueryUsers(Graph):
    def __init__(self, node_name):
        Graph.__init__(self, 'top-query-users',
                       _('Top Query Users'))

        self.__short_name = node_name

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT hostname, count(*) AS hostname_sum
FROM reports.http_query_events
WHERE time_stamp >= %%s AND time_stamp < %%s
""" % ()
        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"
        query += """\
GROUP BY hostname
ORDER BY hostname_sum DESC""" 

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
                ks = KeyStatistic(r[0], r[1], _('Hits'))
                lks.append(ks)
                dataset[r[0]] = r[1]
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART,
                     title=self.title,
                     xlabel=_('Hosts'),
                     ylabel=_('Hits Per Day'))

        plot.add_pie_dataset(dataset, display_limit=10)

        return (lks, plot, 10)

class WebFilterDetailUnblock(DetailSection):
    def __init__(self, node_name):
        DetailSection.__init__(self, 'unblocks', _('Unblock Events'))

        self.__short_name = node_name

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hostname', _('Client')))
        else:
            rv.append(ColumnDesc('hostname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('username', _('User')))
        else:
            rv.append(ColumnDesc('username', _('User'), 'UserLink'))

        rv += [ColumnDesc('url', _('Url'), 'URL'),
               ColumnDesc('%s_category' % self.__short_name, _('Category')),
               ColumnDesc('s_server_addr', _('Server Ip')),
               ColumnDesc('c_client_addr', _('Client Ip'))]

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_http_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT *,
       CASE s_server_port WHEN 443 THEN 'https://' ELSE 'http://' END || host || uri AS url
FROM reports.http_events
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND %s_category = 'unblocked'
""" % (DateFromMx(start_date), DateFromMx(end_date),
       self.__short_name)

        if host:
            sql += " AND hostname = %s" % QuotedString(host)
        if user:
            sql += " AND username = %s" % QuotedString(user)

        return sql + " ORDER BY time_stamp DESC"

class WebFilterDetailQueries(DetailSection):
    def __init__(self, node_name):
        DetailSection.__init__(self, 'queries', _('Query Events'))

        self.__short_name = node_name

    def get_columns(self, host=None, user=None, email=None):
        if email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        if host:
            rv.append(ColumnDesc('hostname', _('Client')))
        else:
            rv.append(ColumnDesc('hostname', _('Client'), 'HostLink'))

        if user:
            rv.append(ColumnDesc('username', _('User')))
        else:
            rv.append(ColumnDesc('username', _('User'), 'UserLink'))

        rv += [ColumnDesc('term', _('Term'), 'Term'),
               ColumnDesc('s_server_addr', _('Server Ip')),
               ColumnDesc('c_client_addr', _('Client Ip'))]

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_http_query_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT *
FROM reports.http_query_events
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
""" % (DateFromMx(start_date), DateFromMx(end_date) )

        if host:
            sql += " AND hostname = %s" % QuotedString(host)
        if user:
            sql += " AND username = %s" % QuotedString(user)

        return sql + " ORDER BY time_stamp DESC"


reports.engine.register_node(WebFilterNode( 'untangle-node-web-filter', 'Web Filter', 'web_filter' ))
