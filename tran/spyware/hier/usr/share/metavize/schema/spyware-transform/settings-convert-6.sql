-- convert script for release 3.2

-- force spyware lists to reinitialize
UPDATE tr_spyware_settings set cookie_version = -1;
UPDATE tr_spyware_settings set activex_version = -1;
UPDATE tr_spyware_settings set subnet_version = -1;

-- indices

CREATE INDEX idx_spyware_rule_ar ON settings.tr_spyware_ar (rule_id);

CREATE INDEX idx_spyware_rule_cr ON settings.tr_spyware_cr (rule_id);

CREATE INDEX idx_spyware_rule_sr ON settings.tr_spyware_sr (rule_id);

