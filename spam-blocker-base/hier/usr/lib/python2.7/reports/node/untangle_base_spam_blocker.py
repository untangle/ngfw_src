import gettext
import logging
import mx
import reports.colors as colors
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from reports.engine import Node

class SpamBaseNode(Node):
    def __init__(self, node_name, title, short_name, vendor_name, spam_label,
                 ham_label, hourly_spam_rate_title, daily_spam_rate_title,
                 top_spammed_title):
        Node.__init__(self, node_name, title)

        self.__title = title
        self.__short_name = short_name
        self.__vendor_name = vendor_name
        self.__spam_label = spam_label
        self.__ham_label = ham_label
        self.__hourly_spam_rate_title = hourly_spam_rate_title
        self.__daily_spam_rate_title = daily_spam_rate_title
        self.__top_spammed_title = top_spammed_title

    def parents(self):
        return ['untangle-casing-smtp']

    def create_tables(self):
        self.__create_smtp_tarpit_events(  )

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table('smtp_tarpit_events', cutoff)

    @sql_helper.print_timing
    def __create_smtp_tarpit_events(self):
        sql_helper.create_table("""\
CREATE TABLE reports.smtp_tarpit_events (
    time_stamp timestamp without time zone,
    ipaddr inet,
    hostname text,
    policy_id int8,
    vendor_name varchar(255),
    event_id bigserial)""",["event_id"],["time_stamp"])

