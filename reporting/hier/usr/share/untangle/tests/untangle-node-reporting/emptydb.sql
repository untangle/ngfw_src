--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- Name: events; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA events;


ALTER SCHEMA events OWNER TO postgres;

SET search_path = events, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: event_data_days; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE event_data_days (
    day_name text NOT NULL,
    day_begin date NOT NULL
);


ALTER TABLE events.event_data_days OWNER TO postgres;

--
-- Name: n_adblocker_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_adblocker_evt (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone,
    request_id bigint,
    action character(1),
    reason text
);


ALTER TABLE events.n_adblocker_evt OWNER TO postgres;

--
-- Name: n_boxbackup_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_boxbackup_evt (
    event_id bigint NOT NULL,
    success boolean,
    description text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_boxbackup_evt OWNER TO postgres;

--
-- Name: n_faild_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_faild_evt (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone,
    action character(1),
    uplink_id integer,
    os_name text,
    name text
);


ALTER TABLE events.n_faild_evt OWNER TO postgres;

--
-- Name: n_faild_test_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_faild_test_evt (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone,
    uplink_id integer,
    os_name text,
    name text,
    description text,
    fail_time_stamp timestamp without time zone
);


ALTER TABLE events.n_faild_test_evt OWNER TO postgres;

--
-- Name: n_firewall_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_firewall_evt (
    event_id bigint NOT NULL,
    pl_endp_id bigint,
    was_blocked boolean,
    rule_index integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_firewall_evt OWNER TO postgres;

--
-- Name: n_firewall_statistic_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_firewall_statistic_evt (
    event_id bigint NOT NULL,
    tcp_block_default integer,
    tcp_block_rule integer,
    tcp_pass_default integer,
    tcp_pass_rule integer,
    udp_block_default integer,
    udp_block_rule integer,
    udp_pass_default integer,
    udp_pass_rule integer,
    icmp_block_default integer,
    icmp_block_rule integer,
    icmp_pass_default integer,
    icmp_pass_rule integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_firewall_statistic_evt OWNER TO postgres;

--
-- Name: n_http_evt_req; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_http_evt_req (
    event_id bigint NOT NULL,
    request_id bigint,
    host text,
    content_length integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_http_evt_req OWNER TO postgres;

--
-- Name: n_http_evt_resp; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_http_evt_resp (
    event_id bigint NOT NULL,
    request_id bigint,
    content_type text,
    content_length integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_http_evt_resp OWNER TO postgres;

--
-- Name: n_http_req_line; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_http_req_line (
    request_id bigint NOT NULL,
    pl_endp_id bigint,
    method character(1),
    uri text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_http_req_line OWNER TO postgres;

--
-- Name: n_ips_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_ips_evt (
    event_id bigint NOT NULL,
    pl_endp_id bigint,
    classification text,
    message text,
    blocked boolean,
    rule_sid integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_ips_evt OWNER TO postgres;

--
-- Name: n_ips_statistic_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_ips_statistic_evt (
    event_id bigint NOT NULL,
    dnc integer,
    logged integer,
    blocked integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_ips_statistic_evt OWNER TO postgres;

--
-- Name: n_mail_message_info; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_mail_message_info (
    id bigint NOT NULL,
    pl_endp_id bigint,
    subject text NOT NULL,
    server_type character(1) NOT NULL,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_mail_message_info OWNER TO postgres;

--
-- Name: n_mail_message_info_addr; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_mail_message_info_addr (
    id bigint NOT NULL,
    addr text NOT NULL,
    personal text,
    kind character(1),
    msg_id bigint,
    "position" integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_mail_message_info_addr OWNER TO postgres;

--
-- Name: n_mail_message_stats; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_mail_message_stats (
    id bigint NOT NULL,
    msg_id bigint,
    msg_bytes bigint,
    msg_attachments integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_mail_message_stats OWNER TO postgres;

--
-- Name: n_openvpn_connect_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_openvpn_connect_evt (
    event_id bigint NOT NULL,
    remote_address inet,
    remote_port integer,
    client_name text,
    rx_bytes bigint,
    tx_bytes bigint,
    time_stamp timestamp without time zone,
    start_time timestamp without time zone,
    end_time timestamp without time zone
);


ALTER TABLE events.n_openvpn_connect_evt OWNER TO postgres;

--
-- Name: n_openvpn_distr_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_openvpn_distr_evt (
    event_id bigint NOT NULL,
    remote_address inet,
    client_name text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_openvpn_distr_evt OWNER TO postgres;

--
-- Name: n_openvpn_statistic_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_openvpn_statistic_evt (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone,
    rx_bytes bigint,
    tx_bytes bigint,
    start_time timestamp without time zone,
    end_time timestamp without time zone
);


ALTER TABLE events.n_openvpn_statistic_evt OWNER TO postgres;

--
-- Name: n_phish_http_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_phish_http_evt (
    event_id bigint NOT NULL,
    request_id bigint,
    action character(1),
    category character varying(255),
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_phish_http_evt OWNER TO postgres;

--
-- Name: n_portal_app_launch_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_portal_app_launch_evt (
    event_id bigint NOT NULL,
    client_addr inet,
    uid text,
    succeeded boolean,
    reason character(1),
    app text,
    destination text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_portal_app_launch_evt OWNER TO postgres;

--
-- Name: n_portal_login_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_portal_login_evt (
    event_id bigint NOT NULL,
    client_addr inet,
    uid text,
    succeeded boolean,
    reason character(1),
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_portal_login_evt OWNER TO postgres;

--
-- Name: n_portal_logout_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_portal_logout_evt (
    event_id bigint NOT NULL,
    client_addr inet,
    uid text,
    reason character(1),
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_portal_logout_evt OWNER TO postgres;

--
-- Name: n_protofilter_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_protofilter_evt (
    event_id bigint NOT NULL,
    pl_endp_id bigint,
    protocol text,
    blocked boolean,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_protofilter_evt OWNER TO postgres;

--
-- Name: n_router_dhcp_abs_lease; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_router_dhcp_abs_lease (
    event_id bigint NOT NULL,
    mac character varying(255),
    hostname character varying(255),
    ip inet,
    end_of_lease timestamp without time zone,
    event_type integer
);


ALTER TABLE events.n_router_dhcp_abs_lease OWNER TO postgres;

--
-- Name: n_router_evt_dhcp; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_router_evt_dhcp (
    event_id bigint NOT NULL,
    mac character varying(255),
    hostname character varying(255),
    ip inet,
    end_of_lease timestamp without time zone,
    event_type integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_router_evt_dhcp OWNER TO postgres;

--
-- Name: n_router_evt_dhcp_abs; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_router_evt_dhcp_abs (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_router_evt_dhcp_abs OWNER TO postgres;

--
-- Name: n_router_evt_dhcp_abs_leases; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_router_evt_dhcp_abs_leases (
    event_id bigint NOT NULL,
    lease_id bigint NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE events.n_router_evt_dhcp_abs_leases OWNER TO postgres;

--
-- Name: n_router_redirect_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_router_redirect_evt (
    event_id bigint NOT NULL,
    pl_endp_id bigint,
    rule_index integer,
    is_dmz boolean,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_router_redirect_evt OWNER TO postgres;

--
-- Name: n_router_statistic_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_router_statistic_evt (
    event_id bigint NOT NULL,
    nat_sessions integer,
    dmz_sessions integer,
    tcp_incoming integer,
    tcp_outgoing integer,
    udp_incoming integer,
    udp_outgoing integer,
    icmp_incoming integer,
    icmp_outgoing integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_router_statistic_evt OWNER TO postgres;

--
-- Name: n_shield_rejection_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_shield_rejection_evt (
    event_id bigint NOT NULL,
    client_addr inet,
    client_intf smallint,
    reputation double precision,
    mode integer,
    limited integer,
    dropped integer,
    rejected integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_shield_rejection_evt OWNER TO postgres;

--
-- Name: n_shield_statistic_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_shield_statistic_evt (
    event_id bigint NOT NULL,
    accepted integer,
    limited integer,
    dropped integer,
    rejected integer,
    relaxed integer,
    lax integer,
    tight integer,
    closed integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_shield_statistic_evt OWNER TO postgres;

--
-- Name: n_spam_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_spam_evt (
    event_id bigint NOT NULL,
    msg_id bigint,
    score real,
    is_spam boolean,
    action character(1),
    vendor_name character varying(255),
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_spam_evt OWNER TO postgres;

--
-- Name: n_spam_evt_smtp; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_spam_evt_smtp (
    event_id bigint NOT NULL,
    msg_id bigint,
    score real,
    is_spam boolean,
    action character(1),
    vendor_name character varying(255),
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_spam_evt_smtp OWNER TO postgres;

--
-- Name: n_spam_smtp_rbl_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_spam_smtp_rbl_evt (
    event_id bigint NOT NULL,
    hostname character varying(255) NOT NULL,
    ipaddr inet NOT NULL,
    skipped boolean NOT NULL,
    pl_endp_id bigint NOT NULL,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_spam_smtp_rbl_evt OWNER TO postgres;

--
-- Name: n_splitd_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_splitd_evt (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone,
    action character(1)
);


ALTER TABLE events.n_splitd_evt OWNER TO postgres;

--
-- Name: n_spyware_evt_access; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_spyware_evt_access (
    event_id bigint NOT NULL,
    pl_endp_id bigint,
    ipmaddr inet,
    ident text,
    blocked boolean,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_spyware_evt_access OWNER TO postgres;

--
-- Name: n_spyware_evt_activex; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_spyware_evt_activex (
    event_id bigint NOT NULL,
    request_id bigint,
    ident text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_spyware_evt_activex OWNER TO postgres;

--
-- Name: n_spyware_evt_blacklist; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_spyware_evt_blacklist (
    event_id bigint NOT NULL,
    request_id bigint,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_spyware_evt_blacklist OWNER TO postgres;

--
-- Name: n_spyware_evt_cookie; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_spyware_evt_cookie (
    event_id bigint NOT NULL,
    request_id bigint,
    ident text,
    to_server boolean,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_spyware_evt_cookie OWNER TO postgres;

--
-- Name: n_spyware_statistic_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_spyware_statistic_evt (
    event_id bigint NOT NULL,
    pass integer,
    cookie integer,
    activex integer,
    url integer,
    subnet_access integer,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_spyware_statistic_evt OWNER TO postgres;

--
-- Name: n_virus_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_virus_evt (
    event_id bigint NOT NULL,
    pl_endp_id bigint,
    clean boolean,
    virus_name text,
    virus_cleaned boolean,
    vendor_name text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_virus_evt OWNER TO postgres;

--
-- Name: n_virus_evt_http; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_virus_evt_http (
    event_id bigint NOT NULL,
    request_line bigint,
    clean boolean,
    virus_name text,
    virus_cleaned boolean,
    vendor_name text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_virus_evt_http OWNER TO postgres;

--
-- Name: n_virus_evt_mail; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_virus_evt_mail (
    event_id bigint NOT NULL,
    msg_id bigint,
    clean boolean,
    virus_name text,
    virus_cleaned boolean,
    action character(1),
    vendor_name text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_virus_evt_mail OWNER TO postgres;

--
-- Name: n_virus_evt_smtp; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_virus_evt_smtp (
    event_id bigint NOT NULL,
    msg_id bigint,
    clean boolean,
    virus_name text,
    virus_cleaned boolean,
    action character(1),
    notify_action character(1),
    vendor_name text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_virus_evt_smtp OWNER TO postgres;

--
-- Name: n_webfilter_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE n_webfilter_evt (
    event_id bigint NOT NULL,
    request_id bigint,
    action character(1),
    reason character(1),
    category character varying(255),
    vendor_name text,
    time_stamp timestamp without time zone
);


ALTER TABLE events.n_webfilter_evt OWNER TO postgres;

--
-- Name: pl_endp; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE pl_endp (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone,
    session_id integer,
    proto smallint,
    client_intf smallint,
    server_intf smallint,
    c_client_addr inet,
    s_client_addr inet,
    c_server_addr inet,
    s_server_addr inet,
    c_client_port integer,
    s_client_port integer,
    c_server_port integer,
    s_server_port integer,
    policy_id bigint,
    policy_inbound boolean
);


ALTER TABLE events.pl_endp OWNER TO postgres;

--
-- Name: pl_stats; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE pl_stats (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone,
    pl_endp_id bigint,
    c2p_bytes bigint,
    s2p_bytes bigint,
    p2c_bytes bigint,
    p2s_bytes bigint,
    c2p_chunks bigint,
    s2p_chunks bigint,
    p2c_chunks bigint,
    p2s_chunks bigint,
    uid text
);


ALTER TABLE events.pl_stats OWNER TO postgres;

--
-- Name: u_login_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE u_login_evt (
    event_id bigint NOT NULL,
    client_addr inet,
    login text,
    local boolean,
    succeeded boolean,
    reason character(1),
    time_stamp timestamp without time zone
);


ALTER TABLE events.u_login_evt OWNER TO postgres;

--
-- Name: u_lookup_evt; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE u_lookup_evt (
    event_id bigint NOT NULL,
    lookup_key bigint NOT NULL,
    address inet,
    username text,
    hostname text,
    lookup_time timestamp without time zone,
    time_stamp timestamp without time zone
);


ALTER TABLE events.u_lookup_evt OWNER TO postgres;

--
-- Name: u_node_state_change; Type: TABLE; Schema: events; Owner: postgres; Tablespace:
--

CREATE TABLE u_node_state_change (
    event_id bigint NOT NULL,
    time_stamp timestamp without time zone,
    tid bigint NOT NULL,
    state text NOT NULL
);


ALTER TABLE events.u_node_state_change OWNER TO postgres;

--
-- Data for Name: event_data_days; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY event_data_days (day_name, day_begin) FROM stdin;
\.


--
-- Data for Name: n_adblocker_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_adblocker_evt (event_id, time_stamp, request_id, action, reason) FROM stdin;
\.


--
-- Data for Name: n_boxbackup_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_boxbackup_evt (event_id, success, description, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_faild_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_faild_evt (event_id, time_stamp, action, uplink_id, os_name, name) FROM stdin;
\.


--
-- Data for Name: n_faild_test_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_faild_test_evt (event_id, time_stamp, uplink_id, os_name, name, description, fail_time_stamp) FROM stdin;
\.


--
-- Data for Name: n_firewall_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_firewall_evt (event_id, pl_endp_id, was_blocked, rule_index, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_firewall_statistic_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_firewall_statistic_evt (event_id, tcp_block_default, tcp_block_rule, tcp_pass_default, tcp_pass_rule, udp_block_default, udp_block_rule, udp_pass_default, udp_pass_rule, icmp_block_default, icmp_block_rule, icmp_pass_default, icmp_pass_rule, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_http_evt_req; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_http_evt_req (event_id, request_id, host, content_length, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_http_evt_resp; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_http_evt_resp (event_id, request_id, content_type, content_length, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_http_req_line; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_http_req_line (request_id, pl_endp_id, method, uri, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_ips_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_ips_evt (event_id, pl_endp_id, classification, message, blocked, rule_sid, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_ips_statistic_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_ips_statistic_evt (event_id, dnc, logged, blocked, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_mail_message_info; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_mail_message_info (id, pl_endp_id, subject, server_type, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_mail_message_info_addr; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_mail_message_info_addr (id, addr, personal, kind, msg_id, "position", time_stamp) FROM stdin;
\.


--
-- Data for Name: n_mail_message_stats; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_mail_message_stats (id, msg_id, msg_bytes, msg_attachments, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_openvpn_connect_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_openvpn_connect_evt (event_id, remote_address, remote_port, client_name, rx_bytes, tx_bytes, time_stamp, start_time, end_time) FROM stdin;
\.


--
-- Data for Name: n_openvpn_distr_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_openvpn_distr_evt (event_id, remote_address, client_name, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_openvpn_statistic_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_openvpn_statistic_evt (event_id, time_stamp, rx_bytes, tx_bytes, start_time, end_time) FROM stdin;
\.


--
-- Data for Name: n_phish_http_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_phish_http_evt (event_id, request_id, action, category, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_portal_app_launch_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_portal_app_launch_evt (event_id, client_addr, uid, succeeded, reason, app, destination, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_portal_login_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_portal_login_evt (event_id, client_addr, uid, succeeded, reason, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_portal_logout_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_portal_logout_evt (event_id, client_addr, uid, reason, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_protofilter_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_protofilter_evt (event_id, pl_endp_id, protocol, blocked, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_router_dhcp_abs_lease; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_router_dhcp_abs_lease (event_id, mac, hostname, ip, end_of_lease, event_type) FROM stdin;
\.


--
-- Data for Name: n_router_evt_dhcp; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_router_evt_dhcp (event_id, mac, hostname, ip, end_of_lease, event_type, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_router_evt_dhcp_abs; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_router_evt_dhcp_abs (event_id, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_router_evt_dhcp_abs_leases; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_router_evt_dhcp_abs_leases (event_id, lease_id, "position") FROM stdin;
\.


--
-- Data for Name: n_router_redirect_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_router_redirect_evt (event_id, pl_endp_id, rule_index, is_dmz, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_router_statistic_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_router_statistic_evt (event_id, nat_sessions, dmz_sessions, tcp_incoming, tcp_outgoing, udp_incoming, udp_outgoing, icmp_incoming, icmp_outgoing, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_shield_rejection_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_shield_rejection_evt (event_id, client_addr, client_intf, reputation, mode, limited, dropped, rejected, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_shield_statistic_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_shield_statistic_evt (event_id, accepted, limited, dropped, rejected, relaxed, lax, tight, closed, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_spam_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_spam_evt (event_id, msg_id, score, is_spam, action, vendor_name, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_spam_evt_smtp; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_spam_evt_smtp (event_id, msg_id, score, is_spam, action, vendor_name, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_spam_smtp_rbl_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_spam_smtp_rbl_evt (event_id, hostname, ipaddr, skipped, pl_endp_id, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_splitd_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_splitd_evt (event_id, time_stamp, action) FROM stdin;
\.


--
-- Data for Name: n_spyware_evt_access; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_spyware_evt_access (event_id, pl_endp_id, ipmaddr, ident, blocked, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_spyware_evt_activex; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_spyware_evt_activex (event_id, request_id, ident, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_spyware_evt_blacklist; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_spyware_evt_blacklist (event_id, request_id, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_spyware_evt_cookie; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_spyware_evt_cookie (event_id, request_id, ident, to_server, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_spyware_statistic_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_spyware_statistic_evt (event_id, pass, cookie, activex, url, subnet_access, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_virus_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_virus_evt (event_id, pl_endp_id, clean, virus_name, virus_cleaned, vendor_name, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_virus_evt_http; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_virus_evt_http (event_id, request_line, clean, virus_name, virus_cleaned, vendor_name, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_virus_evt_mail; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_virus_evt_mail (event_id, msg_id, clean, virus_name, virus_cleaned, action, vendor_name, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_virus_evt_smtp; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_virus_evt_smtp (event_id, msg_id, clean, virus_name, virus_cleaned, action, notify_action, vendor_name, time_stamp) FROM stdin;
\.


--
-- Data for Name: n_webfilter_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY n_webfilter_evt (event_id, request_id, action, reason, category, vendor_name, time_stamp) FROM stdin;
\.


--
-- Data for Name: pl_endp; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY pl_endp (event_id, time_stamp, session_id, proto, client_intf, server_intf, c_client_addr, s_client_addr, c_server_addr, s_server_addr, c_client_port, s_client_port, c_server_port, s_server_port, policy_id, policy_inbound) FROM stdin;
\.


--
-- Data for Name: pl_stats; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY pl_stats (event_id, time_stamp, pl_endp_id, c2p_bytes, s2p_bytes, p2c_bytes, p2s_bytes, c2p_chunks, s2p_chunks, p2c_chunks, p2s_chunks, uid) FROM stdin;
\.


--
-- Data for Name: u_login_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY u_login_evt (event_id, client_addr, login, local, succeeded, reason, time_stamp) FROM stdin;
37  127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:05.356916
38  127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:08.217027
39  127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:18.6181
40  127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:27.366114
41  127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:27.505682
42  127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:52.700869
43  127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:52.898432
44  127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:53.089558
220 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:57.679649
221 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:57.844229
222 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:58.030063
233 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:19:59.483575
237 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:01.014748
240 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:14.138327
243 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:29.551717
244 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:29.83658
245 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:30.200482
246 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:30.435576
300 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:39.224215
303 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:40.462445
304 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:40.903593
305 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:41.216072
311 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:53.927178
312 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:20:54.294998
329 127.0.0.1   admin   f   t   \N  2009-08-06 11:20:57.00501
330 127.0.0.1   admin   f   t   \N  2009-08-06 11:21:02.10812
331 127.0.0.1   localadmin  t   t   \N  2009-08-06 11:21:24.682057
1965    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:21:55.08438
3022    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:22:25.108255
3027    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:22:27.265671
3040    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:22:32.02631
3146    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:22:35.111409
4020    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:22:43.474198
4021    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:22:43.80583
4034    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:22:45.812167
8468    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:16.143117
9220    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:31.87103
9221    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:32.215975
9271    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:33.917254
9275    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:35.494611
9279    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:37.415753
9289    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:40.050874
9290    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:40.46502
9298    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:43.474399
9299    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:43.940506
9308    127.0.0.1   localadmin  t   t   \N  2009-08-06 11:23:46.189331
\.


--
-- Data for Name: u_lookup_evt; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY u_lookup_evt (event_id, lookup_key, address, username, hostname, lookup_time, time_stamp) FROM stdin;
\.


--
-- Data for Name: u_node_state_change; Type: TABLE DATA; Schema: events; Owner: postgres
--

COPY u_node_state_change (event_id, time_stamp, tid, state) FROM stdin;
29  2009-08-06 11:17:02.214 2   initialized
30  2009-08-06 11:17:02.598 1   initialized
31  2009-08-06 11:17:02.777 2   running
32  2009-08-06 11:17:02.854 1   running
33  2009-08-06 11:17:07.452 4   initialized
34  2009-08-06 11:17:18.77  3   initialized
35  2009-08-06 11:17:18.778 4   running
36  2009-08-06 11:17:18.945 3   running
313 2009-08-06 11:19:54.959 7   initialized
314 2009-08-06 11:19:55.805 6   initialized
315 2009-08-06 11:19:59.041 10  initialized
316 2009-08-06 11:20:00.518 11  initialized
317 2009-08-06 11:20:13.74  12  initialized
318 2009-08-06 11:20:29.095 13  initialized
319 2009-08-06 11:20:35.789 16  initialized
320 2009-08-06 11:20:38.215 15  initialized
321 2009-08-06 11:20:40.179 17  initialized
322 2009-08-06 11:20:53.616 20  initialized
5643    2009-08-06 11:22:14.081 21  initialized
5644    2009-08-06 11:22:24.317 22  initialized
5645    2009-08-06 11:22:26.728 23  initialized
5646    2009-08-06 11:22:30.463 24  initialized
5647    2009-08-06 11:22:34.614 25  initialized
5648    2009-08-06 11:22:42.137 26  initialized
5649    2009-08-06 11:22:45.333 28  initialized
9309    2009-08-06 11:23:26.262 29  initialized
9310    2009-08-06 11:23:31.602 30  initialized
9311    2009-08-06 11:23:33.663 32  initialized
9312    2009-08-06 11:23:35.165 33  initialized
9313    2009-08-06 11:23:36.963 34  initialized
9314    2009-08-06 11:23:39.723 35  initialized
9315    2009-08-06 11:23:42.113 36  initialized
9316    2009-08-06 11:23:45.675 37  initialized
\.


--
-- Name: n_adblocker_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_adblocker_evt
    ADD CONSTRAINT n_adblocker_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_boxbackup_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_boxbackup_evt
    ADD CONSTRAINT n_boxbackup_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_faild_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_faild_evt
    ADD CONSTRAINT n_faild_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_faild_test_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_faild_test_evt
    ADD CONSTRAINT n_faild_test_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_firewall_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_firewall_evt
    ADD CONSTRAINT n_firewall_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_firewall_statistic_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_firewall_statistic_evt
    ADD CONSTRAINT n_firewall_statistic_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_http_evt_req_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_http_evt_req
    ADD CONSTRAINT n_http_evt_req_pkey PRIMARY KEY (event_id);


--
-- Name: n_http_evt_resp_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_http_evt_resp
    ADD CONSTRAINT n_http_evt_resp_pkey PRIMARY KEY (event_id);


--
-- Name: n_http_req_line_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_http_req_line
    ADD CONSTRAINT n_http_req_line_pkey PRIMARY KEY (request_id);


--
-- Name: n_ips_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_ips_evt
    ADD CONSTRAINT n_ips_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_ips_statistic_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_ips_statistic_evt
    ADD CONSTRAINT n_ips_statistic_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_mail_message_info_addr_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_mail_message_info_addr
    ADD CONSTRAINT n_mail_message_info_addr_pkey PRIMARY KEY (id);


--
-- Name: n_mail_message_info_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_mail_message_info
    ADD CONSTRAINT n_mail_message_info_pkey PRIMARY KEY (id);


--
-- Name: n_mail_message_stats_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_mail_message_stats
    ADD CONSTRAINT n_mail_message_stats_pkey PRIMARY KEY (id);


--
-- Name: n_openvpn_connect_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_openvpn_connect_evt
    ADD CONSTRAINT n_openvpn_connect_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_openvpn_distr_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_openvpn_distr_evt
    ADD CONSTRAINT n_openvpn_distr_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_openvpn_statistic_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_openvpn_statistic_evt
    ADD CONSTRAINT n_openvpn_statistic_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_phish_http_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_phish_http_evt
    ADD CONSTRAINT n_phish_http_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_portal_app_launch_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_portal_app_launch_evt
    ADD CONSTRAINT n_portal_app_launch_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_portal_login_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_portal_login_evt
    ADD CONSTRAINT n_portal_login_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_portal_logout_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_portal_logout_evt
    ADD CONSTRAINT n_portal_logout_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_protofilter_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_protofilter_evt
    ADD CONSTRAINT n_protofilter_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_router_dhcp_abs_lease_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_router_dhcp_abs_lease
    ADD CONSTRAINT n_router_dhcp_abs_lease_pkey PRIMARY KEY (event_id);


--
-- Name: n_router_evt_dhcp_abs_leases_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_router_evt_dhcp_abs_leases
    ADD CONSTRAINT n_router_evt_dhcp_abs_leases_pkey PRIMARY KEY (event_id, "position");


--
-- Name: n_router_evt_dhcp_abs_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_router_evt_dhcp_abs
    ADD CONSTRAINT n_router_evt_dhcp_abs_pkey PRIMARY KEY (event_id);


--
-- Name: n_router_evt_dhcp_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_router_evt_dhcp
    ADD CONSTRAINT n_router_evt_dhcp_pkey PRIMARY KEY (event_id);


--
-- Name: n_router_redirect_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_router_redirect_evt
    ADD CONSTRAINT n_router_redirect_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_router_statistic_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_router_statistic_evt
    ADD CONSTRAINT n_router_statistic_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_shield_rejection_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_shield_rejection_evt
    ADD CONSTRAINT n_shield_rejection_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_shield_statistic_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_shield_statistic_evt
    ADD CONSTRAINT n_shield_statistic_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_spam_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_spam_evt
    ADD CONSTRAINT n_spam_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_spam_evt_smtp_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_spam_evt_smtp
    ADD CONSTRAINT n_spam_evt_smtp_pkey PRIMARY KEY (event_id);


--
-- Name: n_spam_smtp_rbl_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_spam_smtp_rbl_evt
    ADD CONSTRAINT n_spam_smtp_rbl_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_splitd_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_splitd_evt
    ADD CONSTRAINT n_splitd_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_spyware_evt_access_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_spyware_evt_access
    ADD CONSTRAINT n_spyware_evt_access_pkey PRIMARY KEY (event_id);


--
-- Name: n_spyware_evt_activex_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_spyware_evt_activex
    ADD CONSTRAINT n_spyware_evt_activex_pkey PRIMARY KEY (event_id);


--
-- Name: n_spyware_evt_blacklist_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_spyware_evt_blacklist
    ADD CONSTRAINT n_spyware_evt_blacklist_pkey PRIMARY KEY (event_id);


--
-- Name: n_spyware_evt_cookie_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_spyware_evt_cookie
    ADD CONSTRAINT n_spyware_evt_cookie_pkey PRIMARY KEY (event_id);


--
-- Name: n_spyware_statistic_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_spyware_statistic_evt
    ADD CONSTRAINT n_spyware_statistic_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_virus_evt_http_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_virus_evt_http
    ADD CONSTRAINT n_virus_evt_http_pkey PRIMARY KEY (event_id);


--
-- Name: n_virus_evt_mail_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_virus_evt_mail
    ADD CONSTRAINT n_virus_evt_mail_pkey PRIMARY KEY (event_id);


--
-- Name: n_virus_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_virus_evt
    ADD CONSTRAINT n_virus_evt_pkey PRIMARY KEY (event_id);


--
-- Name: n_virus_evt_smtp_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_virus_evt_smtp
    ADD CONSTRAINT n_virus_evt_smtp_pkey PRIMARY KEY (event_id);


--
-- Name: n_webfilter_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY n_webfilter_evt
    ADD CONSTRAINT n_webfilter_evt_pkey PRIMARY KEY (event_id);


--
-- Name: pl_endp_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY pl_endp
    ADD CONSTRAINT pl_endp_pkey PRIMARY KEY (event_id);


--
-- Name: pl_stats_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY pl_stats
    ADD CONSTRAINT pl_stats_pkey PRIMARY KEY (event_id);


--
-- Name: u_login_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY u_login_evt
    ADD CONSTRAINT u_login_evt_pkey PRIMARY KEY (event_id);


--
-- Name: u_lookup_evt_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY u_lookup_evt
    ADD CONSTRAINT u_lookup_evt_pkey PRIMARY KEY (event_id);


--
-- Name: u_node_state_change_pkey; Type: CONSTRAINT; Schema: events; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY u_node_state_change
    ADD CONSTRAINT u_node_state_change_pkey PRIMARY KEY (event_id);


--
-- Name: n_adblocker_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_adblocker_evt_ts_idx ON n_adblocker_evt USING btree (time_stamp);


--
-- Name: n_boxbackup_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_boxbackup_evt_ts_idx ON n_boxbackup_evt USING btree (time_stamp);


--
-- Name: n_faild_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_faild_evt_ts_idx ON n_faild_evt USING btree (time_stamp);


--
-- Name: n_faild_test_evt_fail_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_faild_test_evt_fail_ts_idx ON n_faild_test_evt USING btree (fail_time_stamp);


--
-- Name: n_faild_test_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_faild_test_evt_ts_idx ON n_faild_test_evt USING btree (time_stamp);


--
-- Name: n_firewall_evt_plepid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_firewall_evt_plepid_idx ON n_firewall_evt USING btree (pl_endp_id);


--
-- Name: n_firewall_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_firewall_evt_ts_idx ON n_firewall_evt USING btree (time_stamp);


--
-- Name: n_http_evt_req_rid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_http_evt_req_rid_idx ON n_http_evt_req USING btree (request_id);


--
-- Name: n_http_evt_req_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_http_evt_req_ts_idx ON n_http_evt_req USING btree (time_stamp);


--
-- Name: n_http_evt_resp_rid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_http_evt_resp_rid_idx ON n_http_evt_resp USING btree (request_id);


--
-- Name: n_ips_evt_plepid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_ips_evt_plepid_idx ON n_ips_evt USING btree (pl_endp_id);


--
-- Name: n_ips_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_ips_evt_ts_idx ON n_ips_evt USING btree (time_stamp);


--
-- Name: n_mail_message_info_addr_parent_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_mail_message_info_addr_parent_idx ON n_mail_message_info_addr USING btree (msg_id);


--
-- Name: n_mail_message_info_plepid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_mail_message_info_plepid_idx ON n_mail_message_info USING btree (pl_endp_id);


--
-- Name: n_phish_http_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_phish_http_evt_ts_idx ON n_phish_http_evt USING btree (time_stamp);


--
-- Name: n_portal_login_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_portal_login_evt_ts_idx ON n_portal_login_evt USING btree (time_stamp);


--
-- Name: n_portal_logout_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_portal_logout_evt_ts_idx ON n_portal_logout_evt USING btree (time_stamp);


--
-- Name: n_protofilter_evt_plepid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_protofilter_evt_plepid_idx ON n_protofilter_evt USING btree (pl_endp_id);


--
-- Name: n_protofilter_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_protofilter_evt_ts_idx ON n_protofilter_evt USING btree (time_stamp);


--
-- Name: n_router_redirect_evt_plepid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_router_redirect_evt_plepid_idx ON n_router_redirect_evt USING btree (pl_endp_id);


--
-- Name: n_router_redirect_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_router_redirect_evt_ts_idx ON n_router_redirect_evt USING btree (time_stamp);


--
-- Name: n_shield_rejection_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_shield_rejection_evt_ts_idx ON n_shield_rejection_evt USING btree (time_stamp);


--
-- Name: n_spam_evt_mid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spam_evt_mid_idx ON n_spam_evt USING btree (msg_id);


--
-- Name: n_spam_evt_smtp_mid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spam_evt_smtp_mid_idx ON n_spam_evt_smtp USING btree (msg_id);


--
-- Name: n_spam_evt_smtp_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spam_evt_smtp_ts_idx ON n_spam_evt_smtp USING btree (time_stamp);


--
-- Name: n_spam_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spam_evt_ts_idx ON n_spam_evt USING btree (time_stamp);


--
-- Name: n_spam_smtp_rbl_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spam_smtp_rbl_evt_ts_idx ON n_spam_smtp_rbl_evt USING btree (time_stamp);


--
-- Name: n_splitd_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_splitd_evt_ts_idx ON n_splitd_evt USING btree (time_stamp);


--
-- Name: n_spyware_acc_plepid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spyware_acc_plepid_idx ON n_spyware_evt_access USING btree (pl_endp_id);


--
-- Name: n_spyware_ax_rid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spyware_ax_rid_idx ON n_spyware_evt_activex USING btree (request_id);


--
-- Name: n_spyware_bl_rid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spyware_bl_rid_idx ON n_spyware_evt_blacklist USING btree (request_id);


--
-- Name: n_spyware_cookie_rid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spyware_cookie_rid_idx ON n_spyware_evt_cookie USING btree (request_id);


--
-- Name: n_spyware_evt_access_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spyware_evt_access_ts_idx ON n_spyware_evt_access USING btree (time_stamp);


--
-- Name: n_spyware_evt_activex_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spyware_evt_activex_ts_idx ON n_spyware_evt_activex USING btree (time_stamp);


--
-- Name: n_spyware_evt_blacklist_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spyware_evt_blacklist_ts_idx ON n_spyware_evt_blacklist USING btree (time_stamp);


--
-- Name: n_spyware_evt_cookie_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_spyware_evt_cookie_ts_idx ON n_spyware_evt_cookie USING btree (time_stamp);


--
-- Name: n_virus_evt_http_rid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_virus_evt_http_rid_idx ON n_virus_evt_http USING btree (request_line);


--
-- Name: n_virus_evt_http_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_virus_evt_http_ts_idx ON n_virus_evt_http USING btree (time_stamp);


--
-- Name: n_virus_evt_mail_mid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_virus_evt_mail_mid_idx ON n_virus_evt_mail USING btree (msg_id);


--
-- Name: n_virus_evt_mail_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_virus_evt_mail_ts_idx ON n_virus_evt_mail USING btree (time_stamp);


--
-- Name: n_virus_evt_smtp_mid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_virus_evt_smtp_mid_idx ON n_virus_evt_smtp USING btree (msg_id);


--
-- Name: n_virus_evt_smtp_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_virus_evt_smtp_ts_idx ON n_virus_evt_smtp USING btree (time_stamp);


--
-- Name: n_virus_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_virus_evt_ts_idx ON n_virus_evt USING btree (time_stamp);


--
-- Name: n_webfilter_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX n_webfilter_evt_ts_idx ON n_webfilter_evt USING btree (time_stamp);


--
-- Name: pl_endp_sid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX pl_endp_sid_idx ON pl_endp USING btree (session_id);


--
-- Name: pl_endp_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX pl_endp_ts_idx ON pl_endp USING btree (time_stamp);


--
-- Name: pl_stats_plepid_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX pl_stats_plepid_idx ON pl_stats USING btree (pl_endp_id);


--
-- Name: u_login_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX u_login_evt_ts_idx ON u_login_evt USING btree (time_stamp);


--
-- Name: u_lookup_evt_ts_idx; Type: INDEX; Schema: events; Owner: postgres; Tablespace:
--

CREATE INDEX u_lookup_evt_ts_idx ON u_lookup_evt USING btree (time_stamp);


--
-- PostgreSQL database dump complete
--

