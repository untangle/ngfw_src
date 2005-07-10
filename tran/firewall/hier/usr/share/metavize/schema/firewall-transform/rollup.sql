-- Create necessary indices
CREATE INDEX tr_firewall_statistic_evt_idx ON tr_firewall_statistic_evt (time_stamp);
CREATE INDEX tr_firewall_evt_idx ON tr_firewall_evt (time_stamp);

-- Delete all of the old events from statistics
DELETE FROM tr_firewall_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Delete all of the old events from the pass/block logs
DELETE FROM tr_firewall_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Delete all of the old rules that are no longer used by events or the settings
DELETE FROM firewall_rule WHERE 
        rule_id NOT IN ( SELECT rule_id FROM tr_firewall_rules ) AND 
        rule_id NOT IN ( SELECT rule_id FROM tr_firewall_evt );

-- Drop the index on the timestamp
DROP INDEX tr_firewall_statistic_evt_idx;
DROP INDEX tr_firewall_evt_idx;

