-- settings convert for release 3.1

--------------------
-- remove varchars |
--------------------

-- settings.tr_virus_settings

ALTER TABLE settings.tr_virus_settings ADD COLUMN tmp text;
UPDATE settings.tr_virus_settings SET tmp = ftp_disable_resume_details;
ALTER TABLE settings.tr_virus_settings DROP COLUMN ftp_disable_resume_details;
ALTER TABLE settings.tr_virus_settings RENAME COLUMN tmp TO ftp_disable_resume_details;

ALTER TABLE settings.tr_virus_settings ADD COLUMN tmp text;
UPDATE settings.tr_virus_settings SET tmp = http_disable_resume_details;
ALTER TABLE settings.tr_virus_settings DROP COLUMN http_disable_resume_details;
ALTER TABLE settings.tr_virus_settings RENAME COLUMN tmp TO http_disable_resume_details;

ALTER TABLE settings.tr_virus_settings ADD COLUMN tmp text;
UPDATE settings.tr_virus_settings SET tmp = trickle_percent_details;
ALTER TABLE settings.tr_virus_settings DROP COLUMN trickle_percent_details;
ALTER TABLE settings.tr_virus_settings RENAME COLUMN tmp TO trickle_percent_details;

-- settings.tr_virus_config

ALTER TABLE settings.tr_virus_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_config SET tmp = notes;
ALTER TABLE settings.tr_virus_config DROP COLUMN notes;
ALTER TABLE settings.tr_virus_config RENAME COLUMN tmp TO notes;

ALTER TABLE settings.tr_virus_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_config SET tmp = copy_on_block_notes;
ALTER TABLE settings.tr_virus_config DROP COLUMN copy_on_block_notes;
ALTER TABLE settings.tr_virus_config RENAME COLUMN tmp TO copy_on_block_notes;

-- settings.tr_virus_smtp_config

ALTER TABLE settings.tr_virus_smtp_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_smtp_config SET tmp = notes;
ALTER TABLE settings.tr_virus_smtp_config DROP COLUMN notes;
ALTER TABLE settings.tr_virus_smtp_config RENAME COLUMN tmp TO notes;


-- settings.tr_virus_pop_config

ALTER TABLE settings.tr_virus_pop_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_pop_config SET tmp = notes;
ALTER TABLE settings.tr_virus_pop_config DROP COLUMN notes;
ALTER TABLE settings.tr_virus_pop_config RENAME COLUMN tmp TO notes;

-- settings.tr_virus_imap_config

ALTER TABLE settings.tr_virus_imap_config ADD COLUMN tmp text;
UPDATE settings.tr_virus_imap_config SET tmp = notes;
ALTER TABLE settings.tr_virus_imap_config DROP COLUMN notes;
ALTER TABLE settings.tr_virus_imap_config RENAME COLUMN tmp TO notes;

-- rename constraints

ALTER TABLE tr_virus_settings DROP CONSTRAINT tr_virus_settings_uk;
ALTER TABLE tr_virus_settings ADD CONSTRAINT tr_virus_settings_tid_key UNIQUE (tid);
