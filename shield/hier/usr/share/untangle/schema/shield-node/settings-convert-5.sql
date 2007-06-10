-- settings conversion for release-5.0

ALTER TABLE settings.tr_airgap_settings RENAME TO n_shield_settings;
ALTER TABLE settings.tr_airgap_shield_node_rule RENAME TO n_shield_node_rule;
