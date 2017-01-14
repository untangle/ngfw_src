import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_http_query_events()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("http_query_events", cutoff)
    
@sql_helper.print_timing
def __create_http_query_events():
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

