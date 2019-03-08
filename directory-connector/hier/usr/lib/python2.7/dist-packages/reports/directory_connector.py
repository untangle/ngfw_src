import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_directory_connector_login_events()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table('directory_connector_login_events', cutoff)        

@sql_helper.print_timing
def __create_directory_connector_login_events( ):
    sql_helper.create_table("""\
CREATE TABLE reports.directory_connector_login_events (
    time_stamp timestamp without time zone,
    login_name text,
    domain text,
    type text,
    client_addr inet)""")
