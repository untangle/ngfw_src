DELETE FROM tr_http_req_line WHERE request_id
    IN (SELECT request_id FROM tr_http_evt_req WHERE time_stamp < :cutoff);

DELETE FROM tr_http_req_line WHERE request_id
    IN (SELECT request_id FROM tr_http_evt_resp WHERE time_stamp < :cutoff);
