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

class MailCasing(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-casing-mail')

    def parents(self):
        return ['untangle-vm']

    @print_timing
    def setup(self, start_date, end_date):
        self.__create_n_mail_msgs(start_date, end_date)
        self.__create_n_mail_addrs(start_date, end_date)

        ft = FactTable('reports.n_mail_msg_totals', 'reports.n_mail_msgs',
                       'time_stamp',
                       [Column('hname', 'text'), Column('uid', 'text'),
                        Column('client_intf', 'smallint'),
                        Column('server_type', 'char(1)')],
                       [Column('msgs', 'bigint', 'count(*)'),
                        Column('msg_bytes', 'bigint',
                                   'sum(msg_bytes)'),
                        Column('s2c_bytes', 'bigint', 'sum(p2c_bytes)'),
                        Column('c2s_bytes', 'bigint', 'sum(p2s_bytes)')])
        reports.engine.register_fact_table(ft)

        ft = FactTable('reports.n_mail_addr_totals', 'reports.n_mail_addrs',
                       'time_stamp',
                       [Column('hname', 'text'), Column('uid', 'text'),
                        Column('client_intf', 'smallint'),
                        Column('server_type', 'char(1)'),
                        Column('addr_pos', 'text'), Column('addr', 'text'),
                        Column('addr_kind', 'char(1)')],
                       [Column('msgs', 'bigint', 'count(*)'),
                        Column('msg_bytes', 'bigint',
                                   'sum(msg_bytes)'),
                        Column('s2c_bytes', 'bigint', 'sum(p2c_bytes)'),
                        Column('c2s_bytes', 'bigint', 'sum(p2s_bytes)')])
        reports.engine.register_fact_table(ft)

    def post_facttable_setup(self, start_date, end_date):
        self.__make_email_table(start_date, end_date)

    def events_cleanup(self, cutoff):
        pass

    def reports_cleanup(self, cutoff):
        sql_helper.drop_partitioned_table("n_mail_addrs", cutoff)
        sql_helper.drop_partitioned_table("n_mail_addr_totals", cutoff)        
        sql_helper.drop_partitioned_table("n_mail_msgs", cutoff)
        sql_helper.drop_partitioned_table("n_mail_msg_totals", cutoff)        
        sql_helper.drop_partitioned_table("email", cutoff)        

    @print_timing
    def __create_n_mail_addrs(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.n_mail_addrs (
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
    msg_id bigint,
    subject text,
    server_type char(1),
    addr_pos integer,
    addr text,
    addr_name text,
    addr_kind char(1),
    msg_bytes bigint,
    msg_attachments integer,
    hname text,
    event_id serial,
    sender text,
    virus_clam_clean boolean,
    virus_clam_name text,
    sa_score real,
    sa_is_spam boolean,
    sa_action character,
    ct_score real,
    ct_is_spam boolean,
    ct_action character,
    virus_kaspersky_clean boolean,
    virus_kaspersky_name text,
    phish_score real,
    phish_is_spam boolean,
    phish_action character,
    vendor text,
    virus_commtouch_clean boolean,
    virus_commtouch_name text)""", 'time_stamp', start_date, end_date)

        sql_helper.add_column('reports.n_mail_addrs', 'event_id', 'serial')
        sql_helper.add_column('reports.n_mail_addrs', 'virus_clam_clean', 'boolean')
        sql_helper.add_column('reports.n_mail_addrs', 'virus_clam_name', 'text')
        sql_helper.add_column('reports.n_mail_addrs', 'sa_score', 'real')
        sql_helper.add_column('reports.n_mail_addrs', 'sa_is_spam', 'boolean')
        sql_helper.add_column('reports.n_mail_addrs', 'sa_action', 'character')
        sql_helper.add_column('reports.n_mail_addrs', 'ct_score', 'real')
        sql_helper.add_column('reports.n_mail_addrs', 'ct_is_spam', 'boolean')
        sql_helper.add_column('reports.n_mail_addrs', 'ct_action', 'character')
        sql_helper.add_column('reports.n_mail_addrs', 'virus_kaspersky_clean', 'boolean')
        sql_helper.add_column('reports.n_mail_addrs', 'virus_kaspersky_name', 'text')
        sql_helper.add_column('reports.n_mail_addrs', 'phish_score', 'real')
        sql_helper.add_column('reports.n_mail_addrs', 'phish_is_spam', 'boolean')
        sql_helper.add_column('reports.n_mail_addrs', 'phish_action', 'character')
        sql_helper.add_column('reports.n_mail_addrs', 'vendor', 'text')
        sql_helper.add_column('reports.n_mail_addrs', 'virus_commtouch_clean', 'boolean')
        sql_helper.add_column('reports.n_mail_addrs', 'virus_commtouch_name', 'text')

        sql_helper.run_sql('CREATE INDEX n_mail_addrs_msg_id_idx ON reports.n_mail_addrs(msg_id)')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_mail_addrs
      (time_stamp, session_id, client_intf, server_intf, c_client_addr,
       s_client_addr, c_server_addr, s_server_addr, c_client_port,
       s_client_port, c_server_port, s_server_port, policy_id, policy_inbound,
       c2p_bytes, s2p_bytes, p2c_bytes, p2s_bytes, c2p_chunks, s2p_chunks,
       p2c_chunks, p2s_chunks, uid, msg_id, subject, server_type, addr_pos,
       addr, addr_name, addr_kind, msg_bytes, msg_attachments, hname, sender)
    SELECT
        -- timestamp from request
        mi.time_stamp,
        -- pipeline endpoints
        ps.session_id, ps.client_intf, ps.server_intf,
        ps.c_client_addr, ps.s_client_addr, ps.c_server_addr, ps.s_server_addr,
        ps.c_client_port, ps.s_client_port, ps.c_server_port, ps.s_server_port,
        ps.policy_id, ps.policy_inbound,
        -- pipeline stats
        ps.c2p_bytes, ps.s2p_bytes, ps.p2c_bytes, ps.p2s_bytes, ps.c2p_chunks,
        ps.s2p_chunks, ps.p2c_chunks, ps.p2s_chunks, ps.uid,
        -- n_message_info
        mi.id, mi.subject, mi.server_type,
        -- events.n_mail_message_info_addr
        mia.position, lower(mia.addr), mia.personal, mia.kind,
        -- events.n_mail_message_stats
        mms.msg_bytes, mms.msg_attachments,
        -- from webpages
        COALESCE(NULLIF(mam.name, ''), host(c_client_addr)) AS hname,
        ''
    FROM events.pl_stats ps
    JOIN events.n_mail_message_info mi ON mi.pl_endp_id = ps.pl_endp_id
    JOIN events.n_mail_message_info_addr mia ON mia.msg_id = mi.id
    LEFT OUTER JOIN events.n_mail_message_stats mms ON mms.msg_id = mi.id
    LEFT OUTER JOIN reports.merged_address_map mam
        ON ps.c_client_addr = mam.addr AND ps.time_stamp >= mam.start_time
           AND ps.time_stamp < mam.end_time""",
                               (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @print_timing
    def __make_email_table(self, start_date, end_date):
        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.email (
        date date NOT NULL,
        email text NOT NULL,
        PRIMARY KEY (date, email));
""", 'date', start_date, end_date)

        sd = sql_helper.get_max_timestamp_with_interval('reports.email')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.email (date, email)
    SELECT DISTINCT date_trunc('day', trunc_time)::date, addr
    FROM reports.n_mail_addr_totals
    WHERE trunc_time >= %s
    AND client_intf = 0 AND addr_kind = 'T'
    AND NOT addr ISNULL""", (sd,), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            print e
            conn.rollback()
            raise e

    @print_timing
    def __create_n_mail_msgs(self, start_date, end_date):

        sql_helper.create_partitioned_table("""\
CREATE TABLE reports.n_mail_msgs (
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
    msg_id bigint,
    subject text,
    server_type char(1),
    msg_bytes bigint,
    msg_attachments integer,
    hname text,
    event_id serial,
    sender text,
    virus_clam_clean boolean,
    virus_clam_name text,
    sa_score real,
    sa_is_spam boolean,
    sa_action character,
    ct_score real,
    ct_is_spam boolean,
    ct_action character,
    virus_kaspersky_clean boolean,
    virus_kaspersky_name text,
    phish_score real,
    phish_is_spam boolean,
    phish_action character,
    vendor text,
    virus_commtouch_clean boolean,
    virus_commtouch_name text)""", 'time_stamp', start_date, end_date)

        sql_helper.add_column('reports.n_mail_msgs', 'event_id', 'serial')
        sql_helper.add_column('reports.n_mail_msgs', 'sender', 'text')
        sql_helper.add_column('reports.n_mail_msgs', 'virus_clam_clean', 'boolean')
        sql_helper.add_column('reports.n_mail_msgs', 'virus_clam_name', 'text')
        sql_helper.add_column('reports.n_mail_msgs', 'sa_score', 'real')
        sql_helper.add_column('reports.n_mail_msgs', 'sa_is_spam', 'boolean')
        sql_helper.add_column('reports.n_mail_msgs', 'sa_action', 'character')
        sql_helper.add_column('reports.n_mail_msgs', 'ct_score', 'real')
        sql_helper.add_column('reports.n_mail_msgs', 'ct_is_spam', 'boolean')
        sql_helper.add_column('reports.n_mail_msgs', 'ct_action', 'character')
        sql_helper.add_column('reports.n_mail_msgs', 'virus_kaspersky_clean', 'boolean')
        sql_helper.add_column('reports.n_mail_msgs', 'virus_kaspersky_name', 'text')
        sql_helper.add_column('reports.n_mail_msgs', 'phish_score', 'real')
        sql_helper.add_column('reports.n_mail_msgs', 'phish_is_spam', 'boolean')
        sql_helper.add_column('reports.n_mail_msgs', 'phish_action', 'character')
        sql_helper.add_column('reports.n_mail_msgs', 'vendor', 'text')
        sql_helper.add_column('reports.n_mail_msgs', 'virus_commtouch_clean', 'boolean')
        sql_helper.add_column('reports.n_mail_msgs', 'virus_commtouch_name', 'text')

        sql_helper.run_sql('CREATE INDEX n_mail_msgs_msg_id_idx ON reports.n_mail_msgs(msg_id)')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_mail_msgs
      (time_stamp, session_id, client_intf, server_intf, c_client_addr,
       s_client_addr, c_server_addr, s_server_addr, c_client_port,
       s_client_port, c_server_port, s_server_port, policy_id, policy_inbound,
       c2p_bytes, s2p_bytes, p2c_bytes, p2s_bytes, c2p_chunks, s2p_chunks,
       p2c_chunks, p2s_chunks, uid, msg_id, subject, server_type, msg_bytes,
       msg_attachments, hname)
    SELECT
        -- timestamp from request
        mi.time_stamp,
        -- pipeline endpoints
        ps.session_id, ps.client_intf, ps.server_intf,
        ps.c_client_addr, ps.s_client_addr, ps.c_server_addr, ps.s_server_addr,
        ps.c_client_port, ps.s_client_port, ps.c_server_port, ps.s_server_port,
        ps.policy_id, ps.policy_inbound,
        -- pipeline stats
        ps.c2p_bytes, ps.s2p_bytes, ps.p2c_bytes, ps.p2s_bytes, ps.c2p_chunks,
        ps.s2p_chunks, ps.p2c_chunks, ps.p2s_chunks, ps.uid,
        -- n_message_info
        mi.id, mi.subject, mi.server_type,
        -- events.n_mail_message_stats
        mms.msg_bytes, mms.msg_attachments,
        -- from webpages
        COALESCE(NULLIF(mam.name, ''), host(c_client_addr)) AS hname
    FROM events.pl_stats ps
    JOIN events.n_mail_message_info mi ON mi.pl_endp_id = ps.pl_endp_id
    LEFT OUTER JOIN events.n_mail_message_stats mms ON mms.msg_id = mi.id
    LEFT OUTER JOIN reports.merged_address_map mam
        ON ps.c_client_addr = mam.addr AND ps.time_stamp >= mam.start_time AND ps.time_stamp < mam.end_time""",
                               (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

reports.engine.register_node(MailCasing())
