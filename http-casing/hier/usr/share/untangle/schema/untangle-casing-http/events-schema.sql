-- HttpResponseEvent
CREATE TABLE events.n_http_evt_resp (
    event_id int8 NOT NULL,
    request_id int8,
    content_type text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- HttpRequestEvent
CREATE TABLE events.n_http_evt_req (
    event_id int8 NOT NULL,
    request_id int8,
    host text,
    content_length int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- RequestLine
CREATE TABLE events.n_http_req_line (
    request_id int8 NOT NULL,
    session_id int8,
    method char(1),
    uri text,
    time_stamp timestamp,
    PRIMARY KEY (request_id));

CREATE INDEX n_http_evt_req_time_stamp_idx ON events.n_http_evt_req (time_stamp);
CREATE INDEX n_http_evt_req_request_id_idx ON events.n_http_evt_req (request_id);
CREATE INDEX n_http_evt_resp_request_id_idx ON events.n_http_evt_resp (request_id);
CREATE INDEX n_http_evt_resp_time_stamp_idx ON events.n_http_evt_resp (time_stamp);
CREATE INDEX n_http_req_line_session_id_idx ON events.n_http_req_line (session_id);
CREATE INDEX n_http_req_line_time_stamp_idx ON events.n_http_req_line (time_stamp);

