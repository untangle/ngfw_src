import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from reports.engine import Node

class SslInspector(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-casing-ssl-inspector','SSL')

    def parents(self):
        return ['untangle-vm']

    def create_tables(self):
        return

    def reports_cleanup(self, cutoff):
        pass

reports.engine.register_node(SslInspector())
