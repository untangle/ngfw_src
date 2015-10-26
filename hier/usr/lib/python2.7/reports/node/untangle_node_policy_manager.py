import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from reports.engine import Node

class PolicyManager(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-policy-manager','Policy Manager')

    def create_tables(self):
        pass

    def parents(self):
        return ['untangle-vm']

reports.engine.register_node(PolicyManager())

