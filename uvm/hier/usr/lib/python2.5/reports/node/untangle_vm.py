import gettext
import logging
import mx
import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper

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

EVT_TYPE_REGISTER = 0
EVT_TYPE_RENEW    = 1
EVT_TYPE_EXPIRE   = 2
EVT_TYPE_RELEASE  = 3

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext
def N_(message): return message

class UvmNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-vm')

    @print_timing
    def setup(self, start_date, end_date):
        self.__generate_address_map(start_date, end_date)

        self.__create_n_admin_logins(start_date, end_date)

        self.__make_sessions_table(start_date, end_date)

        ft = FactTable('reports.session_totals',
                       'reports.sessions',
                       'time_stamp',
                       [Column('hname', 'text'),
                        Column('uid', 'text'),
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

    @print_timing
    def __get_branded_name(self):
        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            curs.execute("SELECT company_name FROM n_branding_settings")
            while 1:
                r = curs.fetchone()
                if not r:
                    break
                return r[0]
        except Exception, e:
            return self.name
        finally:
            conn.commit()
        
    @print_timing
    def __create_n_admin_logins(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.n_admin_logins (
    time_stamp timestamp without time zone,
    login text,
    local boolean,
    client_addr inet,
    succeeded boolean,
    reason char(1) )""", 'time_stamp', start_date, end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_admin_logins
      (time_stamp, login, local, client_addr, succeeded, reason)
    SELECT time_stamp, login, local, client_addr, succeeded, reason
    FROM events.u_login_evt evt""",
                               (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def post_facttable_setup(self, start_date, end_date):
        self.__make_session_counts_table(start_date, end_date)
        self.__make_hnames_table(start_date, end_date)
        self.__make_users_table(start_date, end_date)

    def events_cleanup(self, cutoff):
        sql_helper.run_sql("""\
DELETE FROM events.u_lookup_evt WHERE time_stamp < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.pl_endp WHERE time_stamp < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.pl_stats WHERE time_stamp < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.n_router_evt_dhcp_abs WHERE time_stamp < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.n_router_evt_dhcp WHERE time_stamp < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.n_router_dhcp_abs_lease WHERE end_of_lease < %s""", (cutoff,))

        sql_helper.run_sql("""\
DELETE FROM events.n_router_evt_dhcp_abs_leases glue
WHERE NOT EXISTS
  (SELECT *
   FROM events.n_router_evt_dhcp_abs evt
   WHERE evt.event_id = glue.event_id)""")

        sql_helper.run_sql("""\
DELETE FROM events.u_login_evt WHERE time_stamp < %s""", (cutoff,))

    def reports_cleanup(self, cutoff):
        sql_helper.drop_partitioned_table("n_admin_logins", cutoff)
        sql_helper.drop_partitioned_table("users", cutoff)
        sql_helper.drop_partitioned_table("hnames", cutoff)
        sql_helper.drop_partitioned_table("sessions", cutoff)
        sql_helper.drop_partitioned_table("session_totals", cutoff)        
        sql_helper.drop_partitioned_table("session_counts", cutoff)

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [VmHighlight(self.name, self.branded_name),
                            BandwidthUsage(),
                            ActiveSessions(),
                            DestinationPorts()])

        sections.append(s)
        sections.append(AdministrativeLoginsDetail())

        return Report(self, sections)

    @print_timing
    def __make_users_table(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.users (
        date date NOT NULL,
        username text NOT NULL,
        PRIMARY KEY (date, username));
""", 'date', start_date, end_date)

        sd = sql_helper.get_max_timestamp_with_interval('reports.users')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.users (date, username)
    SELECT DISTINCT date_trunc('day', trunc_time)::date AS day, uid
    FROM reports.session_totals
    WHERE trunc_time >= %s
    AND NOT uid ISNULL""", (sd,), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            print e
            conn.rollback()
            raise e

    @print_timing
    def __make_hnames_table(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.hnames (
        date date NOT NULL,
        hname text NOT NULL,
        PRIMARY KEY (date, hname));
""", 'date', start_date, end_date)

        sd = sql_helper.get_max_timestamp_with_interval('reports.hnames')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.hnames (date, hname)
    SELECT DISTINCT date_trunc('day', trunc_time)::date, hname
    FROM reports.session_totals
    WHERE trunc_time >= %s
    AND server_intf IN """ + get_wan_clause() + """
    AND client_intf NOT IN """ + get_wan_clause() + """
    AND NOT hname ISNULL""", (sd,),
                               connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            print e
            conn.rollback()
            raise e

    @print_timing
    def __make_sessions_table(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.sessions (
        pl_endp_id int8 NOT NULL,
        event_id serial,
        time_stamp timestamp NOT NULL,
        end_time timestamp NOT NULL,
        hname text,
        uid text,
        policy_id bigint,
        c_client_addr inet,
        c_server_addr inet,
        c_server_port int4,
        c_client_port int4,
        client_intf int2,
        server_intf int2,
        c2p_bytes int8,
        p2c_bytes int8,
        s2p_bytes int8,
        p2s_bytes int8)""", 'time_stamp', start_date, end_date)

        sql_helper.add_column('reports.sessions', 'event_id', 'serial')
        sql_helper.add_column('reports.sessions', 'policy_id', 'bigint')
        sql_helper.add_column('reports.sessions', 'server_intf', 'int2')
        sql_helper.add_column('reports.sessions', 'bandwidth_priority', 'bigint')
        sql_helper.add_column('reports.sessions', 'bandwidth_rule', 'bigint')
        sql_helper.add_column('reports.sessions', 'firewall_was_blocked', 'boolean')
        sql_helper.add_column('reports.sessions', 'firewall_rule_index', 'integer')
        sql_helper.add_column('reports.sessions', 'firewall_rule_description', 'text')
        sql_helper.add_column('reports.sessions', 'pf_protocol', 'text')
        sql_helper.add_column('reports.sessions', 'pf_blocked', 'boolean')
        sql_helper.add_column('reports.sessions', 'ips_blocked', 'boolean')
        sql_helper.add_column('reports.sessions', 'ips_name', 'text')
        sql_helper.add_column('reports.sessions', 'ips_description', 'text')
        sql_helper.add_column('reports.sessions', 'sw_access_ident', 'text')

        sql_helper.run_sql('CREATE INDEX sessions_pl_endp_id_idx ON reports.sessions(pl_endp_id)')
        sql_helper.run_sql('CREATE INDEX sessions_event_id_idx ON reports.sessions(event_id)')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""
CREATE TEMPORARY TABLE newsessions AS
    SELECT stats.pl_endp_id, stats.time_stamp, stats.policy_id, uid, mam.name,
           stats.c_client_addr, stats.c_server_addr, stats.c_server_port,
           stats.c_client_port, stats.client_intf, stats.server_intf,
           stats.c2p_bytes, stats.p2c_bytes, stats.s2p_bytes, stats.p2s_bytes
    FROM events.pl_stats stats
    LEFT OUTER JOIN reports.merged_address_map mam
      ON (stats.c_client_addr = mam.addr AND stats.time_stamp >= mam.start_time
         AND stats.time_stamp < mam.end_time)""", (), connection=conn, auto_commit=False)

            sql_helper.run_sql("""
INSERT INTO reports.sessions
    (pl_endp_id, event_id, time_stamp, end_time, hname, uid, policy_id, c_client_addr,
     c_server_addr, c_server_port, c_client_port, client_intf, server_intf,
     c2p_bytes, p2c_bytes, s2p_bytes, p2s_bytes)
    SELECT ses.pl_endp_id, ses.pl_endp_id, ses.time_stamp, ses.time_stamp,
         COALESCE(NULLIF(ses.name, ''), HOST(ses.c_client_addr)) AS hname,
         ses.uid, policy_id, c_client_addr, c_server_addr, c_server_port,
         c_client_port, client_intf, server_intf, ses.c2p_bytes,
         ses.p2c_bytes, ses.s2p_bytes, ses.p2s_bytes
    FROM newsessions ses""",
                               connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @print_timing
    def __make_session_counts_table(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.session_counts (
        trunc_time timestamp,
        uid text,
        hname text,
        client_intf smallint,
        server_intf smallint,
        num_sessions int8)""", 'trunc_time', start_date, end_date)

        sql_helper.add_column('reports.session_counts', 'client_intf',
                              'smallint')
        sql_helper.add_column('reports.session_counts', 'server_intf',
                              'smallint')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.session_counts
    (trunc_time, uid, hname, client_intf, server_intf, num_sessions)
SELECT (date_trunc('minute', time_stamp)
        + (generate_series(0, (extract('epoch' from (end_time - time_stamp))
        / 60)::int) || ' minutes')::interval) AS time, uid, hname,
        client_intf, server_intf, count(*)
FROM reports.sessions
GROUP BY time, uid, hname, client_intf, server_intf
""", (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def teardown(self):
        pass

    @print_timing
    def __generate_address_map(self, start_date, end_date):
        self.__do_housekeeping()

        m = {}

        if self.__nat_installed():
            self.__generate_abs_leases(m, start_date, end_date)
            self.__generate_relative_leases(m, start_date, end_date)

        self.__generate_manual_map(m, start_date, 
                                   mx.DateTime.now())

        self.__write_leases(m)

    @print_timing
    def __do_housekeeping(self):
        sql_helper.run_sql("""\
DELETE FROM settings.n_reporting_settings WHERE tid NOT IN
(SELECT tid FROM settings.u_node_persistent_state
WHERE NOT target_state = 'destroyed')""")

        sql_helper.run_sql("""\
DELETE FROM settings.u_ipmaddr_dir_entries WHERE ipmaddr_dir_id NOT IN
(SELECT id FROM settings.u_ipmaddr_dir WHERE id IN
(SELECT network_directory FROM settings.n_reporting_settings))""")

        sql_helper.run_sql("""\
DELETE FROM settings.u_ipmaddr_dir WHERE id NOT IN
(SELECT network_directory FROM settings.n_reporting_settings)""")

        if sql_helper.table_exists('reports', 'merged_address_map'):
            sql_helper.run_sql("DROP TABLE reports.merged_address_map");

        sql_helper.run_sql("""\
CREATE TABLE reports.merged_address_map (
    id         SERIAL8 NOT NULL,
    addr       INET NOT NULL,
    name       VARCHAR(255),
    start_time TIMESTAMP NOT NULL,
    end_time   TIMESTAMP,
    PRIMARY KEY (id))""")

    @print_timing
    def __write_leases(self, m):
        values = []

        for v in m.values():
            for l in v:
                if l.hostname:
                    values.append(l.values())

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.executemany("""\
INSERT INTO reports.merged_address_map (addr, name, start_time, end_time)
VALUES (%s, %s, %s, %s)""", values)

        finally:
            conn.commit()

    def __nat_installed(self):
        return sql_helper.table_exists('events',
                                       'n_router_evt_dhcp_abs_leases')

    @print_timing
    def __generate_abs_leases(self, m, start_date, end_date):
        self.__generate_leases(m, """\
SELECT evt.time_stamp, lease.end_of_lease, lease.ip, lease.hostname,
       CASE WHEN (lease.event_type = 0) THEN 0 ELSE 3 END AS event_type
FROM events.n_router_evt_dhcp_abs_leases AS glue,
     events.n_router_evt_dhcp_abs AS evt,
     events.n_router_dhcp_abs_lease AS lease
WHERE lease.hostname != '' AND glue.event_id = evt.event_id
      AND glue.lease_id = lease.event_id
      AND ((%s <= evt.time_stamp and evt.time_stamp <= %s)
      OR ((%s <= lease.end_of_lease and lease.end_of_lease <= %s)))
ORDER BY evt.time_stamp""", start_date, end_date)

    @print_timing
    def __generate_relative_leases(self, m, start_date, end_date):
        self.__generate_leases(m, """\
SELECT evt.time_stamp, evt.end_of_lease, evt.ip, evt.hostname, evt.event_type
FROM events.n_router_evt_dhcp AS evt
WHERE hostname != '' AND ((%s <= evt.time_stamp AND evt.time_stamp <= %s)
    OR (%s <= evt.end_of_lease AND evt.end_of_lease <= %s))
ORDER BY evt.time_stamp""", start_date, end_date)

    @print_timing
    def __generate_manual_map(self, m, start_date, end_date):
        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute("""\
SELECT addr, name
FROM (SELECT addr, min(position) AS min_idx
      FROM (SELECT c_client_addr AS addr
            FROM events.pl_stats WHERE pl_stats.client_intf NOT IN %s
            UNION
            SELECT c_server_addr AS addr
            FROM events.pl_stats WHERE pl_stats.server_intf NOT IN %s
            UNION
            SELECT client_addr AS addr
            FROM events.u_login_evt) AS addrs
      JOIN settings.u_ipmaddr_dir_entries entry
      JOIN settings.u_ipmaddr_rule rule USING (rule_id)
      ON rule.ipmaddr >>= addr
      WHERE NOT addr ISNULL
      GROUP BY addr) AS pos_idxs
LEFT OUTER JOIN settings.u_ipmaddr_dir_entries entry
JOIN settings.u_ipmaddr_rule rule USING (rule_id)
ON min_idx = position""" % (2*(reports.engine.get_wan_clause(),)))

            while 1:
                r = curs.fetchone()
                if not r:
                    break

                (ip, hostname) = r

                m[ip] = [Lease((start_date, end_date, ip, hostname, None))]
        finally:
            conn.commit()

    def __generate_leases(self, m, q, start_date, end_date):
        st = DateFromMx(start_date)
        et = DateFromMx(end_date)

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            curs.execute(q, (st, et, st, et))

            while 1:
                r = curs.fetchone()
                if not r:
                    break

                self.__insert_lease(m, Lease(r))
        finally:
            conn.commit()

    def __insert_lease(self, m, event):
        et = event.event_type

        if et == EVT_TYPE_REGISTER or et == EVT_TYPE_RENEW:
            self.__merge_event(m, event)
        elif et == EVT_TYPE_RELEASE or et == EVT_TYPE_EXPIRE:
            self.__truncate_event(m, event)
        else:
            logging.warn('do not know type: %d' % et)

    def __merge_event(self, m, event):
        l = m.get(event.ip, None)

        if not l:
            m[event.ip] = [event]
        else:
            for (index, lease) in enumerate(l):
                same_hostname = lease.hostname = event.hostname

                if lease.after(event):
                    l.insert(index, lease)
                    return
                elif lease.intersects_before(event):
                    if same_hostname:
                        lease.start = event.start
                        return
                    else:
                        event.end_of_lease = lease.start
                        l.insert(index, lease)
                        return
                elif lease.encompass(event):
                    if same_hostname:
                        return
                    else:
                        lease.end_of_lease = event.start
                        l.insert(index + 1, lease)
                        return
                elif lease.intersects_after(event):
                    if same_hostname:
                        lease.end_of_lease = event.end_of_lease
                    else:
                        lease.end_of_lease = event.start
                        index = index + 1
                        l.insert(index, event)

                    if index + 1 < len(l):
                        index = index + 1
                        next_lease = l[index]

                        if (next_lease.start > lease.start
                            and next_lease.start < lease.end_of_lease):
                            if next_lease.hostname == lease.hostname:
                                del(l[index])
                                lease.end_of_lease = next_lease.end_of_lease
                            else:
                                lease.end_of_lease = next_lease.start
                    return
                elif lease.encompassed(event):
                    lease.start = event.start
                    return

            l.append(event)

    def __truncate_event(self, m, event):
        l = m.get(event.ip, None)

        if l:
            for (index, lease) in enumerate(l):
                if (lease.start < event.start
                    and lease.end_of_lease > event.start):
                    lease.end_of_lease = event.start
                    return

class VmHighlight(Highlight):
    def __init__(self, name, branded_name):
        xml_escapes = { '&' : '&amp;',
                        '>' : '&gt;',
                        '<' : '&lt;',
                        "'" : '&apos',
                        '"' : '&quot;' }

        for char, escape in xml_escapes.iteritems():
            branded_name = branded_name.replace(char, escape)

        Highlight.__init__(self, name,
                           branded_name + " " +
                           _("scanned") + " " +
                           "%(traffic)s" + " " +
                           _("GB") +
                           " " + _("and") + " " +
                           "%(sessions)s" + " " + _("sessions"))

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT (SELECT round((COALESCE(sum(s2c_bytes + c2s_bytes), 0) / 1000000000)::numeric, 2)
        FROM reports.session_totals
        WHERE trunc_time >= %s AND trunc_time < %s) AS traffic,
       (SELECT COALESCE(sum(num_sessions), 0)::int
        FROM reports.session_counts
        WHERE trunc_time >= %s AND trunc_time < %s) AS sessions"""

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed, one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h

class BandwidthUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-usage', _('Bandwidth Usage'))

    @print_timing
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
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            time_interval = 60
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
            ks = KeyStatistic(_('Max Data Rate'), max(throughput), N_('kB/s'))
            lks.append(ks)
            ks = KeyStatistic(_('Data Transferred'), sum(throughput) * time_interval, N_('kB'))
            lks.append(ks)

                
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Date'), ylabel=_('Throughput (kB/s)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, throughput, _('Usage'))

        return (lks, plot)

class ActiveSessions(Graph):
    def __init__(self):
        Graph.__init__(self, 'active-sessions', _('Active Sessions'))

    @print_timing
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
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

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

            ks = KeyStatistic(_('Avg Active Sessions'),
                              int(sum(num_sessions)/len(num_sessions)),
                              _('Sessions'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Active Sessions'),
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

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT c_server_port, sum(new_sessions)::int as sessions
FROM reports.session_totals
WHERE trunc_time >= %s AND trunc_time < %s"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

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

class Lease:
    def __init__(self, row):
        self.start = row[0]
        self.end_of_lease = row[1]
        self.ip = row[2]
        self.hostname = row[3]
        self.event_type = row[4]

    def after(self, event):
        return self.start > event.end_of_lease

    def intersects_before(self, event):
        return ((self.start > event.start
                 and self.start < event.end_of_lease)
                and (self.end_of_lease > event.end_of_lease
                     or self.end_of_lease == event.end_of_lease))

    def intersects_after(self, event):
        return ((self.start < event.start
                 and self.end_of_lease > event.start)
                and (self.end_of_lease == event.end_of_lease
                     or self.end_of_lease < event.end_of_lease))

    def encompass(self, event):
        return ((self.start == event.start or self.start < event.start)
                and (self.end_of_lease == event.end_of_lease
                     or self.end_of_lease > event.end_of_lease))

    def encompassed(self, event):
        return ((self.start == event.start or self.start > event.start)
                and ( self.end_of_lease == event.end_of_lease
                      or self.end_of_lease < event.end_of_lease))

    def values(self):
        return (self.ip, self.hostname,
                TimestampFromMx(sql_helper.date_convert(self.start)),
                TimestampFromMx(sql_helper.date_convert(self.end_of_lease)))

class AdministrativeLoginsDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'admin-logins-events', _('Administrative Logins Events'))

    def get_columns(self, host=None, user=None, email=None):
        if host or user or email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        rv += [ColumnDesc('client_addr', _('Client Ip')),
               ColumnDesc('succeeded', _('Success'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, host(client_addr), succeeded::text
FROM reports.n_admin_logins
WHERE time_stamp >= %s AND time_stamp < %s AND not local
""" % (DateFromMx(start_date), DateFromMx(end_date))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(UvmNode())
