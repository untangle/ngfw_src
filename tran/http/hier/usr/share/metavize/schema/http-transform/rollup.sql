CREATE INDEX tr_http_evtreq_ts_idx ON tr_http_evt_req (time_stamp);
CREATE INDEX tr_http_evtreq_rid_idx ON tr_http_evt_req (request_id);
CREATE INDEX tr_http_evtresp_ts_idx ON tr_http_evt_resp (time_stamp);
CREATE INDEX tr_http_evtresp_rid_idx ON tr_http_evt_resp (request_id);

-- XXX other transform's tables
CREATE INDEX tr_httpblk_evt_rid_idx ON tr_httpblk_evt_blk (request_id);
CREATE INDEX tr_spyware_ac_rid_idx ON tr_spyware_evt_access (request_id);
CREATE INDEX tr_spyware_ax_rid_idx ON tr_spyware_evt_activex (request_id);
CREATE INDEX tr_spyware_c_rid_idx ON tr_spyware_evt_cookie (request_id);
CREATE INDEX tr_virus_http_rid_idx ON tr_virus_evt_http (request_line);

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
DROP INDEX tr_httpblk_evt_rid_idx;
DROP INDEX tr_spyware_ac_rid_idx;
DROP INDEX tr_spyware_ax_rid_idx;
DROP INDEX tr_spyware_c_rid_idx;
DROP INDEX tr_virus_http_rid_idx;
