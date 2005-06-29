CREATE INDEX mvvm_evt_pl_ts_idx ON mvvm_evt_pipeline (time_stamp);
CREATE INDEX mvvm_evt_pl_pid_idx ON mvvm_evt_pipeline (pipeline_info);
CREATE INDEX mvvm_login_evt_ts_idx ON mvvm_login_evt (time_stamp);

DELETE FROM mvvm_evt_pipeline WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM mvvm_login_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Rollup the shield events
DELETE FROM shield_evt WHERE time_stamp < (:cutoff)::timestamp;

ANALYZE;

DELETE FROM pipeline_info WHERE NOT EXISTS (SELECT 1 FROM mvvm_evt_pipeline WHERE pipeline_info = pipeline_info.id);

DROP INDEX mvvm_evt_pl_ts_idx;
DROP INDEX mvvm_evt_pl_pid_idx;
DROP INDEX mvvm_login_evt_ts_idx;
