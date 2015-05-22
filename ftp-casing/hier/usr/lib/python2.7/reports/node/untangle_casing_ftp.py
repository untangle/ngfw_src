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
        Node.__init__(self, 'untangle-casing-ftp', 'FTP')

    def parents(self):
        return ['untangle-vm']

    @sql_helper.print_timing
    def setup(self):
        ft = FactTable('reports.ftp_totals', 'reports.ftp_events',
                       'time_stamp',
                       [Column('hostname', 'text'),
                        Column('username', 'text')],
                       [Column('hits', 'bigint', 'count(*)')])
        reports.engine.register_fact_table(ft)

    def create_tables(self):
        self.__create_ftp_events()

    @sql_helper.print_timing
    def __create_ftp_events(self):
        sql_helper.create_table("""\
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
    policy_id bigint,
    username text,
    hostname text,
    request_id bigint,
    method character(1),
    uri text,
    virus_blocker_lite_clean boolean,
    virus_blocker_lite_name text,
    virus_blocker_clean boolean,
    virus_blocker_name text)""",
                                ["request_id","event_id"],
                                ["policy_id",
                                 "session_id",
                                 "time_stamp",
                                 "hostname",
                                 "username",
                                 "c_client_addr",
                                 "s_server_addr",
                                 "virus_blocker_clean",
                                 "virus_blocker_lite_clean"])

        sql_helper.rename_column('ftp_events','clam_clean','virus_blocker_lite_clean') # 11.2
        sql_helper.rename_column('ftp_events','clam_name','virus_blocker_lite_name') # 11.2
        sql_helper.rename_column('ftp_events','virusblocker_clean','virus_blocker_clean') # 11.2
        sql_helper.rename_column('ftp_events','virusblocker_name','virus_blocker_name') # 11.2

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("ftp_events", cutoff)
        sql_helper.clean_table("ftp_totals", cutoff)

reports.engine.register_node(FtpCasing())
