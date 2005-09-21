-- Create necessary indices
CREATE INDEX tr_firewall_statistic_evt_idx ON tr_firewall_statistic_evt (time_stamp);
CREATE INDEX tr_firewall_evt_idx ON tr_firewall_evt (time_stamp);

-- Delete all of the old events from statistics
DELETE FROM tr_firewall_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Delete all of the old events from the pass/block logs
DELETE FROM tr_firewall_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Drop the index on the timestamp
DROP INDEX tr_firewall_statistic_evt_idx;
DROP INDEX tr_firewall_evt_idx;

-- Delete all of the old firewall rules that are no longer referenced.
DELETE FROM firewall_rule WHERE
       rule_id NOT IN ( SELECT rule_id FROM tr_firewall_rules );
