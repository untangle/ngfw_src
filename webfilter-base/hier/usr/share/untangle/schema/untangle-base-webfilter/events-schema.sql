
CREATE TABLE events.n_webfilter_evt (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    request_id int8,
    blocked bool,
    flagged bool,
    reason char(1),
    category varchar(255),
    vendor_name text,
    PRIMARY KEY (event_id));

CREATE TABLE events.n_webfilter_evt_unblock (
    event_id int8 NOT NULL,
    time_stamp timestamp,
    policy_id int8 NOT NULL,
    vendor_name text,
    client_address inet,
    uid text,
    is_permanent bool,
    request_uri text,
    PRIMARY KEY (event_id));

CREATE INDEX n_webfilter_evt_time_stamp_idx ON events.n_webfilter_evt (time_stamp);
CREATE INDEX n_webfilter_evt_request_id_idx ON events.n_webfilter_evt (request_id);

CREATE INDEX n_webfilter_evt_unblock_time_stamp_idx ON events.n_webfilter_evt_unblock (time_stamp);
