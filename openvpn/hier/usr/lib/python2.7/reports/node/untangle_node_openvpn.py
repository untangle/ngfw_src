import gettext
import logging
import mx
import uvm.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper

from reports.engine import Node

class OpenVpn(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-openvpn','OpenVPN')

    def create_tables(self):
        self.__create_openvpn_stats( )
        self.__create_openvpn_events_table( )

    def parents(self):
        return ['untangle-vm']

    @sql_helper.print_timing
    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("openvpn_stats", cutoff)

    @sql_helper.print_timing
    def __create_openvpn_stats( self ):
        sql_helper.create_table("""\
CREATE TABLE reports.openvpn_stats (
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
    def __create_openvpn_events_table( self ):
        sql_helper.create_table("""\
CREATE TABLE reports.openvpn_events (
    time_stamp timestamp without time zone,
    remote_address inet,
    pool_address inet,
    client_name text,
    type text
)""",[],["time_stamp"])

reports.engine.register_node(OpenVpn())

