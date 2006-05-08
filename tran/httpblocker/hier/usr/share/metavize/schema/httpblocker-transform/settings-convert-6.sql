-- settings convert for release 4.0

ALTER TABLE settings.tr_httpblk_settings ADD COLUMN block_requests bool;
ALTER TABLE settings.tr_httpblk_settings ADD COLUMN block_responses bool;

UPDATE settings.tr_httpblk_settings SET block_requests = true;
UPDATE settings.tr_httpblk_settings SET block_responses = true;

ALTER TABLE settings.tr_httpblk_settings ALTER COLUMN block_requests SET NOT NULL;
ALTER TABLE settings.tr_httpblk_settings ALTER COLUMN block_responses SET NOT NULL;