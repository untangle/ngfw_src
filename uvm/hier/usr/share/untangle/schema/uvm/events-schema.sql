-- events schema for release-5.0
-- $HeadURL$
-- Copyright (c) 2003-2007 Untangle, Inc.
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--

CREATE SCHEMA events;

-- SET search_path TO settings,events,public;

-- com.untangle.mvvm.engine.LoginEvent
CREATE TABLE events.u_login_evt (
    event_id int8 NOT NULL,
    client_addr inet,
    login text,
    local bool,
    succeeded bool,
    reason char(1),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.tran.PipelineEndpoints
CREATE TABLE events.pl_endp (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    session_id int4,
    proto int2,
    client_intf int2,
    server_intf int2,
    c_client_addr inet,
    s_client_addr inet,
    c_server_addr inet,
    s_server_addr inet,
    c_client_port int4,
    s_client_port int4,
    c_server_port int4,
    s_server_port int4,
    policy_id int8,
    policy_inbound bool,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.tran.PipelineStats
CREATE TABLE events.pl_stats (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    pl_endp_id int8,
    session_id int4,
    proto int2,
    client_intf int2,
    server_intf int2,
    c_client_addr inet,
    s_client_addr inet,
    c_server_addr inet,
    s_server_addr inet,
    c_client_port int4,
    s_client_port int4,
    c_server_port int4,
    s_server_port int4,
    policy_id int8,
    policy_inbound bool,
    c2p_bytes int8,
    s2p_bytes int8,
    p2c_bytes int8,
    p2s_bytes int8,
    c2p_chunks int8,
    s2p_chunks int8,
    p2c_chunks int8,
    p2s_chunks int8,
    uid        text,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.shield.ShieldRejectionEvent
CREATE TABLE events.n_shield_rejection_evt (
    event_id int8 NOT NULL,
    client_addr inet,
    client_intf int2,
    reputation float8,
    mode int4,
    limited int4,
    dropped int4,
    rejected int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.mvvm.shield.ShieldStatisticEvent
CREATE TABLE events.n_shield_statistic_evt (
    event_id int8 NOT NULL,
    accepted int4,
    limited  int4,
    dropped  int4,
    rejected int4,
    relaxed  int4,
    lax      int4,
    tight    int4,
    closed   int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));


-- com.untangle.mvvm.user.LookupLogEvent
CREATE TABLE events.u_lookup_evt (
    event_id    INT8 NOT NULL,
    lookup_key  INT8 NOT NULL,
    address     INET,
    username    TEXT,
    hostname    TEXT,
    lookup_time TIMESTAMP,
    time_stamp  TIMESTAMP,
    PRIMARY KEY (event_id));

-- LoginEvent
CREATE TABLE events.n_login_evt (
    event_id    INT8 NOT NULL,
    login_name  TEXT,
    domain	TEXT,
    type	CHAR, -- LOGIN|UPDATE|LOGOUT
    time_stamp  TIMESTAMP,
    client_addr inet,
    PRIMARY KEY (event_id));

-- SystemStatEvent
CREATE TABLE events.n_server_evt (
    event_id    INT8 NOT NULL,
    time_stamp  TIMESTAMP,
    mem_free 	INT8,
    mem_cache 	INT8,
    mem_buffers INT8,
    load_1 	DECIMAL(6, 2),
    load_5 	DECIMAL(6, 2),
    load_15	DECIMAL(6, 2),
    cpu_user 	DECIMAL(6, 3),
    cpu_system 	DECIMAL(6, 3),
    disk_total 	INT8,
    disk_free 	INT8,
    swap_total 	INT8,
    swap_free 	INT8,
    PRIMARY KEY (event_id));

CREATE TABLE events.event_data_days (
        day_name text NOT NULL,
        day_begin date NOT NULL);

----------------
-- constraints |
----------------

-- indices for reporting and event log viewing

CREATE INDEX pl_endp_sid_idx ON events.pl_endp (session_id);
CREATE INDEX pl_endp_ts_idx ON events.pl_endp (time_stamp);
CREATE INDEX pl_stats_plepid_idx ON events.pl_stats (pl_endp_id);

CREATE INDEX u_login_evt_ts_idx ON events.u_login_evt (time_stamp);
CREATE INDEX u_lookup_evt_ts_idx ON events.u_lookup_evt (time_stamp);
CREATE INDEX n_shield_rejection_evt_ts_idx ON n_shield_rejection_evt (time_stamp);



