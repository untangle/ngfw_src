DELETE FROM tr_http_req_line WHERE request_id
    IN (SELECT request_id FROM tr_spyware_evt_activex WHERE time_stamp < :cutoff);

DELETE FROM tr_http_req_line WHERE request_id
    IN (SELECT request_id FROM tr_spyware_evt_access WHERE time_stamp < :cutoff);

DELETE FROM tr_http_req_line WHERE request_id
    IN (SELECT request_id FROM tr_spyware_evt_cookie WHERE time_stamp < :cutoff);