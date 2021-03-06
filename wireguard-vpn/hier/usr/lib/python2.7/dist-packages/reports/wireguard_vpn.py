import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_wireguard_vpn_events_table()
    __create_wireguard_vpn_stats_table()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("wireguard_vpn_events", cutoff)
    sql_helper.clean_table("wireguard_vpn_stats", cutoff)

@sql_helper.print_timing
def __create_wireguard_vpn_events_table():
    sql_helper.create_table("""\
CREATE TABLE reports.wireguard_vpn_events (
    time_stamp timestamp without time zone,
    tunnel_name text,
    event_type text,
    event_id bigserial)""",["event_id"],["time_stamp"])

@sql_helper.print_timing
def __create_wireguard_vpn_stats_table():
    sql_helper.create_table("""\
CREATE TABLE reports.wireguard_vpn_stats (
    time_stamp timestamp without time zone,
    tunnel_name text,
    peer_address inet,
    in_bytes bigint,
    out_bytes bigint,
    event_id bigserial)""",["event_id"],["time_stamp"])
