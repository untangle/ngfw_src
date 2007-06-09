-- settings conversion for release-5.0

ALTER TABLE settings.tr_firewall_rule RENAME TO n_firewall_rule;
ALTER TABLE settings.tr_firewall_settings RENAME TO n_firewall_settings;
