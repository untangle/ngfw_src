-- convert script for release 3.0

-----------------------------------------------
-- Firewall events no longer references rules |
-- whether or not the session was blocked is  |
-- stored in the event itself                 |
-----------------------------------------------

-- com.metavize.tran.firewall.FirewallEvent (adding was_blocked)
ALTER TABLE events.tr_firewall_evt ADD COLUMN was_blocked BOOL;

-- Setting was_blocked to true initially (assuming that default, 
-- and then updating with what was in the rule.
UPDATE events.tr_firewall_evt SET was_blocked = true;

-- Update with what was in the latest rule that it referenced.
UPDATE tr_firewall_evt SET was_blocked = is_traffic_blocker 
        FROM tr_firewall_evt evt JOIN firewall_rule rule ON evt.rule_id = rule.rule_id 
        WHERE tr_firewall_evt.event_id=evt.event_id;

-- Remove the rule_id column
ALTER TABLE tr_firewall_evt DROP COLUMN rule_id;

-- Delete all of the dangling firewall rules( rules that were only referenced by events)
DELETE FROM firewall_rule WHERE 
       rule_id NOT IN ( SELECT rule_id FROM tr_firewall_rules );
