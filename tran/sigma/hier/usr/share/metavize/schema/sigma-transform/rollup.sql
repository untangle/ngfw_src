CREATE INDEX tr_sigma_ts_idx ON tr_protofilter_evt (time_stamp);

DELETE FROM tr_sigma_evt WHERE time_stamp < (:cutoff)::timestamp;

DROP INDEX tr_sigma_ts_idx;
