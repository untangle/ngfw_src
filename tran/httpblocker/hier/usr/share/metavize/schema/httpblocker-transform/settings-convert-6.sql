-- settings convert for release 4.0

ALTER TABLE settings.tr_httpblk_settings ADD COLUMN fascist_mode bool;

UPDATE settings.tr_httpblk_settings SET fascist_mode = true;

ALTER TABLE settings.tr_httpblk_settings ALTER COLUMN fascist_mode SET NOT NULL;
