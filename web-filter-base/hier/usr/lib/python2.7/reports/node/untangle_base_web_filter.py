import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from reports.engine import Node

class WebFilterBaseNode(Node):
    def __init__(self, node_name, title, short_name):
        Node.__init__(self, node_name, title)

        self.__short_name = short_name

    def parents(self):
        return ['untangle-casing-http']

    def reports_cleanup(self, cutoff):
        pass

