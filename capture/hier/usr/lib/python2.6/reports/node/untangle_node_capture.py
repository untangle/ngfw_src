import gettext
import logging
import mx
import reports.colors as colors
import reports.i18n_helper
import reports.sql_helper as sql_helper

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
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import HOST_DRILLDOWN
from reports.engine import Node
from reports.engine import TOP_LEVEL
from reports.engine import USER_DRILLDOWN
from reports.sql_helper import print_timing

_ = reports.i18n_helper.get_translation('untangle-node-capture').lgettext
def N_(message): return message

LOGIN = _('Login Success')
FAILED = _('Login Failure')
TIMEOUT = _('Session Timeout')
INACTIVE = _('Idle Timeout')
USER_LOGOUT = _('User Logout')
ADMIN_LOGOUT = _('Admin Logout')

def auto_incr(start_value=0, amount = 1):
    v = [start_value]
    def f():
        current = v[0]
        v[0] += amount
        return current

    return f

class Capture(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-node-capture')

    def setup(self):
        self.__make_capture_user_events_table()

        ft = FactTable('reports.capture_user_totals',
                       'reports.capture_user_events',
                       'time_stamp', [], [])
        reports.engine.register_fact_table(ft)

        ft.measures.append(Column('success',
                                    'integer',
                                    "count(CASE WHEN event_info = 'LOGIN' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('failure',
                                    'integer',
                                    "count(CASE WHEN event_info = 'FAILED' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('timeout',
                                    'integer',
                                    "count(CASE WHEN event_info = 'TIMEOUT' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('inactive',
                                    'integer',
                                    "count(CASE WHEN event_info = 'INACTIVE' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('user_logout',
                                    'integer',
                                    "count(CASE WHEN event_info = 'USER_LOGOUT' THEN 1 ELSE NULL END)"))
        ft.measures.append(Column('admin_logout',
                                    'integer',
                                    "count(CASE WHEN event_info = 'ADMIN_LOGOUT' THEN 1 ELSE NULL END)"))

        ft = reports.engine.get_fact_table('reports.session_totals')
        ft.measures.append(Column('capture_blocks', 'integer', "count(CASE WHEN capture_blocked THEN 1 ELSE null END)"))
        ft.dimensions.append(Column('capture_rule_index', 'integer'))

    def get_toc_membership(self):
        return [TOP_LEVEL]

    def parents(self):
        return ['untangle-vm']

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [CaptureHighlight(self.name),
                            DailyUsage(),
                            TopUsers(),
                            TopBlockedClients()])
        sections.append(s)

        sections.append(UserDetail())
        sections.append(RuleDetail())

        return Report(self, sections)

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table("capture_user_events", cutoff)
        sql_helper.drop_fact_table("capture_user_totals", cutoff)

    @print_timing
    def __make_capture_user_events_table(self):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.capture_user_events (
    time_stamp timestamp without time zone,
    policy_id bigint,
    login_name text,
    event_info text,
    auth_type text,
    client_addr text,
    event_id bigserial)""")

        sql_helper.add_column('reports', 'capture_user_events', 'event_id', 'bigserial')

        # we used to create event_id as serial instead of bigserial - convert if necessary
        sql_helper.convert_column("reports","capture_user_events","event_id","integer","bigint");

        sql_helper.create_index("reports","capture_user_events","event_id");
        sql_helper.create_index("reports","capture_user_events","time_stamp");

class CaptureHighlight(Highlight):
    def __init__(self, name):
        Highlight.__init__(self, name,
                           _(name) + " " +
                           _("processed") + " " + "%(logins)s" + " " +
                           _("user login events"))

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT COALESCE(sum(success), 0) as logins
FROM reports.capture_user_totals
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
        Graph.__init__(self, 'usage', _('Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

        # dt = Day Totals.  These are the totals over the entire day.
        # pd = Per Day.  This is the number of a particular event per day.
        lks = []
        query = """
SELECT COALESCE(SUM(dt.success_pd)/%s,0), COALESCE(MAX(dt.success_pd),0),
       COALESCE(SUM(dt.failure_pd)/%s,0), COALESCE(MAX(dt.failure_pd),0),
       COALESCE(SUM(dt.timeout_pd)/%s,0), COALESCE(MAX(dt.timeout_pd),0),
       COALESCE(SUM(dt.inactive_pd)/%s,0), COALESCE(MAX(dt.inactive_pd),0),
       COALESCE(SUM(dt.user_logout_pd)/%s,0), COALESCE(MAX(dt.user_logout_pd),0),
       COALESCE(SUM(dt.admin_logout_pd)/%s,0), COALESCE(MAX(dt.admin_logout_pd),0),
       FROM (
           SELECT SUM(success) AS success_pd,
                  SUM(failure) AS failure_pd,
                  SUM(timeout) AS timeout_pd,
                  SUM(inactive) AS inactive_pd,
                  SUM(user_logout) AS user_logout_pd,
                  SUM(admin_logout) AS admin_logout_pd,
                  DATE_TRUNC('day',time_stamp) AS day
           FROM reports.capture_user_totals
           WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
           GROUP BY day
       ) AS dt
"""
        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            sums = ["COALESCE(SUM(success), 0)::float",
                    "COALESCE(SUM(failure), 0)::float",
                    "COALESCE(SUM(timeout), 0)::float",
                    "COALESCE(SUM(inactive), 0)::float",
                    "COALESCE(SUM(user_logout), 0)::float",
                    "COALESCE(SUM(admin_logout), 0)::float"]

            extra_where = []

            if report_days == 1:
                time_interval = 60 * 60
                unit = "Hour"
                formatter = HOUR_FORMATTER
            else:
                time_interval = 24 * 60 * 60
                unit = "Day"
                formatter = DATE_FORMATTER

            q, h = sql_helper.get_averaged_query(sums, "reports.capture_user_totals",
                                                 start_date,
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            success = []
            failure = []
            timeout = []
            inactive = []
            user_logout = []
            admin_logout = []

            for r in curs.fetchall():
                dates.append(r[0])
                success.append(r[1])
                failure.append(r[2])
                timeout.append(r[3])
                inactive.append(r[4])
                user_logout.append(r[5])
                admin_logout.append(r[6])

            if not success:
                success = [0,]
            if not failure:
                failure = [0,]
            if not timeout:
                timeout = [0,]
            if not inactive:
                inactive = [0,]
            if not user_logout:
                user_logout = [0,]
            if not admin_logout:
                logout = [0,]

            rp = sql_helper.get_required_points(start_date, end_date,
                                            mx.DateTime.DateTimeDeltaFromSeconds(time_interval))

            ks = KeyStatistic(_('Average Logins'), sum(success) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Logins'), max(success),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Average Failures'), sum(failure) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Failures'), max(failure),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Average Timeouts'), sum(timeout+inactive) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Timeouts'), max(timeout+inactive),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Average Logouts'), sum(user_logout+admin_logout) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Logouts'), max(user_logout+admin_logout),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Average Events'), sum(success+failure+timeout+inactive+user_logout+admin_logout) / len(rp),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)
            ks = KeyStatistic(_('Max Events'), max(success+failure+timeout+inactive+user_logout+admin_logout),
                              N_('Events')+'/'+_(unit))
            lks.append(ks)

        finally:
            conn.commit()

        plot = Chart(type=STACKED_BAR_CHART,
                     title=self.title,
                     xlabel=_(unit),
                     ylabel=_('Events'),
                     major_formatter=HOUR_FORMATTER,
                     required_points=rp)
        plot.add_dataset(dates, success, label=_('Logins'),
                         color=colors.goodness)
        plot.add_dataset(dates, failure, label=_('Failures'),
                         color=colors.badness)
        plot.add_dataset(dates, timeout, label=_('Timeouts'),
                         color=colors.blue)
        plot.add_dataset(dates, inactive, label=_('Logouts'),
                         color=colors.detected)

        return (lks, plot)


class TopUsers(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-users', _('Top Captive Portal Users'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT login_name,count(*)::int as logins
FROM reports.capture_user_events
WHERE NOT login_name IS NULL
AND event_info = 'LOGIN'
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
                logins = r[1]

                ks = KeyStatistic(username, logins, N_('Logins'))
                lks.append(ks)

                pie_data[username] = logins
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title)

        plot.add_pie_dataset(pie_data, display_limit=10)

        return (lks, plot, 10)

class TopBlockedClients(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-blocked-users', _('Top Blocked Clients'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT host(c_client_addr),count(c_client_addr)::int as failure
FROM reports.sessions
WHERE NOT c_client_addr IS NULL AND capture_blocked = TRUE AND time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
GROUP BY c_client_addr"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pie_data = {}

            for r in curs.fetchall():
                c_client_addr = r[0]
                failure = r[1]

                ks = KeyStatistic(c_client_addr, failure, _('Blocked'))
                lks.append(ks)

                pie_data[c_client_addr] = failure
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title)

        plot.add_pie_dataset(pie_data, display_limit=10)

        return (lks, plot, 10)


class UserDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'capture-user-events', _('Capture User Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email or user or host:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date'),
              ColumnDesc('login_name', _('User Name')),
              ColumnDesc('event_info', _('Type'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email or host or user:
            return None

        sql = """
SELECT time_stamp, login_name,
CASE WHEN event_info = 'LOGIN' THEN '%s'
     WHEN event_info = 'FAILED' THEN '%s'
     WHEN event_info = 'TIMEOUT' THEN '%s'
     WHEN event_info = 'INACTIVE' THEN '%s'
     WHEN event_info = 'USER_LOGOUT' THEN '%s'
     WHEN event_info = 'ADMIN_LOGOUT' THEN '%s' END
FROM reports.capture_user_events
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
ORDER BY time_stamp DESC
""" % (LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT,
       DateFromMx(start_date),
       DateFromMx(end_date))

        return sql

class RuleDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'capture-rule-events', _('Capture Rule Events'))

    def get_columns(self, host=None, user=None, email=None):
        if email or user or host:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date'),
              ColumnDesc('c_client_addr', _('Client Address')),
              ColumnDesc('c_client_port', _('Client Port')),
              ColumnDesc('c_server_address', _('Server Address')),
              ColumnDesc('c_server_port', _('Server Port')),
              ColumnDesc('capture_rule_index', _('Rule Applied')),
              ColumnDesc('capture_blocked', _('Blocked'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email or host or user:
            return None

        sql = """
SELECT time_stamp, host(c_client_addr), c_client_port,
       host(c_server_addr), c_server_port, capture_rule_index, capture_blocked::text
FROM reports.sessions
WHERE capture_blocked = TRUE OR capture_blocked = FALSE
AND time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
ORDER BY time_stamp DESC
""" % (DateFromMx(start_date),
       DateFromMx(end_date))

        return sql

reports.engine.register_node(Capture())
