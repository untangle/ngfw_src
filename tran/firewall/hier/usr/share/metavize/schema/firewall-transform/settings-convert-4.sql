-- settings convert for release 4.2

-------------
-- settings |
-------------

-- Get rid of the intermediate table
CREATE TABLE settings.tr_firewall_rule (
    rule_id,
    settings_id,
    position,
    is_traffic_blocker,
    protocol_matcher,
    src_ip_matcher,
    dst_ip_matcher,
    src_port_matcher,
    dst_port_matcher,
    inbound,
    outbound,
    name,
    category,
    description,
    live,
    alert,
    log)
AS SELECT r.rule_id, j.setting_id, j.position,
        r.is_traffic_blocker, r.protocol_matcher,
        r.src_ip_matcher, r.dst_ip_matcher,
        r.src_port_matcher, r.dst_port_matcher,
        r.inbound, r.outbound, r.name, r.category, r.description,
        r.live, r.alert, r.log
FROM settings.tr_firewall_rules j JOIN settings.firewall_rule r USING (rule_id);

----------------
-- constraints |
----------------

-- Create the primary key
ALTER TABLE settings.tr_firewall_rule
    ADD CONSTRAINT tr_firewall_rule_pkey PRIMARY KEY (rule_id);

----------------
-- cruft       |
----------------

-- Remove the old tables
DROP TABLE settings.tr_firewall_rules CASCADE;
DROP TABLE settings.firewall_rule CASCADE;

