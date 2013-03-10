import reports.engine
import reports.sql_helper as sql_helper
import mx
import sys

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import TimestampFromMx
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.sql_helper import print_timing

class HttpCasing(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-casing-http')

    def parents(self):
        return ['untangle-vm']

    @print_timing
    def setup(self):
        self.__create_http_events()

        ft = FactTable('reports.http_totals', 'reports.http_events',
                       'time_stamp',
                       [Column('hostname', 'text'), 
                        Column('username', 'text'),
                        Column('host', 'text'),
                        Column('s2c_content_type', 'text')],
                       [Column('hits', 'bigint', 'count(*)'),
                        Column('c2s_content_length', 'bigint', 'sum(c2s_content_length)'),
                        Column('s2c_content_length', 'bigint', 'sum(s2c_content_length)')])
        
        # remove obsolete columns
        sql_helper.drop_column('reports', 'http_totals', 's2c_bytes')
        sql_helper.drop_column('reports', 'http_totals', 'c2s_bytes')

        reports.engine.register_fact_table(ft)

    @print_timing
    def __create_http_events(self):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.http_events (
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
    host text, 
    c2s_content_length integer,
    s2c_content_length integer, 
    s2c_content_type text,
    spyware_blocked boolean,
    spyware_cookie_ident text,
    adblocker_action character,
    webfilter_reason character(1),
    webfilter_category text,
    webfilter_blocked boolean,
    webfilter_flagged boolean,
    sitefilter_reason character(1),
    sitefilter_category text,
    sitefilter_blocked boolean,
    sitefilter_flagged boolean,
    virus_clam_clean boolean,
    virus_clam_name text,
    virus_commtouchav_clean boolean,
    virus_commtouchav_name text)""")

        # If the new index does not exist, create it
        if not sql_helper.index_exists("reports","http_events","request_id", unique=True):
            sql_helper.create_index("reports","http_events","request_id", unique=True);

        # If the new index does not exist, create it
        if not sql_helper.index_exists("reports","http_events","event_id", unique=True):
            sql_helper.create_index("reports","http_events","event_id", unique=True);
        
        sql_helper.create_index("reports","http_events","session_id");
        sql_helper.create_index("reports","http_events","policy_id");
        sql_helper.create_index("reports","http_events","time_stamp");

        # web filter event log indexes
        sql_helper.create_index("reports","http_events","sitefilter_blocked");
        sql_helper.create_index("reports","http_events","sitefilter_flagged");

        # web filter lite event log indexes
        # sql_helper.create_index("reports","http_events","webfilter_blocked");
        # sql_helper.create_index("reports","http_events","webfilter_flagged");

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table("http_events", cutoff)
        sql_helper.drop_fact_table("http_totals", cutoff)        

reports.engine.register_node(HttpCasing())
