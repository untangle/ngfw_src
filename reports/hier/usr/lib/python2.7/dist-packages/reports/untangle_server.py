import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_server_events_table()
    __create_interface_stat_events_table()

@sql_helper.print_timing
def cleanup_tables( cutoff ):
    sql_helper.clean_table("server_events", cutoff)
    sql_helper.clean_table("interface_stat_events", cutoff)

@sql_helper.print_timing
def __create_server_events_table():
    sql_helper.create_table("""\
CREATE TABLE reports.server_events (
    time_stamp  TIMESTAMP,
    load_1 	DECIMAL(6, 2),
    load_5 	DECIMAL(6, 2),
    load_15	DECIMAL(6, 2),
    cpu_user 	DECIMAL(6, 3),
    cpu_system 	DECIMAL(6, 3),
    mem_total 	INT8,
    mem_free 	INT8,
    disk_total 	INT8,
    disk_free 	INT8,
    swap_total 	INT8,
    swap_free 	INT8,
    active_hosts 	INT4)""")
        
@sql_helper.print_timing
def __create_interface_stat_events_table():
    sql_helper.create_table("""\
CREATE TABLE reports.interface_stat_events (
    time_stamp  TIMESTAMP,
    interface_id INT,
    rx_rate 	float8,
    rx_bytes 	int8,
    tx_rate 	float8,
    tx_bytes 	int8)""")
    sql_helper.add_column('interface_stat_events','rx_bytes','int8') # 13.1
    sql_helper.add_column('interface_stat_events','tx_bytes','int8') # 13.1

