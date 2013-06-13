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

class FtpCasing(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-casing-ftp')

    def parents(self):
        return ['untangle-vm']

    @print_timing
    def setup(self):
        self.__create_ftp_events()

        ft = FactTable('reports.ftp_totals', 'reports.ftp_events',
                       'time_stamp',
                       [Column('hostname', 'text'),
                        Column('username', 'text'),
                        Column('host', 'text'),
                        Column('s2c_content_type', 'text')],
                       [Column('hits', 'bigint', 'count(*)'),
                        Column('c2s_content_length', 'bigint', 'sum(c2s_content_length)'),
                        Column('s2c_content_length', 'bigint', 'sum(s2c_content_length)')])
        
        # remove obsolete columns
        sql_helper.drop_column('reports', 'ftp_totals', 's2c_bytes')
        sql_helper.drop_column('reports', 'ftp_totals', 'c2s_bytes')

        reports.engine.register_fact_table(ft)

    @print_timing
    def __create_ftp_events(self):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.ftp_events (
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
    clam_clean boolean,
    clam_name text,
    commtouchav_clean boolean,
    commtouchav_name text)""")

        # If the new index does not exist, create it
        if not sql_helper.index_exists("reports", "ftp_events", "request_id", unique=True):
            sql_helper.create_index("reports", "ftp_events", "request_id", unique=True);

        # If the new index does not exist, create it
        if not sql_helper.index_exists("reports", "ftp_events", "event_id", unique=True):
            sql_helper.create_index("reports", "ftp_events", "event_id", unique=True);
        
        sql_helper.create_index("reports", "ftp_events", "session_id");
        sql_helper.create_index("reports", "ftp_events", "policy_id");
        sql_helper.create_index("reports", "ftp_events", "time_stamp");

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table("ftp_events", cutoff)
        sql_helper.drop_fact_table("ftp_totals", cutoff)        

reports.engine.register_node(FtpCasing())
