DELETE FROM tr_http_req_line WHERE request_id
    IN (SELECT request_id FROM tr_httpblk_evt_blk WHERE time_stamp < :cutoff);