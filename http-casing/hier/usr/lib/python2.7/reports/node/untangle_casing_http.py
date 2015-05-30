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
    domain text,
    c2s_content_length bigint,
    s2c_content_length bigint,
    s2c_content_type text,
    ad_blocker_cookie_ident text,
    ad_blocker_action character,
    web_filter_lite_reason character(1),
    web_filter_lite_category text,
    web_filter_lite_blocked boolean,
    web_filter_lite_flagged boolean,
    web_filter_reason character(1),
    web_filter_category text,
    web_filter_blocked boolean,
    web_filter_flagged boolean,
    virus_blocker_lite_clean boolean,
    virus_blocker_lite_name text,
    virus_blocker_clean boolean,
    virus_blocker_name text)""",
                                ["request_id"],
                                ["session_id",
                                 "policy_id",
                                 "time_stamp",
                                 "web_filter_blocked",
                                 "web_filter_flagged",
                                 "web_filter_category",
                                 "web_filter_lite_blocked",
                                 "web_filter_lite_flagged",
                                 "web_filter_lite_category",
                                 "virus_blocker_clean",
                                 "virus_blocker_lite_clean",
                                 "ad_blocker_action",
                                 "host",
                                 "domain",
                                 "username",
                                 "hostname",
                                 "c_client_addr",
                                 "client_intf",
                                 "server_intf"])

        sql_helper.drop_column('http_events','event_id') # 11.2 - drop unused column
        sql_helper.drop_column('http_events','adblocker_blocked') # 11.2 - drop unused column

        sql_helper.add_column('http_events','domain', 'text') # 11.2 - new column

        sql_helper.rename_column('http_events','adblocker_cookie_ident','ad_blocker_cookie_ident') # 11.2
        sql_helper.rename_column('http_events','adblocker_action','ad_blocker_action') # 11.2
        sql_helper.rename_column('http_events','webfilter_reason','web_filter_lite_reason') # 11.2
        sql_helper.rename_column('http_events','webfilter_category','web_filter_lite_category') # 11.2
        sql_helper.rename_column('http_events','webfilter_blocked','web_filter_lite_blocked') # 11.2
        sql_helper.rename_column('http_events','webfilter_flagged','web_filter_lite_flagged') # 11.2
        sql_helper.rename_column('http_events','sitefilter_reason','web_filter_reason') # 11.2
        sql_helper.rename_column('http_events','sitefilter_category','web_filter_category') # 11.2
        sql_helper.rename_column('http_events','sitefilter_blocked','web_filter_blocked') # 11.2
        sql_helper.rename_column('http_events','sitefilter_flagged','web_filter_flagged') # 11.2
        sql_helper.rename_column('http_events','clam_clean','virus_blocker_lite_clean') # 11.2
        sql_helper.rename_column('http_events','clam_name','virus_blocker_lite_name') # 11.2
        sql_helper.rename_column('http_events','virusblocker_clean','virus_blocker_clean') # 11.2
        sql_helper.rename_column('http_events','virusblocker_name','virus_blocker_name') # 11.2

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("http_events", cutoff)
        sql_helper.clean_table("http_totals", cutoff)

reports.engine.register_node(HttpCasing())
