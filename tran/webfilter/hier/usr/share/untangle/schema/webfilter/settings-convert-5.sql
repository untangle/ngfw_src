-- settings convert for release 3.1

ALTER TABLE tr_httpblk_settings DROP CONSTRAINT tr_httpblk_settings_uk;
ALTER TABLE tr_httpblk_settings ADD CONSTRAINT tr_httpblk_settings_tid_key UNIQUE (tid);
