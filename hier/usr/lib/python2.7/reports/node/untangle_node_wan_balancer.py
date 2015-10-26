import gettext
import logging
import mx
import re
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import string
import uvm.i18n_helper

from reports.engine import Node

class WanBalancer(reports.engine.Node):
    def __init__(self):
        reports.engine.Node.__init__(self, 'untangle-node-wan-balancer','WAN Balancer')

    def create_tables(self):
        pass

    def parents(self):
        return ['untangle-vm']

    def reports_cleanup(self, cutoff):
        pass

reports.engine.register_node(WanBalancer())
