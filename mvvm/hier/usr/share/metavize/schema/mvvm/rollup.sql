CREATE INDEX pl_endp_ts ON pl_endp (time_stamp);
CREATE INDEX pl_stats_ts ON pl_stats (time_stamp);
CREATE INDEX mvvm_login_evt_ts_idx ON mvvm_login_evt (time_stamp);

DELETE FROM pl_stats WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM pl_endp WHERE time_stamp < (:cutoff)::timestamp;

DELETE FROM mvvm_login_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Rollup the shield events
DELETE FROM shield_evt WHERE time_stamp < (:cutoff)::timestamp;

ANALYZE;

DROP INDEX pl_endp_ts;
DROP INDEX pl_stats_ts;
DROP INDEX mvvm_login_evt_ts_idx;
