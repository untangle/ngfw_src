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
        Node.__init__(self, 'untangle-casing-http', 'HTTP')

    def parents(self):
        return ['untangle-vm']

    @sql_helper.print_timing
    def setup(self):
        ft = FactTable('reports.http_totals', 'reports.http_events',
                       'time_stamp',
                       [Column('hostname', 'text'),
                        Column('username', 'text'),
                        Column('host', 'text')],
                       [Column('hits', 'bigint', 'count(*)'),
                        Column('c2s_content_length', 'bigint', 'sum(c2s_content_length)'),
                        Column('s2c_content_length', 'bigint', 'sum(s2c_content_length)')])
        reports.engine.register_fact_table(ft)

    def create_tables(self):
        self.__create_http_events()

    @sql_helper.print_timing
    def __create_http_events(self):
        sql_helper.create_table("""\
CREATE TABLE reports.http_events (
    request_id bigint NOT NULL,
    time_stamp timestamp NOT NULL,
    session_id bigint,
    client_intf int2,
    server_intf int2,
    c_client_addr inet,
    s_client_addr inet,
    c_server_addr inet,
    s_server_addr inet,
    c_client_port integer,
    s_client_port integer,
    c_server_port integer,
    s_server_port integer,
    policy_id int2,
    username text,
    hostname text,
    method character(1),
    uri text,
    host text,
    c2s_content_length bigint,
    s2c_content_length bigint,
    s2c_content_type text,
    adblocker_cookie_ident text,
    adblocker_action character,
    webfilter_reason character(1),
    webfilter_category text,
    webfilter_blocked boolean,
    webfilter_flagged boolean,
    sitefilter_reason character(1),
    sitefilter_category text,
    sitefilter_blocked boolean,
    sitefilter_flagged boolean,
    clam_clean boolean,
    clam_name text,
    virusblocker_clean boolean,
    virusblocker_name text)""",
                                ["request_id"],
                                ["session_id",
                                 "policy_id",
                                 "time_stamp",
                                 "webfilter_blocked",
                                 "webfilter_flagged",
                                 "webfilter_category",
                                 "virusblocker_clean",
                                 "clam_clean",
                                 "adblocker_action",
                                 "host",
                                 "username",
                                 "hostname",
                                 "c_client_addr",
                                 "client_intf",
                                 "server_intf",
                                 "sitefilter_blocked",
                                 "sitefilter_flagged",
                                 "sitefilter_category"])

        sql_helper.drop_column('http_events','event_id') # 11.2 - drop unused column
        sql_helper.drop_column('http_events','adblocker_blocked') # 11.2 - drop unused column

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("http_events", cutoff)
        sql_helper.clean_table("http_totals", cutoff)

reports.engine.register_node(HttpCasing())
