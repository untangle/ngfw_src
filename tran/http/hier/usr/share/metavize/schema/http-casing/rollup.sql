DELETE FROM tr_http_evt_req WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM tr_http_evt_resp WHERE time_stamp < (:cutoff)::timestamp;

ANALYZE tr_http_evt_req;
ANALYZE tr_http_evt_resp;

DELETE FROM tr_http_req_line
    WHERE NOT EXISTS
        (SELECT 1 FROM tr_http_evt_req
            WHERE tr_http_req_line.request_id = request_id)
    AND NOT EXISTS
        (SELECT 1 FROM tr_http_evt_resp
            WHERE tr_http_req_line.request_id = request_id);
