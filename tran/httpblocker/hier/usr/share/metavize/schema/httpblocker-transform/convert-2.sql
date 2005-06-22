-- convert script for release 1.5

-- indexes for reporting

CREATE INDEX tr_http_evt_req_tstmp ON tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evt_resp_reqid ON tr_http_evt_resp (request_id);
