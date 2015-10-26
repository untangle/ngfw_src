import reports.engine
import reports.sql_helper as sql_helper

import mx
import sys

from reports.engine import Node

class SmtpCasing(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-casing-smtp', 'SMTP')

    def parents(self):
        return ['untangle-vm']

    def create_tables(self):
        self.__create_mail_msgs()
        self.__create_mail_addrs()

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("mail_addrs", cutoff)
        sql_helper.clean_table("mail_msgs", cutoff)

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
    virus_blocker_lite_clean boolean,
    virus_blocker_lite_name text,
    virus_blocker_clean boolean,
    virus_blocker_name text,
    spam_blocker_lite_score real,
    spam_blocker_lite_is_spam boolean,
    spam_blocker_lite_action character,
    spam_blocker_lite_tests_string text,
    spam_blocker_score real,
    spam_blocker_is_spam boolean,
    spam_blocker_action character,
    spam_blocker_tests_string text,
    phish_blocker_score real,
    phish_blocker_is_spam boolean,
    phish_blocker_tests_string text,
    phish_blocker_action character)""", ["event_id"],
                                ["policy_id",
                                 "time_stamp",
                                 "session_id",
                                 "hostname",
                                 "username",
                                 "c_client_addr",
                                 "s_server_addr",
                                 "addr",
                                 "addr_kind",
                                 "virus_blocker_lite_clean",
                                 "virus_blocker_clean",
                                 "spam_blocker_lite_is_spam",
                                 "spam_blocker_is_spam",
                                 "phish_blocker_is_spam"])

        # remove obsolete columns (11.1)
        sql_helper.drop_column( 'mail_addrs', 'addr_pos' )

        # remove obsolete columns (11.2)
        sql_helper.drop_column( 'mail_addrs', 'msg_attachments' )
        sql_helper.drop_column( 'mail_addrs', 'msg_bytes' )

        # rename columns (11.2)
        sql_helper.rename_column('mail_addrs','spamassassin_score','spam_blocker_lite_score')
        sql_helper.rename_column('mail_addrs','spamassassin_is_spam','spam_blocker_lite_is_spam')
        sql_helper.rename_column('mail_addrs','spamassassin_action','spam_blocker_lite_action')
        sql_helper.rename_column('mail_addrs','spamassassin_tests_string','spam_blocker_lite_tests_string')
        sql_helper.rename_column('mail_addrs','spamblocker_score','spam_blocker_score')
        sql_helper.rename_column('mail_addrs','spamblocker_is_spam','spam_blocker_is_spam')
        sql_helper.rename_column('mail_addrs','spamblocker_action','spam_blocker_action')
        sql_helper.rename_column('mail_addrs','spamblocker_tests_string',' spam_blocker_tests_string ')
        sql_helper.rename_column('mail_addrs','phish_score','phish_blocker_score')
        sql_helper.rename_column('mail_addrs','phish_is_spam','phish_blocker_is_spam')
        sql_helper.rename_column('mail_addrs','phish_tests_string','phish_blocker_tests_string')
        sql_helper.rename_column('mail_addrs','phish_action','phish_blocker_action')
        sql_helper.rename_column('mail_addrs','virusblocker_clean','virus_blocker_clean')
        sql_helper.rename_column('mail_addrs','virusblocker_name','virus_blocker_name')
        sql_helper.rename_column('mail_addrs','clam_clean','virus_blocker_lite_clean')
        sql_helper.rename_column('mail_addrs','clam_name','virus_blocker_lite_name')


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
    virus_blocker_lite_clean boolean,
    virus_blocker_lite_name text,
    virus_blocker_clean boolean,
    virus_blocker_name text,
    spam_blocker_lite_score real,
    spam_blocker_lite_is_spam boolean,
    spam_blocker_lite_tests_string text,
    spam_blocker_lite_action character,
    spam_blocker_score real,
    spam_blocker_is_spam boolean,
    spam_blocker_tests_string text,
    spam_blocker_action character,
    phish_blocker_score real,
    phish_blocker_is_spam boolean,
    phish_blocker_tests_string text,
    phish_blocker_action character)""", 
                                ["msg_id"], ["policy_id","time_stamp"])

        # remove obsolete columns (11.2)
        sql_helper.drop_column( 'mail_msgs', 'msg_attachments' )
        sql_helper.drop_column( 'mail_msgs', 'msg_bytes' )

        # rename columns (11.2)
        sql_helper.rename_column('mail_msgs','spamassassin_score','spam_blocker_lite_score')
        sql_helper.rename_column('mail_msgs','spamassassin_is_spam','spam_blocker_lite_is_spam')
        sql_helper.rename_column('mail_msgs','spamassassin_action','spam_blocker_lite_action')
        sql_helper.rename_column('mail_msgs','spamassassin_tests_string','spam_blocker_lite_tests_string')
        sql_helper.rename_column('mail_msgs','spamblocker_score','spam_blocker_score')
        sql_helper.rename_column('mail_msgs','spamblocker_is_spam','spam_blocker_is_spam')
        sql_helper.rename_column('mail_msgs','spamblocker_action','spam_blocker_action')
        sql_helper.rename_column('mail_msgs','spamblocker_tests_string',' spam_blocker_tests_string ')
        sql_helper.rename_column('mail_msgs','phish_score','phish_blocker_score')
        sql_helper.rename_column('mail_msgs','phish_is_spam','phish_blocker_is_spam')
        sql_helper.rename_column('mail_msgs','phish_tests_string','phish_blocker_tests_string')
        sql_helper.rename_column('mail_msgs','phish_action','phish_blocker_action')
        sql_helper.rename_column('mail_msgs','virusblocker_clean','virus_blocker_clean')
        sql_helper.rename_column('mail_msgs','virusblocker_name','virus_blocker_name')
        sql_helper.rename_column('mail_msgs','clam_clean','virus_blocker_lite_clean')
        sql_helper.rename_column('mail_msgs','clam_name','virus_blocker_lite_name')

reports.engine.register_node(SmtpCasing())
