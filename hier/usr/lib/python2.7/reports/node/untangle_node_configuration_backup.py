import gettext
import logging
import mx
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import TimestampFromMx
from psycopg2.extensions import QuotedString
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import Highlight
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import SummarySection
from reports import TIME_OF_DAY_FORMATTER
from reports.engine import Column
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle').lgettext

class ConfigurationBackup(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-configuration-backup','Configuration Backup')

    @sql_helper.print_timing
    def setup(self):
        return

    def create_tables(self):
        self.__create_configuration_backup_events()

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [ConfigurationBackupHighlight(self.name),])

        sections.append(s)
        sections.append(ConfigurationBackupDetail())

        return Report(self, sections)

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table('configuration_backup_events', cutoff)

    @sql_helper.print_timing
    def __create_configuration_backup_events( self ):
        # rename old name if exists
        sql_helper.rename_table("configuration-backup_events","configuration_backup_events") #11.2

        sql_helper.create_table("""\
CREATE TABLE reports.configuration_backup_events (
    time_stamp timestamp without time zone,
    success boolean,
    description text,
    event_id bigserial)""",["event_id"],["time_stamp"])


class ConfigurationBackupHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("backed up settings") + " " + "%(backups)s" + " " +
                           _("times"))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT count(*) AS backups
FROM reports.configuration_backup_events
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
AND success"""

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h


class ConfigurationBackupDetail(DetailSection):

    def __init__(self):
        DetailSection.__init__(self, 'backup-events', _('Backup Events'))

    def get_columns(self, host=None, user=None, email=None):
        if host or user or email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        rv = rv + [ColumnDesc('description', _('Description'))]
        rv = rv + [ColumnDesc('success', _('Success'))]

        return rv

    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_columns(host, user, email)
    
    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if host or user or email:
            return None

        sql = "SELECT time_stamp, "

        sql = sql + ("""description, success::text
FROM reports.configuration_backup_events
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone """ % (DateFromMx(start_date),
                                                  DateFromMx(end_date)))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(ConfigurationBackup())
