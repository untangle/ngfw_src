import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_smtp_tarpit_events(  )

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table('smtp_tarpit_events', cutoff)

@sql_helper.print_timing
def __create_smtp_tarpit_events():
    sql_helper.create_table("""\
CREATE TABLE reports.smtp_tarpit_events (
    time_stamp timestamp without time zone,
    ipaddr inet,
    hostname text,
    policy_id int8,
    vendor_name varchar(255),
    event_id bigserial)""",["event_id"],["time_stamp"])

