import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import sys
import uvm.i18n_helper

from reports.engine import Node

class WebCache(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-web-cache','Web Cache')

    def create_tables(self):
        self.__create_web_cache_stats()

    def parents(self):
        return ['untangle-vm']

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("web_cache_stats", cutoff)

    def __create_web_cache_stats( self ):
        sql_helper.create_table("""\
CREATE TABLE reports.web_cache_stats (
    time_stamp timestamp without time zone,
    hits bigint,
    misses bigint,
    bypasses bigint,
    systems bigint,
    hit_bytes bigint,
    miss_bytes bigint,
    event_id bigserial)""",["event_id"],["time_stamp"])

reports.engine.register_node(WebCache())
