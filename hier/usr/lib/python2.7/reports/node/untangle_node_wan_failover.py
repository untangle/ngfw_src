import gettext
import logging
import mx
import re
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from reports.engine import Node

class WanFailover(reports.engine.Node):
    def __init__(self):
        reports.engine.Node.__init__(self, 'untangle-node-faild','WAN Failover')

    def create_tables(self):
        self.__create_wan_failover_test_events()
        self.__create_wan_failover_action_events()

    def parents(self):
        return ['untangle-vm']

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table('wan_failover_test_events', cutoff)
        sql_helper.clean_table('wan_failover_action_events', cutoff)

    @property
    def num_wan_interfaces(self):
        return reports.engine.get_number_wan_interfaces()

    @property
    def wan_interfaces(self):
        a = []

        str = reports.engine.get_wan_clause()[1:-1]

        for i in str.split(','):
            try:
                a.append(int(i))
            except ValueError:
                logging.warn('could not add interface: %s' % i, exc_info=True)

        return a

    @property
    def interface_names(self):
        return reports.engine.get_wan_names_map()

    @sql_helper.print_timing
    def __create_wan_failover_action_events( self ):
        # rename old table if exists
        sql_helper.rename_table("faild_action_events","wan_failover_action_events") #11.2

        sql_helper.create_table("""\
CREATE TABLE reports.wan_failover_action_events (
    time_stamp timestamp without time zone,
    interface_id int,
    action text,
    os_name text,
    name text,
    event_id bigserial)""", ["event_id"], ["time_stamp"])


    @sql_helper.print_timing
    def __create_wan_failover_test_events( self ):
        # rename old table if exists
        sql_helper.rename_table("faild_test_events","wan_failover_test_events") #11.2

        sql_helper.create_table("""\
CREATE TABLE reports.wan_failover_test_events (
    time_stamp timestamp without time zone,
    interface_id int,
    name text,
    description text,
    success bool,
    event_id bigserial)""", ["event_id"], ["time_stamp"])

reports.engine.register_node(WanFailover())
