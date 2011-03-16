-- settings conversion for release-9.0

-- interface enumeration changes
UPDATE settings.n_cpd_capture_rule SET client_interface = '1' WHERE client_interface = '0';
UPDATE settings.n_cpd_capture_rule SET client_interface = '2' WHERE client_interface = '1';
UPDATE settings.n_cpd_capture_rule SET client_interface = '3' WHERE client_interface = '2';
UPDATE settings.n_cpd_capture_rule SET client_interface = '4' WHERE client_interface = '3';
UPDATE settings.n_cpd_capture_rule SET client_interface = '5' WHERE client_interface = '4';
UPDATE settings.n_cpd_capture_rule SET client_interface = '6' WHERE client_interface = '5';
UPDATE settings.n_cpd_capture_rule SET client_interface = '7' WHERE client_interface = '6';
UPDATE settings.n_cpd_capture_rule SET client_interface = '250' WHERE client_interface = '7';

UPDATE settings.n_cpd_capture_rule SET client_interface = 'any' WHERE client_interface = 'more_trusted';
UPDATE settings.n_cpd_capture_rule SET client_interface = 'any' WHERE client_interface = 'less_trusted';

-- obsolete setting
ALTER TABLE settings.n_cpd_settings DROP COLUMN logout_button;