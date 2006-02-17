-- events converter for release 3.2

-- The rule id is no longer used
ALTER TABLE events.tr_nat_redirect_evt DROP COLUMN rule_id;
