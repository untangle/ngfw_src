-- convert script for release 3.0

CREATE INDEX tr_http_evt_req_rid_idx ON tr_http_evt_req (request_id);
