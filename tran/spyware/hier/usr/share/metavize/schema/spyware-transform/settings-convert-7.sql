-- convert script for release 4.1

ALTER TABLE settings.tr_spyware_settings
      ADD COLUMN enable_user_whitelisting bool;

UPDATE settings.tr_spyware_settings SET enable_user_whitelisting = true;

ALTER TABLE settings.tr_spyware_settings
      ALTER COLUMN enable_user_whitelisting SET NOT NULL;