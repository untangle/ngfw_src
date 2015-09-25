import gettext
import logging
import mx
import reports.colors as colors
import reports.sql_helper as sql_helper
import reports.engine
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
from reports import TIME_SERIES_CHART
from reports import TIMESTAMP_FORMATTER
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = uvm.i18n_helper.get_translation('untangle').lgettext
def N_(message): return message

LOGIN = _('Login')
UPDATE = _('Update')
LOGOUT = _('Logout')

class DirectoryConnector(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-directory-connector','Directory Connector')

    @sql_helper.print_timing
    def setup(self):
        ft = FactTable('reports.directory_connector_login_totals', 'reports.directory_connector_login_events', 'time_stamp', [], [])
        reports.engine.register_fact_table(ft)
        ft.measures.append(Column('logins', 'integer', "count(CASE WHEN type = 'I' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('updates', 'integer', "count(CASE WHEN type = 'U' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('logouts', 'integer', "count(CASE WHEN type = 'O' THEN 1 ELSE NULL END)"))

    def create_tables(self):
        self.__create_directory_connector_login_events()

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [AdHighlight(self.name),
                            DailyUsage(),
                            TopUsers(),
                            IdentifiedSessions(),
                            Sessions()])
        sections.append(s)

        sections.append(AdDetail())
        
        return Report(self, sections)

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table('directory_connector_login_totals', cutoff)
        sql_helper.clean_table('directory_connector_login_events', cutoff)        

    @sql_helper.print_timing
    def __create_directory_connector_login_events( self ):
        # rename old name if exists
        sql_helper.rename_table("adconnector_login_events","directory_connector_login_events") #11.2

        sql_helper.create_table("""\
CREATE TABLE reports.directory_connector_login_events (
    time_stamp timestamp without time zone,
    login_name text,
    domain text,
    type text,
    client_addr inet)""")

class AdHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("processed") + " " + "%(events)s" + " " +
                           _("AD events"))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(sum(logins)+sum(updates), 0) as events
FROM reports.directory_connector_login_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""
        
        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h

class DailyUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'events', _('AD Events'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["COALESCE(SUM(logins), 0)::float",
                    "COALESCE(SUM(logouts), 0)::float",                    
                    "COALESCE(SUM(updates), 0)::float"]
            
            extra_where = []
            
            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER
                
            q, h = sql_helper.get_averaged_query(sums, "reports.directory_connector_login_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            logins = []
            logouts = []
            updates = []

            for r in curs.fetchall():
                dates.append(r[0])
                logins.append(r[1])
                logouts.append(r[2])
                updates.append(r[3])

            if not logins:
                logins = [0,]
            if not logouts:
                logouts = [0,]
            if not updates:
                updates = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Average Logins'), sum(logins) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Logins'), max(logins),
                              N_('Events')+'/'+_(unit))
            ks = KeyStatistic(_('Average Logouts'), sum(logouts) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Logouts'), max(logouts),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Average Updates'), sum(updates) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Updates'), max(updates),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Average Events'), sum(logins+logouts+updates) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Events'), max(logouts+logouts+updates),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('AD Authentication Events'),
                         major_formatter=TIMESTAMP_FORMATTER,
                         required_points=rp)
            plot.add_dataset(dates, logins, label=_('Logins'),
                             color=colors.goodness)
            plot.add_dataset(dates, updates, label=_('Updates'),
                             color=colors.detected)
            plot.add_dataset(dates, logouts, label=_('Logouts'),
                             color=colors.badness)
        finally:
            conn.commit()

        return (lks, plot)

class IdentifiedSessions(Graph):
    def __init__(self):
        Graph.__init__(self, 'total-sessions', _('Total Sessions'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT username, COALESCE(sum(new_sessions), 0)::int AS sessions
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
GROUP BY username
ORDER BY sessions"""

        identified = 0
        unidentified = 0

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            for r in curs.fetchall():
                username = r[0]
                sessions = r[1]

                if username:
                    identified += sessions
                else:
                    unidentified += sessions
        finally:
            conn.commit()

        lks = []
        pie_data = {}

        ks = KeyStatistic(_('Identified'), identified, N_('sessions'))
        lks.append(ks)
        pie_data[_('Identified')] = identified
        ks = KeyStatistic(_('Unidentified'), unidentified, N_('sessions'))
        lks.append(ks)
        pie_data[_('Unidentified')] = unidentified

        plot = Chart(type=PIE_CHART, title=self.title)

        plot.add_pie_dataset(pie_data,
                             colors={_('Identified'): colors.goodness,
                                     _('Unidentified'): colors.badness})

        return (lks, plot)

class TopUsers(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-users', _('Top AD Users'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT login_name,count(*)::int as logins
FROM reports.directory_connector_login_events
WHERE NOT login_name IS NULL
AND type != 'FAILED'
AND time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
GROUP BY login_name"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pie_data = {}

            for r in curs.fetchall():
                username = r[0]
                bytes = r[1]

                ks = KeyStatistic(username, bytes, N_('Logins'))
                lks.append(ks)

                pie_data[username] = bytes
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title)

        plot.add_pie_dataset(pie_data)

        return (lks, plot, 10)

class Sessions(Graph):
    def __init__(self):
        Graph.__init__(self, 'sessions', _('Sessions'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                           email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        lks = []

        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = [ "COALESCE(SUM(CASE WHEN username IS NULL THEN 0 ELSE new_sessions END), 0)::int",
                     "COALESCE(SUM(CASE WHEN username IS NULL THEN new_sessions ELSE 0 END), 0)::int"]
            
            extra_where = []
            
            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER
                
            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            identified = []
            unidentified = []

            for r in curs.fetchall():
                dates.append(r[0])
                identified.append(r[1])
                unidentified.append(r[2])

            if not identified:
                identified = [0,]
            if not unidentified:
                unidentified = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Average Identified'), sum(identified) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Identified'), max(identified),
                              N_('Events')+'/'+_(unit))
            ks = KeyStatistic(_('Average Unidentified'), sum(unidentified) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Unidentified'), max(unidentified),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)

            plot = Chart(type=STACKED_BAR_CHART,
                         title=self.title,
                         xlabel=_(unit),
                         ylabel=_('Sessions'),
                         major_formatter=HOUR_FORMATTER,
                         required_points=rp)
            plot.add_dataset(dates, identified, label=_('Identified'),
                             color=colors.goodness)
            plot.add_dataset(dates, unidentified, label=_('Unidentified'),
                             color=colors.badness)
        finally:
            conn.commit()

        return (lks, plot)

class AdDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'ad-events', _('AD Authentication Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email or user or host:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date'),
              ColumnDesc('client_addr', _('Client IP')),
              ColumnDesc('login_name', _('Login Name')),
              ColumnDesc('domain', _('Domain')),
              ColumnDesc('type', _('Type'))]

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email or host or user:
            return None

        sql = """
SELECT time_stamp, host(client_addr) as client_addr, login_name, domain,
CASE WHEN type = 'I' THEN '%s'
     WHEN type = 'U' THEN '%s'
     WHEN type = 'O' THEN '%s' END AS type
FROM reports.directory_connector_login_events
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
ORDER BY time_stamp DESC
""" % (LOGIN, UPDATE, LOGOUT,
       DateFromMx(start_date),
       DateFromMx(end_date))


        return sql
    
reports.engine.register_node(DirectoryConnector())
