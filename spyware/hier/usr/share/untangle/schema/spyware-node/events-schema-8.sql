-- events schema for release-5.0

-----------
-- events |
-----------

-- com.untangle.tran.spyware.SpywareAccessEvent
CREATE TABLE events.n_spyware_evt_access (
    event_id int8 NOT NULL,
    pl_endp_id int8,
    ipmaddr inet,
    ident text,
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.spyware.SpywareActiveXEvent
CREATE TABLE events.n_spyware_evt_activex (
    event_id int8 NOT NULL,
    request_id int8,
    ident text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.spyware.SpywareCookieEvent
CREATE TABLE events.n_spyware_evt_cookie (
    event_id int8 NOT NULL,
    request_id int8,
    ident text,
    to_server bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.spyware.SpywareBlacklistEvent
CREATE TABLE events.n_spyware_evt_blacklist (
    event_id int8 NOT NULL,
    request_id int8,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.spyware.SpywareStatisticEvent
CREATE TABLE events.n_spyware_statistic_evt (
    event_id int8 NOT NULL,
    pass int4,
    cookie int4,
    activeX int4,
    url int4,
    subnet_access int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- indeces for reporting

CREATE INDEX n_spyware_evt_cookie_rid_idx ON events.n_spyware_evt_cookie (request_id);
CREATE INDEX n_spyware_evt_blacklist_rid_idx ON events.n_spyware_evt_blacklist (request_id);
CREATE INDEX n_spyware_evt_activex_rid_idx ON events.n_spyware_evt_activex (request_id);
CREATE INDEX n_spyware_evt_access_plepid_idx ON events.n_spyware_evt_access (pl_endp_id);
CREATE INDEX n_spyware_evt_cookie_ts_idx ON events.n_spyware_evt_cookie (time_stamp);
CREATE INDEX n_spyware_evt_blacklist_ts_idx ON events.n_spyware_evt_blacklist (time_stamp);
CREATE INDEX n_spyware_evt_activex_ts_idx ON events.n_spyware_evt_activex (time_stamp);
CREATE INDEX n_spyware_evt_access_ts_idx ON events.n_spyware_evt_access (time_stamp);
