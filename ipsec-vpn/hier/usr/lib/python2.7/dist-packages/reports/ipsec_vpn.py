import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_ipsec_user_events_table()
    __create_ipsec_vpn_events_table()
    __create_ipsec_tunnel_stats_table()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("ipsec_user_events", cutoff)
    sql_helper.clean_table("ipsec_tunnel_stats", cutoff)
    sql_helper.clean_table("ipsec_vpn_events", cutoff)

@sql_helper.print_timing
def __create_ipsec_user_events_table():
    sql_helper.create_table("""\
CREATE TABLE reports.ipsec_user_events (
    event_id bigint,
    time_stamp timestamp without time zone,
    connect_stamp timestamp without time zone,
    goodbye_stamp timestamp without time zone,
    client_address text,
    client_protocol text,
    client_username text,
    net_process text,
    net_interface text,
    elapsed_time text,
    rx_bytes bigint,
    tx_bytes bigint)""",["event_id"])

@sql_helper.print_timing
def __create_ipsec_vpn_events_table():
    sql_helper.create_table("""\
CREATE TABLE reports.ipsec_vpn_events (
    event_id bigint,
    time_stamp timestamp without time zone,
    local_address text,
    remote_address text,
    tunnel_description text,
    event_type text)""",["event_id"])

@sql_helper.print_timing
def __create_ipsec_tunnel_stats_table():
    sql_helper.create_table("""\
CREATE TABLE reports.ipsec_tunnel_stats (
    time_stamp timestamp without time zone,
    tunnel_name text,
    in_bytes bigint,
    out_bytes bigint,
    event_id bigserial)""",["event_id"],["time_stamp"])
