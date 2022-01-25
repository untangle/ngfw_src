import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_wan_failover_test_events()
    __create_wan_failover_action_events()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table('wan_failover_test_events', cutoff)
    sql_helper.clean_table('wan_failover_action_events', cutoff)

@sql_helper.print_timing
def __create_wan_failover_action_events( ):
    sql_helper.create_table("""\
CREATE TABLE reports.wan_failover_action_events (
    time_stamp timestamp without time zone,
    interface_id int,
    action text,
    os_name text,
    name text,
    event_id bigserial)""", ["event_id"], ["time_stamp"])

@sql_helper.print_timing
def __create_wan_failover_test_events( ):
    sql_helper.create_table("""\
CREATE TABLE reports.wan_failover_test_events (
    time_stamp timestamp without time zone,
    interface_id int,
    name text,
    description text,
    success bool,
    event_id bigserial)""", ["event_id"], ["time_stamp"])

