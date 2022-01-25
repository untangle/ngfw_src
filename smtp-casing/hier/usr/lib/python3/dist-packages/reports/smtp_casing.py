import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_mail_msgs()
    __create_mail_addrs()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("mail_addrs", cutoff)
    sql_helper.clean_table("mail_msgs", cutoff)

@sql_helper.print_timing
def __create_mail_addrs():
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

@sql_helper.print_timing
def __create_mail_msgs():
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
