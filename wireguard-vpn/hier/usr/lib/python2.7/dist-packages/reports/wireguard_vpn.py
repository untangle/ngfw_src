import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_wireguard_vpn_events_table()
    __create_wireguard_tunnel_stats_table()

@sql_helper.print_timing
def __create_wireguard_vpn_events_table():
    sql_helper.create_table("""\
CREATE TABLE reports.wireguard_vpn_events (
    event_id bigint,
    time_stamp timestamp without time zone,
    local_address text,
    remote_address text,
    tunnel_description text,
    event_type text)""",["event_id"])

@sql_helper.print_timing
def __create_wireguard_tunnel_stats_table():
    sql_helper.create_table("""\
CREATE TABLE reports.wireguard_tunnel_stats (
    time_stamp timestamp without time zone,
    tunnel_name text,
    in_bytes bigint,
    out_bytes bigint,
    event_id bigserial)""",["event_id"],["time_stamp"])
