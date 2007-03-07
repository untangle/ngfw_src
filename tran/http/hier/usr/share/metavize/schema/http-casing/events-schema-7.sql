-- events schema for release-4.2

-----------
-- tables |
-----------

-- com.untangle.tran.http.HttpResponseEvent
CREATE TABLE events.tr_http_evt_resp (
    event_id int8 NOT NULL,
    request_id int8,
    content_type text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.http.HttpRequestEvent
CREATE TABLE events.tr_http_evt_req (
    event_id int8 NOT NULL,
    request_id int8,
    host text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- com.untangle.tran.http.RequestLine
CREATE TABLE events.tr_http_req_line (
    request_id int8 NOT NULL,
    pl_endp_id int8,
    method char(1),
    uri text,
    time_stamp timestamp,
    PRIMARY KEY (request_id));

----------------
-- constraints |
----------------

-- indeces for reporting

CREATE INDEX tr_http_evt_req_ts_idx ON events.tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evt_req_rid_idx ON events.tr_http_evt_req (request_id);
CREATE INDEX tr_http_evt_resp_rid_idx ON events.tr_http_evt_resp (request_id);
