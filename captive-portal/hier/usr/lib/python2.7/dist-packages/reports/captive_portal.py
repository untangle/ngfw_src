import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __make_captive_portal_user_events_table()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("captive_portal_user_events", cutoff)

@sql_helper.print_timing
def __make_captive_portal_user_events_table():
    sql_helper.create_table("""\
CREATE TABLE reports.captive_portal_user_events (
    time_stamp timestamp without time zone,
    policy_id bigint,
    event_id bigserial,
    login_name text,
    event_info text,
    auth_type text,
    client_addr text)""",["event_id"],["time_stamp"])

