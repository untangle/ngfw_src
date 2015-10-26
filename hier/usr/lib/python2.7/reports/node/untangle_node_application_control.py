# $HeadURL: https://untangle.svn.beanstalkapp.com/ngfw/hades/src/application-control/hier/usr/lib/python2.7/reports/node/untangle_node_application_control.py $
import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import sys
import string
import uvm.i18n_helper

from reports.engine import Node

class ApplicationControl(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-application-control', 'Application Control')

    def create_tables(self):
        return

    def parents(self):
        return ['untangle-vm',]

    def reports_cleanup(self, cutoff):
        pass

reports.engine.register_node(ApplicationControl())
