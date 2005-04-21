CREATE INDEX tr_httpblk_ts_idx ON tr_httpblk_evt_blk (time_stamp);

DELETE FROM tr_httpblk_evt_blk WHERE time_stamp < (:cutoff)::timestamp;

DROP INDEX tr_httpblk_ts_idx;
