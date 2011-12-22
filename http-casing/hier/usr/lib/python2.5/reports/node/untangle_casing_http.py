import reports.engine
import reports.sql_helper as sql_helper

import mx
import sys

from psycopg2.extensions import DateFromMx
from psycopg2.extensions import TimestampFromMx
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.sql_helper import print_timing

class HttpCasing(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-casing-http')

    def parents(self):
        return ['untangle-vm']

    @print_timing
    def setup(self, start_date, end_date):
        self.__create_n_http_events(start_date, end_date)

        ft = FactTable('reports.n_http_totals', 'reports.n_http_events',
                       'time_stamp',
                       [Column('hname', 'text'), Column('uid', 'text'),
                        Column('host', 'text'),
                        Column('s2c_content_type', 'text')],
                       [Column('hits', 'bigint', 'count(*)'),
                        Column('c2s_content_length', 'bigint',
                                   'sum(c2s_content_length)'),
                        Column('s2c_content_length', 'bigint',
                                   'sum(s2c_content_length)'),
                        Column('s2c_bytes', 'bigint', 'sum(p2c_bytes)'),
                        Column('c2s_bytes', 'bigint', 'sum(p2s_bytes)')])
        reports.engine.register_fact_table(ft)

    @print_timing
    def __create_n_http_events(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.n_http_events (
    time_stamp timestamp without time zone,
    session_id integer, client_intf smallint,
    server_intf smallint,
    c_client_addr inet, s_client_addr inet, c_server_addr inet,
    s_server_addr inet,
    c_client_port integer, s_client_port integer, c_server_port integer,
    s_server_port integer,
    policy_id bigint, policy_inbound boolean,
    c2p_bytes bigint, s2p_bytes bigint, p2c_bytes bigint, p2s_bytes bigint,
    c2p_chunks bigint, s2p_chunks bigint, p2c_chunks bigint, p2s_chunks bigint,
    uid text,
    request_id bigint, method character(1), uri text,
    host text, c2s_content_length integer,
    s2c_content_length integer, s2c_content_type text,
    hname text,
    wf_esoft_reason character,
    wf_esoft_category text,
    virus_clam_clean boolean,
    virus_clam_name text,
    sw_blacklisted boolean,
    sw_cookie_ident text,
    virus_kaspersky_clean boolean,
    virus_kaspersky_name text,
    wf_untangle_reason character,
    wf_untangle_category text,
    event_id bigserial,
    ab_action character,
    wf_untangle_blocked boolean,
    wf_untangle_flagged boolean,
    wf_esoft_blocked boolean,
    wf_esoft_flagged boolean,
    virus_commtouch_clean boolean,
    virus_commtouch_name text)""", 'time_stamp', start_date, end_date)

        sql_helper.add_column('reports.n_http_events', 'event_id', 'bigserial')
        sql_helper.add_column('reports.n_http_events', 'ab_action', 'character(1)')
        sql_helper.add_column('reports.n_http_events', 'sw_cookie_ident', 'text')
        sql_helper.add_column('reports.n_http_events', 'sw_blacklisted', 'boolean')
        sql_helper.add_column('reports.n_http_events', 'start_time', 'timestamp')

        # we used to create event_id as serial instead of bigserial - convert if necessary
        sql_helper.convert_column("reports","n_http_events","event_id","integer","bigint");

        for vendor in ("untangle", "esoft"):
            sql_helper.drop_column('reports.n_http_events', 'wf_%s_action' % vendor)
            sql_helper.add_column('reports.n_http_events', 'wf_%s_reason' % vendor, 'character(1)')
            sql_helper.add_column('reports.n_http_events', 'wf_%s_category' % vendor, 'text')
            sql_helper.add_column('reports.n_http_events', 'wf_%s_blocked' % vendor, 'boolean')
            sql_helper.add_column('reports.n_http_events', 'wf_%s_flagged' % vendor, 'boolean')

        for vendor in ("clam", "kaspersky", "commtouch"):
            sql_helper.add_column('reports.n_http_events', 'virus_%s_clean' % vendor, 'boolean')
            sql_helper.add_column('reports.n_http_events', 'virus_%s_name' % vendor, 'text')

        sql_helper.run_sql('CREATE INDEX n_http_events_request_id_idx ON reports.n_http_events(request_id)')
        sql_helper.run_sql('CREATE INDEX n_http_events_event_id_idx ON reports.n_http_events(event_id)')
        sql_helper.run_sql('CREATE INDEX n_http_events_policy_id_idx ON reports.n_http_events(policy_id)')
        sql_helper.run_sql('CREATE INDEX n_http_events_time_stamp_idx ON reports.n_http_events(time_stamp)')

        # web filter event log indexes
        sql_helper.run_sql('CREATE INDEX n_http_events_wf_esoft_blocked_idx ON reports.n_http_events(wf_esoft_blocked)')
        sql_helper.run_sql('CREATE INDEX n_http_events_wf_esoft_flagged_idx ON reports.n_http_events(wf_esoft_flagged)')
        sql_helper.run_sql('CREATE INDEX n_http_events_wf_esoft_category_idx ON reports.n_http_events(wf_esoft_category)')

        # web filter lite event log indexs
        # sql_helper.run_sql('CREATE INDEX n_http_events_wf_untangle_blocked_idx ON reports.n_http_events(wf_untangle_blocked)')
        # sql_helper.run_sql('CREATE INDEX n_http_events_wf_untangle_flagged_idx ON reports.n_http_events(wf_untangle_flagged)')
        # sql_helper.run_sql('CREATE INDEX n_http_events_wf_untangle_category_idx ON reports.n_http_events(wf_untangle_category)')

        # virus blockers(s)
        # sql_helper.run_sql('CREATE INDEX n_http_events_virus_commtouch_clean_idx ON reports.n_http_events(virus_commtouch_clean)')
        # sql_helper.run_sql('CREATE INDEX n_http_events_virus_clam_clean_idx ON reports.n_http_events(virus_clam_clean)')
        # sql_helper.run_sql('CREATE INDEX n_http_events_virus_kaspersky_clean_idx ON reports.n_http_events(virus_kaspersky_clean)')

        # ad blocker
        # sql_helper.run_sql('CREATE INDEX n_http_events_ab_action_idx ON reports.n_http_events(ab_action)')

        # spyware blocker
        # sql_helper.run_sql('CREATE INDEX n_http_events_sw_blacklisted_idx ON reports.n_http_events(sw_blacklisted)')
        # sql_helper.run_sql('CREATE INDEX n_http_events_sw_cookie_ident_idx ON reports.n_http_events(sw_cookie_ident)')


        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_http_events
      (time_stamp, session_id, client_intf, server_intf, c_client_addr,
       s_client_addr, c_server_addr, s_server_addr, c_client_port,
       s_client_port, c_server_port, s_server_port, policy_id, policy_inbound,
       c2p_bytes, s2p_bytes, p2c_bytes, p2s_bytes, c2p_chunks, s2p_chunks,
       p2c_chunks, p2s_chunks, uid, request_id, method, uri, host,
       c2s_content_length, s2c_content_length, s2c_content_type, hname)
    SELECT
        -- timestamp from request
        req.time_stamp,
        -- pipeline endpoints
        ps.session_id, ps.client_intf, ps.server_intf,
        ps.c_client_addr, ps.s_client_addr, ps.c_server_addr, ps.s_server_addr,
        ps.c_client_port, ps.s_client_port, ps.c_server_port, ps.s_server_port,
        ps.policy_id, ps.policy_inbound,
        -- pipeline stats
        ps.c2p_bytes, ps.s2p_bytes, ps.p2c_bytes, ps.p2s_bytes, ps.c2p_chunks,
        ps.s2p_chunks, ps.p2c_chunks, ps.p2s_chunks, ps.uid,
        -- n_http_req_line
        req.request_id, req.method, req.uri,
        -- n_http_evt_req
        er.host, er.content_length,
        -- n_http_evt_resp
        resp.content_length, resp.content_type,
        -- from webpages
        COALESCE(NULLIF(mam.name, ''), host(c_client_addr)) AS hname
    FROM events.pl_stats ps
    JOIN events.n_http_req_line req ON ps.pl_endp_id = req.pl_endp_id
    JOIN events.n_http_evt_req er ON er.request_id = req.request_id
    LEFT OUTER JOIN events.n_http_evt_resp resp on req.request_id = resp.request_id
    LEFT OUTER JOIN reports.merged_address_map mam
        ON ps.c_client_addr = mam.addr AND ps.time_stamp >= mam.start_time AND ps.time_stamp < mam.end_time""",
                               (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def events_cleanup(self, cutoff, safety_margin):
        # first clean up all rows n_http_req_line that join with pl_stats (they have been harvested)
        sql_helper.run_sql("""\
DELETE FROM events.n_http_req_line WHERE pl_endp_id IN (SELECT pl_endp_id FROM events.pl_stats) OR (time_stamp < %s - interval %s);""", (cutoff,safety_margin))
        # now clean up all rows in n_http_evt_req  that do NOT join with n_http_req_line (they have been harvested)
        sql_helper.run_sql("""\
DELETE FROM events.n_http_evt_req WHERE request_id NOT IN (select request_id FROM events.n_http_req_line) OR (time_stamp < %s - interval %s);""", (cutoff,safety_margin))
        # now clean up all rows in n_http_evt_resp that do NOT join with n_http_req_line (they have been harvested)
        sql_helper.run_sql("""\
DELETE FROM events.n_http_evt_resp WHERE request_id NOT IN (select request_id FROM events.n_http_req_line) OR (time_stamp < %s - interval %s);""", (cutoff,safety_margin))

    def reports_cleanup(self, cutoff):
        sql_helper.drop_partitioned_table("n_http_events", cutoff)
        sql_helper.drop_partitioned_table("n_http_totals", cutoff)        

reports.engine.register_node(HttpCasing())
