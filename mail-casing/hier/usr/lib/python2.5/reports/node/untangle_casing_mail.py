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
        self.__update_n_mail_msgs(start_date, end_date)
        self.__update_n_mail_addrs(start_date, end_date)

        ft = FactTable('reports.n_mail_msg_totals', 'reports.n_mail_msgs',
                       'time_stamp',
                       [Column('hname', 'text'), Column('uid', 'text'),
                        Column('client_intf', 'smallint'),
                        Column('server_type', 'char(1)')],
                       [Column('msgs', 'bigint', 'count(*)'),
                        Column('msg_bytes', 'bigint', 'sum(msg_bytes)'),
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
                        Column('msg_bytes', 'bigint', 'sum(msg_bytes)'),
                        Column('s2c_bytes', 'bigint', 'sum(p2c_bytes)'),
                        Column('c2s_bytes', 'bigint', 'sum(p2s_bytes)')])
        reports.engine.register_fact_table(ft)

    def post_facttable_setup(self, start_date, end_date):
        self.__make_email_table(start_date, end_date)

    def events_cleanup(self, cutoff):
        sql_helper.clean_table("events", "n_mail_message_info_addr", cutoff);
        sql_helper.clean_table("events", "n_mail_message_info", cutoff);

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table("n_mail_addrs", cutoff)
        sql_helper.drop_fact_table("n_mail_addr_totals", cutoff)        
        sql_helper.drop_fact_table("n_mail_msgs", cutoff)
        sql_helper.drop_fact_table("n_mail_msg_totals", cutoff)        
        sql_helper.drop_fact_table("email", cutoff)        

    @print_timing
    def __create_n_mail_addrs(self, start_date, end_date):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.n_mail_addrs (
    time_stamp timestamp without time zone,
    session_id integer, client_intf smallint,
    server_intf smallint,
    c_client_addr inet, s_client_addr inet, c_server_addr inet,
    s_server_addr inet,
    c_client_port integer, s_client_port integer, c_server_port integer,
    s_server_port integer,
    policy_id bigint, 
    c2p_bytes bigint, s2p_bytes bigint, p2c_bytes bigint, p2s_bytes bigint,
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
    event_id bigserial,
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

        # remove obsolete columns
        sql_helper.drop_column('reports', 'n_mail_addrs', 'policy_inbound')
        sql_helper.drop_column('reports', 'n_mail_addrs', 'c2p_chunks')
        sql_helper.drop_column('reports', 'n_mail_addrs', 's2p_chunks')
        sql_helper.drop_column('reports', 'n_mail_addrs', 'p2c_chunks')
        sql_helper.drop_column('reports', 'n_mail_addrs', 'p2s_chunks')

        sql_helper.add_column('reports', 'n_mail_addrs', 'event_id', 'bigserial')
        sql_helper.add_column('reports', 'n_mail_addrs', 'sender', 'text')
        sql_helper.add_column('reports', 'n_mail_addrs', 'virus_clam_clean', 'boolean')
        sql_helper.add_column('reports', 'n_mail_addrs', 'virus_clam_name', 'text')
        sql_helper.add_column('reports', 'n_mail_addrs', 'sa_score', 'real')
        sql_helper.add_column('reports', 'n_mail_addrs', 'sa_is_spam', 'boolean')
        sql_helper.add_column('reports', 'n_mail_addrs', 'sa_action', 'character')
        sql_helper.add_column('reports', 'n_mail_addrs', 'ct_score', 'real')
        sql_helper.add_column('reports', 'n_mail_addrs', 'ct_is_spam', 'boolean')
        sql_helper.add_column('reports', 'n_mail_addrs', 'ct_action', 'character')
        sql_helper.add_column('reports', 'n_mail_addrs', 'virus_kaspersky_clean', 'boolean')
        sql_helper.add_column('reports', 'n_mail_addrs', 'virus_kaspersky_name', 'text')
        sql_helper.add_column('reports', 'n_mail_addrs', 'phish_score', 'real')
        sql_helper.add_column('reports', 'n_mail_addrs', 'phish_is_spam', 'boolean')
        sql_helper.add_column('reports', 'n_mail_addrs', 'phish_action', 'character')
        sql_helper.add_column('reports', 'n_mail_addrs', 'vendor', 'text')
        sql_helper.add_column('reports', 'n_mail_addrs', 'virus_commtouch_clean', 'boolean')
        sql_helper.add_column('reports', 'n_mail_addrs', 'virus_commtouch_name', 'text')

        # we used to create event_id as serial instead of bigserial - convert if necessary
        sql_helper.convert_column("reports","n_mail_addrs","event_id","integer","bigint");

        sql_helper.create_index("reports","n_mail_addrs","msg_id");
        sql_helper.create_index("reports","n_mail_addrs","event_id");
        sql_helper.create_index("reports","n_mail_addrs","policy_id");
        sql_helper.create_index("reports","n_mail_addrs","time_stamp");
        sql_helper.create_index("reports","n_mail_addrs","addr_kind");

        # virus blocker event log query indexes
        # sql_helper.create_index("reports","n_mail_addrs","virus_commtouch_clean");
        # sql_helper.create_index("reports","n_mail_addrs","virus_clam_clean");
        # sql_helper.create_index("reports","n_mail_addrs","virus_kaspersky_clean");

        # spam blocker event log query indexes
        # sql_helper.create_index("reports","n_mail_addrs","addr_kind");
        # sql_helper.create_index("reports","n_mail_addrs","sa_action");
        # sql_helper.create_index("reports","n_mail_addrs","ct_action");
        # sql_helper.create_index("reports","n_mail_addrs","phish_action");

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_mail_addrs
      (time_stamp, session_id, client_intf, server_intf, c_client_addr,
       s_client_addr, c_server_addr, s_server_addr, c_client_port,
       s_client_port, c_server_port, s_server_port, policy_id, 
       uid, msg_id, subject, server_type, addr_pos,
       addr, addr_name, addr_kind, msg_bytes, msg_attachments, hname, sender)
    SELECT
        -- timestamp from request
        mi.time_stamp,
        -- pipeline endpoints
        pe.session_id, pe.client_intf, pe.server_intf,
        pe.c_client_addr, pe.s_client_addr, pe.c_server_addr, pe.s_server_addr,
        pe.c_client_port, pe.s_client_port, pe.c_server_port, pe.s_server_port,
        pe.policy_id, 
        -- pipeline stats
        pe.username,
        -- n_message_info
        mi.id, mi.subject, mi.server_type,
        -- events.n_mail_message_info_addr
        mia.position, lower(mia.addr), mia.personal, mia.kind,
        -- events.n_mail_message_stats
        mms.msg_bytes, mms.msg_attachments,
        -- from webpages
        COALESCE(NULLIF(mam.name, ''), host(c_client_addr)) AS hname,
        ''
    FROM events.pl_endp pe
    JOIN events.n_mail_message_info mi ON mi.session_id = pe.session_id
    JOIN events.n_mail_message_info_addr mia ON mia.msg_id = mi.id
    LEFT OUTER JOIN events.n_mail_message_stats mms ON mms.msg_id = mi.id
    LEFT OUTER JOIN reports.merged_address_map mam
        ON pe.c_client_addr = mam.addr AND pe.time_stamp >= mam.start_time
           AND pe.time_stamp < mam.end_time""",
                               (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @print_timing
    def __update_n_mail_addrs(self, start_date, end_date):
        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
UPDATE reports.n_mail_addrs
SET c2p_bytes = ps.c2p_bytes, 
    s2p_bytes = ps.s2p_bytes,
    p2c_bytes = ps.p2c_bytes,
    p2s_bytes = ps.p2s_bytes
FROM events.pl_stats as ps
WHERE reports.n_mail_addrs.session_id = ps.session_id""",
                                         (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @print_timing
    def __make_email_table(self, start_date, end_date):
        sql_helper.create_fact_table("""\
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

        sql_helper.create_fact_table("""\
CREATE TABLE reports.n_mail_msgs (
    time_stamp timestamp without time zone,
    session_id integer, client_intf smallint,
    server_intf smallint,
    c_client_addr inet, s_client_addr inet, c_server_addr inet,
    s_server_addr inet,
    c_client_port integer, s_client_port integer, c_server_port integer,
    s_server_port integer,
    policy_id bigint, 
    c2p_bytes bigint, s2p_bytes bigint, p2c_bytes bigint, p2s_bytes bigint,
    uid text,
    msg_id bigint,
    subject text,
    server_type char(1),
    msg_bytes bigint,
    msg_attachments integer,
    hname text,
    event_id bigserial,
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

        # remove obsolete columns
        sql_helper.drop_column('reports', 'n_mail_msgs', 'policy_inbound')
        sql_helper.drop_column('reports', 'n_mail_msgs', 'c2p_chunks')
        sql_helper.drop_column('reports', 'n_mail_msgs', 's2p_chunks')
        sql_helper.drop_column('reports', 'n_mail_msgs', 'p2c_chunks')
        sql_helper.drop_column('reports', 'n_mail_msgs', 'p2s_chunks')

        sql_helper.add_column('reports', 'n_mail_msgs', 'event_id', 'bigserial')
        sql_helper.add_column('reports', 'n_mail_msgs', 'sender', 'text')
        sql_helper.add_column('reports', 'n_mail_msgs', 'virus_clam_clean', 'boolean')
        sql_helper.add_column('reports', 'n_mail_msgs', 'virus_clam_name', 'text')
        sql_helper.add_column('reports', 'n_mail_msgs', 'sa_score', 'real')
        sql_helper.add_column('reports', 'n_mail_msgs', 'sa_is_spam', 'boolean')
        sql_helper.add_column('reports', 'n_mail_msgs', 'sa_action', 'character')
        sql_helper.add_column('reports', 'n_mail_msgs', 'ct_score', 'real')
        sql_helper.add_column('reports', 'n_mail_msgs', 'ct_is_spam', 'boolean')
        sql_helper.add_column('reports', 'n_mail_msgs', 'ct_action', 'character')
        sql_helper.add_column('reports', 'n_mail_msgs', 'virus_kaspersky_clean', 'boolean')
        sql_helper.add_column('reports', 'n_mail_msgs', 'virus_kaspersky_name', 'text')
        sql_helper.add_column('reports', 'n_mail_msgs', 'phish_score', 'real')
        sql_helper.add_column('reports', 'n_mail_msgs', 'phish_is_spam', 'boolean')
        sql_helper.add_column('reports', 'n_mail_msgs', 'phish_action', 'character')
        sql_helper.add_column('reports', 'n_mail_msgs', 'vendor', 'text')
        sql_helper.add_column('reports', 'n_mail_msgs', 'virus_commtouch_clean', 'boolean')
        sql_helper.add_column('reports', 'n_mail_msgs', 'virus_commtouch_name', 'text')

        # we used to create event_id as serial instead of bigserial - convert if necessary
        sql_helper.convert_column("reports","n_mail_msgs","event_id","integer","bigint");

        sql_helper.create_index("reports","n_mail_msgs","msg_id");
        sql_helper.create_index("reports","n_mail_msgs","event_id");
        sql_helper.create_index("reports","n_mail_msgs","policy_id");
        sql_helper.create_index("reports","n_mail_msgs","time_stamp");

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_mail_msgs
      (time_stamp, session_id, client_intf, server_intf, 
       c_client_addr, s_client_addr, c_server_addr, s_server_addr, 
       c_client_port, s_client_port, c_server_port, s_server_port, 
       policy_id, 
       uid, 
       msg_id, subject, server_type, 
       hname)
    SELECT
        -- timestamp from request
        mi.time_stamp,
        -- pipeline endpoints
        pe.session_id, pe.client_intf, pe.server_intf,
        pe.c_client_addr, pe.s_client_addr, pe.c_server_addr, pe.s_server_addr,
        pe.c_client_port, pe.s_client_port, pe.c_server_port, pe.s_server_port,
        pe.policy_id, 
        -- pipeline stats
        pe.username,
        -- n_message_info
        mi.id, mi.subject, mi.server_type,
        -- from webpages
        COALESCE(NULLIF(mam.name, ''), host(c_client_addr)) AS hname
    FROM events.pl_endp pe
    JOIN events.n_mail_message_info mi ON mi.session_id = pe.session_id
    LEFT OUTER JOIN reports.merged_address_map mam
        ON pe.c_client_addr = mam.addr AND pe.time_stamp >= mam.start_time AND pe.time_stamp < mam.end_time""",
                               (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    @print_timing
    def __update_n_mail_msgs(self, start_date, end_date):
        conn = sql_helper.get_connection()
        try: 
            sql_helper.run_sql("""\
UPDATE reports.n_mail_msgs
SET c2p_bytes = ps.c2p_bytes, 
    s2p_bytes = ps.s2p_bytes,
    p2c_bytes = ps.p2c_bytes,
    p2s_bytes = ps.p2s_bytes
FROM events.pl_stats as ps
WHERE reports.n_mail_msgs.session_id = ps.session_id""",
                                         (), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

reports.engine.register_node(MailCasing())
