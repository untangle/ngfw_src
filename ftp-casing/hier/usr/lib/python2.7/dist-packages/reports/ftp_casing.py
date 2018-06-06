import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_ftp_events()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("ftp_events", cutoff)
    
@sql_helper.print_timing
def __create_ftp_events():
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

