import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
import uvm.i18n_helper

from reports.engine import Node

class CaptivePortal(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-captive-portal','Captive Portal')

    def create_tables(self):
        self.__make_captive_portal_user_events_table()

    def parents(self):
        return ['untangle-vm']

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("captive_portal_user_events", cutoff)

    @sql_helper.print_timing
    def __make_captive_portal_user_events_table(self):
        sql_helper.create_table("""\
CREATE TABLE reports.captive_portal_user_events (
    time_stamp timestamp without time zone,
    policy_id bigint,
    event_id bigserial,
    login_name text,
    event_info text,
    auth_type text,
    client_addr text)""",["event_id"],["time_stamp"])

        sql_helper.rename_table("capture_user_events","captive_portal_user_events") #12.0

reports.engine.register_node(CaptivePortal())
