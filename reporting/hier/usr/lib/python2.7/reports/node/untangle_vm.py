import gettext
import logging
import mx
import reports.engine
import reports.sql_helper as sql_helper
import uvm.i18n_helper

from mx.DateTime import DateTimeDelta
from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
from psycopg2.extensions import TimestampFromMx
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
from reports import TIMESTAMP_FORMATTER
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.engine import get_wan_clause
from reports.sql_helper import print_timing
from uvm.settings_reader import get_node_settings_item

EVT_TYPE_REGISTER = 0
EVT_TYPE_RENEW    = 1
EVT_TYPE_EXPIRE   = 2
EVT_TYPE_RELEASE  = 3

_ = uvm.i18n_helper.get_translation('untangle-libuvm').lgettext
def N_(message): return message

class UvmNode(Node):
    def __init__(self):
        self.branded_name = self.__get_branded_name() or "Untangle"
        Node.__init__(self, 'untangle-vm', self.branded_name + " " + _("Server"))

    @sql_helper.print_timing
    def setup(self):
        ft = FactTable('reports.session_totals',
                       'reports.sessions',
                       'time_stamp',
                       [Column('hostname', 'text'),
                        Column('username', 'text'),
                        Column('policy_id', 'bigint'),
                        Column('client_intf', 'smallint'),
                        Column('server_intf', 'smallint'),
                        Column('c_server_port', 'int4'),
                        Column('c_client_port', 'int4')],
                       [Column('new_sessions', 'bigint', 'count(*)'),
                        Column('s2c_bytes', 'bigint', 'sum(p2c_bytes)'),
                        Column('c2s_bytes', 'bigint', 'sum(p2s_bytes)')])
        reports.engine.register_fact_table(ft)

        self.branded_name = self.__get_branded_name() or self.name

    def create_tables(self):
        self.__build_admin_logins_table()
        self.__build_sessions_table()
        self.__build_penaltybox_table()
        self.__build_quotas_table()
        self.__build_host_table_updates_table()
        self.__build_alerts_events_table()

    @sql_helper.print_timing
    def __get_branded_name(self):
        brandco = get_node_settings_item('untangle-node-branding','companyName')
        if (brandco != None):
            return brandco
        return None

    def post_facttable_setup(self, start_date, end_date):
        self.__make_session_counts_table(start_date, end_date)

    def reports_cleanup(self, cutoff):
        sql_helper.clean_table("admin_logins", cutoff)
        sql_helper.clean_table("sessions", cutoff)
        sql_helper.clean_table("session_totals", cutoff)
        sql_helper.clean_table("session_counts", cutoff)
        sql_helper.clean_table("penaltybox", cutoff)
        sql_helper.clean_table("quotas", cutoff)
        sql_helper.clean_table("host_table_updates", cutoff)
        sql_helper.clean_table("alerts", cutoff)

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [VmHighlight(self.name, self.branded_name),
                            BandwidthUsage(),
                            SessionsPerMinute(),
                            DestinationPorts(),
                            HostsByPenalty()])

        sections.append(s)
        sections.append(AdministrativeLoginsDetail())
        sections.append(AlertEventsDetail())

        return Report(self, sections)

    @sql_helper.print_timing
    def __build_admin_logins_table(self):
        sql_helper.create_table("""\
CREATE TABLE reports.admin_logins (
    time_stamp timestamp without time zone,
    login text,
    local boolean,
    client_addr inet,
    succeeded boolean,
    reason char(1) )""")

    @sql_helper.print_timing
    def __build_sessions_table( self ):
        sql_helper.create_table("""\
CREATE TABLE reports.sessions (
        session_id int8 NOT NULL,
        event_id bigserial,
        time_stamp timestamp NOT NULL,
        end_time timestamp NOT NULL,
        hostname text,
        username text,
        policy_id bigint,
        c_client_addr inet,
        c_server_addr inet,
        c_server_port int4,
        c_client_port int4,
        s_client_addr inet,
        s_server_addr inet,
        s_server_port int4,
        s_client_port int4,
        client_intf int2,
        server_intf int2,
        c2p_bytes int8 default 0,
        p2c_bytes int8 default 0,
        s2p_bytes int8 default 0,
        p2s_bytes int8 default 0,
        shield_blocked boolean,
        firewall_blocked boolean,
        firewall_flagged boolean,
        firewall_rule_index integer,
        protofilter_protocol text,
        protofilter_blocked boolean,
        capture_blocked boolean,
        capture_rule_index integer,
        classd_application text,
        classd_protochain text,
        classd_blocked boolean,
        classd_flagged boolean,
        classd_confidence integer,
        classd_ruleid integer,
        classd_detail text,
        ips_blocked boolean,
        ips_ruleid integer,
        ips_description text,
        bandwidth_priority integer,
        bandwidth_rule integer,
        https_ruleid integer,
        https_status text,
        https_detail text)""", 
                                ["session_id","event_id"],
                                ["policy_id","time_stamp"])

    @sql_helper.print_timing
    def __build_alerts_events_table( self ):
        sql_helper.create_table("""\
CREATE TABLE reports.alerts (
        time_stamp timestamp NOT NULL,
        description text NOT NULL,
        summary_text text NOT NULL,
        json text NOT NULL)""")

    @sql_helper.print_timing
    def __make_session_counts_table(self, start_date, end_date):
        sql_helper.create_table("""\
CREATE TABLE reports.session_counts (
        time_stamp timestamp,
        username text,
        hostname text,
        client_intf smallint,
        server_intf smallint,
        num_sessions int8)""")

        sd = TimestampFromMx(sql_helper.get_update_info('reports.session_counts', start_date))

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.session_counts
    (time_stamp, username, hostname, client_intf, server_intf, num_sessions)
SELECT (date_trunc('minute', time_stamp)
        + (generate_series(0, (extract('epoch' from (end_time - time_stamp))
        / 60)::int) || ' minutes')::interval) AS time, username, hostname,
        client_intf, server_intf, count(*)
FROM reports.sessions
WHERE time_stamp >= %s::timestamp without time zone
GROUP BY time, username, hostname, client_intf, server_intf
""", (sd,), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.session_counts',
                                       TimestampFromMx(mx.DateTime.now()),
                                       connection=conn, auto_commit=False,
                                       origin_table='reports.sessions')
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def __build_penaltybox_table( self ):
        sql_helper.create_table("""
CREATE TABLE reports.penaltybox (
        address inet,
        reason text,
        start_time timestamp,
        end_time timestamp,
        time_stamp timestamp)""", [], ["time_stamp","start_time"])

    def __build_quotas_table( self ):
        sql_helper.create_table("""
CREATE TABLE reports.quotas (
        time_stamp timestamp,
        event_id bigserial,
        address inet,
        action integer,
        size bigint,
        reason text)""", ["event_id"], ["time_stamp"])

    def __build_host_table_updates_table( self ):
        sql_helper.create_table("""
CREATE TABLE reports.host_table_updates (
        address inet,
        key text,
        value text,
        time_stamp timestamp)""",[],["time_stamp"])

    def teardown(self):
        pass

class VmHighlight(Highlight):
    def __init__(self, name, branded_name):
        xml_escapes = { '&' : '&amp;',
                        '>' : '&gt;',
                        '<' : '&lt;',
                        "'" : '&apos;',
                        '"' : '&quot;' }

        for char, escape in xml_escapes.iteritems():
            branded_name = branded_name.replace(char, escape)

        Highlight.__init__(self, name,
                           branded_name + " " +
                           _("scanned") + " " +
                           "%(traffic)s" + " " +
                           _("GB") +
                           " " + _("and") + " " +
                           "%(sessions)s" + " " + _("sessions") +
                           " " + _("and") + " " + 
                           _("logged") + " " + 
                           "%(alerts)s" + " " + _("alerts"))

    @sql_helper.print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT (SELECT round((COALESCE(sum(s2c_bytes + c2s_bytes), 0) / 1000000000)::numeric, 2)
        FROM reports.session_totals
        WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone) AS traffic,
       (SELECT COALESCE(sum(num_sessions), 0)::int
        FROM reports.session_counts
        WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone) AS sessions,
       (SELECT COALESCE(count(*), 0)::int
        FROM reports.alerts
        WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone) AS alerts"""

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed, one_week, ed, one_week, ed))

            h = sql_helper.get_result_dictionary(curs)

        finally:
            conn.commit()

        return h

class BandwidthUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-usage', _('Bandwidth Usage'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        lks = []

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            # kB
            sums = ["coalesce(sum(s2c_bytes + c2s_bytes), 0) / 1000",]

            extra_where = []
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            time_interval = 3600
            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            throughput = []

            for r in curs.fetchall():
                dates.append(r[0])
                throughput.append(float(r[1]) / time_interval)

            if not throughput:
                throughput = [0,]

            ks = KeyStatistic(_('Avg Data Rate'),
                              sum(throughput)/len(throughput),
                              N_('kB/s'))
            lks.append(ks)
            #ks = KeyStatistic(_('Max Data Rate'), max(throughput), N_('kB/s'))
            #lks.append(ks)
            ks = KeyStatistic(_('Data Transferred'), sum(throughput) * time_interval, N_('kB'))
            lks.append(ks)


        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Date'), ylabel=_('Throughput (kB/s)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, throughput, _('Usage'))

        return (lks, plot)

class SessionsPerMinute(Graph):
    def __init__(self):
        Graph.__init__(self, 'sessions-per-minute', _('Sessions per Minute'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        lks = []

        conn = sql_helper.get_connection()
        try:
            # per minute
            sums = ["coalesce(sum(num_sessions), 0)"]

            extra_where = []
            if host:
                extra_where.append(("hostname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("username = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.session_counts",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where)

            curs = conn.cursor()

            curs.execute(q, h)

            dates = []
            num_sessions = []

            for r in curs.fetchall():
                dates.append(r[0])
                num_sessions.append(float(r[1]))

            if not num_sessions:
                num_sessions = [0,]

            ks = KeyStatistic(_('Avg Sessions/Min'),
                              int(sum(num_sessions)/len(num_sessions)),
                              _('Sessions'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Sessions/Min'),
                              int(max(num_sessions)), _('Sessions'))
            lks.append(ks)
            ks = KeyStatistic(_('Total Sessions'), int(sum(num_sessions)), _('sessions'))
            lks.append(ks)
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Date'), ylabel=_('Sessions'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, num_sessions, _("Usage"))

        return (lks, plot)

class DestinationPorts(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-dest-ports', _('Top Destination Ports'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT c_server_port, sum(new_sessions)::int as sessions
FROM reports.session_totals
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone"""

        if host:
            query += " AND hostname = %s"
        elif user:
            query += " AND username = %s"

        query += """
GROUP BY c_server_port
ORDER BY sessions DESC"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))


            lks = []
            pds = {}

            for r in curs.fetchall():
                port = r[0]
                sessions = r[1]
                ks = KeyStatistic(str(port), sessions, _('Sessions'))
                lks.append(ks)
                pds[port] = sessions
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Port'),
                     ylabel=_('Sessions'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class HostsByPenalty(Graph):
    def __init__(self):
        Graph.__init__(self, 'hosts-by-penalty', _('Top Penalty Box Hosts'))

    @sql_helper.print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email or host or user:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        lks = []

        query = """
SELECT address, 
       ROUND(COALESCE(EXTRACT('epoch' FROM sum(end_time - start_time)::interval),0)::numeric,1) AS time
FROM reports.penaltybox
WHERE start_time >= %s AND start_time < %s"""

        query += " GROUP BY address ORDER BY time DESC"

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(query, (one_week, ed))

            lks = []
            pds = {}

            for r in curs.fetchall():
                address = r[0]
                time = r[1]
                ks = KeyStatistic(address, time, _('seconds'), link_type=reports.HNAME_LINK)
                lks.append(ks)
                pds[address] = time
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Address'), ylabel=_('seconds'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class AdministrativeLoginsDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'admin-logins-events', _('Administrative Login Events'))

    def get_columns(self, host=None, user=None, email=None):
        if host or user or email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        rv += [ColumnDesc('client_addr', _('Client Ip')),
               ColumnDesc('login', _('Login')),
               ColumnDesc('succeeded', _('Success'))]

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, host(client_addr) as client_addr, login, succeeded::text
FROM reports.admin_logins
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone AND not local
""" % (DateFromMx(start_date), DateFromMx(end_date))

        return sql + " ORDER BY time_stamp DESC"

class AlertEventsDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'alert-events', _('Alert Events'))

    def get_columns(self, host=None, user=None, email=None):
        if host or user or email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        rv += [ColumnDesc('description', _('Description')),
               ColumnDesc('summary_text', _('Summary')),
               ColumnDesc('json', _('JSON'))]

        return rv
    
    def get_all_columns(self, host=None, user=None, email=None):
        return self.get_columns(host, user, email)

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, description, summary_text, json
FROM reports.alerts
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone
""" % (DateFromMx(start_date), DateFromMx(end_date))

        return sql + " ORDER BY time_stamp DESC"


reports.engine.register_node(UvmNode())
