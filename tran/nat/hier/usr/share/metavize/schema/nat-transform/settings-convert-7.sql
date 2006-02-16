-- convert script for release 3.2

-- added column for the setup state
ALTER TABLE settings.tr_nat_settings ADD COLUMN setup_state INT2;

-- Indicate that the current settings are deprecated
UPDATE settings.tr_nat_settings SET setup_state=1;
