import gettext
import logging
import mx
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from reports.engine import Node

class ConfigurationBackup(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-configuration-backup','Configuration Backup')

    def create_tables(self):
        self.__create_configuration_backup_events()

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table('configuration_backup_events', cutoff)

    @sql_helper.print_timing
    def __create_configuration_backup_events( self ):
        sql_helper.create_table("""\
CREATE TABLE reports.configuration_backup_events (
    time_stamp timestamp without time zone,
    success boolean,
    description text,
    destination text,
    event_id bigserial)""",["event_id"],["time_stamp"])

reports.engine.register_node(ConfigurationBackup())
