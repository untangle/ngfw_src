"""
Intrusion Detection & Prevention Reports
"""
import mx
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from reports.engine import Node

class IntrusionPrevention(Node):
    """
    Intrusion Prevention
    """
    def __init__(
        self, node_name, title, vendor_name
        ):
        Node.__init__(
            self, 
            node_name,
            'Intrusion Prevention'
        )

        self.__title = title
        self.__vendor_name = vendor_name

    def create_tables(self):
        self.__create_intrusion_prevention_events()
        
    @sql_helper.print_timing
    def __create_intrusion_prevention_events(self):
        """
        Generate table and indices
        """
        # rename old name if exists
        sql_helper.rename_table("idps_events","intrusion_prevention_events") #11.2

        sql_helper.create_table("""\
CREATE TABLE reports.intrusion_prevention_events (
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
        msg text)""", [], ["time_stamp"])

    def reports_cleanup(self, cutoff):
        pass

reports.engine.register_node(IntrusionPrevention('untangle-node-intrusion-prevention', 'Intrusion Prevention', 'intrusion-prevention'))
