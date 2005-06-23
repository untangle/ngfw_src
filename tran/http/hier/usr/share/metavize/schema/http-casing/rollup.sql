CREATE INDEX tr_http_evtreq_ts_idx ON tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evtreq_rid_idx ON tr_http_evt_req (request_id);
CREATE INDEX tr_http_evtresp_ts_idx ON tr_http_evt_resp (time_stamp);
CREATE INDEX tr_http_evtresp_rid_idx ON tr_http_evt_resp (request_id);

DELETE FROM tr_http_evt_req WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_http_evt_resp WHERE time_stamp < (:cutoff)::timestamp;

ANALYZE;

DELETE FROM tr_http_req_line
    WHERE NOT EXISTS
        (SELECT 1 FROM tr_http_evt_req
            WHERE tr_http_req_line.request_id = request_id)
    OR NOT EXISTS
        (SELECT 1 FROM tr_http_evt_resp
            WHERE tr_http_req_line.request_id = request_id);

DROP INDEX tr_http_evtreq_ts_idx;
DROP INDEX tr_http_evtreq_rid_idx;
DROP INDEX tr_http_evtresp_ts_idx;
DROP INDEX tr_http_evtresp_rid_idx;
