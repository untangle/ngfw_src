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
-- CREATE TABLE events.u_node_state_change (
--    event_id int8 NOT NULL,
--    time_stamp timestamp,
--    tid int8 NOT NULL,
--    state text NOT NULL,
--    PRIMARY KEY (event_id));

-- LoginEvent
CREATE TABLE events.n_login_evt (
    event_id    INT8 NOT NULL,
    login_name  TEXT,
    domain	TEXT,
    type	CHAR, -- LOGIN|UPDATE|LOGOUT
    time_stamp  TIMESTAMP,
    client_addr inet,
    PRIMARY KEY (event_id));

CREATE TABLE events.event_data_days (
        day_name text NOT NULL,
        day_begin date NOT NULL);

----------------
-- constraints |
----------------

-- CREATE INDEX pl_stats_session_id_idx ON events.pl_stats (session_id);

CREATE INDEX u_login_evt_time_stamp_idx ON events.u_login_evt (time_stamp);

CREATE INDEX n_shield_rejection_evt_time_stamp_idx ON n_shield_rejection_evt (time_stamp);



