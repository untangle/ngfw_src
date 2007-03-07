-- Delete all of the old events from statistics
DELETE FROM tr_firewall_statistic_evt WHERE time_stamp < (:cutoff)::timestamp;

-- Delete all of the old firewall rules that are no longer referenced.
DELETE FROM firewall_rule WHERE
       rule_id NOT IN ( SELECT rule_id FROM tr_firewall_rules );
