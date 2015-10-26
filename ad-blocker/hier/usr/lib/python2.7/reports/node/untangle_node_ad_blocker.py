import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from reports.engine import Node

class AdBlocker(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-ad-blocker','Ad Blocker')

    def parents(self):
        return ['untangle-casing-http']

    def reports_cleanup(self, cutoff):
        pass

reports.engine.register_node(AdBlocker())
