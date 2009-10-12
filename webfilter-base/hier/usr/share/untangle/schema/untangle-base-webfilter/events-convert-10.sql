CREATE TABLE events.n_webfilter_evt_unblock (
    event_id int8 NOT NULL,
    policy_id int8 NOT NULL,
    time_stamp timestamp,
    vendor_name text,
    client_address inet,
    is_permanent bool,
    request_uri text,
    PRIMARY KEY (event_id));

CREATE INDEX n_webfilter_evt_unblock_ts_idx ON events.n_webfilter_evt_unblock (time_stamp);
