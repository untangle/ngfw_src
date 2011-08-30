-- events schema for release-9.1

-- com.untangle.tran.httpblocker.HttpBlockerEvent
CREATE TABLE events.n_webfilter_evt (
    event_id int8 NOT NULL,
    request_id int8,
    blocked bool,
    flagged bool,
    reason char(1),
    category varchar(255),
    vendor_name text,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- indices for reporting

CREATE INDEX n_webfilter_evt_ts_idx ON events.n_webfilter_evt (time_stamp);

-- bypass events & index
CREATE TABLE events.n_webfilter_evt_unblock (
    event_id int8 NOT NULL,
    policy_id int8 NOT NULL,
    time_stamp timestamp,
    vendor_name text,
    client_address inet,
    uid text,
    is_permanent bool,
    request_uri text,
    PRIMARY KEY (event_id));

CREATE INDEX n_webfilter_evt_unblock_ts_idx ON events.n_webfilter_evt_unblock (time_stamp);
