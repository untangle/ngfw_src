-- Create necessary indices
CREATE INDEX tr_ids_statistic_evt_idx ON tr_ids_statistic_evt (time_stamp);
CREATE INDEX tr_ids_evt_idx ON tr_ids_evt (time_stamp);

-- Delete all of the old events from statistics
DELETE FROM tr_ids_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Delete all of the old events from the pass/block logs
DELETE FROM tr_ids_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Drop the index on the timestamp
DROP INDEX tr_ids_statistic_evt_idx;
DROP INDEX tr_ids_evt_idx;

