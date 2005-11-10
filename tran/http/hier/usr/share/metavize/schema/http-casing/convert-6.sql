-- convert for release-3.1

--------------------
-- remove varchars |
--------------------

-- tr_http_evt_req

DROP TABLE events.tr_http_tmp;

CREATE TABLE events.tr_http_tmp AS
    SELECT event_id, session_id, request_id, host::text, content_length,
           time_stamp
    FROM events.tr_http_evt_req;

DROP TABLE events.tr_http_evt_req;
ALTER TABLE events.tr_http_tmp RENAME TO tr_http_evt_req;
ALTER TABLE events.tr_http_evt_req ADD PRIMARY KEY (event_id);
ALTER TABLE events.tr_http_evt_req ALTER COLUMN event_id SET NOT NULL;

-- tr_http_req_line

DROP TABLE events.tr_http_tmp;

CREATE TABLE events.tr_http_tmp AS
    SELECT request_id, method, uri::text FROM events.tr_http_req_line;

DROP TABLE events.tr_http_req_line;
ALTER TABLE events.tr_http_tmp RENAME TO tr_http_req_line;
ALTER TABLE events.tr_http_req_line ADD PRIMARY KEY (request_id);
ALTER TABLE events.tr_http_req_line ALTER COLUMN request_id SET NOT NULL;

-- tr_http_evt_resp

DROP TABLE events.tr_http_tmp;

CREATE TABLE events.tr_http_tmp AS
    SELECT event_id, request_id, content_type::text, content_length,
           time_stamp
    FROM events.tr_http_evt_resp;

DROP TABLE events.tr_http_evt_resp;
ALTER TABLE events.tr_http_tmp RENAME TO tr_http_evt_resp;
ALTER TABLE events.tr_http_evt_resp ADD PRIMARY KEY (event_id);
ALTER TABLE events.tr_http_evt_resp ALTER COLUMN event_id SET NOT NULL;

-- recreate indeces

CREATE INDEX tr_http_evt_req_ts_idx ON tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evt_req_sid_idx ON tr_http_evt_req (session_id);
CREATE INDEX tr_http_evt_req_rid_idx ON tr_http_evt_req (request_id);
CREATE INDEX tr_http_evt_resp_rid_idx ON tr_http_evt_resp (request_id);
