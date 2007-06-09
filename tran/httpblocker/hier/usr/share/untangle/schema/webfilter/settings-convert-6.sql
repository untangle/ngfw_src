-- settings convert for release 4.0

ALTER TABLE settings.tr_httpblk_settings ADD COLUMN fascist_mode bool;
UPDATE settings.tr_httpblk_settings SET fascist_mode = false;
ALTER TABLE settings.tr_httpblk_settings ALTER COLUMN fascist_mode SET NOT NULL;

ALTER TABLE settings.tr_httpblk_blcat ADD COLUMN log_only bool;
UPDATE settings.tr_httpblk_blcat SET log_only = false;
ALTER TABLE settings.tr_httpblk_blcat ALTER COLUMN log_only SET NOT NULL;

ALTER TABLE settings.tr_httpblk_blcat ALTER COLUMN block_domains SET NOT NULL;
ALTER TABLE settings.tr_httpblk_blcat ALTER COLUMN block_urls SET NOT NULL;
ALTER TABLE settings.tr_httpblk_blcat ALTER COLUMN block_expressions SET NOT NULL;
