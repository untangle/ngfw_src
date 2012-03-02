-- events schema 
-- $HeadURL$

CREATE SCHEMA events;

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

-- com.untangle.uvm.node.NodeStateChange
CREATE TABLE events.u_node_state_change (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    tid int8 NOT NULL,
    state text NOT NULL,
    PRIMARY KEY (event_id));

-- PipelineStats
CREATE TABLE events.pl_stats (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    session_id int8,
    c2p_bytes int8,
    s2p_bytes int8,
    p2c_bytes int8,
    p2s_bytes int8,
    c2p_chunks int8,
    s2p_chunks int8,
    p2c_chunks int8,
    p2s_chunks int8,
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

CREATE INDEX pl_stats_session_id_idx ON events.pl_stats (session_id);

CREATE INDEX u_login_evt_time_stamp_idx ON events.u_login_evt (time_stamp);

CREATE INDEX n_shield_rejection_evt_time_stamp_idx ON n_shield_rejection_evt (time_stamp);



