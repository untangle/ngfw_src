import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_configuration_backup_events()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table('configuration_backup_events', cutoff)

@sql_helper.print_timing
def __create_configuration_backup_events():
    sql_helper.create_table("""\
CREATE TABLE reports.configuration_backup_events (
    time_stamp timestamp without time zone,
    success boolean,
    description text,
    destination text,
    event_id bigserial)""",["event_id"],["time_stamp"])

