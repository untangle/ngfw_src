import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
from psycopg2.extensions import TimestampFromMx
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import Highlight
from reports import HOUR_FORMATTER
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import STACKED_BAR_CHART
from reports import SummarySection
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle').lgettext
def N_(message): return message

class IPsec(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-ipsec-vpn','IPsec VPN')

    @sql_helper.print_timing
    def setup(self):
        return

    def create_tables(self):
        self.__make_ipsec_user_events_table()
        self.__make_ipsec_tunnel_stats_table()

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        return Report(self, sections)

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

    # added in 11.1
    sql_helper.add_column( "ipsec_user_events", "client_protocol", "text" )

    # added in 11.2
    sql_helper.add_column( "ipsec_user_events", "time_stamp", "timestamp without time zone" )

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

