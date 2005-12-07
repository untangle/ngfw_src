-- settings convert for release 3.1

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

------------------------
-- clean convert cruft |
------------------------

ALTER TABLE tr_firewall_settings DROP CONSTRAINT tr_firewall_settings_uk;
ALTER TABLE tr_firewall_settings ADD CONSTRAINT tr_firewall_settings_tid_key UNIQUE (tid);
