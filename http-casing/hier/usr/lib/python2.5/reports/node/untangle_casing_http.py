# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
# Copyright (c) 2003-2009 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

import reports.engine
import reports.sql_helper as sql_helper
import sys

from psycopg import DateFromMx
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from sql_helper import print_timing

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
    hname text
)""",
                                            'time_stamp', start_date, end_date)

        sd = DateFromMx(sql_helper.get_update_info('reports.n_http_events', start_date))
        ed = DateFromMx(end_date)

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
        er.time_stamp,
        -- pipeline endpoints
        pe.session_id, pe.client_intf, pe.server_intf,
        pe.c_client_addr, pe.s_client_addr, pe.c_server_addr, pe.s_server_addr,
        pe.c_client_port, pe.s_client_port, pe.c_server_port, pe.s_server_port,
        pe.policy_id, pe.policy_inbound,
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
    FROM events.pl_endp pe
    JOIN events.pl_stats ps ON pe.event_id = ps.pl_endp_id
    JOIN events.n_http_req_line req ON pe.event_id = req.pl_endp_id
    JOIN events.n_http_evt_req er ON er.request_id = req.request_id
    LEFT OUTER JOIN events.n_http_evt_resp resp on req.request_id = resp.request_id
    LEFT OUTER JOIN reports.merged_address_map mam
        ON pe.c_client_addr = mam.addr AND pe.time_stamp >= mam.start_time AND pe.time_stamp < mam.end_time
    WHERE pe.time_stamp >= %s AND pe.time_stamp < %s""",
                               (sd, ed), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.n_http_events', ed,
                                       connection=conn, auto_commit=False)

            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def events_cleanup(self, cutoff):
        sql_helper.run_sql("""\
DELETE FROM events.n_http_evt_req WHERE time_stamp < %s""", (cutoff,))
        sql_helper.run_sql("""\
DELETE FROM events.n_http_req_line WHERE time_stamp < %s""", (cutoff,))
        sql_helper.run_sql("""\
DELETE FROM events.n_http_evt_resp WHERE time_stamp < %s""", (cutoff,))

    def reports_cleanup(self, cutoff):
        sql_helper.drop_partitioned_table("n_http_events", cutoff)
        sql_helper.drop_partitioned_table("n_http_totals", cutoff)        

reports.engine.register_node(HttpCasing())
