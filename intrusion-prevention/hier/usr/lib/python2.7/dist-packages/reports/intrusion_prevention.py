import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_intrusion_prevention_events()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("intrusion_prevention_events", cutoff)
    
@sql_helper.print_timing
def __create_intrusion_prevention_events():
    sql_helper.create_table("""\
CREATE TABLE reports.intrusion_prevention_events (
        time_stamp timestamp NOT NULL,
        sig_id int8,
        gen_id int8,
        class_id int8,
        source_addr inet,
        source_port int4,
        dest_addr inet,
        dest_port int4,
        protocol int4,
        blocked boolean,
        category text,
        classtype text,
        msg text,
        rid text)""", [], ["time_stamp"])
    sql_helper.add_column('intrusion_prevention_events', 'rule_id', 'text') #rule_14.2
