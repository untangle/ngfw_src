-- events schema for release-5.0

-----------
-- tables |
-----------

-- com.untangle.tran.http.HttpResponseEvent
CREATE TABLE events.n_http_evt_resp (
    event_id int8 NOT NULL,
    request_id int8,
    content_type text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.http.HttpRequestEvent
CREATE TABLE events.n_http_evt_req (
    event_id int8 NOT NULL,
    request_id int8,
    host text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.http.RequestLine
CREATE TABLE events.n_http_req_line (
    request_id int8 NOT NULL,
    pl_endp_id int8,
    method char(1),
    uri text,
    time_stamp timestamp,
    PRIMARY KEY (request_id));

----------------
-- constraints |
----------------

-- indices for reporting

CREATE INDEX n_http_evt_req_ts_idx ON events.n_http_evt_req (time_stamp);
CREATE INDEX n_http_evt_req_rid_idx ON events.n_http_evt_req (request_id);
CREATE INDEX n_http_evt_resp_rid_idx ON events.n_http_evt_resp (request_id);
-- No resp or line time_stamp index since no event log is filled from them.
