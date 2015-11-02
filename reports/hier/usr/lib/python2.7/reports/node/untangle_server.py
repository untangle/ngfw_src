import gettext
import logging
import mx
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from reports.engine import Node

class ServerNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-reports','Server')

    def create_tables(self):
        self.__create_server_events()
        self.__create_interface_stat_events()

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("server_events", cutoff)

    @sql_helper.print_timing
    def __create_server_events(self):
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

        sql_helper.add_column('server_events','active_hosts','int4') # 12.0
        
    @sql_helper.print_timing
    def __create_interface_stat_events(self):
        sql_helper.create_table("""\
CREATE TABLE reports.interface_stat_events (
    time_stamp  TIMESTAMP,
    interface_id INT,
    rx_rate 	FLOAT,
    tx_rate 	FLOAT)""")

reports.engine.register_node(ServerNode())
