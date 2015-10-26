import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from reports.engine import Node

class BandwidthControl(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-bandwidth-control','Bandwidth Control')

    def reports_cleanup(self, cutoff):
        pass

    def parents(self):
        return ['untangle-vm']

reports.engine.register_node(BandwidthControl())

