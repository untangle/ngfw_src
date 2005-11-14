-- convert for release 3.1

---------------------
-- no more varchars |
---------------------

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = protocol_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN protocol_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO protocol_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = src_ip_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN src_ip_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO src_ip_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = dst_ip_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN dst_ip_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO dst_ip_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = src_port_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN src_port_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO src_port_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = dst_port_matcher;
ALTER TABLE settings.firewall_rule DROP COLUMN dst_port_matcher;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO dst_port_matcher;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = name;
ALTER TABLE settings.firewall_rule DROP COLUMN name;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO name;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = category;
ALTER TABLE settings.firewall_rule DROP COLUMN category;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO category;

ALTER TABLE settings.firewall_rule ADD COLUMN tmp text;
UPDATE settings.firewall_rule SET tmp = description;
ALTER TABLE settings.firewall_rule DROP COLUMN description;
ALTER TABLE settings.firewall_rule RENAME COLUMN tmp TO description;

----------------------
-- PipelineEndpoints |
----------------------

DROP TABLE events.tr_firewall_tmp;

CREATE TABLE events.tr_firewall_tmp AS
    SELECT evt.event_id, endp.event_id AS pl_endp_id, was_blocked,
          rule_id, rule_index, evt.time_stamp
    FROM events.tr_firewall_evt evt JOIN events.pl_endp endp USING (session_id);

DROP TABLE events.tr_firewall_evt;
ALTER TABLE events.tr_firewall_tmp RENAME TO tr_firewall_evt;
ALTER TABLE events.tr_firewall_evt ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE events.tr_firewall_evt ADD PRIMARY KEY (event_id);


