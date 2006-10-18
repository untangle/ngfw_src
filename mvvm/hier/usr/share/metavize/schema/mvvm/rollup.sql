DELETE FROM pl_stats WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM pl_endp WHERE time_stamp < (:cutoff)::timestamp;

DELETE FROM mvvm_login_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Rollup the shield events
DELETE FROM shield_rejection_evt WHERE time_stamp < (:cutoff)::timestamp;
DELETE FROM shield_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;
