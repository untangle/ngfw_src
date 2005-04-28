CREATE INDEX mvvm_login_ts_idx ON mvvm_login_evt (time_stamp);

DELETE FROM mvvm_login_evt WHERE time_stamp < (:cutoff)::timestamp;

DROP INDEX mvvm_login_ts_idx;
