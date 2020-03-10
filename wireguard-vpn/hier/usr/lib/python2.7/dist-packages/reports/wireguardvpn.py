import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_wireguard_vpn_stats( )
    __create_wireguard_vpn_events_table( )

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("wireguard_vpn_stats", cutoff)

@sql_helper.print_timing
def __create_wireguard_vpn_stats( ):
    sql_helper.create_table("""\
CREATE TABLE reports.wireguard_vpn_stats (
    time_stamp timestamp without time zone,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    rx_bytes bigint,
    tx_bytes bigint,
    remote_address inet,
    pool_address inet,
    remote_port integer,
    client_name text,
    event_id bigserial
)""",["event_id"],["time_stamp"])

@sql_helper.print_timing
def __create_wireguard_vpn_events_table( ):
    sql_helper.create_table("""\
CREATE TABLE reports.wireguard_vpn_events (
    time_stamp timestamp without time zone,
    remote_address inet,
    pool_address inet,
    client_name text,
    type text
)""",[],["time_stamp"])


