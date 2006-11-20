-- convert script for release 4.1

ALTER TABLE settings.tr_spyware_settings
      ADD COLUMN user_whitelist_mode text;

UPDATE settings.tr_spyware_settings SET user_whitelist_mode = 'USER_ONLY';

ALTER TABLE settings.tr_spyware_settings
      ALTER COLUMN user_whitelist_mode SET NOT NULL;