import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper
import string
import sys

from reports.engine import Node

class VirusBaseNode(Node):
    def __init__(self, node_name, title, vendor_name):
        Node.__init__(self, node_name, title)
        self.__vendor_name = vendor_name

    def parents(self):
        return ['untangle-casing-http', 'untangle-casing-smtp', 'untangle-casing-ftp']

