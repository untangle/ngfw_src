import gettext
import logging
import mx
import reports.engine
import reports.sql_helper as sql_helper
import sys
import uvm.i18n_helper

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
from psycopg2.extensions import TimestampFromMx
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import Highlight
from reports import HOUR_FORMATTER
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import STACKED_BAR_CHART
from reports import SummarySection
from reports import TIME_OF_DAY_FORMATTER
from reports import TIMESTAMP_FORMATTER
from reports.engine import Column
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle-node-idps').lgettext

class Idps(Node):
    def __init__(self, node_name, title, vendor_name):
        Node.__init__(self, node_name,'Intrusion Prevention')

        self.__title = title
        self.__vendor_name = vendor_name

    @print_timing
    def setup(self):
        self.__create_idps_events(  )

    def get_toc_membership(self):
        return [TOP_LEVEL, HOST_DRILLDOWN, USER_DRILLDOWN]

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [IdpsHighlight(self.name)])
        sections.append(s)

        sections.append(IdpsDetail())

        return Report(self, sections)

    def parents(self):
        return ['untangle-vm']

    def reports_cleanup(self, cutoff):
        pass
    
    @print_timing
    def __create_idps_events(self):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.idps_events (
        time_stamp timestamp NOT NULL,
        sig_id int8,
        gen_id int8,
        class_id int8,
        source_addr inet,
        source_port int4,
        dest_addr inet,
        dest_port int4,
        protocol int4,
        blocked boolean,
        category text,
        classtype text,
        description text)""")
        
        sql_helper.create_index("reports","sessions","time_stamp");
        sql_helper.create_index("reports","sessions","blocked");
        sql_helper.create_index("reports","sessions","sig_id");
        sql_helper.create_index("reports","sessions","class_id");

class IdpsHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name, _(name) + " " + "FIXME")

    @print_timing
    def get_highlights(self, end_date, report_days, host=None, user=None, email=None):
        return None # FIXME

reports.engine.register_node(Idps('untangle-node-idps', 'IDPS', 'idps'))
