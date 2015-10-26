import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper
import sys

from reports.engine import Node

class Shield(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-shield','Shield')

    def parents(self):
        return ['untangle-vm']

    def reports_cleanup(self, cutoff):
        pass

reports.engine.register_node(Shield())
