-- settings convert for 3.2

-- Remove the non-null constaints, these are preventing saving.
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN settings_id DROP NOT NULL;
ALTER TABLE settings.tr_airgap_shield_node_rule ALTER COLUMN position DROP NOT NULL;
