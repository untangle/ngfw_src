import reports.node.untangle_base_web_filter
import reports.sql_helper as sql_helper
import uvm.i18n_helper
import mx
import sys

from reports.engine import Node

class WebFilterNode(reports.node.untangle_base_web_filter.WebFilterBaseNode):
    def __init__(self, node_name, title, short_name):
	    reports.node.untangle_base_web_filter.WebFilterBaseNode.__init__(self, node_name, title, short_name)
	    self.__short_name = short_name

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

    def reports_cleanup(self, cutoff):
	    sql_helper.clean_table("http_query_events", cutoff)

reports.engine.register_node(WebFilterNode( 'untangle-node-web-filter', 'Web Filter', 'web_filter' ))
