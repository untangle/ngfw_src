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

class SmtpCasing(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-casing-smtp', 'SMTP')

    def parents(self):
        return ['untangle-vm']

    @sql_helper.print_timing
    def setup(self):
        ft = FactTable('reports.mail_msg_totals', 'reports.mail_msgs', 'time_stamp',
                       [Column('hostname', 'text'), 
                        Column('username', 'text'),
                        Column('client_intf', 'smallint')],
                       [Column('msgs', 'bigint', 'count(*)')])
        reports.engine.register_fact_table(ft)

        ft = FactTable('reports.mail_addr_totals', 'reports.mail_addrs', 'time_stamp',
                       [Column('hostname', 'text'), 
                        Column('username', 'text'),
                        Column('client_intf', 'smallint'),
                        Column('addr', 'text'),
                        Column('addr_kind', 'char(1)')],
                       [Column('msgs', 'bigint', 'count(*)')])
        reports.engine.register_fact_table(ft)

    def create_tables(self):
        self.__create_mail_msgs()
        self.__create_mail_addrs()

    def post_facttable_setup(self, start_date, end_date):
        pass

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("mail_addrs", cutoff)
        sql_helper.clean_table("mail_addr_totals", cutoff)
        sql_helper.clean_table("mail_msgs", cutoff)
        sql_helper.clean_table("mail_msg_totals", cutoff)

    @sql_helper.print_timing
    def __create_mail_addrs(self):
        sql_helper.create_table("""\
CREATE TABLE reports.mail_addrs (
    time_stamp timestamp without time zone,
    session_id bigint, client_intf smallint,
    server_intf smallint,
    c_client_addr inet, s_client_addr inet, c_server_addr inet,
    s_server_addr inet,
    c_client_port integer, s_client_port integer, c_server_port integer,
    s_server_port integer,
    policy_id bigint,
    username text,
    msg_id bigint,
    subject text,
    addr text,
    addr_name text,
    addr_kind char(1),
    hostname text,
    event_id bigserial,
    sender text,
    clam_clean boolean,
    clam_name text,
    spamassassin_score real,
    spamassassin_is_spam boolean,
    spamassassin_action character,
    spamassassin_tests_string text,
    spamblocker_score real,
    spamblocker_is_spam boolean,
    spamblocker_action character,
    spamblocker_tests_string text,
    phish_score real,
    phish_is_spam boolean,
    phish_tests_string text,
    phish_action character,
    virusblocker_clean boolean,
    virusblocker_name text)""", ["event_id"],["policy_id","time_stamp","addr_kind","msg_id"])

        # remove obsolete columns (11.1)
        sql_helper.drop_column( 'mail_addrs', 'addr_pos' )

        # remove obsolete columns (11.2)
        sql_helper.drop_column( 'mail_addrs', 'msg_attachments' )
        sql_helper.drop_column( 'mail_addrs', 'msg_bytes' )

    @sql_helper.print_timing
    def __create_mail_msgs(self):

        sql_helper.create_table("""\
CREATE TABLE reports.mail_msgs (
    time_stamp timestamp without time zone,
    session_id bigint, client_intf smallint,
    server_intf smallint,
    c_client_addr inet, s_client_addr inet, c_server_addr inet,
    s_server_addr inet,
    c_client_port integer, s_client_port integer, c_server_port integer,
    s_server_port integer,
    policy_id bigint,
    username text,
    msg_id bigint,
    subject text,
    hostname text,
    event_id bigserial,
    sender text,
    receiver text,
    clam_clean boolean,
    clam_name text,
    spamassassin_score real,
    spamassassin_is_spam boolean,
    spamassassin_tests_string text,
    spamassassin_action character,
    spamblocker_score real,
    spamblocker_is_spam boolean,
    spamblocker_tests_string text,
    spamblocker_action character,
    phish_score real,
    phish_is_spam boolean,
    phish_tests_string text,
    phish_action character,
    virusblocker_clean boolean,
    virusblocker_name text)""", 
                                ["msg_id"], ["policy_id","time_stamp"])

        # remove obsolete columns (11.2)
        sql_helper.drop_column( 'mail_msgs', 'msg_attachments' )
        sql_helper.drop_column( 'mail_msgs', 'msg_bytes' )

reports.engine.register_node(SmtpCasing())
