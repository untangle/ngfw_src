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
                       [Column('hname', 'text'), 
                        Column('uid', 'text'),
                        Column('host', 'text'),
                        Column('s2c_content_type', 'text')],
                       [Column('hits', 'bigint', 'count(*)'),
                        Column('c2s_content_length', 'bigint', 'sum(c2s_content_length)'),
                        Column('s2c_content_length', 'bigint', 'sum(s2c_content_length)')])
        reports.engine.register_fact_table(ft)

    @print_timing
    def __create_n_http_events(self, start_date, end_date):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.n_http_events (
    time_stamp timestamp without time zone,
    session_id bigint, client_intf smallint,
    server_intf smallint,
    c_client_addr inet, s_client_addr inet, c_server_addr inet, s_server_addr inet,
    c_client_port integer, s_client_port integer, c_server_port integer, s_server_port integer,
    policy_id bigint, policy_inbound boolean,
    c2p_bytes bigint, s2p_bytes bigint, p2c_bytes bigint, p2s_bytes bigint,
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

        sql_helper.add_column('reports', 'n_http_events', 'event_id', 'bigserial')
        sql_helper.add_column('reports', 'n_http_events', 'ab_action', 'character(1)')
        sql_helper.add_column('reports', 'n_http_events', 'sw_cookie_ident', 'text')
        sql_helper.add_column('reports', 'n_http_events', 'sw_blacklisted', 'boolean')
        sql_helper.add_column('reports', 'n_http_events', 'start_time', 'timestamp')

        # we used to create event_id as serial instead of bigserial - convert if necessary
        sql_helper.convert_column("reports","n_http_events","event_id","integer","bigint");
        sql_helper.convert_column("reports","n_http_events","session_id","integer","bigint");
        
        # remove obsolete columns
        sql_helper.drop_column('reports', 'n_http_events', 'policy_inbound')
        sql_helper.drop_column('reports', 'n_http_events', 'c2p_chunks')
        sql_helper.drop_column('reports', 'n_http_events', 's2p_chunks')
        sql_helper.drop_column('reports', 'n_http_events', 'p2c_chunks')
        sql_helper.drop_column('reports', 'n_http_events', 'p2s_chunks')

        for vendor in ("untangle", "esoft"):
            sql_helper.drop_column('reports', 'n_http_events', 'wf_%s_action' % vendor)
            sql_helper.add_column('reports', 'n_http_events', 'wf_%s_reason' % vendor, 'character(1)')
            sql_helper.add_column('reports', 'n_http_events', 'wf_%s_category' % vendor, 'text')
            sql_helper.add_column('reports', 'n_http_events', 'wf_%s_blocked' % vendor, 'boolean')
            sql_helper.add_column('reports', 'n_http_events', 'wf_%s_flagged' % vendor, 'boolean')

        for vendor in ("clam", "kaspersky", "commtouch"):
            sql_helper.add_column('reports', 'n_http_events', 'virus_%s_clean' % vendor, 'boolean')
            sql_helper.add_column('reports', 'n_http_events', 'virus_%s_name' % vendor, 'text')

        sql_helper.create_index("reports","n_http_events","session_id");
        sql_helper.create_index("reports","n_http_events","request_id");
        sql_helper.create_index("reports","n_http_events","event_id");
        sql_helper.create_index("reports","n_http_events","policy_id");
        sql_helper.create_index("reports","n_http_events","time_stamp");

        # web filter event log indexes
        sql_helper.create_index("reports","n_http_events","wf_esoft_blocked");
        sql_helper.create_index("reports","n_http_events","wf_esoft_flagged");
        sql_helper.create_index("reports","n_http_events","wf_esoft_category");

        # web filter lite event log indexs
        # sql_helper.create_index("reports","n_http_events","wf_untangle_blocked");
        # sql_helper.create_index("reports","n_http_events","wf_untangle_flagged");
        # sql_helper.create_index("reports","n_http_events","wf_untangle_category");

        # virus blockers(s)
        # sql_helper.create_index("reports","n_http_events","virus_commtouch_clean");
        # sql_helper.create_index("reports","n_http_events","virus_clam_clean");
        # sql_helper.create_index("reports","n_http_events","virus_kaspersky_clean");

        # ad blocker
        # sql_helper.create_index("reports","n_http_events","ab_action");

        # spyware blocker
        # sql_helper.create_index("reports","n_http_events","sw_blacklisted");
        # sql_helper.create_index("reports","n_http_events","sw_cookie_ident");


        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_http_events
      (time_stamp, 
       session_id, client_intf, server_intf, 
       c_client_addr, s_client_addr, c_server_addr, s_server_addr, 
       c_client_port, s_client_port, c_server_port, s_server_port, 
       policy_id, uid,
       request_id, method, uri, 
       host, c2s_content_length, 
       s2c_content_length, s2c_content_type, 
       hname)
    SELECT
        -- timestamp from request
        req.time_stamp,
        -- pipeline endpoints
        pe.session_id, pe.client_intf, pe.server_intf,
        pe.c_client_addr, pe.s_client_addr, pe.c_server_addr, pe.s_server_addr,
        pe.c_client_port, pe.s_client_port, pe.c_server_port, pe.s_server_port,
        pe.policy_id, pe.username,
        -- n_http_req_line
        req.request_id, req.method, req.uri,
        -- n_http_evt_req
        er.host, er.content_length,
        -- n_http_evt_resp
        resp.content_length, resp.content_type,
        -- from webpages
        COALESCE(NULLIF(mam.name, ''), host(c_client_addr)) AS hname
    FROM events.pl_endp pe
    JOIN events.n_http_req_line req 
        ON pe.session_id = req.session_id
    JOIN events.n_http_evt_req er 
        ON er.request_id = req.request_id
    LEFT OUTER JOIN events.n_http_evt_resp resp 
        ON req.request_id = resp.request_id
    LEFT OUTER JOIN reports.merged_address_map mam
        ON pe.c_client_addr = mam.addr AND pe.time_stamp >= mam.start_time AND pe.time_stamp < mam.end_time
    WHERE pe.time_stamp < %s::timestamp without time zone""",
                               (start_date,), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @sql_helper.print_timing
    def events_cleanup(self, cutoff):
        sql_helper.clean_table("events", "n_http_req_line ", cutoff);
        sql_helper.clean_table("events", "n_http_evt_req ", cutoff);
        sql_helper.clean_table("events", "n_http_evt_resp ", cutoff);

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table("n_http_events", cutoff)
        sql_helper.drop_fact_table("n_http_totals", cutoff)        

reports.engine.register_node(HttpCasing())
