DELETE FROM tr_virus_evt WHERE time_stamp < :cutoff;

DELETE FROM tr_http_req_line WHERE request_id
    IN (SELECT request_line AS request_id FROM tr_virus_evt_http WHERE time_stamp < :cutoff);