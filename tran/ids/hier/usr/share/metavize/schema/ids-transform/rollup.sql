-- Delete all of the old events from statistics
DELETE FROM tr_ids_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Delete all of the old events from the pass/block logs
DELETE FROM tr_ids_evt WHERE time_stamp < (:cutoff)::timestamp;
