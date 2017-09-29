import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_tunnel_vpn_events_table()
    __create_tunnel_vpn_stats_table()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("tunnel_vpn_events", cutoff)

@sql_helper.print_timing
def __create_tunnel_vpn_events_table():
    sql_helper.create_table("""\
CREATE TABLE reports.tunnel_vpn_events (
    event_id bigint,
    time_stamp timestamp without time zone,
    tunnel_name text,
    server_address text,
    local_address text,
    event_type text)""",["event_id"])

@sql_helper.print_timing
def __create_tunnel_vpn_stats_table():
    sql_helper.create_table("""\
CREATE TABLE reports.tunnel_vpn_stats (
    time_stamp timestamp without time zone,
    tunnel_name text,
    in_bytes bigint,
    out_bytes bigint,
    event_id bigserial)""",["event_id"],["time_stamp"])
