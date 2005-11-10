-- schema for release-3.0

-------------
-- settings |
-------------

-- com.metavize.tran.http.HttpSettings
CREATE TABLE settings.tr_http_settings (
    settings_id int8 NOT NULL,
    enabled bool NOT NULL,
    non_http_blocked bool NOT NULL,
    max_header_length int4 NOT NULL,
    block_long_headers bool NOT NULL,
    max_uri_length int4 NOT NULL,
    block_long_uris bool NOT NULL,
    PRIMARY KEY (settings_id));

-----------
-- events |
-----------

-- com.metavize.tran.http.HttpResponseEvent
CREATE TABLE events.tr_http_evt_resp (
    event_id int8 NOT NULL,
    request_id int8,
    content_type text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.metavize.tran.http.HttpRequestEvent
CREATE TABLE events.tr_http_evt_req (
    event_id int8 NOT NULL,
    session_id int4,
    request_id int8,
    host text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.metavize.tran.http.RequestLine
CREATE TABLE events.tr_http_req_line (
    request_id int8 NOT NULL,
    method char(1),
    uri text,
    PRIMARY KEY (request_id));

----------------
-- constraints |
----------------

-- indeces for reporting

CREATE INDEX tr_http_evt_req_ts_idx ON tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evt_req_sid_idx ON tr_http_evt_req (session_id);
CREATE INDEX tr_http_evt_req_rid_idx ON tr_http_evt_req (request_id);
CREATE INDEX tr_http_evt_resp_rid_idx ON tr_http_evt_resp (request_id);
