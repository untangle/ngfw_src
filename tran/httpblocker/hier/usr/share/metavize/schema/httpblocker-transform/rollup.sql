DELETE FROM tr_httpblk_evt_blk WHERE time_stamp < (:cutoff)::timestamp;
