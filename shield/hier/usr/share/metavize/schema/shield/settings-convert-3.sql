-- settings convert for 3.1

DROP TABLE settings.tr_airgap_tmp;

CREATE TABLE settings.tr_airgap_tmp AS
    SELECT rule_id, name::text, category::text, description::text, live, alert,
           log, address, netmask, divider, settings_id, position
    FROM settings.tr_airgap_shield_node_rule;

DROP TABLE settings.tr_airgap_shield_node_rule;
ALTER TABLE settings.tr_airgap_tmp RENAME TO tr_airgap_shield_node_rule;
ALTER TABLE settings.tr_airgap_shield_node_rule ADD PRIMARY KEY (rule_id);
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN live SET NOT NULL;
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN alert SET NOT NULL;
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN log SET NOT NULL;
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN divider SET NOT NULL;
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN settings_id SET NOT NULL;
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN position SET NOT NULL;

------------------------
-- clean convert cruft |
------------------------

ALTER TABLE tr_airgap_settings DROP CONSTRAINT tr_airgap_settings_uk;
ALTER TABLE tr_airgap_settings ADD CONSTRAINT tr_airgap_settings_tid_key UNIQUE (tid);
