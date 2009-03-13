import logging
import psycopg
import reports.engine
import sql_helper

from sql_helper import print_timing
from reports.engine import Node
from psycopg import DateFromMx

EVT_TYPE_REGISTER = 0
EVT_TYPE_RENEW    = 1
EVT_TYPE_EXPIRE   = 2
EVT_TYPE_RELEASE  = 3

class UvmNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-vm')

    @print_timing
    def setup(self, start_date, end_date):
        self.__generate_address_map(start_date, end_date)

        self.__make_sessions_table(start_date, end_date)

        self.__create_users_table(start_date, end_date)
        self.__create_hnames_table(start_date, end_date)

    @print_timing
    def __create_users_table(self, start_date, end_date):
        sql_helper.create_table_from_query('reports.users', """\
SELECT DISTINCT username FROM events.u_lookup_evt
WHERE time_stamp >= %s AND time_stamp < %s""", (DateFromMx(start_date),
                                                DateFromMx(end_date)))

    @print_timing
    def __create_hnames_table(self, start_date, end_date):
        sql_helper.create_table_from_query('reports.hnames', """\
SELECT DISTINCT hname FROM reports.sessions
WHERE time_stamp >= %s AND time_stamp < %s AND client_intf=1""",
                                           (DateFromMx(start_date),
                                            DateFromMx(end_date)))

    @print_timing
    def __make_sessions_table(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.sessions (
        pl_endp_id int8 NOT NULL,
        time_stamp timestamp NOT NULL,
        hname text,
        uid text,
        c_client_addr inet,
        c_server_addr inet,
        c_server_port int4,
        client_intf int2,
        c2p_bytes int8,
        p2c_bytes int8,
        s2p_bytes int8,
        p2s_bytes int8,
        PRIMARY KEY (pl_endp_id));
""", 'time_stamp', start_date, end_date)

        sd = DateFromMx(sql_helper.get_update_info('reports.sessions', start_date))
        ed = DateFromMx(end_date)

        conn = sql_helper.get_connection()

        try:
            sql_helper.run_sql("""\
CREATE TEMPORARY TABLE newsessions AS
    SELECT endp.event_id, endp.time_stamp, mam.name,
           endp.c_client_addr, endp.c_server_addr, endp.c_server_port,
           endp.client_intf
    FROM events.pl_endp endp
    LEFT OUTER JOIN reports.merged_address_map mam
      ON (endp.c_client_addr = mam.addr AND endp.time_stamp >= mam.start_time
         AND endp.time_stamp < mam.end_time)
    WHERE endp.time_stamp >= %s AND endp.time_stamp < %s
""", (sd, ed), connection=conn, auto_commit=False)

            sql_helper.run_sql("""\
INSERT INTO reports.sessions
  SELECT ses.event_id, ses.time_stamp,
         COALESCE(NULLIF(ses.name, ''), HOST(ses.c_client_addr)) AS hname,
         stats.uid, c_client_addr, c_server_addr, c_server_port, client_intf,
         stats.c2p_bytes, stats.p2c_bytes, stats.s2p_bytes, stats.p2s_bytes
    FROM newsessions ses
    JOIN events.pl_stats stats ON (ses.event_id = stats.pl_endp_id)""",
                               connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.sessions', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e


    def teardown(self):
        print "TEARDOWN"

    @print_timing
    def __generate_address_map(self, start_date, end_date):
        self.__do_housekeeping()

        m = {}

        if self.__nat_installed():
            self.__generate_abs_leases(m, start_date, end_date)
            self.__generate_relative_leases(m, start_date, end_date)
            self.__generate_static_leases(m, start_date, end_date)

        self.__generate_manual_map(m, start_date, end_date)

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
WHERE glue.event_id=evt.event_id AND glue.lease_id = lease.event_id
      AND ((%s <= evt.time_stamp and evt.time_stamp <= %s)
      OR ((%s <= lease.end_of_lease and lease.end_of_lease <= %s)))
ORDER BY evt.time_stamp""", start_date, end_date)

    @print_timing
    def __generate_relative_leases(self, m, start_date, end_date):
        self.__generate_leases(m, """\
SELECT evt.time_stamp, evt.end_of_lease, evt.ip, evt.hostname, evt.event_type
FROM events.n_router_evt_dhcp AS evt
WHERE (%s <= evt.time_stamp AND evt.time_stamp <= %s)
    OR (%s <= evt.end_of_lease AND evt.end_of_lease <= %s)
ORDER BY evt.time_stamp""", start_date, end_date)

    @print_timing
    def __generate_static_leases(self, m, start_date, end_date):
        conn = sql_helper.get_connection()

        try:
            curs = conn.cursor()

            curs.execute("""\
SELECT hostname_list, static_address
FROM settings.u_dns_static_host_rule AS rule,
     settings.n_router_dns_hosts AS list,
     settings.n_router_settings AS settings
WHERE rule.rule_id = list.rule_id
      AND settings.settings_id = list.setting_id""")

            while 1:
                r = curs.fetchone()
                if not r:
                    break

                (hostname, ip) = r

                hostname = hostname.split(" ")[0]

                m[ip] = [Lease((start_date, end_date, ip, hostname, None))]
        finally:
            conn.commit()

    @print_timing
    def __generate_manual_map(self, m, start_date, end_date):
        conn = sql_helper.get_connection()

        try:
            curs = conn.cursor()

            curs.execute("""\
SELECT addr, name
FROM (SELECT addr, min(position) AS min_idx
      FROM (SELECT c_client_addr AS addr
            FROM events.pl_endp WHERE pl_endp.client_intf = 1
            UNION
            SELECT c_server_addr AS addr
            FROM events.pl_endp WHERE pl_endp.server_intf = 1
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
ON min_idx = position""")

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

    def values(self, ):
        return (self.ip, self.hostname, DateFromMx(self.start),
                DateFromMx(self.end_of_lease))


reports.engine.register_node(UvmNode())
