import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from reports.engine import Node

class DirectoryConnector(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-directory-connector','Directory Connector')

    def create_tables(self):
        self.__create_directory_connector_login_events()

    def parents(self):
        return ['untangle-vm']

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table('directory_connector_login_events', cutoff)        

    @sql_helper.print_timing
    def __create_directory_connector_login_events( self ):
        # rename old name if exists
        sql_helper.rename_table("adconnector_login_events","directory_connector_login_events") #11.2

        sql_helper.create_table("""\
CREATE TABLE reports.directory_connector_login_events (
    time_stamp timestamp without time zone,
    login_name text,
    domain text,
    type text,
    client_addr inet)""")

reports.engine.register_node(DirectoryConnector())
