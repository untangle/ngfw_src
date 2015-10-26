import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from reports.engine import Node

class IPsec(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-ipsec-vpn','IPsec VPN')

    def create_tables(self):
        self.__make_ipsec_user_events_table()
        self.__make_ipsec_tunnel_stats_table()

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("ipsec_user_events", cutoff)

    @sql_helper.print_timing
    def __make_ipsec_user_events_table(self):
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
    def __make_ipsec_tunnel_stats_table(self):
        sql_helper.create_table("""\
CREATE TABLE reports.ipsec_tunnel_stats (
    time_stamp timestamp without time zone,
    tunnel_name text,
    in_bytes bigint,
    out_bytes bigint,
    event_id bigserial)""",["event_id"],["time_stamp"])

reports.engine.register_node(IPsec())

