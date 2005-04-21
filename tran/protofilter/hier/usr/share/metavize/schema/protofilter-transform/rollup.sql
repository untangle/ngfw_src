CREATE INDEX tr_protofilter_ts_idx ON tr_protofilter_evt (time_stamp);

DELETE FROM tr_protofilter_evt WHERE time_stamp < (:cutoff)::timestamp;

DROP INDEX tr_protofilter_ts_idx;
